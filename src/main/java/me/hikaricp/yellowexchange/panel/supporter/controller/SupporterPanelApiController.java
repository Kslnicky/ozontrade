package me.hikaricp.yellowexchange.panel.supporter.controller;

import me.hikaricp.yellowexchange.config.Resources;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.CoinService;
import me.hikaricp.yellowexchange.exchange.service.CooldownService;
import me.hikaricp.yellowexchange.exchange.service.UserDetailsServiceImpl;
import me.hikaricp.yellowexchange.exchange.service.UserService;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.*;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;
import me.hikaricp.yellowexchange.panel.supporter.model.Supporter;
import me.hikaricp.yellowexchange.panel.supporter.model.SupporterSupportPreset;
import me.hikaricp.yellowexchange.panel.supporter.repository.SupporterRepository;
import me.hikaricp.yellowexchange.panel.supporter.repository.SupporterSupportPresetsRepository;
import me.hikaricp.yellowexchange.security.xss.utils.XSSUtils;
import me.hikaricp.yellowexchange.utils.DataUtil;
import me.hikaricp.yellowexchange.utils.FileUploadUtil;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping(value = "/api/supporter")
@PreAuthorize("hasRole('ROLE_SUPPORTER')")
public class SupporterPanelApiController {

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
    private EmailBanRepository emailBanRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserService userService;

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

    //start support preset settings
    @PostMapping(value = "/settings/presets")
    public ResponseEntity<String> settingsPresetsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_PRESET_SETTINGS" -> {
                return editSupportPresetSettings(authentication, data);
            }
            case "ADD_SUPPORT_PRESET" -> {
                return addSupportPreset(authentication, data);
            }
            case "DELETE_SUPPORT_PRESET" -> {
                return deleteSupportPreset(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportPresetSettings(Authentication authentication, Map<String, Object> data) {
        Supporter supporter = getSupporter(authentication);

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

            SupporterSupportPreset supportPreset = supporterSupportPresetsRepository.findByIdAndSupporterId(id, supporter.getId()).orElse(null);
            if (supportPreset != null) {
                supportPreset.setTitle(title);
                supportPreset.setMessage(message);
                supportPreset.setSupporter(supporter);

                supporterSupportPresetsRepository.save(supportPreset);
            }
        }

        supporter.setSupportPresetsEnabled(enabled);

        supporterRepository.save(supporter);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addSupportPreset(Authentication authentication, Map<String, Object> data) {
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

        Supporter supporter = getSupporter(authentication);

        if (supporterSupportPresetsRepository.countBySupporterId(supporter.getId()) >= 40) {
            return ResponseEntity.ok("limit");
        }

        SupporterSupportPreset adminSupportPreset = new SupporterSupportPreset();
        adminSupportPreset.setTitle(title);
        adminSupportPreset.setMessage(message);
        adminSupportPreset.setSupporter(supporter);

        supporterSupportPresetsRepository.save(adminSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Authentication authentication, Map<String, Object> data) {
        Supporter supporter = getSupporter(authentication);
        long id = (long) (int) data.get("id");
        if (!supporterSupportPresetsRepository.existsByIdAndSupporterId(id, supporter.getId())) {
            return ResponseEntity.ok("not_found");
        }

        supporterSupportPresetsRepository.deleteById(id, supporter.getId());

        return ResponseEntity.ok("success");
    }
    //end support preset settings

    //start user-edit
    @PostMapping(value = "/user-edit")
    public ResponseEntity<String> userEditController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_OVERVIEW" -> {
                return editOverview(authentication, data);
            }
            case "SET_BALANCE" -> {
                return setBalance(authentication, data);
            }
            case "EDIT_KYC" -> {
                return editKyc(authentication, data);
            }
            case "CREATE_TRANSACTION" -> {
                return createTransaction(authentication, data);
            }
            case "EDIT_TRANSACTION" -> {
                return editTransaction(authentication, data);
            }
            case "EDIT_TRANSACTION_AMOUNT" -> {
                return editTransactionAmount(authentication, data);
            }
            case "EDIT_WITHDRAW_VERIFY" -> {
                return editWithdrawVerify(authentication, data);
            }
            case "ADD_WITHDRAW_VERIFY_COIN" -> {
                return addWithdrawVerifyCoin(authentication, data);
            }
            case "DELETE_WITHDRAW_VERIFY_COIN" -> {
                return deleteWithdrawVerifyCoin(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("panel.api.action.not.found");
            }
        }
    }

    private ResponseEntity<String> editOverview(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("panel.api.user.not.found");
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

        if (!user.getPassword().equals(password) || user.isTwoFactorEnabled() != twoFactorEnabled || user.isFakeKycLv1() != fakeVerifiedLv1 || user.isFakeKycLv2() != fakeVerifiedLv2
                || user.isEmailConfirmed() != emailConfirmed || user.isVip() != vipEnabled) {
            user.setPassword(password);
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

        if (user.getRoleType() == UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> editKyc(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> setBalance(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> createTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> editTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("user_not_found");
        }

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

    private ResponseEntity<String> editTransactionAmount(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("user_not_found");
        }

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

    private ResponseEntity<String> editWithdrawVerify(Authentication authentication, Map<String, Object> data) {
        boolean verifModal = (boolean) data.get("verif_modal");
        boolean amlModal = (boolean) data.get("aml_modal");
        double verifAmount = DataUtil.getDouble(data, "verif_amount");
        double btcVerifAmount = DataUtil.getDouble(data, "btc_verif_amount");

        if (Double.isNaN(verifAmount) || verifAmount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> addWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    private ResponseEntity<String> deleteWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(data.get("coin_type").toString());
        if (coinType == null) {
            return ResponseEntity.ok("not_found");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("user_not_found");
        }

        if (userRequiredDepositCoinRepository.findByUserIdAndType(userId, coinType).isEmpty()) {
            return ResponseEntity.ok("not_found");
        }

        userRequiredDepositCoinRepository.deleteByUserIdAndType(userId, coinType);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/user-edit/errors")
    public ResponseEntity<String> userEditErrorsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

    //todo: кулдауны
    @PostMapping(value = "/user-edit/alert")
    public ResponseEntity<String> userEditAlertController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String cooldownKey = "alert-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.ok("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

        if (user.getRoleType() == UserRole.UserRoleType.ROLE_USER && ban) {
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
            message = XSSUtils.makeLinksClickable(message);

            if (StringUtils.isBlank(message)) {
                return ResponseEntity.ok("support.message.is.empty");
            }

            UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, message, false, true, user);

            createOrUpdateSupportDialog(supportMessage, user);

            userSupportMessageRepository.save(supportMessage);
        }

        if (image != null && image.getOriginalFilename() != null && FileUploadUtil.isAllowedContentType(image)) {
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

    public Supporter getSupporter(Authentication authentication) {
        User user = userService.getUser(authentication);
        return supporterRepository.findByUserId(user.getId()).orElseThrow();
    }
}
