package me.hikaricp.yellowexchange.panel.common.service;

import lombok.SneakyThrows;
import me.hikaricp.yellowexchange.panel.admin.model.AdminTelegramId;
import me.hikaricp.yellowexchange.panel.admin.model.AdminTelegramSettings;
import me.hikaricp.yellowexchange.panel.admin.model.TelegramMessages;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminTelegramIdRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminTelegramSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.TelegramMessagesRepository;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.panel.worker.model.WorkerTelegramSettings;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerTelegramSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TelegramService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminTelegramIdRepository adminTelegramIdRepository;

    @Autowired
    private TelegramMessagesRepository telegramMessagesRepository;

    @Autowired
    private WorkerTelegramSettingsRepository workerTelegramSettingsRepository;

    private String getApiToken() {
        AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
        if (adminTelegramSettings.getBotToken() == null) {
            LOGGER.error("Укажите настройки для телеграм бота в админе панели");
        }

        return adminTelegramSettings.getBotToken();
    }

    public TelegramMessages getTelegramMessages() {
        return this.telegramMessagesRepository.findFirst();
    }

    public AdminTelegramSettings getAdminSettings() {
        return this.adminTelegramSettingsRepository.findFirst();
    }

    public WorkerTelegramSettings getWorkerSettings(Worker worker) {
        return workerTelegramSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
    }

    public void sendMessageToChannel(String message, long channelId, boolean markdown) {
        sendMessageAsync(getApiToken(), channelId, message, markdown);
    }

    public void sendMessageToWorker(Worker worker, String message, boolean duplicateToAdmins) {
        String apiToken = getApiToken();

        if (worker != null) {
            WorkerTelegramSettings workerTelegramSettings = getWorkerSettings(worker);
            if (workerTelegramSettings.getTelegramId() > 0) {
                sendMessageAsync(apiToken, workerTelegramSettings.getTelegramId(), message, false);
            }
        }

        if (duplicateToAdmins) {
            sendMessageToAdmins(message);
        }
    }

    public void sendMessageToAdmins(String message) {
        String apiToken = getApiToken();
        if (apiToken == null) {
            return;
        }

        sendMessageToAdmins(apiToken, message);
    }

    private void sendMessageToAdmins(String apiToken, String message) {
        for (AdminTelegramId adminTelegramId : adminTelegramIdRepository.findAll()) {
            sendMessageAsync(apiToken, adminTelegramId.getTelegramId(), message, false);
        }
    }

    private void sendMessageAsync(String apiToken, long userId, String message, boolean markdown) {
        EXECUTOR.execute(() -> sendMessage(apiToken, userId, message, markdown));
    }

    @SneakyThrows
    private void sendMessage(String apiToken, long userId, String message, boolean markdown) {
        URL url = new URL("https://api.telegram.org/bot" + apiToken + "/sendMessage");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String data = "chat_id=" + userId + (markdown ? "&parse_mode=Markdown" : "") + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(data);
        writer.flush();

        conn.getResponseCode();

        writer.close();
        conn.disconnect();
    }
}
