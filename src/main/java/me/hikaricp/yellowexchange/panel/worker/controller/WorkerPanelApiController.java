package me.hikaricp.yellowexchange.panel.worker.controller;

import me.hikaricp.yellowexchange.config.Resources;
import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.*;
import me.hikaricp.yellowexchange.panel.worker.model.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.*;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;
import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.common.model.Promocode;
import me.hikaricp.yellowexchange.panel.common.repository.DomainRepository;
import me.hikaricp.yellowexchange.panel.common.repository.PromocodeRepository;
import me.hikaricp.yellowexchange.panel.common.service.DomainService;
import me.hikaricp.yellowexchange.panel.common.types.KycAcceptTimer;
import me.hikaricp.yellowexchange.panel.worker.model.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.panel.worker.service.WorkerService;
import me.hikaricp.yellowexchange.security.auth.AuthTokenFilter;
import me.hikaricp.yellowexchange.security.xss.utils.XSSUtils;
import me.hikaricp.yellowexchange.utils.DataUtil;
import me.hikaricp.yellowexchange.utils.DataValidator;
import me.hikaricp.yellowexchange.utils.FileUploadUtil;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping(value = "/api/worker")
@PreAuthorize("hasRole('ROLE_WORKER')")
public class WorkerPanelApiController {


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
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerSupportPresetRepository workerSupportPresetRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

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
    private EmailService emailService;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerLegalSettingsRepository workerLegalSettingsRepository;

    @Autowired
    private AuthTokenFilter authenticationJwtTokenFilter;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private WorkerTelegramSettingsRepository workerTelegramSettingsRepository;

    @Autowired
    private WorkerErrorMessagesRepository workerErrorMessagesRepository;

    @Autowired
    private WorkerCryptoLendingRepository workerCryptoLendingRepository;

    //start counters
    @PostMapping(value = "/counters")
    public ResponseEntity<String> countersController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_COUNTERS" -> {
                return getCounters(authentication);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getCounters(Authentication authentication) {
        Worker worker = workerService.getWorker(authentication);

        long supportUnviewed = userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(false, worker.getId(), 0);
        long depositsUnviewed = userDepositRepository.countByViewedAndWorkerId(false, worker.getId());
        long withdrawalsUnviewed = userTransactionRepository.countByUnviewedAndWorkerId(UserTransaction.Type.WITHDRAW.ordinal(), worker.getId());
        long kycUnviewed = userKycRepository.countByViewedAndUserWorkerId(false, worker.getId());

        Map<String, Long> map = new HashMap<>();
        map.put("support_unviewed", supportUnviewed);
        map.put("deposits_unviewed", depositsUnviewed);
        map.put("withdrawals_unviewed", withdrawalsUnviewed);
        map.put("kyc_unviewed", kycUnviewed);

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }
    //end counters

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
        Worker worker = workerService.getWorker(authentication);
        
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
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

