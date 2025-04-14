package me.hikaricp.yellowexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import me.hikaricp.yellowexchange.exchange.data.EmailPasswordRecovery;
import me.hikaricp.yellowexchange.exchange.data.EmailRegistration;
import me.hikaricp.yellowexchange.exchange.model.user.User;
import me.hikaricp.yellowexchange.exchange.model.user.UserEmailConfirm;
import me.hikaricp.yellowexchange.exchange.repository.user.UserEmailConfirmRepository;
import me.hikaricp.yellowexchange.panel.admin.model.AdminEmailSettings;
import me.hikaricp.yellowexchange.panel.admin.model.AdminSettings;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminEmailSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.common.repository.DomainRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailService {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Pair<EmailRegistration, Long>> emailRegistrations = new ConcurrentHashMap<>();
    private final Map<String, Pair<EmailPasswordRecovery, Long>> emailPasswordRecoveries = new ConcurrentHashMap<>();

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private UserEmailConfirmRepository userEmailConfirmRepository;

    @Autowired
    private DomainRepository domainRepository;

    @PostConstruct
    public void init() {
        startClearTask();
    }

    private void startClearTask() {
        executor.execute(() -> {
            while (true) {
                long currentTime = System.currentTimeMillis();
                List<String> keysToRemove = new ArrayList<>();
                List<String> keysToRemove2 = new ArrayList<>();
                for (Map.Entry<String, Pair<EmailRegistration, Long>> entry : this.emailRegistrations.entrySet()) {
                    long time = entry.getValue().getSecond();
                    if (time < currentTime) {
                        keysToRemove.add(entry.getKey());
                    }
                }

                for (Map.Entry<String, Pair<EmailPasswordRecovery, Long>> entry : this.emailPasswordRecoveries.entrySet()) {
                    long time = entry.getValue().getSecond();
                    if (time < currentTime) {
                        keysToRemove2.add(entry.getKey());
                    }
                }

                keysToRemove.forEach(this.emailRegistrations::remove);
                keysToRemove2.forEach(this.emailPasswordRecoveries::remove);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public EmailPasswordRecovery getEmailPasswordRecovery(String hash) {
        Pair<EmailPasswordRecovery, Long> pair = this.emailPasswordRecoveries.get(hash);
        if (pair == null) {
            return null;
        }

        return pair.getFirst();
    }

    public EmailRegistration getEmailRegistration(String hash) {
        Pair<EmailRegistration, Long> pair = this.emailRegistrations.get(hash);
        if (pair == null) {
            return null;
        }

        return pair.getFirst();
    }

    public void removeEmailPasswordRecovery(String hash) {
        this.emailPasswordRecoveries.remove(hash);
    }

    public void removeEmailRegistration(String hash) {
        this.emailRegistrations.remove(hash);
    }

    public boolean hasEmailPasswordRecovery(String email) {
        return this.emailPasswordRecoveries.values().stream().anyMatch(pair -> pair.getFirst().getEmail().equals(email));
    }

    public void createEmailPasswordRecovery(User user) {
        String hash = RandomStringUtils.random(32, true, true);

        String password = RandomStringUtils.random(16, true, true);
        EmailPasswordRecovery emailPasswordRecovery = new EmailPasswordRecovery(user.getEmail(), password);

        this.emailPasswordRecoveries.put(hash, Pair.of(emailPasswordRecovery, System.currentTimeMillis() + (60 * 60 * 1000)));

        sendEmailPasswordRecoveryAsync(user, password, hash);
    }

    private void sendEmailPasswordRecoveryAsync(User user, String password, String hash) {
        executor.execute(() -> {
            Domain domain = domainRepository.findByName(user.getDomain()).orElse(null);
            String domainName = user.getDomain();

            AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
            String title = adminEmailSettings.getPasswordRecoveryTitle();
            String html = adminEmailSettings.getPasswordRecoveryMessage();

            html = html.replace("{domain_url}", "https://" + domainName).replace("{confirm_url}", "https://" + domainName + "/email?action=password_recovery&hash=" + hash).replace("{password}", password);

            try {
                if (domain != null) {
                    title = title.replace("{domain_exchange_name}", domain.getExchangeName());
                    html = html.replace("{domain_exchange_name}", domain.getExchangeName());
                    sendEmail(domain, user.getEmail(), title, html);
                } else {
                    AdminSettings adminSettings = adminSettingsRepository.findFirst();
                    title = title.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    html = html.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    sendEmail(adminEmailSettings.getServer(), adminEmailSettings.getPort(), adminEmailSettings.getEmail(), adminEmailSettings.getPassword(), user.getEmail(), title, html);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.emailRegistrations.remove(hash);
            }
        });
    }

    public void createEmailConfirmation(Domain domain, String email, String domainName, User user) {
        String hash = RandomStringUtils.random(32, true, true);

        UserEmailConfirm userEmailConfirm = new UserEmailConfirm();
        userEmailConfirm.setHash(hash);
        userEmailConfirm.setUser(user);

        userEmailConfirmRepository.save(userEmailConfirm);

        sendEmailConfirmationAsync(domain, email, domainName, "confirmation&user_id=" + user.getId(), hash);
    }

    public void createEmailRegistration(String referrer, Domain domain, String email, String password, String domainName, String platform, String regIp, String promocodeName, String refCode) {
        String hash = RandomStringUtils.random(32, true, true);

        EmailRegistration emailRegistration = new EmailRegistration(referrer, email, password, domainName, platform, regIp, promocodeName, refCode);
        this.emailRegistrations.put(hash, Pair.of(emailRegistration, System.currentTimeMillis() + (60 * 60 * 1000)));

        sendEmailConfirmationAsync(domain, email, domainName, "registration", hash);
    }

    private void sendEmailConfirmationAsync(Domain domain, String email, String domainName, String action, String hash) {
        executor.execute(() -> {
            AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
            String title = adminEmailSettings.getRegistrationTitle();
            String html = adminEmailSettings.getRegistrationMessage();

            html = html.replace("{domain_url}", "https://" + domainName).replace("{confirm_url}", "https://" + domainName + "/email?action=" + action + "&hash=" + hash);

            try {
                if (domain != null) {
                    title = title.replace("{domain_exchange_name}", domain.getExchangeName());
                    html = html.replace("{domain_exchange_name}", domain.getExchangeName());
                    sendEmail(domain, email, title, html);
                } else {
                    AdminSettings adminSettings = adminSettingsRepository.findFirst();
                    title = title.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    html = html.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    sendEmail(adminEmailSettings.getServer(), adminEmailSettings.getPort(), adminEmailSettings.getEmail(), adminEmailSettings.getPassword(), email, title, html);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.emailRegistrations.remove(hash);
            }
        });
    }

    public boolean validateEmail(String server, int port, String email, String password) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", server);
            properties.put("mail.smtp.port", String.valueOf(port));
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.connectiontimeout", "3000");
            properties.put("mail.smtp.writetimeout", "1500");
            properties.put("mail.smtp.timeout", "1500");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (MessagingException e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
    }

    public void sendEmail(Domain domainEmail, String toEmail, String subject, String htmlContent) throws RuntimeException {
        sendEmail(domainEmail.getServer(), domainEmail.getPort(), domainEmail.getEmail(), domainEmail.getPassword(), toEmail, subject, htmlContent);
    }

    private void sendEmail(String server, int port, String email, String password, String toEmail, String subject, String htmlContent) throws RuntimeException {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", server);
            properties.put("mail.smtp.port", String.valueOf(port));
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.connectiontimeout", "5000");
            properties.put("mail.smtp.writetimeout", "5000");
            properties.put("mail.smtp.timeout", "5000");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
