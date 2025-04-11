package me.yukitale.yellowexchange.panel.admin.controller;

import me.yukitale.yellowexchange.config.Resources;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.*;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.*;
import me.yukitale.yellowexchange.exchange.service.*;
import me.yukitale.yellowexchange.panel.admin.model.*;
import me.yukitale.yellowexchange.panel.admin.repository.*;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.model.Promocode;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.repository.PromocodeRepository;
import me.yukitale.yellowexchange.panel.common.service.DomainService;
import me.yukitale.yellowexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.yellowexchange.panel.supporter.model.Supporter;
import me.yukitale.yellowexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.yellowexchange.panel.supporter.service.SupporterService;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.repository.*;
import me.yukitale.yellowexchange.panel.worker.service.WorkerService;
import me.yukitale.yellowexchange.security.xss.utils.XSSUtils;
import me.yukitale.yellowexchange.utils.DataUtil;
import me.yukitale.yellowexchange.utils.DataValidator;
import me.yukitale.yellowexchange.utils.FileUploadUtil;
import me.yukitale.yellowexchange.utils.JsonUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping(value = "/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPanelApiController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private AdminErrorMessagesRepository adminErrorMessagesRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;
    
    @Autowired
    private AdminTelegramIdRepository adminTelegramIdRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;
    
    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private TelegramMessagesRepository telegramMessagesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserErrorMessagesRepository userErrorMessagesRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private SupporterService supporterService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CooldownService cooldownService;

    //start counters
    @PostMapping(value = "/counters")
    public ResponseEntity<String> countersController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_COUNTERS" -> {
                return getCounters();
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getCounters() {
        long supportUnviewed = userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0);
        long depositsUnviewed = userDepositRepository.countByViewed(false);
        long withdrawalsUnviewed = userTransactionRepository.countByUnviewed(UserTransaction.Type.WITHDRAW.ordinal());
        long kycUnviewed = userKycRepository.countByViewed(false);

        Map<String, Long> map = new HashMap<>();
        map.put("support_unviewed", supportUnviewed);
        map.put("deposits_unviewed", depositsUnviewed);
        map.put("withdrawals_unviewed", withdrawalsUnviewed);
        map.put("kyc_unviewed", kycUnviewed);

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }
    //end counters

    //start workers
    @PostMapping(value = "/workers")
    public ResponseEntity<String> workersController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_WORKER" -> {
                return addWorker(data);
            }
            case "DELETE_WORKER" -> {
                return deleteWorker(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addWorker(Map<String, Object> data) {
        String email = ((String) data.get("email")).toLowerCase();
        if (!userRepository.existsByEmail(email.toLowerCase())) {
            return ResponseEntity.ok("email_not_found");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("already_exists");
        }

        workerService.createWorker(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWorker(Map<String, Object> data) {
        long workerId = Long.valueOf((int) data.get("worker_id"));
        if (!workerRepository.existsById(workerId)) {
            return ResponseEntity.ok("not_found");
        }

        Worker worker = workerRepository.findById(workerId).orElseThrow();

        workerService.deleteWorker(worker);

        return ResponseEntity.ok("success");
    }

    //end workers

    //start wallet-connect
    @PostMapping(value = "wallet-connect")
    public ResponseEntity<String> walletConnectController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_WALLET" -> {
                return editWallet(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editWallet(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        UserWalletConnect walletConnect = userWalletConnectRepository.findById(id).orElse(null);
        if (walletConnect == null) {
            return ResponseEntity.ok("not_found");
        }

        String type = String.valueOf(data.get("type"));
        if (type.equals("DELETE")) {
            userWalletConnectRepository.delete(walletConnect);
        } else if (walletConnect.getStatus() != UserWalletConnect.Status.PENDING) {
            return ResponseEntity.ok("reload_page");
        } else {
            if (type.equals("ACCEPT")) {
                walletConnect.setStatus(UserWalletConnect.Status.VERIFIED);
            } else {
                walletConnect.setStatus(UserWalletConnect.Status.NOT_VERIFIED);
            }

            userWalletConnectRepository.save(walletConnect);
        }

        return ResponseEntity.ok("success");
    }
    //end wallet-connect

    //start supporters
    @PostMapping(value = "/supporters")
    public ResponseEntity<String> supportersController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_SUPPORTER" -> {
                return addSupporter(data);
            }
            case "DELETE_SUPPORTER" -> {
                return deleteSupporter(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addSupporter(Map<String, Object> data) {
        String email = ((String) data.get("email")).toLowerCase();
        if (!userRepository.existsByEmail(email.toLowerCase())) {
            return ResponseEntity.ok("email_not_found");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("already_exists");
        }

        supporterService.createSupporter(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupporter(Map<String, Object> data) {
        long supporterId = Long.valueOf((int) data.get("supporter_id"));
        if (!supporterRepository.existsById(supporterId)) {
            return ResponseEntity.ok("not_found");
        }

        Supporter supporter = supporterRepository.findById(supporterId).orElseThrow();

        supporterService.deleteSupporter(supporter);

        return ResponseEntity.ok("success");
    }
    //end supporters

    //start payments
    @PostMapping(value = "/payments")
    public ResponseEntity<String> paymentsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_BUY_CRYPTO_SETTINGS" -> {
                return editBuyCryptoSettings(data);
            }
            case "EDIT_DEPOSIT_SETTINGS" -> {
                return editDepositSettings(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    public ResponseEntity<String> editBuyCryptoSettings(Map<String, Object> data) {
        boolean enabled = (boolean) data.get("enabled");

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setBuyCryptoEnabled(enabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> editDepositSettings(Map<String, Object> data) {
        String publicKey = (String) data.get("public_key");
        String privateKey = (String) data.get("private_key");
        if (publicKey.length() < 10 || privateKey.length() < 10) {
            return ResponseEntity.ok("error");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setWestWalletPublicKey(publicKey);
        adminSettings.setWestWalletPrivateKey(privateKey);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }
    //end payments

    //start support preset settings
    @PostMapping(value = "/settings/presets")
    public ResponseEntity<String> settingsPresetsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_PRESET_SETTINGS" -> {
                return editSupportPresetSettings(data);
            }
            case "ADD_SUPPORT_PRESET" -> {
                return addSupportPreset(data);
            }
            case "DELETE_SUPPORT_PRESET" -> {
                return deleteSupportPreset(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportPresetSettings(Map<String, Object> data) {
        boolean enabled = (boolean) data.get("enabled");

        List<Map<String, Object>> presets = (List<Map<String, Object>>) data.get("presets");
        for (Map<String, Object> preset : presets) {
            long id = Long.parseLong(String.valueOf(preset.get("id")));
            String title = String.valueOf(preset.get("title"));
            title = XSSUtils.stripXSS(title);
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.ok("invalid_title");
            }

            String message = String.valueOf(preset.get("message"));
            message = XSSUtils.stripXSS(message);
            if (StringUtils.isBlank(message) || message.length() > 2000) {
                return ResponseEntity.ok("invalid_message");
            }

            AdminSupportPreset adminSupportPreset = adminSupportPresetRepository.findById(id).orElse(null);
            if (adminSupportPreset != null) {
                adminSupportPreset.setTitle(title);
                adminSupportPreset.setMessage(message);

                adminSupportPresetRepository.save(adminSupportPreset);
            }
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        adminSettings.setSupportPresetsEnabled(enabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addSupportPreset(Map<String, Object> data) {
        String title = (String) data.get("title");
        title = XSSUtils.stripXSS(title);
        if (StringUtils.isBlank(title)) {
            return ResponseEntity.ok("invalid_title");
        }

        String message = (String) data.get("message");
        message = XSSUtils.stripXSS(message);
        if (StringUtils.isBlank(message) || message.length() > 2000) {
            return ResponseEntity.ok("invalid_message");
        }

        AdminSupportPreset adminSupportPreset = new AdminSupportPreset();
        adminSupportPreset.setTitle(title);
        adminSupportPreset.setMessage(message);

        adminSupportPresetRepository.save(adminSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        if (!adminSupportPresetRepository.existsById(id)) {
            return ResponseEntity.ok("not_found");
        }

        adminSupportPresetRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end support preset settings

    //start legals
    @PostMapping(value = "/settings/legals")
    public ResponseEntity<String> legalsController(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String html = (String) data.get("html");

        if (StringUtils.isBlank(html)) {
            return ResponseEntity.ok("invalid_html");
        }

        html = sanitizeLegals(html);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        if (type.equals("AML")) {
            adminLegalSettings.setAml(html);
        } else {
            adminLegalSettings.setTerms(html);
        }

        adminLegalSettingsRepository.save(adminLegalSettings);

        return ResponseEntity.ok("success");
    }

    public String sanitizeLegals(String legal) {
        PolicyFactory policyFactory = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .allowStyling()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowElements("a")
                .allowElements("table")
                .allowElements("tbody")
                .allowElements("thead")
                .allowElements("tr")
                .allowElements("td")
                .allowAttributes("href").onElements("a")
                .toFactory();
        return policyFactory.sanitize(legal);
    }
    //end legals

    //start user-edit
    @PostMapping(value = "/user-edit")
    public ResponseEntity<String> userEditController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_OVERVIEW" -> {
                return editOverview(data);
            }
            case "SET_BALANCE" -> {
                return setBalance(data);
            }
            case "EDIT_KYC" -> {
                return editKyc(data);
            }
            case "CREATE_TRANSACTION" -> {
                return createTransaction(data);
            }
            case "EDIT_TRANSACTION" -> {
                return editTransaction(data);
            }
            case "EDIT_TRANSACTION_AMOUNT" -> {
                return editTransactionAmount(data);
            }
            case "EDIT_WITHDRAW_VERIFY" -> {
                return editWithdrawVerify(data);
            }
            case "ADD_WITHDRAW_VERIFY_COIN" -> {
                return addWithdrawVerifyCoin(data);
            }
            case "DELETE_WITHDRAW_VERIFY_COIN" -> {
                return deleteWithdrawVerifyCoin(data);
            }
            default -> {
                return ResponseEntity.ok("panel.api.action.not.found");
            }
        }
    }

    private ResponseEntity<String> editOverview(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("panel.api.user.not.found");
        }

        String email = String.valueOf(data.get("email")).toLowerCase();
        if (!DataValidator.isEmailValided(email)) {
            return ResponseEntity.ok("invalid_email");
        }

        String password = String.valueOf(data.get("password"));
        if (password.length() < 8 || password.length() > 64) {
            return ResponseEntity.ok("panel.user.edit.password.length");
        }

        String note = String.valueOf(data.get("note"));
        if (note.length() > 128) {
            return ResponseEntity.ok("panel.user.edit.note.length");
        }

        boolean firstDepositBonusEnabled = DataUtil.getBoolean(data, "first_deposit_bonus_enabled");
        double firstDepositBonusAmount = DataUtil.getDouble(data, "first_deposit_bonus_amount");

        if (firstDepositBonusAmount < 0) {
            return ResponseEntity.ok("panel.user.edit.bonus.amount.error");
        }

        boolean tradingEnabled = DataUtil.getBoolean(data, "trading_enabled");
        boolean swapEnabled = DataUtil.getBoolean(data, "swap_enabled");
        boolean supportEnabled = DataUtil.getBoolean(data, "support_enabled");
        boolean transferEnabled = DataUtil.getBoolean(data, "transfer_enabled");
        boolean cryptoLendingEnabled = DataUtil.getBoolean(data, "crypto_lending_enabled");

        boolean emailBanned = DataUtil.getBoolean(data, "email_ban");
        boolean walletConnectEnabled = DataUtil.getBoolean(data, "wallet_connect_enabled");
        boolean vipEnabled = DataUtil.getBoolean(data, "vip_enabled");
        boolean twoFactorEnabled = DataUtil.getBoolean(data, "two_factor_enabled");
        boolean emailConfirmed = DataUtil.getBoolean(data, "email_confirmed");

        boolean fakeVerifiedLv1 = DataUtil.getBoolean(data, "fake_verified_lv1");
        boolean fakeVerifiedLv2 = DataUtil.getBoolean(data, "fake_verified_lv2");

        boolean fakeWithdrawPending = DataUtil.getBoolean(data, "fake_withdraw_pending");
        boolean fakeWithdrawConfirmed = DataUtil.getBoolean(data, "fake_withdraw_confirmed");

        String depositCommission = data.get("deposit_commission").toString();
        String withdrawCommission = data.get("withdraw_commission").toString();
        boolean depositPercent = depositCommission.endsWith("%");
        boolean withdrawPercent = withdrawCommission.endsWith("%");
        double depositAmount = depositCommission.isEmpty() ? -1 : 0;
        double withdrawAmount = withdrawCommission.isEmpty() ? -1 : 0;
        try {
            if (depositAmount == 0) {
                depositAmount = Double.parseDouble(depositCommission.replace("%", ""));
            }
            if (withdrawAmount == 0) {
                withdrawAmount = Double.parseDouble(withdrawCommission.replace("%", ""));
            }
        } catch (Exception ex) {
            return ResponseEntity.ok("commissions_error");
        }
        if (depositAmount < -1) {
            depositAmount = -1;
        }

        if (withdrawAmount < -1) {
            withdrawAmount = -1;
        }

        if (!user.getPassword().equals(password) || !user.getEmail().equals(email) || user.isTwoFactorEnabled() != twoFactorEnabled || user.isFakeKycLv1() != fakeVerifiedLv1 || user.isFakeKycLv2() != fakeVerifiedLv2
                || user.isEmailConfirmed() != emailConfirmed || user.isVip() != vipEnabled) {
            user.setPassword(password);
            user.setEmail(email);
            user.setTwoFactorEnabled(twoFactorEnabled);
            user.setFakeKycLv1(fakeVerifiedLv1);
            user.setFakeKycLv2(fakeVerifiedLv2);
            user.setEmailConfirmed(emailConfirmed);
            user.setVip(vipEnabled);

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        }

        String depCommission = depositCommission + (depositPercent ? "%" : "");
        String witCommission = withdrawCommission + (withdrawPercent ? "%" : "");

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).get();
        if (userSettings.isCryptoLendingEnabled() != cryptoLendingEnabled || !userSettings.getNote().equals(note) || !Objects.equals(userSettings.getWithdrawCommission(), witCommission) || !Objects.equals(userSettings.getDepositCommission(), depCommission) || userSettings.isTradingEnabled() != tradingEnabled
                || userSettings.isSwapEnabled() != swapEnabled || userSettings.isSupportEnabled() != supportEnabled || userSettings.isWalletConnectEnabled() != walletConnectEnabled || userSettings.isFirstDepositBonusEnabled() != firstDepositBonusEnabled
                || userSettings.getFirstDepositBonusAmount() != firstDepositBonusAmount || userSettings.isFakeWithdrawPending() != fakeWithdrawPending || userSettings.isFakeWithdrawConfirmed() != fakeWithdrawConfirmed || userSettings.isTransferEnabled() != transferEnabled) {
            userSettings.setNote(note);
            userSettings.setWithdrawCommission(withdrawCommission);
            userSettings.setDepositCommission(depositCommission);
            userSettings.setTradingEnabled(tradingEnabled);
            userSettings.setSwapEnabled(swapEnabled);
            userSettings.setSupportEnabled(supportEnabled);
            userSettings.setTransferEnabled(transferEnabled);
            userSettings.setCryptoLendingEnabled(cryptoLendingEnabled);
            userSettings.setWalletConnectEnabled(walletConnectEnabled);
            userSettings.setFirstDepositBonusEnabled(firstDepositBonusEnabled);
            userSettings.setFirstDepositBonusAmount(firstDepositBonusAmount);
            userSettings.setFakeWithdrawPending(fakeWithdrawPending);
            userSettings.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);

            userSettingsRepository.save(userSettings);
        }

        if (user.getRoleType() != UserRole.UserRoleType.ROLE_ADMIN) {
            boolean banned = emailBanRepository.existsByEmail(user.getEmail());
            if (banned != emailBanned) {
                if (emailBanned) {
                    EmailBan emailBan = new EmailBan();
                    emailBan.setEmail(user.getEmail());
                    emailBan.setUser(user);
                    emailBan.setDate(new Date());

                    emailBanRepository.save(emailBan);
                } else {
                    emailBanRepository.deleteByEmail(user.getEmail());
                }
            }
        }
        
        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKyc(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        UserKyc userKyc = userKycRepository.findByUserId(user.getId()).orElse(null);

        if (userKyc == null) {
            return ResponseEntity.ok("kyc_error");
        }

        int level = Integer.parseInt(data.get("level").toString());
        if (level != 1 && level != 2) {
            return ResponseEntity.ok("level_error");
        }

        if (level == 1 && userKyc.isAcceptedLv1() || level == 2 && userKyc.isAcceptedLv2()) {
            return ResponseEntity.ok("kyc_error");
        }

        if (userKyc.getLevel() != level) {
            return ResponseEntity.ok("kyc_error");
        }

        String type = (String) data.get("type");
        if (type.equals("ACCEPT")) {
            if (level == 1) {
                userKyc.setAcceptedLv1(true);
            } else {
                userKyc.setAcceptedLv2(true);
            }

            userKycRepository.save(userKyc);

            user.setVerificationLvl(level);
            userRepository.save(user);
        } else if (type.equals("CANCEL")) {
            if (level == 2) {
                userKyc.setAcceptedLv2(false);
                userKyc.setLevel(1);
                userKycRepository.save(userKyc);
            } else {
                userKycRepository.deleteByUserId(user.getId());
            }
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> setBalance(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        double balance = DataUtil.getDouble(data, "balance");
        if (Double.isNaN(balance) || balance < 0) {
            return ResponseEntity.ok("amount_error");
        }

        String coinSymbol = data.get("coin_symbol").toString();
        if (!coinService.hasCoin(coinSymbol)) {
            return ResponseEntity.ok("coin_not_found");
        }
        
        userService.setBalance(user, coinSymbol, balance);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> createTransaction(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        String coin = data.get("coin").toString();
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("coin_not_found");
        }

        String typeName = (String) data.get("type");
        UserTransaction.Type type = UserTransaction.Type.valueOf(typeName);

        if (!type.isIncrementBalance()) {
            if (userService.getBalance(user, coin) - amount < 0) {
                return ResponseEntity.ok("user_no_balance");
            }
        }

        String dateString = (String) data.get("date");
        Date date = null;
        if (!StringUtils.isBlank(dateString)) {
            try {
                date = new Date(dateString);
            } catch (Exception ex) {
                date = new Date();
            }
        } else {
            date = new Date();
        }

        String address = data.containsKey("address") && data.get("address") != null ? String.valueOf(data.get("address")) : null;

        UserTransaction userTransaction = new UserTransaction();

        userTransaction.setUser(user);
        userTransaction.setAmount(amount);
        userTransaction.setType(type);
        userTransaction.setAddress(address);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setDate(date);
        userTransaction.setCoinSymbol(coin);

        userTransactionRepository.save(userTransaction);

        userService.addBalance(user, coin, type.isIncrementBalance() ? amount : -amount);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTransaction(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserId(transactionId, userId).orElse(null);
        if (userTransaction == null) {
            return ResponseEntity.ok("not_found");
        }

        String type = String.valueOf(data.get("type"));
        if (type.equals("DELETE")) {
            if (userTransaction.getType() == UserTransaction.Type.DEPOSIT && userDepositRepository.existsByTransactionId(transactionId)) {
                return ResponseEntity.ok("not_editable");
            }

            userTransactionRepository.deleteById(transactionId);

            return ResponseEntity.ok("success");
        } else if(userTransaction.getStatus() == UserTransaction.Status.IN_PROCESSING) {
            if (type.equals("PAID_OUT")) {
                if (userTransaction.getType() == UserTransaction.Type.DEPOSIT) {
                    UserDeposit userDeposit = userDepositRepository.findByTransactionId(transactionId).orElse(null);
                    if (userDeposit != null) {
                        userService.addBalance(userDeposit.getUser(), userTransaction.getCoinSymbol(), userDeposit.getAmount());
                    }
                }

                userTransaction.setStatus(UserTransaction.Status.COMPLETED);
                userTransactionRepository.save(userTransaction);

                return ResponseEntity.ok("success");
            } else if (type.equals("CANCEL")) {
                if (userTransaction.getType() == UserTransaction.Type.DEPOSIT && userDepositRepository.existsByTransactionId(transactionId)) {
                    return ResponseEntity.ok("not_editable");
                }

                userService.addBalance(userTransaction.getUser(), userTransaction.getCoinSymbol(), userTransaction.getPay());

                userTransaction.setStatus(UserTransaction.Status.CANCELED);

                userTransactionRepository.save(userTransaction);

                return ResponseEntity.ok("success");
            }
        }

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> editTransactionAmount(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserId(transactionId, userId).orElse(null);
        if (userTransaction == null) {
            return ResponseEntity.ok("not_found");
        }

        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        userTransaction.setAmount(amount);
        if (userTransaction.getPay() > 0) {
            double commission = userTransaction.getPay() - userTransaction.getReceive();
            userTransaction.setPay(amount);
            userTransaction.setReceive(amount - commission);
        }

        userTransactionRepository.save(userTransaction);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWithdrawVerify(Map<String, Object> data) {
        boolean verifModal = (boolean) data.get("verif_modal");
        boolean amlModal = (boolean) data.get("aml_modal");
        double verifAmount = DataUtil.getDouble(data, "verif_amount");
        double btcVerifAmount = DataUtil.getDouble(data, "btc_verif_amount");

        if (Double.isNaN(verifAmount) || verifAmount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);

        userSettings.setVerificationModal(verifModal);
        userSettings.setAmlModal(amlModal);
        userSettings.setVerifDepositAmount(verifAmount);
        userSettings.setBtcVerifDepositAmount(btcVerifAmount < 0 ? 0 : btcVerifAmount);

        userSettingsRepository.save(userSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addWithdrawVerifyCoin(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(data.get("coin_type").toString());
        if (coinType == null) {
            return ResponseEntity.ok("coin_not_found");
        }

        if (userRequiredDepositCoinRepository.findByUserIdAndType(userId, coinType).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        UserRequiredDepositCoin userRequiredDepositCoin = new UserRequiredDepositCoin();
        userRequiredDepositCoin.setUser(user);
        userRequiredDepositCoin.setType(coinType);

        userRequiredDepositCoinRepository.save(userRequiredDepositCoin);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWithdrawVerifyCoin(Map<String, Object> data) {
        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(data.get("coin_type").toString());
        if (coinType == null) {
            return ResponseEntity.ok("not_found");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (userRequiredDepositCoinRepository.findByUserIdAndType(userId, coinType).isEmpty()) {
            return ResponseEntity.ok("not_found");
        }

        userRequiredDepositCoinRepository.deleteByUserIdAndType(userId, coinType);

        return ResponseEntity.ok("success");
    }
    
    @PostMapping(value = "/user-edit/errors")
    public ResponseEntity<String> userEditErrorsController(@RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        String tradingError = XSSUtils.sanitize(data.get("trading_error").toString());
        String swapError = XSSUtils.sanitize(data.get("swap_error").toString());
        String supportError = XSSUtils.sanitize(data.get("support_error").toString());
        String transferError = XSSUtils.sanitize(data.get("transfer_error").toString());
        String withdrawError = XSSUtils.sanitize(data.get("withdraw_error").toString());
        String withdrawVerificationError = XSSUtils.sanitize(data.get("withdraw_verification_error").toString());
        String withdrawAmlError = XSSUtils.sanitize(data.get("withdraw_aml_error").toString());
        String otherError = XSSUtils.sanitize(data.get("other_error").toString());
        String cryptoLendingError = XSSUtils.sanitize(data.get("crypto_lending_error").toString());
        String p2pError = XSSUtils.sanitize(data.get("p2p_error").toString());

        UserErrorMessages errorMessages = userErrorMessagesRepository.findByUserId(user.getId()).orElse(new UserErrorMessages());
        errorMessages.setUser(user);
        errorMessages.setTradingMessage(tradingError);
        errorMessages.setSwapMessage(swapError);
        errorMessages.setSupportMessage(supportError);
        errorMessages.setTransferMessage(transferError);
        errorMessages.setWithdrawMessage(withdrawError);
        errorMessages.setWithdrawVerificationMessage(withdrawVerificationError);
        errorMessages.setWithdrawAmlMessage(withdrawAmlError);
        errorMessages.setOtherMessage(otherError);
        errorMessages.setCryptoLendingMessage(cryptoLendingError);
        errorMessages.setP2pMessage(p2pError);

        userErrorMessagesRepository.save(errorMessages);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/user-edit/alert")
    public ResponseEntity<String> userEditAlertController(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String cooldownKey = "alert-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.ok("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (StringUtils.isBlank(type)) {
            return ResponseEntity.ok("type_error");
        }

        String message = (String) data.get("message");

        message = XSSUtils.sanitize(message);

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.ok("message_is_empty");
        }

        if (message.length() > 1000) {
            return ResponseEntity.ok("message_is_too_large");
        }

        UserAlert.Type alertType = UserAlert.Type.ALERT;
        String coin = data.containsKey("coin") ? data.get("coin").toString() : null;
        double amount = 0;
        List<User> users;
        if (type.equals("CURRENT")) {
            users = Collections.singletonList(user);
        } else if (type.equals("ALL")) {
            users = userRepository.findAll();
        } else if (type.equals("BONUS_CURRENT") || type.equals("BONUS_ALL")) {
            alertType = UserAlert.Type.BONUS;
            if (!coinService.hasCoin(coin)) {
                return ResponseEntity.ok("coin_not_found");
            }

            amount = DataUtil.getDouble(data, "amount");
            if (amount <= 0) {
                return ResponseEntity.ok("amount_error");
            }

            if (type.equals("BONUS_CURRENT")) {
                users = Collections.singletonList(user);
            } else {
                users = userRepository.findAll();
            }
        } else {
            return ResponseEntity.ok("error");
        }

        //todo: придумать че то с этим
        for (User alertUser : users) {
            UserAlert alert = new UserAlert();
            alert.setUser(alertUser);
            alert.setType(alertType);
            alert.setMessage(message);
            alert.setCoin(coin);
            alert.setAmount(amount);

            userAlertRepository.save(alert);
        }

        if (type.contains("_ALL")) {
            cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(60));
        } else {
            cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(5));
        }

        return ResponseEntity.ok("success");
    }
    //end user-edit

    //start telegram
    @PostMapping(value = "/telegram")
    public ResponseEntity<String> telegramController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_BOT_SETTINGS" -> {
                return editBotSettings(data);
            }
            case "EDIT_CHANNEL_SETTINGS" -> {
                return editChannelSettings(data);
            }
            case "ADD_TELEGRAM_ID" -> {
                return addTelegramId(data);
            }
            case "DELETE_TELEGRAM_ID" -> {
                return deleteTelegramId(data);
            }
            case "EDIT_NOTIFICATIONS" -> {
                return editTelegramNotifications(data);
            }
            case "EDIT_MESSAGES" -> {
                return editTelegramMessages(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editBotSettings(Map<String, Object> data) {
        String botUsername = data.get("username").toString();
        String botToken = data.get("token").toString();
        if (!botUsername.isEmpty() && (botUsername.length() < 6 || !botUsername.startsWith("@"))) {
            return ResponseEntity.ok("username_error");
        }

        if (!botToken.isEmpty() && (botToken.length() < 40 || !botToken.contains(":"))) {
            return ResponseEntity.ok("token_error");
        }

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();

        telegramSettings.setBotUsername(botUsername);
        telegramSettings.setBotToken(botToken);

        adminTelegramSettingsRepository.save(telegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editChannelSettings(Map<String, Object> data) {
        boolean enabled = Boolean.parseBoolean(String.valueOf(data.get("enabled")));

        long channelId = -1;
        try {
            if (enabled) {
                channelId = Long.parseLong(String.valueOf(data.get("id")));
            }
        } catch (Exception ex) {
            return ResponseEntity.ok("channel_id_error");
        }

        String message = String.valueOf(data.get("message"));
        if (StringUtils.isBlank(message)) {
            return ResponseEntity.ok("error");
        }

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();

        telegramSettings.setChannelNotification(enabled);
        telegramSettings.setChannelId(channelId);
        telegramSettings.setChannelMessage(message);

        adminTelegramSettingsRepository.save(telegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addTelegramId(Map<String, Object> data) {
        String idLine = String.valueOf(data.get("id"));
        long id = 0;
        try {
            id = Long.parseLong(idLine);
        } catch (Exception ex) {
            return ResponseEntity.ok("id_error");
        }

        if (id == 0) {
            return ResponseEntity.ok("id_error");
        }

        if (adminTelegramIdRepository.existsByTelegramId(id)) {
            return ResponseEntity.ok("already_exists");
        }

        AdminTelegramId adminTelegramId = new AdminTelegramId();
        adminTelegramId.setTelegramId(id);

        adminTelegramIdRepository.save(adminTelegramId);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteTelegramId(Map<String, Object> data) {
        long id = (long) (int) data.get("id");

        if (!adminTelegramIdRepository.existsById(id)) {
            return ResponseEntity.ok("not_found");
        }

        adminTelegramIdRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTelegramNotifications(Map<String, Object> data) {
        AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();

        boolean supportEnabled = DataUtil.getBoolean(data, "support_enabled");
        boolean depositEnabled = DataUtil.getBoolean(data, "deposit_enabled");
        boolean withdrawEnabled = DataUtil.getBoolean(data, "withdraw_enabled");
        boolean walletConnectEnabled = DataUtil.getBoolean(data, "wallet_connect_enabled");

        adminTelegramSettings.setSupportEnabled(supportEnabled);
        adminTelegramSettings.setDepositEnabled(depositEnabled);
        adminTelegramSettings.setWithdrawEnabled(withdrawEnabled);
        adminTelegramSettings.setWalletConnectEnabled(walletConnectEnabled);

        adminTelegramSettingsRepository.save(adminTelegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTelegramMessages(Map<String, Object> data) {
        TelegramMessages telegramMessages = telegramMessagesRepository.findFirst();

        String supportMessage = data.get("support_message").toString();
        String supportImageMessage = data.get("support_image_message").toString();
        String enable2faMessage = data.get("enable_2fa_message").toString();
        String sendKycMessage = data.get("send_kyc_message").toString();
        String withdrawMessage = data.get("withdraw_message").toString();
        String depositPendingMessage = data.get("deposit_pending_message").toString();
        String depositConfirmedMessage = data.get("deposit_confirmed_message").toString();
        String walletWorkerMessage = data.get("wallet_worker_message").toString();
        String walletAdminMessage = data.get("wallet_admin_message").toString();

        telegramMessages.setSupportMessage(supportMessage);
        telegramMessages.setSupportImageMessage(supportImageMessage);
        telegramMessages.setEnable2faMessage(enable2faMessage);
        telegramMessages.setSendKycMessage(sendKycMessage);
        telegramMessages.setWithdrawMessage(withdrawMessage);
        telegramMessages.setDepositPendingMessage(depositPendingMessage);
        telegramMessages.setDepositConfirmedMessage(depositConfirmedMessage);
        telegramMessages.setWalletWorkerMessage(walletWorkerMessage);
        telegramMessages.setWalletAdminMessage(walletAdminMessage);

        telegramMessagesRepository.save(telegramMessages);

        return ResponseEntity.ok("success");
    }
    //end telegram

    //start support
    @PostMapping(value = "/support")
    public ResponseEntity<String> supportController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_SUPPORT_USER" -> {
                return getSupportUser(data);
            }
            case "DELETE_SUPPORT_MESSAGE" -> {
                return deleteSupportMessage(data);
            }
            case "EDIT_SUPPORT_MESSAGE" -> {
                return editSupportMessage(data);
            }
            case "DELETE_SUPPORT_DIALOG" -> {
                return deleteSupportDialog(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getSupportUser(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("error");
        }

        Map<String, Object> map = new HashMap<>() {{
            put("email", user.getEmail());
            put("domain", user.getDomain());

            if (user.getProfilePhoto() != null) {
                put("profile_photo", user.getProfilePhoto());
            }

            put("online", user.isOnline());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }

    private ResponseEntity<String> deleteSupportMessage(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));

        UserSupportMessage supportMessage = userSupportMessageRepository.findById(id).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.ok("not_found");
        }

        userSupportMessageRepository.deleteByIdAndUserId(id, supportMessage.getUser().getId());

        if (!supportMessage.isSupportViewed() || !supportMessage.isUserViewed()) {
            UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(supportMessage.getUser().getId()).orElse(null);

            if (userSupportDialog != null) {
                if (!supportMessage.isSupportViewed()) {
                    userSupportDialog.setSupportUnviewedMessages(userSupportDialog.getSupportUnviewedMessages() - 1);
                }
                if (!supportMessage.isUserViewed()) {
                    userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() - 1);
                }

                userSupportDialogRepository.save(userSupportDialog);
            }
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSupportMessage(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));

        String message = String.valueOf(data.get("message"));

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.ok("message_is_empty");
        }
        if (message.length() > 2000) {
            return ResponseEntity.ok("message_limit");
        }

        message = XSSUtils.stripXSS(message);

        UserSupportMessage supportMessage = userSupportMessageRepository.findById(id).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.ok("not_found");
        }

        supportMessage.setMessage(message);

        userSupportMessageRepository.save(supportMessage);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportDialog(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("not_found");
        }

        boolean ban = (boolean) data.get("ban");

        userSupportDialogRepository.deleteByUserId(id);

        userSupportMessageRepository.deleteAllByUserId(id);

        if (user.getRoleType() != UserRole.UserRoleType.ROLE_ADMIN && ban) {
            EmailBan emailBan = new EmailBan();
            emailBan.setEmail(user.getEmail());
            emailBan.setUser(user);
            emailBan.setDate(new Date());

            emailBanRepository.save(emailBan);
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(@RequestParam(value = "user_id") String userId, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.ok("message_is_empty");
        }

        if (cooldownService.isCooldown("admin-support")) {
            return ResponseEntity.ok("cooldown");
        }
        User user = userRepository.findById(Long.parseLong(userId)).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (message != null) {
            if (StringUtils.isBlank(message)) {
                return ResponseEntity.ok("message_is_empty");
            }
            if (message.length() > 2000) {
                return ResponseEntity.ok("message_limit");
            }

            message = XSSUtils.stripXSS(message);

            UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, message, false, true, user);

            createOrUpdateSupportDialog(supportMessage, user);

            userSupportMessageRepository.save(supportMessage);
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png";
            try {
                FileUploadUtil.saveFile(Resources.SUPPORT_IMAGES, fileName, image);

                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.IMAGE, "../" + Resources.SUPPORT_IMAGES + "/" + fileName, false, true, user);

                createOrUpdateSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        cooldownService.addCooldown("admin-support", Duration.ofSeconds(2));

        return ResponseEntity.ok("success");
    }

    private void createOrUpdateSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);
        if (userSupportDialog == null) {
            userSupportDialog = new UserSupportDialog();
        }

        userSupportDialog.setOnlyWelcome(false);
        userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }
    //end support

    //start coins
    @PostMapping(value = "/coins")
    public ResponseEntity<String> coinsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_DEPOSIT_COINS" -> {
                return editDepositCoins(data);
            }
            case "EDIT_WITHDRAW_COINS" -> {
                return editWithdrawCoins(data);
            }
            case "DELETE_WITHDRAW_COIN" -> {
                return deleteWithdrawCoin(data);
            }
            case "EDIT_MIN_DEPOSIT" -> {
                return editMinDeposit(data);
            }
            case "EDIT_TRANSACTION_COMMISSIONS" -> {
                return editDepositCommission(data);
            }
            case "EDIT_VERIFICATION_REQUIREMENT" -> {
                return editVerificationRequirement(data);
            }
            case "EDIT_VERIFICATION_AML" -> {
                return editVerificationAml(data);
            }
            case "EDIT_MIN_VERIF" -> {
                return editMinVerif(data);
            }
            case "EDIT_MIN_WITHDRAW" -> {
                return editMinWithdraw(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editDepositCoins(Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        boolean useBtcVerifDeposit = (boolean) data.get("use_btc_verif_deposit");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.ok("title_is_empty");
            }

            double minDepositAmount = coin.get("min_deposit_amount") == null ? -1D : Double.parseDouble(String.valueOf(coin.get("min_deposit_amount")));
            if (minDepositAmount != -1 && minDepositAmount <= 0) {
                return ResponseEntity.ok("min_deposit_amount_error");
            }

            double verifDepositAmount = coin.get("verif_deposit_amount") == null ? 0D : Double.parseDouble(String.valueOf(coin.get("verif_deposit_amount")));
            if (verifDepositAmount < 0) {
                verifDepositAmount = 0;
            }
        }

        for (Map<String, Object> coin : coins) {
            long id = (long) (int) coin.get("id");
            AdminDepositCoin depositCoin = adminDepositCoinRepository.findById(id).orElse(null);
            if (depositCoin == null) {
                continue;
            }

            String title = (String) coin.get("title");
            double minDepositAmount = coin.get("min_deposit_amount") == null ? -1D : Double.parseDouble(String.valueOf(coin.get("min_deposit_amount")));
            double verifDepositAmount = coin.get("verif_deposit_amount") == null ? 0D : Double.parseDouble(String.valueOf(coin.get("verif_deposit_amount")));
            boolean enabled = (boolean) coin.get("enabled");

            if (useBtcVerifDeposit && verifDepositAmount <= 0 && depositCoin.getType() == DepositCoin.CoinType.BTC) {
                return ResponseEntity.ok("use_btc_verif_deposit");
            }

            long position = -1;
            try {
                position = Long.parseLong(String.valueOf(coin.get("position")));
            } catch (Exception ex) {
                return ResponseEntity.ok("position_error");
            }

            if (depositCoin.getPosition() != position || !depositCoin.getTitle().equals(title) || depositCoin.getMinDepositAmount() != minDepositAmount || depositCoin.isEnabled() != enabled || depositCoin.getVerifDepositAmount() != verifDepositAmount) {
                depositCoin.setTitle(title);
                depositCoin.setMinDepositAmount(minDepositAmount);
                depositCoin.setVerifDepositAmount(verifDepositAmount);
                depositCoin.setEnabled(enabled);
                depositCoin.setPosition(position);

                adminDepositCoinRepository.save(depositCoin);
            }
        }

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();

        if (adminCoinSettings.isUseBtcVerifDeposit() != useBtcVerifDeposit) {
            adminCoinSettings.setUseBtcVerifDeposit(useBtcVerifDeposit);

            adminCoinSettingsRepository.save(adminCoinSettings);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWithdrawCoins(Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.ok("title_is_empty");
            }
        }

        for (Map<String, Object> coin : coins) {
            long id = (long) (int) coin.get("id");
            Coin withdrawCoin = coinRepository.findById(id).orElse(null);
            if (withdrawCoin == null) {
                continue;
            }

            String title = (String) coin.get("title");
            String networks = (String) coin.get("networks");
            if (networks.equalsIgnoreCase(withdrawCoin.getSymbol())) {
                networks = null;
            }

            boolean memo = (boolean) coin.get("memo");
            long position = -1;
            try {
                position = Long.parseLong(String.valueOf(coin.get("position")));
            } catch (Exception ex) {
                return ResponseEntity.ok("position_error");
            }

            if (withdrawCoin.getPosition() != position || !withdrawCoin.getTitle().equals(title) || withdrawCoin.isMemo() != memo || (withdrawCoin.getNetworks() == null && networks != null) || (withdrawCoin.getNetworks() != null && !withdrawCoin.getNetworks().equals(networks))) {
                withdrawCoin.setTitle(title);
                withdrawCoin.setMemo(memo);
                withdrawCoin.setNetworks(networks);
                withdrawCoin.setPosition(position);

                coinRepository.save(withdrawCoin);
            }
        }

        return ResponseEntity.ok("success");
    }

    //todo:
    private ResponseEntity<String> deleteWithdrawCoin(Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        Coin coin = coinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("not_found");
        }

        //todo: test
        fastPumpRepository.deleteAllByCoin(coin);
        stablePumpRepository.deleteAllByCoin(coin);
        withdrawCoinLimitRepository.deleteAllByCoinSymbol(coin.getSymbol());
        workerSettingsRepository.deleteAllByCoinSymbol(coin.getSymbol());
        userBalanceRepository.deleteAllByCoinSymbol(coin.getSymbol());
        userAlertRepository.deleteAllByCoin(coin.getSymbol());
        promocodeRepository.deleteAllByCoinSymbol(coin.getSymbol());
        
        coinRepository.delete(coin);
        //todo: delete from workers and users

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinDeposit(Map<String, Object> data) {
        double minDepositAmount = DataUtil.getDouble(data, "min_deposit_amount");
        if (Double.isNaN(minDepositAmount) || minDepositAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinDepositAmount(minDepositAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositCommission(Map<String, Object> data) {
        String depositCommission = data.get("deposit_commission").toString();
        String withdrawCommission = data.get("withdraw_commission").toString();
        boolean depositPercent = depositCommission.endsWith("%");
        boolean withdrawPercent = withdrawCommission.endsWith("%");
        double depositAmount = 0;
        double withdrawAmount = 0;
        try {
            depositAmount = Double.parseDouble(depositCommission.replace("%", ""));
            withdrawAmount = Double.parseDouble(withdrawCommission.replace("%", ""));
        } catch (Exception ex) {
            return ResponseEntity.ok("amount_error");
        }

        if (Double.isNaN(depositAmount) || depositAmount < 0 || (depositPercent && depositAmount >= 100) ||
                Double.isNaN(withdrawAmount) || withdrawAmount < 0 || (withdrawPercent && withdrawAmount >= 100)) {
            return ResponseEntity.ok("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setDepositCommission(depositAmount + (depositPercent ? "%" : ""));
        coinSettings.setWithdrawCommission(withdrawAmount + (withdrawPercent ? "%" : ""));

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationRequirement(Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setVerifRequirement(enabled);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationAml(Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setVerifAml(enabled);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinVerif(Map<String, Object> data) {
        double minVerifAmount = DataUtil.getDouble(data, "min_verif_amount");
        if (Double.isNaN(minVerifAmount) || minVerifAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinVerifAmount(minVerifAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinWithdraw(Map<String, Object> data) {
        double minWithdrawAmount = DataUtil.getDouble(data, "min_withdraw_amount");
        if (Double.isNaN(minWithdrawAmount) || minWithdrawAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinWithdrawAmount(minWithdrawAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/coins")
    public ResponseEntity<String> settingsCoinsController(@RequestParam("coinSymbol") String symbol, @RequestParam(value = "coinTitle") String title, @RequestParam(value = "coinPosition") String positionLine, @RequestParam(value = "coinMemo") boolean memo, @RequestParam(value = "coinIcon") MultipartFile image) {
        if (StringUtils.isBlank(symbol) || StringUtils.isBlank(title)) {
            return ResponseEntity.ok("symbol_title_error");
        }

        symbol = symbol.toUpperCase();

        if (coinRepository.findBySymbol(symbol).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        long position = -1;
        try {
            position = Long.parseLong(positionLine);
        } catch (Exception ex) {
            return ResponseEntity.ok("position_error");
        }

        Coin coin = new Coin();
        coin.setSymbol(symbol);
        coin.setTitle(title);
        coin.setMemo(memo);
        coin.setPosition(position);

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + org.springframework.util.StringUtils.cleanPath(image.getOriginalFilename());
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                coinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/edit-deposit-coin")
    public ResponseEntity<String> editDepositCoinController(@RequestParam("coinId") String coinIdString, @RequestParam(value = "coinIcon") MultipartFile image) {
        long id = Long.parseLong(coinIdString);
        AdminDepositCoin coin = adminDepositCoinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("coin_not_found");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + coin.getId();
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                adminDepositCoinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/edit-coin")
    public ResponseEntity<String> editCoinController(@RequestParam("coinId") String coinIdString, @RequestParam(value = "coinIcon") MultipartFile image) {
        long id = Long.parseLong(coinIdString);
        Coin coin = coinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("coin_not_found");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + coin.getId();
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                coinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }
    //end coins

    //start domains
    @PostMapping(value = "/domains")
    public ResponseEntity<String> domainsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_DOMAIN" -> {
                return addDomain(data);
            }
            case "ADD_ADMIN_DOMAIN" -> {
                return addAdminDomain(data);
            }
            case "DELETE_DOMAIN" -> {
                return deleteDomain(data);
            }
            case "EDIT_EMAIL" -> {
                return editEmail(data);
            }
            case "EDIT_SOCIALS" -> {
                return editSocials(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addDomain(Map<String, Object> data) {
        String email = (String) data.get("worker");
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("worker_not_found");
        }

        Worker worker = workerRepository.findByUserId(user.getId()).orElse(null);
        if (worker == null) {
            return ResponseEntity.ok("worker_not_found");
        }

        String domain = ((String) data.get("domain")).toLowerCase();
        if (domainRepository.findByName(domain).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        if (!DataValidator.isDomainValided(domain)) {
            return ResponseEntity.ok("bad_domain");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = adminSettings.getSiteName();
        String icon = adminSettings.getSiteIcon();

        domainService.createDomain(worker, domain);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addAdminDomain(Map<String, Object> data) {
        String domain = ((String) data.get("domain")).toLowerCase();
        if (domainRepository.findByName(domain).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        if (!DataValidator.isDomainValided(domain)) {
            return ResponseEntity.ok("bad_domain");
        }

        domainService.createDomain(null, domain);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteDomain(Map<String, Object> data) {
        long domainId = (long) (int) data.get("domain_id");
        Domain domain = domainRepository.findById(domainId).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok("domain_not_found");
        }

        domainRepository.delete(domain);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "domains/edit")
    public ResponseEntity<String> domainsEditController(@RequestParam(value = "id") long id,
                                                        @RequestParam("note") String note,
                                                        @RequestParam("exchangeName") String exchangeName,
                                                        @RequestParam("keywords") String keywords,
                                                        @RequestParam("title") String title,
                                                        @RequestParam("description") String description,
                                                        @RequestParam("promoEnabled") boolean promoEnabled,
                                                        @RequestParam("buyCryptoEnabled") boolean buyCryptoEnabled,
                                                        @RequestParam("signupPromoEnabled") boolean signupPromoEnabled,
                                                        @RequestParam("signupRefEnabled") boolean signupRefEnabled,
                                                        @RequestParam("fiatWithdrawEnabled") boolean fiatWithdrawEnabled,
                                                        @RequestParam("promoPopupEnabled") boolean promoPopupEnabled,
                                                        @RequestParam("verif2Enabled") boolean verif2Enabled,
                                                        @RequestParam("verif2Balance") String verif2Balance,
                                                        @RequestParam("robotsTxt") String robotsTxt,
                                                        @RequestParam("fbpixel") String fbpixel,
                                                        @RequestParam(value = "icon", required = false) MultipartFile image) {
        if (!DataValidator.isTextValidedLowest(exchangeName.toLowerCase()) || !DataValidator.isTextValidedLowest(keywords.toLowerCase()) || !DataValidator.isTextValidedLowest(description.toLowerCase()) || !DataValidator.isTextValided(title.toLowerCase())) {
            return ResponseEntity.ok("name_title_error");
        }

        double amount = 0D;
        try {
            amount = Double.parseDouble(verif2Balance);
        } catch (Exception ex) {
            return ResponseEntity.ok("amount_error");
        }

        Domain domain = domainRepository.findById(id).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok("not_found");
        }

        long fbpixelLong = -1;
        try {
            fbpixelLong = Long.parseLong(fbpixel);
        } catch (Exception ex) {
            return ResponseEntity.ok("error");
        }

        domain.setNote(note);
        domain.setExchangeName(exchangeName);
        domain.setKeywords(keywords);
        domain.setTitle(title);
        domain.setDescription(description);
        domain.setPromoEnabled(promoEnabled);
        domain.setBuyCryptoEnabled(buyCryptoEnabled);
        domain.setSignupPromoEnabled(signupPromoEnabled);
        domain.setSignupRefEnabled(signupRefEnabled);
        domain.setFiatWithdrawEnabled(fiatWithdrawEnabled);
        domain.setPromoPopupEnabled(promoPopupEnabled);
        domain.setVerif2Enabled(verif2Enabled);
        domain.setVerif2Balance(amount);
        domain.setRobotsTxt(robotsTxt);
        domain.setFbpixel(fbpixelLong);

        if (image != null && image.getOriginalFilename() != null) {
            try {
                String fileName = domain.getId() + "_" + System.currentTimeMillis() + "." + FilenameUtils.getExtension(image.getOriginalFilename()).toLowerCase();
                FileUploadUtil.saveFile(Resources.DOMAIN_ICONS_DIR, fileName, image);
                domain.setIcon("../" + Resources.DOMAIN_ICONS_DIR + "/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        domainRepository.save(domain);

        return ResponseEntity.ok("success:" + domain.getIcon());
    }

    private ResponseEntity<String> editEmail(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Domain domain = domainRepository.findById(id).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok("not_found");
        }

        boolean enabled = (boolean) data.get("enabled");
        boolean required = (boolean) data.get("required");

        String server = (String) data.get("server");
        if (enabled && !DataValidator.isDomainValided(server.toLowerCase())) {
            return ResponseEntity.ok("invalid_server");
        }
        int port = Integer.parseInt(String.valueOf(data.get("port")));
        if (enabled && port <= 0 || port > 65535) {
            return ResponseEntity.ok("invalid_port");
        }

        String email = String.valueOf(data.get("email")).toLowerCase();
        if (enabled && !DataValidator.isEmailValided(email)) {
            return ResponseEntity.ok("invalid_email");
        }

        String password = String.valueOf(data.get("password"));

        boolean validate = (!domain.isEmailEnabled() && enabled) || (enabled && (!domain.getServer().equals(server) || domain.getPort() != port || !domain.getEmail().equals(email) || !domain.getPassword().equals(password)));
        if (validate && !emailService.validateEmail(server, port, email, password)) {
            return ResponseEntity.ok("connection_error");
        }

        domain.setEmailEnabled(enabled);
        domain.setEmailRequiredEnabled(required);
        domain.setServer(server);
        domain.setPort(port);
        domain.setEmail(email);
        domain.setPassword(password);

        domainRepository.save(domain);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSocials(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Domain domain = domainRepository.findById(id).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok("not_found");
        }

        String facebook = data.get("facebook").toString();
        String x = data.get("x").toString();
        String instagram = data.get("instagram").toString();
        String youtube = data.get("youtube").toString();
        String linkedin = data.get("linkedin").toString();
        String telegram = data.get("telegram").toString();
        String tiktok = data.get("tiktok").toString();
        String reddit = data.get("reddit").toString();
        String discord = data.get("discord").toString();
        String medium = data.get("medium").toString();

        domain.setFacebook(facebook);
        domain.setX(x);
        domain.setInstagram(instagram);
        domain.setYoutube(youtube);
        domain.setLinkedin(linkedin);
        domain.setTelegram(telegram);
        domain.setTiktok(tiktok);
        domain.setReddit(reddit);
        domain.setDiscord(discord);
        domain.setMedium(medium);

        domainRepository.save(domain);

        return ResponseEntity.ok("success");
    }
    //domains end

    //start promocodes
    @PostMapping(value = "/promocodes")
    public ResponseEntity<String> promocodesController(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.ok("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_PROMOCODE" -> {
                return addPromocode(data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addPromocode(Map<String, Object> data) {
        String name = (String) data.get("promocode");
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            return ResponseEntity.ok("invalid_promocode");
        }
        if (promocodeRepository.existsByNameIgnoreCase(name.toLowerCase())) {
            return ResponseEntity.ok("promocode_already_exists");
        }

        String symbol = data.get("symbol").toString().toUpperCase();
        if (!coinService.hasCoin(symbol)) {
            return ResponseEntity.ok("symbol_not_found");
        }

        String text = (String) data.get("text");
        if (!DataValidator.isTextValided(text) || text.length() > 512) {
            return ResponseEntity.ok("invalid_text");
        }

        double minAmount = data.get("amount").equals("false") || ((String) data.get("amount")).isEmpty() ? 0D : Double.parseDouble((String) data.get("amount"));
        double maxAmount = data.get("amount_2").equals("false") || ((String) data.get("amount_2")).isEmpty() ? minAmount : Double.parseDouble((String) data.get("amount_2"));
        double bonus = data.get("bonus").equals("false") || ((String) data.get("bonus")).isEmpty() ? 0D : Double.parseDouble((String) data.get("bonus"));

        if (minAmount <= 0 && maxAmount <= 0 && bonus <= 0) {
            return ResponseEntity.ok("invalid_amount");
        }

        if (minAmount > maxAmount) {
            return ResponseEntity.ok("invalid_min_amount");
        }

        Promocode promocode = new Promocode();
        promocode.setName(name);
        promocode.setText(text);
        promocode.setCoinSymbol(symbol);
        promocode.setMinAmount(minAmount);
        promocode.setMaxAmount(maxAmount);
        promocode.setBonusAmount(bonus);
        promocode.setCreated(new Date());

        promocodeRepository.save(promocode);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> deletePromocode(Map<String, Object> data) {
        long id = (long) (Integer) data.get("promocode_id");
        Promocode promocode = promocodeRepository.findById(id).orElse(null);
        if (promocode == null) {
            return ResponseEntity.ok("not_found");
        }

        promocodeRepository.delete(promocode);

        return ResponseEntity.ok("success");
    }
    //end promocodes

    //start main settings
    @PostMapping(value = "settings/site")
    public ResponseEntity<String> settingsSiteController(@RequestParam("siteName") String name,
                                                         @RequestParam("siteTitle") String siteTitle,
                                                         @RequestParam("siteKeywords") String siteKeywords,
                                                         @RequestParam("siteDescription") String siteDescription,
                                                         @RequestParam("blockedCountries") String blockedCountries,
                                                         @RequestParam("promoEnabled") boolean promoEnabled,
                                                         @RequestParam("buyCryptoEnabled") boolean buyCryptoEnabled,
                                                         @RequestParam("promoPopupEnabled") boolean promoPopupEnabled,
                                                         @RequestParam("verif2Enabled") boolean verif2Enabled,
                                                         @RequestParam("signupPromoEnabled") boolean signupPromoEnabled,
                                                         @RequestParam("signupRefEnabled") boolean signupRefEnabled,
                                                         @RequestParam("fiatWithdrawEnabled") boolean fiatWithdrawEnabled,
                                                         @RequestParam("verif2Balance") String verif2Balance,
                                                         @RequestParam("robotsTxt") String robotsTxt,
                                                         @RequestParam(value = "siteIcon", required = false) MultipartFile image) {
        if (!DataValidator.isTextValidedLowest(name.toLowerCase()) || !DataValidator.isTextValidedLowest(siteKeywords.toLowerCase()) || !DataValidator.isTextValidedLowest(siteDescription.toLowerCase()) || !DataValidator.isTextValidedLowest(siteTitle.toLowerCase())) {
            return ResponseEntity.ok("name_title_error");
        }

        double amount = 0D;
        try {
            amount = Double.parseDouble(verif2Balance);
        } catch (Exception ex) {
            return ResponseEntity.ok("amount_error");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setSiteName(name);
        adminSettings.setSiteTitle(siteTitle);
        adminSettings.setSiteKeywords(siteKeywords);
        adminSettings.setSiteDescription(siteDescription);
        adminSettings.setBlockedCountries(blockedCountries);
        adminSettings.setPromoEnabled(promoEnabled);
        adminSettings.setBuyCryptoEnabled(buyCryptoEnabled);
        adminSettings.setPromoPopupEnabled(promoPopupEnabled);
        adminSettings.setVerif2Enabled(verif2Enabled);
        adminSettings.setSignupPromoEnabled(signupPromoEnabled);
        adminSettings.setSignupRefEnabled(signupRefEnabled);
        adminSettings.setFiatWithdrawEnabled(fiatWithdrawEnabled);
        adminSettings.setVerif2Balance(amount);
        adminSettings.setRobotsTxt(robotsTxt);

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "." + FilenameUtils.getExtension(image.getOriginalFilename());
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_ICON_DIR, fileName, image);
                adminSettings.setSiteIcon("../" + Resources.ADMIN_ICON_DIR + "/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings")
    public ResponseEntity<String> settingsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_SETTINGS" -> {
                return editSupportSettings(data);
            }
            case "EDIT_KYC_ACCEPT" -> {
                return editKycAccept(data);
            }
            case "EDIT_WORKER_PANEL" -> {
                return editWorkerPanel(data);
            }
            case "EDIT_FEATURES" -> {
                return editFeatures(data);
            }
            case "ADD_CRYPTO_LENDING" -> {
                return addCryptoLending(data);
            }
            case "DELETE_CRYPTO_LENDING" -> {
                return deleteCryptoLending(data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }


    private ResponseEntity<String> editSupportSettings(Map<String, Object> data) {
        String message = (String) data.get("message");
        boolean enabled = (boolean) data.get("enabled");
        if (enabled && StringUtils.isBlank(message)) {
            return ResponseEntity.ok("message_is_empty");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setSupportWelcomeMessage(XSSUtils.stripXSS(message));
        adminSettings.setSupportWelcomeEnabled(enabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKycAccept(Map<String, Object> data) {
        String type = String.valueOf(data.get("type"));
        KycAcceptTimer kycAcceptTimer = KycAcceptTimer.getByName("TIMER_" + type.toUpperCase());
        if (kycAcceptTimer == null) {
            return ResponseEntity.ok("error");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setKycAcceptTimer(kycAcceptTimer);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWorkerPanel(Map<String, Object> data) {
        boolean workerTopStats = (boolean) data.get("worker_top_stats");

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setWorkerTopStats(workerTopStats);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editFeatures(Map<String, Object> data) {
        boolean tradingEnabled = DataUtil.getBoolean(data, "trading_enabled");
        boolean swapEnabled = DataUtil.getBoolean(data, "swap_enabled");
        boolean supportEnabled = DataUtil.getBoolean(data, "support_enabled");
        boolean transferEnabled = DataUtil.getBoolean(data, "transfer_enabled");
        boolean walletConnectEnabled = DataUtil.getBoolean(data, "wallet_connect_enabled");
        boolean vipEnabled = DataUtil.getBoolean(data, "vip_enabled");
        boolean fakeWithdrawPending = DataUtil.getBoolean(data, "fake_withdraw_pending");
        boolean fakeWithdrawConfirmed = DataUtil.getBoolean(data, "fake_withdraw_confirmed");
        boolean cryptoLendingEnabled = DataUtil.getBoolean(data, "crypto_lending_enabled");

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        adminSettings.setTradingEnabled(tradingEnabled);
        adminSettings.setSwapEnabled(swapEnabled);
        adminSettings.setSupportEnabled(supportEnabled);
        adminSettings.setTransferEnabled(transferEnabled);
        adminSettings.setWalletConnectEnabled(walletConnectEnabled);
        adminSettings.setVipEnabled(vipEnabled);
        adminSettings.setFakeWithdrawPending(fakeWithdrawPending);
        adminSettings.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);
        adminSettings.setCryptoLendingEnabled(cryptoLendingEnabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addCryptoLending(Map<String, Object> data) {
        String coin = String.valueOf(data.get("coin"));
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("coin_not_found");
        }

        double percent7 = DataUtil.getDouble(data, "percent_7");
        double percent14 = DataUtil.getDouble(data, "percent_14");
        double percent30 = DataUtil.getDouble(data, "percent_30");
        double percent90 = DataUtil.getDouble(data, "percent_90");
        double percent180 = DataUtil.getDouble(data, "percent_180");
        double percent360 = DataUtil.getDouble(data, "percent_360");

        if (percent7 < 0 | percent14 < 0 || percent30 < 0 || percent90 < 0 || percent180 < 0 || percent360 < 0) {
            return ResponseEntity.ok("percent_error");
        }

        double min = DataUtil.getDouble(data, "min");
        double max = DataUtil.getDouble(data, "max");

        if (min < 0 || max < 0) {
            return ResponseEntity.ok("amount_error");
        }

        if (adminCryptoLendingRepository.findByCoinSymbol(coin).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        AdminCryptoLending cryptoLending = new AdminCryptoLending();
        cryptoLending.setCoinSymbol(coin);
        cryptoLending.setMinAmount(min);
        cryptoLending.setMaxAmount(max);
        cryptoLending.setPercents(percent7, percent14, percent30, percent90, percent180, percent360);

        adminCryptoLendingRepository.save(cryptoLending);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteCryptoLending(Map<String, Object> data) {
        long id = Long.parseLong(data.get("id").toString());
        AdminCryptoLending cryptoLending = adminCryptoLendingRepository.findById(id).orElse(null);
        if (cryptoLending == null) {
            return ResponseEntity.ok("not_found");
        }

        adminCryptoLendingRepository.delete(cryptoLending);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/email")
    public ResponseEntity<String> settingsDefaultEmailController(@RequestBody Map<String, Object> data) {
        String server = (String) data.get("server");
        if (!DataValidator.isDomainValided(server.toLowerCase())) {
            return ResponseEntity.ok("invalid_server");
        }
        int port = (int) data.get("port");
        if (port <= 0 || port > 65535) {
            return ResponseEntity.ok("invalid_port");
        }

        String titleRegistration = String.valueOf(data.get("title_registration"));
        String titlePasswordRecovery = String.valueOf(data.get("title_password_recovery"));
        if (StringUtils.isBlank(titleRegistration) || StringUtils.isBlank(titlePasswordRecovery)) {
            return ResponseEntity.ok("invalid_title");
        }

        String htmlRegistration = (String) data.get("html_registration");
        String htmlPasswordRecovery = (String) data.get("html_password_recovery");
        if (StringUtils.isBlank(htmlRegistration) || StringUtils.isBlank(htmlPasswordRecovery)) {
            return ResponseEntity.ok("invalid_html");
        }

        AdminEmailSettings emailSettings = adminEmailSettingsRepository.findFirst();
        emailSettings.setRegistrationMessage(htmlRegistration);
        emailSettings.setPasswordRecoveryMessage(htmlPasswordRecovery);
        emailSettings.setRegistrationTitle(titleRegistration);
        emailSettings.setPasswordRecoveryTitle(titlePasswordRecovery);
        emailSettings.setDefaultServer(server);
        emailSettings.setDefaultPort(port);

        adminEmailSettingsRepository.save(emailSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/errors")
    public ResponseEntity<String> settingsErrorsController(@RequestBody Map<String, Object> data) {
        AdminErrorMessages errorMessages = adminErrorMessagesRepository.findFirst();

        String tradingError = XSSUtils.sanitize(data.get("trading_error").toString());
        String swapError = XSSUtils.sanitize(data.get("swap_error").toString());
        String supportError = XSSUtils.sanitize(data.get("support_error").toString());
        String transferError = XSSUtils.sanitize(data.get("transfer_error").toString());
        String withdrawError = XSSUtils.sanitize(data.get("withdraw_error").toString());
        String withdrawVerificationError = XSSUtils.sanitize(data.get("withdraw_verification_error").toString());
        String withdrawAmlError = XSSUtils.sanitize(data.get("withdraw_aml_error").toString());
        String otherError = XSSUtils.sanitize(data.get("other_error").toString());
        String cryptoLendingError = XSSUtils.sanitize(data.get("crypto_lending_error").toString());
        String p2pError = XSSUtils.sanitize(data.get("p2p_error").toString());

        errorMessages.setTradingMessage(tradingError);
        errorMessages.setSwapMessage(swapError);
        errorMessages.setSupportMessage(supportError);
        errorMessages.setTransferMessage(transferError);
        errorMessages.setWithdrawMessage(withdrawError);
        errorMessages.setWithdrawVerificationMessage(withdrawVerificationError);
        errorMessages.setWithdrawAmlMessage(withdrawAmlError);
        errorMessages.setOtherMessage(otherError);
        errorMessages.setCryptoLendingMessage(cryptoLendingError);
        errorMessages.setP2pMessage(p2pError);

        adminErrorMessagesRepository.save(errorMessages);

        return ResponseEntity.ok("success");
    }
}