        if (user.getRoleType() != UserRole.UserRoleType.ROLE_WORKER) {
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
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    private ResponseEntity<String> setBalance(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    private ResponseEntity<String> createTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    private ResponseEntity<String> editTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserIdAndUserWorkerId(transactionId, userId, worker.getId()).orElse(null);
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
        Worker worker = workerService.getWorker(authentication);

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserIdAndUserWorkerId(transactionId, userId, worker.getId()).orElse(null);
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
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    private ResponseEntity<String> addWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    private ResponseEntity<String> deleteWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(data.get("coin_type").toString());
        if (coinType == null) {
            return ResponseEntity.ok("not_found");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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
    public ResponseEntity<String> userEditErrorsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

    //todo: кулдауны
    @PostMapping(value = "/user-edit/alert")
    public ResponseEntity<String> userEditAlertController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String cooldownKey = "alert-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.ok("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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
        Worker worker = workerService.getWorker(authentication);
        String cooldownKey = worker.getId() + "-presets_edit";
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.ok("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        boolean enabled = (boolean) data.get("enabled");

        List<Map<String, Object>> presets = (List<Map<String, Object>>) data.get("presets");
        Map<Long, Map<String, Object>> presetsMap = new HashMap<>();
        for (Map<String, Object> preset : presets) {
            long id = Long.parseLong(String.valueOf(preset.get("id")));
            presetsMap.put(id, preset);
        }
        List<WorkerSupportPreset> workerSupportPresets = workerSupportPresetRepository.findAllByWorkerId(worker.getId());
        for (WorkerSupportPreset workerSupportPreset : workerSupportPresets) {
            Map<String, Object> preset = presetsMap.get(workerSupportPreset.getId());
            if (preset == null) {
                continue;
            }

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

            workerSupportPreset.setTitle(title);
            workerSupportPreset.setMessage(message);
        }

        workerSupportPresetRepository.saveAllByWorkerId(workerSupportPresets, worker.getId());

        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        workerSettings.setSupportPresetsEnabled(enabled);

        workerSettingsRepository.save(workerSettings);

        cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(15));

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

        Worker worker = workerService.getWorker(authentication);

        if (workerSupportPresetRepository.countByWorkerId(worker.getId()) >= 40) {
            return ResponseEntity.ok("limit");
        }

        WorkerSupportPreset workerSupportPreset = new WorkerSupportPreset();
        workerSupportPreset.setTitle(title);
        workerSupportPreset.setMessage(message);
        workerSupportPreset.setWorker(worker);

        workerSupportPresetRepository.save(workerSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        if (!workerSupportPresetRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.ok("not_found");
        }

        workerSupportPresetRepository.deleteById(id, worker.getId());

        return ResponseEntity.ok("success");
    }
    //end support preset settings

    //start legals
    @PostMapping(value = "/settings/legals")
    public ResponseEntity<String> legalsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String html = (String) data.get("html");

        if (StringUtils.isBlank(html)) {
            return ResponseEntity.ok("invalid_html");
        }

        Worker worker = workerService.getWorker(authentication);

        html = sanitizeLegals(html);

        WorkerLegalSettings workerLegalSettings = workerLegalSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        if (type.equals("AML")) {
            workerLegalSettings.setAml(html);
        } else {
            workerLegalSettings.setTerms(html);
        }

        workerLegalSettingsRepository.save(workerLegalSettings);

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

    //start domain-edit
    @PostMapping(value = "/domains")
    public ResponseEntity<String> domainsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_EMAIL" -> {
                return editEmail(authentication, data);
            }
            case "EDIT_DOMAIN_HOME_PAGE" -> {
                return editDomainHomePage(authentication, data);
            }
            case "EDIT_SOCIALS" -> {
                return editSocials(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    @PostMapping(value = "domains/edit")
    public ResponseEntity<String> domainsEditController(Authentication authentication, @RequestParam(value = "id") long id,
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
                                                        @RequestParam("googlepixel") String googlepixel,
                                                        @RequestParam(value = "icon", required = false) MultipartFile image) {
        if (!DataValidator.isTextValidedLowest(exchangeName.toLowerCase()) || !DataValidator.isTextValidedLowest(keywords.toLowerCase()) || !DataValidator.isTextValidedLowest(description.toLowerCase()) || !DataValidator.isTextValidedLowest(title.toLowerCase())) {
            return ResponseEntity.ok("name_title_error");
        }

        double amount = 0D;
        try {
            amount = Double.parseDouble(verif2Balance);
        } catch (Exception ex) {
            return ResponseEntity.ok("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);
        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
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
        domain.setGooglepixel(googlepixel);

        if (image != null && image.getOriginalFilename() != null && FileUploadUtil.isAllowedContentType(image)) {
            try {
                String fileName = domain.getId() + "_" + System.currentTimeMillis() + ".png";// + FilenameUtils.getExtension(image.getOriginalFilename()).toLowerCase();
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

    private ResponseEntity<String> editEmail(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
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

    private ResponseEntity<String> editDomainHomePage(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok("not_found");
        }

        int homePage = Integer.parseInt(String.valueOf(data.get("home_page"))) - 1;
        if (homePage < 0 || homePage > Domain.HomePageDesign.values().length) {
            return ResponseEntity.ok("invalid_home_page");
        }

        domain.setHomeDesign(homePage);

        domainRepository.save(domain);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSocials(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
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
    //end domain-edit

    //start coins
    @PostMapping(value = "/coins")
    public ResponseEntity<String> coinsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_DEPOSIT_COINS" -> {
                return editDepositCoins(authentication, data);
            }
            case "EDIT_MIN_DEPOSIT" -> {
                return editMinDeposit(authentication, data);
            }
            case "EDIT_TRANSACTION_COMMISSIONS" -> {
                return editDepositCommission(authentication, data);
            }
            case "EDIT_VERIFICATION_REQUIREMENT" -> {
                return editVerificationRequirement(authentication, data);
            }
            case "EDIT_VERIFICATION_AML" -> {
                return editVerificationAml(authentication, data);
            }
            case "EDIT_MIN_VERIF" -> {
                return editMinVerif(authentication, data);
            }
            case "EDIT_MIN_WITHDRAW" -> {
                return editMinWithdraw(authentication, data);
            }
            case "ADD_WITHDRAW_COIN_LIMIT" -> {
                return addWithdrawCoinLimit(authentication, data);
            }
            case "DELETE_WITHDRAW_COIN_LIMIT" -> {
                return deleteWithdrawCoinLimit(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editDepositCoins(Authentication authentication, Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        boolean useBtcVerifDeposit = (boolean) data.get("use_btc_verif_deposit");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.ok("title_is_empty");
            }
            if (!title.matches("[a-zA-Z0-9 _-]+")) {
                return ResponseEntity.ok("title_error");
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

        Map<Long, Map<String, Object>> coinMap = new HashMap<>();
        for (Map<String, Object> coin : coins) {
            coinMap.put((long) (int) coin.get("id"), coin);
        }

        Worker worker = workerService.getWorker(authentication);
        List<WorkerDepositCoin> coinList = workerDepositCoinRepository.findAllByWorkerId(worker.getId());
        for (WorkerDepositCoin depositCoin : coinList) {
            Map<String, Object> coin = coinMap.get(depositCoin.getId());
            if (coin == null) {
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

                workerDepositCoinRepository.save(depositCoin);
            }
        }

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        if (workerCoinSettings.isUseBtcVerifDeposit() != useBtcVerifDeposit) {
            workerCoinSettings.setUseBtcVerifDeposit(useBtcVerifDeposit);

            workerCoinSettingsRepository.save(workerCoinSettings);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinDeposit(Authentication authentication, Map<String, Object> data) {
        double minDepositAmount = DataUtil.getDouble(data, "min_deposit_amount");
        if (Double.isNaN(minDepositAmount) || minDepositAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setMinDepositAmount(minDepositAmount);

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositCommission(Authentication authentication, Map<String, Object> data) {
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

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setDepositCommission(depositAmount + (depositPercent ? "%" : ""));
        workerCoinSettings.setWithdrawCommission(withdrawAmount + (withdrawPercent ? "%" : ""));

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationRequirement(Authentication authentication, Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setVerifRequirement(enabled);

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationAml(Authentication authentication, Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setVerifAml(enabled);

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinVerif(Authentication authentication, Map<String, Object> data) {
        double minVerifAmount = DataUtil.getDouble(data, "min_verif_amount");
        if (Double.isNaN(minVerifAmount) || minVerifAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setMinVerifAmount(minVerifAmount);

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinWithdraw(Authentication authentication, Map<String, Object> data) {
        double minWithdrawAmount = DataUtil.getDouble(data, "min_withdraw_amount");
        if (Double.isNaN(minWithdrawAmount) || minWithdrawAmount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        workerCoinSettings.setMinWithdrawAmount(minWithdrawAmount);

        workerCoinSettingsRepository.save(workerCoinSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/edit-deposit-coin")
    public ResponseEntity<String> editDepositCoinController(Authentication authentication, @RequestParam("coinId") String coinIdString, @RequestParam(value = "coinIcon") MultipartFile image) {
        long id = Long.parseLong(coinIdString);
        Worker worker = workerService.getWorker(authentication);
        WorkerDepositCoin coin = workerDepositCoinRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("coin_not_found");
        }

        if (image != null && image.getOriginalFilename() != null && FileUploadUtil.isAllowedContentType(image)) {
            String fileName = System.currentTimeMillis() + "_" + coin.getId() + ".png";
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                workerDepositCoinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addWithdrawCoinLimit(Authentication authentication, Map<String, Object> data) {
        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        String coin = data.get("coin").toString();
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("coin_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WithdrawCoinLimit withdrawCoinLimit = withdrawCoinLimitRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coin).orElse(new WithdrawCoinLimit());

        withdrawCoinLimit.setCoinSymbol(coin);
        withdrawCoinLimit.setMinAmount(amount);
        withdrawCoinLimit.setWorker(worker);

        withdrawCoinLimitRepository.save(withdrawCoinLimit);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWithdrawCoinLimit(Authentication authentication, Map<String, Object> data) {
        String coinSymbol = data.get("coin").toString();
        Worker worker = workerService.getWorker(authentication);

        Optional<WithdrawCoinLimit> coin = withdrawCoinLimitRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coinSymbol);
        if (coin.isEmpty()) {
            return ResponseEntity.ok("not_found");
        }

        withdrawCoinLimitRepository.delete(coin.get());

        return ResponseEntity.ok("success");
    }
    //end coins

    //start support
    @PostMapping(value = "/support")
    public ResponseEntity<String> supportController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_SUPPORT_USER" -> {
                return getSupportUser(authentication, data);
            }
            case "DELETE_SUPPORT_MESSAGE" -> {
                return deleteSupportMessage(authentication, data);
            }
            case "EDIT_SUPPORT_MESSAGE" -> {
                return editSupportMessage(authentication, data);
            }
            case "DELETE_SUPPORT_DIALOG" -> {
                return deleteSupportDialog(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getSupportUser(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
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

    private ResponseEntity<String> deleteSupportMessage(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));
        Worker worker = workerService.getWorker(authentication);

        UserSupportMessage supportMessage = userSupportMessageRepository.findByIdAndUserWorkerId(id, worker.getId()).orElse(null);
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

    private ResponseEntity<String> editSupportMessage(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));
        Worker worker = workerService.getWorker(authentication);

        String message = String.valueOf(data.get("message"));

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.ok("message_is_empty");
        }
        if (message.length() > 2000) {
            return ResponseEntity.ok("message_limit");
        }

        message = XSSUtils.stripXSS(message);

        UserSupportMessage supportMessage = userSupportMessageRepository.findByIdAndUserWorkerId(id, worker.getId()).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.ok("not_found");
        }

        supportMessage.setMessage(message);

        userSupportMessageRepository.save(supportMessage);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportDialog(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("not_found");
        }

        boolean ban = (boolean) data.get("ban");

        userSupportDialogRepository.deleteByUserId(id);

        userSupportMessageRepository.deleteAllByUserId(id);

        if (user.getRoleType() != UserRole.UserRoleType.ROLE_WORKER && ban) {
            EmailBan emailBan = new EmailBan();
            emailBan.setEmail(user.getEmail());
            emailBan.setUser(user);
            emailBan.setDate(new Date());

            emailBanRepository.save(emailBan);
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(Authentication authentication, @RequestParam(value = "user_id") String userId, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.ok("message_is_empty");
        }

        if (cooldownService.isCooldown("admin-support")) {
            return ResponseEntity.ok("cooldown");
        }

        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(Long.parseLong(userId), worker.getId()).orElse(null);
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

    //start utility
    @PostMapping(value = "/utility")
    public ResponseEntity<String> utilityController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_FEATURE" -> {
                return editFeature(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    //todo: оптимизировать
    private ResponseEntity<String> editFeature(Authentication authentication, Map<String, Object> data) {
        User user = userService.getUser(authentication);
        String cooldownKey = user.getId() + "-edit-feature";
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.ok("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        String featureType = (String) data.get("feature");
        boolean enabled = (boolean) data.get("enabled");

        Worker worker = workerService.getWorker(user);

        if (!featureType.equals("VIP")) {
            switch (featureType.toLowerCase()) {
                case "trading" -> userSettingsRepository.enableTradingForAll(worker.getId(), enabled);
                case "swap" -> userSettingsRepository.enableSwapForAll(worker.getId(), enabled);
                case "support" -> userSettingsRepository.enableSupportForAll(worker.getId(), enabled);
                case "fake_withdraw_pending" ->
                        userSettingsRepository.enableFakeWithdrawPendingForAll(worker.getId(), enabled);
                case "fake_withdraw_confirmed" ->
                        userSettingsRepository.enableFakeWithdrawConfirmedForAll(worker.getId(), enabled);
                case "wallet_connect" -> userSettingsRepository.enableWalletConnectForAll(worker.getId(), enabled);
                case "crypto_lending" -> userSettingsRepository.enableCryptoLendingForAll(worker.getId(), enabled);
            }
        } else {
            userRepository.enableVipForAll(worker.getId(), enabled);
        }

        cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(30));

        return ResponseEntity.ok("success");
    }
    //end utility

    //start settings
    @PostMapping(value = "/settings")
    public ResponseEntity<String> settingsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.ok("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_TELEGRAM_SETTINGS" -> {
                return editTelegramSettings(authentication, data);
            }
            case "EDIT_FEATURE_SETTINGS" -> {
                return editFeatureSettings(authentication, data);
            }
            case "EDIT_SUPPORT_SETTINGS" -> {
                return editSupportSettings(authentication, data);
            }
            case "ADD_RECORD_SETTINGS" -> {
                return addRecordSettings(authentication, data);
            }
            case "DELETE_RECORD_SETTINGS" -> {
                return deleteRecordSettings(authentication, data);
            }
            case "EDIT_KYC_ACCEPT" -> {
                return editKycAccept(authentication, data);
            }
            case "EDIT_BONUS_SETTINGS" -> {
                return editBonusSettings(authentication, data);
            }
            case "EDIT_FEATURES" -> {
                return editFeatures(authentication, data);
            }
            case "ADD_CRYPTO_LENDING" -> {
                return addCryptoLending(authentication, data);
            }
            case "DELETE_CRYPTO_LENDING" -> {
                return deleteCryptoLending(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editTelegramSettings(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerTelegramSettings workerTelegramSettings = workerTelegramSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        long telegramId = Long.parseLong(data.get("telegram_id").toString());
        boolean supportEnabled = DataUtil.getBoolean(data, "support_enabled");
        boolean depositEnabled = DataUtil.getBoolean(data, "deposit_enabled");
        boolean withdrawEnabled = DataUtil.getBoolean(data, "withdraw_enabled");
        boolean walletConnectEnabled = DataUtil.getBoolean(data, "wallet_connect_enabled");
        boolean enable2faEnabled = DataUtil.getBoolean(data, "enable_2fa_enabled");
        boolean sendKycEnabled = DataUtil.getBoolean(data, "send_kyc_enabled");

        workerTelegramSettings.setTelegramId(telegramId);
        workerTelegramSettings.setSupportEnabled(supportEnabled);
        workerTelegramSettings.setDepositEnabled(depositEnabled);
        workerTelegramSettings.setWithdrawEnabled(withdrawEnabled);
        workerTelegramSettings.setWalletConnectEnabled(walletConnectEnabled);
        workerTelegramSettings.setEnable2faEnabled(enable2faEnabled);
        workerTelegramSettings.setSendKycEnabled(sendKycEnabled);

        workerTelegramSettingsRepository.save(workerTelegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editFeatureSettings(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        boolean promoEnabled = DataUtil.getBoolean(data, "promo_enabled");
        boolean buyCryptoEnabled = DataUtil.getBoolean(data, "buy_crypto_enabled");
        boolean fiatWithdrawEnabled = DataUtil.getBoolean(data, "fiat_withdraw_enabled");

        workerSettings.setPromoEnabled(promoEnabled);
        workerSettings.setBuyCryptoEnabled(buyCryptoEnabled);
        workerSettings.setFiatWithdrawEnabled(fiatWithdrawEnabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSupportSettings(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        String message = (String) data.get("message");
        boolean enabled = (boolean) data.get("enabled");
        if (enabled && StringUtils.isBlank(message)) {
            return ResponseEntity.ok("message_is_empty");
        }

        workerSettings.setSupportWelcomeMessage(XSSUtils.stripXSS(message));
        workerSettings.setSupportWelcomeEnabled(enabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKycAccept(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        String type = String.valueOf(data.get("type"));
        KycAcceptTimer kycAcceptTimer = KycAcceptTimer.getByName("TIMER_" + type.toUpperCase());
        if (kycAcceptTimer == null) {
            return ResponseEntity.ok("error");
        }

        workerSettings.setKycAcceptTimer(kycAcceptTimer);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editBonusSettings(Authentication authentication, Map<String, Object> data) {
        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount < 0) {
            return ResponseEntity.ok("amount_error");
        }

        String text = (String) data.get("text");
        if (StringUtils.isBlank(text)) {
            return ResponseEntity.ok("text_error");
        }

        String coin = data.get("coin").toString();
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("coin_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));
        workerSettings.setBonusAmount(amount);
        workerSettings.setBonusCoin(coin);
        workerSettings.setBonusText(XSSUtils.sanitize(text));

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editFeatures(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        boolean tradingEnabled = DataUtil.getBoolean(data, "trading_enabled");
        boolean swapEnabled = DataUtil.getBoolean(data, "swap_enabled");
        boolean supportEnabled = DataUtil.getBoolean(data, "support_enabled");
        boolean transferEnabled = DataUtil.getBoolean(data, "transfer_enabled");
        boolean walletConnectEnabled = DataUtil.getBoolean(data, "wallet_connect_enabled");
        boolean vipEnabled = DataUtil.getBoolean(data, "vip_enabled");
        boolean fakeWithdrawPending = DataUtil.getBoolean(data, "fake_withdraw_pending");
        boolean fakeWithdrawConfirmed = DataUtil.getBoolean(data, "fake_withdraw_confirmed");
        boolean cryptoLendingEnabled = DataUtil.getBoolean(data, "crypto_lending_enabled");

        workerSettings.setTradingEnabled(tradingEnabled);
        workerSettings.setSwapEnabled(swapEnabled);
        workerSettings.setSupportEnabled(supportEnabled);
        workerSettings.setTransferEnabled(transferEnabled);
        workerSettings.setWalletConnectEnabled(walletConnectEnabled);
        workerSettings.setVipEnabled(vipEnabled);
        workerSettings.setFakeWithdrawPending(fakeWithdrawPending);
        workerSettings.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);
        workerSettings.setCryptoLendingEnabled(cryptoLendingEnabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addCryptoLending(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
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

        if (workerCryptoLendingRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coin).isPresent()) {
            return ResponseEntity.ok("already_exists");
        }

        WorkerCryptoLending cryptoLending = new WorkerCryptoLending();
        cryptoLending.setCoinSymbol(coin);
        cryptoLending.setMinAmount(min);
        cryptoLending.setMaxAmount(max);
        cryptoLending.setPercents(percent7, percent14, percent30, percent90, percent180, percent360);
        cryptoLending.setWorker(worker);

        workerCryptoLendingRepository.save(cryptoLending);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteCryptoLending(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        long id = Long.parseLong(data.get("id").toString());
        WorkerCryptoLending cryptoLending = workerCryptoLendingRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (cryptoLending == null) {
            return ResponseEntity.ok("not_found");
        }
        
        workerCryptoLendingRepository.delete(cryptoLending);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/errors")
    public ResponseEntity<String> settingsErrorsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        WorkerErrorMessages errorMessages = workerErrorMessagesRepository.findByWorkerId(worker.getId()).orElse(null);

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

        workerErrorMessagesRepository.save(errorMessages);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addRecordSettings(Authentication authentication, Map<String, Object> data) {
        boolean fakeWithdrawPending = Boolean.parseBoolean(String.valueOf(data.get("fake_withdraw_pending")));
        boolean fakeWithdrawConfirmed = Boolean.parseBoolean(String.valueOf(data.get("fake_withdraw_confirmed")));
        boolean vip = Boolean.parseBoolean(String.valueOf(data.get("vip")));
        boolean walletConnect = Boolean.parseBoolean(String.valueOf(data.get("wallet_connect")));
        boolean fakeVerifiedLv1 = Boolean.parseBoolean(String.valueOf(data.get("fake_verified_lv1")));
        boolean fakeVerifiedLv2 = Boolean.parseBoolean(String.valueOf(data.get("fake_verified_lv2")));

        if (!fakeWithdrawPending && !fakeWithdrawConfirmed && !vip && !walletConnect && !fakeVerifiedLv1 && !fakeVerifiedLv2) {
            return ResponseEntity.ok("no_settings");
        }

        if (fakeWithdrawPending && fakeWithdrawConfirmed) {
            return ResponseEntity.ok("fake_withdraw");
        }

        Worker worker = workerService.getWorker(authentication);
        if (workerRecordSettingsRepository.countByWorkerId(worker.getId()) >= 3) {
            return ResponseEntity.ok("limit");
        }

        long emailEnd = ThreadLocalRandom.current().nextLong(100_000, 999_999);
        if (workerRecordSettingsRepository.existsByEmailEnd(emailEnd)) {
            return ResponseEntity.ok("error");
        }

        WorkerRecordSettings workerRecordSettings = new WorkerRecordSettings();
        workerRecordSettings.setWorker(worker);
        workerRecordSettings.setEmailEnd(emailEnd);
        workerRecordSettings.setFakeWithdrawPending(fakeWithdrawPending);
        workerRecordSettings.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);
        workerRecordSettings.setVip(vip);
        workerRecordSettings.setWalletConnect(walletConnect);
        workerRecordSettings.setFakeVerifiedLv1(fakeVerifiedLv1);
        workerRecordSettings.setFakeVerifiedLv2(fakeVerifiedLv2);

        workerRecordSettingsRepository.save(workerRecordSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteRecordSettings(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        WorkerRecordSettings workerRecordSettings = workerRecordSettingsRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (workerRecordSettings == null) {
            return ResponseEntity.ok("not_found");
        }

        workerRecordSettingsRepository.deleteByIdAndEmailEnd(id, workerRecordSettings.getEmailEnd());

        return ResponseEntity.ok("success");
    }
    //end settings

    //start binding
    @PostMapping(value = "/binding")
    public ResponseEntity<String> bindingController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.ok("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "BIND_BY_EMAIL" -> {
                return bindByEmail(authentication, data);
            }
            case "ADD_PROMOCODE" -> {
                return addPromocode(authentication, data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    //todo: caching
    private ResponseEntity<String> bindByEmail(Authentication authentication, Map<String, Object> data) {
        String email = (String) data.get("email");
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            return ResponseEntity.ok("not_found");
        }

        if (user.getWorker() != null) {
            return ResponseEntity.ok("already_bind");
        }

        Worker worker = workerService.getWorker(authentication);

        userService.bindToWorker(user, worker);

        userRepository.save(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addPromocode(Authentication authentication, Map<String, Object> data) {
        String name = (String) data.get("promocode");
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            return ResponseEntity.ok("invalid_promocode");
        }
        if (promocodeRepository.existsByNameIgnoreCase(name.toLowerCase())) {
            return ResponseEntity.ok("promocode_already_exists");
        }

        String symbol = (String) data.get("symbol");
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

        Worker worker = workerService.getWorker(authentication);

        if (promocodeRepository.countByWorkerId(worker.getId()) > 50) {
            return ResponseEntity.ok("max_promocodes");
        }

        Promocode promocode = new Promocode();
        promocode.setName(name);
        promocode.setText(text);
        promocode.setCoinSymbol(symbol);
        promocode.setMinAmount(minAmount);
        promocode.setMaxAmount(maxAmount);
        promocode.setBonusAmount(bonus);
        promocode.setCreated(new Date());
        promocode.setWorker(worker);

        promocodeRepository.save(promocode);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> deletePromocode(Authentication authentication, Map<String, Object> data) {
        long id = (long) (Integer) data.get("promocode_id");
        Worker worker = workerService.getWorker(authentication);

        Promocode promocode = promocodeRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (promocode == null) {
            return ResponseEntity.ok("not_found");
        }

        promocodeRepository.delete(promocode);

        return ResponseEntity.ok("success");
    }
    //end binding

    //start pumps
    @PostMapping(value = "/pumps")
    public ResponseEntity<String> pumpsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.ok("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "FAST_PUMP_EDIT" -> {
                return fastPumpEdit(authentication, data);
            }
            case "FAST_PUMP_RESET" -> {
                return fastPumpReset(authentication, data);
            }
            case "STABLE_PUMP_EDIT" -> {
                return stablePumpEdit(authentication, data);
            }
            case "STABLE_PUMP_DELETE" -> {
                return stablePumpDelete(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    public ResponseEntity<String> fastPumpEdit(Authentication authentication, Map<String, Object> data) {
        String symbol = (String) data.get("symbol");
        if (!coinService.hasCoin(symbol) || symbol.equals("USDT")) {
            return ResponseEntity.ok("not_supported");
        }

        Coin coin = coinRepository.findBySymbol(symbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("not_supported");
        }

        double percent = 0;
        try {
            percent = Double.parseDouble((String) data.get("percent")) / 100D;
        } catch (Exception ex) {
            return ResponseEntity.ok("percent");
        }

        Worker worker = workerService.getWorker(authentication);

        List<FastPump> fastPumps = fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol);

        long nextPumpTime;
        if (!fastPumps.isEmpty()) {
            FastPump lastPump = fastPumps.get(fastPumps.size() - 1);
            if ((lastPump.getTime() - System.currentTimeMillis()) / 60000 > 15) {
                return ResponseEntity.ok("wait_old_klines");
            }

            nextPumpTime = lastPump.getTime() + 60000;
        } else {
            nextPumpTime = System.currentTimeMillis();
        }

        FastPump fastPump = new FastPump(coin, percent, nextPumpTime, worker);
        fastPumpRepository.save(fastPump);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> fastPumpReset(Authentication authentication, Map<String, Object> data) {
        long coinId = ((long) (int) data.get("coin_id"));
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null || coin.getSymbol().equals("USDT")) {
            return ResponseEntity.ok("not_supported");
        }

        Worker worker = workerService.getWorker(authentication);
        if (!fastPumpRepository.existsByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol())) {
            return ResponseEntity.ok("not_found");
        }

        fastPumpRepository.deleteAllByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol());

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> stablePumpEdit(Authentication authentication, Map<String, Object> data) {
        String symbol = (String) data.get("symbol");
        if (!coinService.hasCoin(symbol) || symbol.equals("USDT")) {
            return ResponseEntity.ok("not_supported");
        }

        Coin coin = coinRepository.findBySymbol(symbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("not_supported");
        }

        double percent = 0;
        try {
            percent = Double.parseDouble((String) data.get("percent")) / 100D;
        } catch (Exception ex) {
            return ResponseEntity.ok("percent");
        }

        Worker worker = workerService.getWorker(authentication);

        StablePump stablePump = stablePumpRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol()).orElse(null);
        if (stablePump != null) {
            stablePump.setPercent(percent);
        } else {
            stablePump = new StablePump(coin, percent, worker);
        }

        stablePumpRepository.save(stablePump);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> stablePumpDelete(Authentication authentication, Map<String, Object> data) {
        long id = (long) (int) data.get("pump_id");
        Worker worker = workerService.getWorker(authentication);
        StablePump stablePump = stablePumpRepository.findByWorkerIdAndId(worker.getId(), id).orElse(null);
        if (stablePump == null) {
            return ResponseEntity.ok("not_found");
        }

        stablePumpRepository.deleteByIdAndWorkerIdAndCoinSymbol(stablePump.getId(), worker.getId(), stablePump.getCoin().getSymbol());

        return ResponseEntity.ok("success");
    }
    //end pumps

    //start deposits
    @PostMapping(value = "/deposits")
    public ResponseEntity<String> depositsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "PAID_OUT" -> {
                return depositPaidOut(authentication, data);
            }
            default -> {
                return ResponseEntity.ok("invalid_action");
            }
        }
    }

    private ResponseEntity<String> depositPaidOut(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        UserDeposit userDeposit = userDepositRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (userDeposit == null) {
            return ResponseEntity.ok("not_found");
        }

        UserTransaction userTransaction = userDeposit.getTransaction();
        if (userTransaction.getStatus() == UserTransaction.Status.COMPLETED) {
            return ResponseEntity.ok("success");
        }

        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransactionRepository.save(userTransaction);

        userService.addBalance(userDeposit.getUser(), userTransaction.getCoinSymbol(), userDeposit.getAmount());

        return ResponseEntity.ok("success");
    }
    //end deposits
}
