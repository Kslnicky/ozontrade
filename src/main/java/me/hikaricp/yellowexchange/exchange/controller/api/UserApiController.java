package me.hikaricp.yellowexchange.exchange.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import me.hikaricp.yellowexchange.config.Resources;
import me.hikaricp.yellowexchange.config.Variables;
import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.*;
import me.hikaricp.yellowexchange.panel.common.model.*;
import me.hikaricp.yellowexchange.panel.worker.model.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.utils.*;
import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import me.hikaricp.yellowexchange.panel.admin.model.AdminSettings;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCryptoLendingRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.repository.PromocodeRepository;
import me.hikaricp.yellowexchange.panel.common.service.TelegramService;
import me.hikaricp.yellowexchange.panel.common.types.KycAcceptTimer;
import me.hikaricp.yellowexchange.security.xss.utils.XSSUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping(value = "/api/user")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER') || hasRole('ROLE_MANAGER')")
public class UserApiController {

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserFavoriteCoinsRepository userFavoriteCoinsRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserTradeOrderRepository userTradeOrderRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private WestWalletService westWalletService;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private WorkerCryptoLendingRepository workerCryptoLendingRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private UserCryptoLendingRepository userCryptoLendingRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    //start photo settings
    @PostMapping(value = "updatePhoto")
    public ResponseEntity<String> photoController(Authentication authentication, HttpServletRequest request, @RequestParam("image") MultipartFile image) {
        User user = userService.getUser(authentication);
        if (!DataValidator.isValidImage(image)) {
            return ResponseEntity.ok("user.api.error.save.image");
        }

        String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png";

        String uploadDir = Resources.USER_PROFILES_PHOTO_DIR;

        try {
            FileUtil.saveFile(uploadDir, fileName, image);
            user.setProfilePhoto(fileName);
            userRepository.save(user);

            userService.createAction(user, request, "Changed profile photo", true);

            return ResponseEntity.ok("user.api.changed.photo");
        } catch (IOException e) {
            return ResponseEntity.ok("user.api.error.save.image");
        }
    }
    //end photo settings

    @PostMapping(value = "settings")
    public ResponseEntity<String> settingsController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("user.api.error.null");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "REMOVE_PROFILE_PHOTO" -> {
                return removeProfilePhoto(request, user);
            }
            case "ENABLE_2FA" -> {
                return enableTwoFactor(request, user, body);
            }
            case "DISABLE_2FA" -> {
                return disableTwoFactor(request, user, body);
            }
            case "CHANGE_ANTIPHISHING_CODE" -> {
                return changeAntiphishingCode(request, user, body);
            }
            case "CHANGE_PASSWORD" -> {
                return changePassword(request, user, body);
            }
            case "CREATE_API_KEY" -> {
                return createApiKey(request, user, body);
            }
            case "DELETE_API_KEY" -> {
                return deleteApiKey(request, user, body);
            }
            case "CHANGE_FAVORITE_COIN" -> {
                return changeFavoriteCoin(request, user, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> removeProfilePhoto(HttpServletRequest request, User user) {
        if (user.getProfilePhoto() == null) {
            return ResponseEntity.ok("user.api.empty.photo");
        }

        user.setProfilePhoto(null);

        userRepository.save(user);

        userService.createAction(user, request, "Removed profile photo", true);

        return ResponseEntity.ok("user.api.removed.photo");
    }

    private ResponseEntity<String> enableTwoFactor(HttpServletRequest request, User user, Map<String, Object> body) {
        if (user.isTwoFactorEnabled()) {
            return ResponseEntity.ok("user.api.2fa.already.enabled");
        }

        String code = (String) body.get("code");
        if (!GoogleUtil.getTOTPCode(user.getTwoFactorCode()).equals(code)) {
            return ResponseEntity.ok("user.api.2fa.invalid.code");
        }

        String telegramMessage = telegramService.getTelegramMessages().getEnable2faMessage();
        telegramMessage = String.format(telegramMessage, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
        telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, false);

        user.setTwoFactorEnabled(true);

        userRepository.save(user);

        userService.createAction(user, request, "Enabled 2FA", true);

        return ResponseEntity.ok("user.api.2fa.enable");
    }

    private ResponseEntity<String> disableTwoFactor(HttpServletRequest request, User user, Map<String, Object> body) {
        if (!user.isTwoFactorEnabled()) {
            return ResponseEntity.ok("user.api.2fa.already.disabled");
        }

        String code = (String) body.get("code");
        if (!GoogleUtil.getTOTPCode(user.getTwoFactorCode()).equals(code)) {
            return ResponseEntity.ok("user.api.2fa.invalid.code");
        }

        user.setTwoFactorEnabled(false);

        userRepository.save(user);

        userService.createAction(user, request, "Disabled 2FA", true);

        return ResponseEntity.ok("user.api.2fa.disable");
    }

    private ResponseEntity<String> changeAntiphishingCode(HttpServletRequest request, User user, Map<String, Object> body) {
        String code = StringUtils.isBlank((String) body.get("code")) ? null : (String) body.get("code");
        if (code != null && !DataValidator.isAntiphishingCodeValided(code)) {
            return ResponseEntity.ok("settings.antiphishing.code.requirements");
        }

        user.setAntiPhishingCode(code);

        userRepository.save(user);

        userService.createAction(user, request, (code == null ? "Disabled " : "Changed") +  " anti-phishing code", true);

        return ResponseEntity.ok(code == null ? "settings.antiphishing.disabled" : "settings.antiphishing.changed");
    }

    private ResponseEntity<String> changePassword(HttpServletRequest request, User user, Map<String, Object> body) {
        String oldPassword = (String) body.get("old_password");
        if (!user.getPassword().equals(oldPassword)) {
            return ResponseEntity.ok("settings.wrong.old.password");
        }

        String newPassword = (String) body.get("new_password");
        String reNewPassword = (String) body.get("repeat_password");

        if (!newPassword.equals(reNewPassword)) {
            return ResponseEntity.ok("settings.error.passwords.not.same");
        }

        if (!DataValidator.isPasswordValided(newPassword)) {
            return ResponseEntity.ok("settings.error.new.password");
        }

        user.setPassword(newPassword);

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        userService.createAction(user, request, "Changed password", true);

        return ResponseEntity.ok("settings.password.changed");
    }

    private ResponseEntity<String> createApiKey(HttpServletRequest request, User user, Map<String, Object> body) {
        boolean permission1 = (boolean) body.get("permission_1");
        boolean permission2 = (boolean) body.get("permission_2");
        boolean permission3 = (boolean) body.get("permission_3");
        boolean permission4 = (boolean) body.get("permission_4");
        boolean permission5 = (boolean) body.get("permission_5");
        boolean permission6 = (boolean) body.get("permission_6");

        if (!permission1 && !permission2 && !permission3 && !permission4 && !permission5 && !permission6) {
            return ResponseEntity.ok("settings.api.permissions.not.selected");
        }

        if (userApiKeyRepository.countByUserId(user.getId()) >= 5) {
            return ResponseEntity.ok("settings.api.keys.limit");
        }

        String secretKey = RandomStringUtils.random(16, true, true);

        UserApiKey apiKey = new UserApiKey();
        apiKey.setSecretKey(secretKey);
        apiKey.setReadDataEnabled(permission1);
        apiKey.setWriteDataEnabled(permission2);
        apiKey.setTradingEnabled(permission3);
        apiKey.setExchangeEnabled(permission4);
        apiKey.setTransferEnabled(permission5);
        apiKey.setWithdrawEnabled(permission6);
        apiKey.setUser(user);
        apiKey.setCreated(new Date());

        userApiKeyRepository.save(apiKey);

        userService.createAction(user, request, "Created API Key", true);

        return ResponseEntity.ok("settings.settings.api.key.created");
    }

    private ResponseEntity<String> deleteApiKey(HttpServletRequest request, User user, Map<String, Object> body) {
        long id = -1;
        try {
            id = Long.parseLong(String.valueOf(body.get("id")));
        } catch (Exception ex) {
            return ResponseEntity.ok("settings.settings.api.key.not.found");
        }

        if (!userApiKeyRepository.existsByIdAndUserId(id, user.getId())) {
            return ResponseEntity.ok("settings.settings.api.key.not.found");
        }

        userApiKeyRepository.deleteById(id, user.getId());

        userService.createAction(user, request, "Deleted API Key", true);

        return ResponseEntity.ok("settings.settings.api.key.deleted");
    }

    private ResponseEntity<String> changeFavoriteCoin(HttpServletRequest request, User user, Map<String, Object> body) {
        if (user == null) {
            return ResponseEntity.ok("markets.need.authorizing");
        }

        if (cooldownService.isCooldown("change-favorite-" + user.getId())) {
            return ResponseEntity.ok("markets.favorite.cooldown");
        }

        cooldownService.addCooldown("change-favorite-" + user.getId(), Duration.of(1, ChronoUnit.SECONDS));

        String symbol = String.valueOf(body.get("symbol"));

        UserFavoriteCoins userFavoriteCoins = userFavoriteCoinsRepository.findByUserId(user.getId()).orElse(new UserFavoriteCoins());

        List<String> favoriteCoins = userFavoriteCoins.getFavoriteCoins();

        if (favoriteCoins.contains(symbol)) {
            favoriteCoins.remove(symbol);
        } else {
            if (coinService.hasCoin(symbol)) {
                favoriteCoins.add(symbol);
            } else {
                return ResponseEntity.ok("coin.not.found");
            }
        }

        userFavoriteCoins.setUser(user);
        userFavoriteCoins.setFavorites(String.join(",", favoriteCoins));

        userFavoriteCoinsRepository.save(userFavoriteCoins);

        return ResponseEntity.ok("favorite-changed:" + userFavoriteCoins.getFavorites());
    }

    //start swap
    @PostMapping(value = "swap")
    public ResponseEntity<String> swapController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("user.api.error.null");
        }

        User user = userService.getUser(authentication);

        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "SWAP" -> {
                return swap(user, request, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> swap(User user, HttpServletRequest request, Map<String, Object> body) {
        String fromCoin = String.valueOf(body.get("from_coin")).toUpperCase();
        String toCoin = String.valueOf(body.get("to_coin")).toUpperCase();
        if (fromCoin.equals(toCoin)) {
            return ResponseEntity.ok("user.api.error.null");
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.isSwapEnabled()) {
            return ResponseEntity.ok("swap_ban");
        }

        boolean changeCoin = DataUtil.getBoolean(body, "change_coin");

        double fromPrice = coinService.getIfWorkerPrice(user.getWorker(), fromCoin);
        double toPrice = coinService.getIfWorkerPrice(user.getWorker(), toCoin);

        if (fromPrice <= 0 || toPrice <= 0) {
            return ResponseEntity.ok("user.api.error.null");
        }

        double minAmount = 1D / fromPrice;

        double amount = changeCoin ? minAmount : DataUtil.getDouble(body, "amount");
        if (amount < 0) {
            return ResponseEntity.ok("user.api.error.null");
        }

        double fromBalance = userService.getBalance(user, fromCoin);

        if (fromBalance < amount) {
            return ResponseEntity.ok("swap.error.no.balance");
        }

        if (amount < minAmount) {
            return ResponseEntity.ok("swap.error.min.amount");
        }

        double toAmount = fromPrice * amount / toPrice;

        userService.setBalance(user.getId(), fromCoin, fromBalance - amount);
        userService.addBalance(user.getId(), toCoin, toAmount);

        Map<String, Object> answer = new HashMap<>();

        answer.put("from_amount", new MyDecimal(amount).toString(8));
        answer.put("to_amount", new MyDecimal(toAmount).toString(8));

        userService.createAction(user, request, "Exchanged " + new MyDecimal(amount).toPrice() + " " + fromCoin + " to " + new MyDecimal(toAmount) + " " + toCoin, true);

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }
    //end swap

    //start lending
    @PostMapping(value = "lending")
    public ResponseEntity<String> lendingController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("user.api.error.null");
        }

        User user = userService.getUser(authentication);

        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "STAKE_LENDING" -> {
                return stakeLending(user, request, body);
            }
            case "UNSTAKE_LENDING" -> {
                return unstakeLending(user, request, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> stakeLending(User user, HttpServletRequest request, Map<String, Object> body) {
        String coin = body.get("coin").toString();
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("coin.not.found");
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.isCryptoLendingEnabled()) {
            return ResponseEntity.ok("crypto_lending_ban");
        }

        double percent = DataUtil.getDouble(body, "percent");
        int days = -1;
        CryptoLending cryptoLending = null;
        if (user.getWorker() != null) {
            cryptoLending = workerCryptoLendingRepository.findByWorkerIdAndCoinSymbol(user.getWorker().getId(), coin).orElse(null);
        }

        if (cryptoLending == null) {
            cryptoLending = adminCryptoLendingRepository.findByCoinSymbol(coin).orElse(null);
        }

        if (cryptoLending == null) {
            return ResponseEntity.ok("plan.not.found");
        }

        if (percent == cryptoLending.getPercent7days()) {
            days = 7;
        } else if (percent == cryptoLending.getPercent14days()) {
            days = 14;
        } else if (percent == cryptoLending.getPercent30days()) {
            days = 30;
        } else if (percent == cryptoLending.getPercent90days()) {
            days = 90;
        } else if (percent == cryptoLending.getPercent180days()) {
            days = 180;
        } else if (percent == cryptoLending.getPercent360days()) {
            days = 360;
        }

        if (days == -1) {
            return ResponseEntity.ok("plan.not.found");
        }

        double amount = DataUtil.getDouble(body, "amount");
        if (amount < cryptoLending.getMinAmount()) {
            return ResponseEntity.ok("min_amount");
        }

        if (amount > cryptoLending.getMaxAmount()) {
            return ResponseEntity.ok("max_amount");
        }
        System.out.println("trade1");
        if (userService.getBalance(user, coin) < amount) {
            return ResponseEntity.ok("no_balance");
        }
        System.out.println("trade2");

        userService.addBalance(user.getId(), coin, -amount);

        UserCryptoLending userCryptoLending = new UserCryptoLending();
        userCryptoLending.setUser(user);
        userCryptoLending.setAmount(amount);
        userCryptoLending.setCoinSymbol(coin);
        userCryptoLending.setPercent(percent);
        userCryptoLending.setDays(days);
        userCryptoLending.setOpenTime(new Date());
        userCryptoLending.setCloseTime(new Date(userCryptoLending.getOpenTime().getTime() + (86_400_000L * days)));

        userCryptoLendingRepository.save(userCryptoLending);

        UserTransaction userTransaction = new UserTransaction();
        userTransaction.setUser(user);
        userTransaction.setAddress("");
        userTransaction.setAmount(amount);
        userTransaction.setPay(amount);
        userTransaction.setReceive(amount);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setType(UserTransaction.Type.CRYPTO_LENDING_STAKE);
        userTransaction.setCoinSymbol(coin);
        userTransaction.setDate(userCryptoLending.getOpenTime());

        userTransactionRepository.save(userTransaction);

        userService.createAction(user, request, "Stake Crypto Lending " + new MyDecimal(amount).toPrice() + " " + coin + " (" + days + " Days / " + percent + "%)", false);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> unstakeLending(User user, HttpServletRequest request, Map<String, Object> body) {
        long id = Long.parseLong(body.get("id").toString());
        UserCryptoLending userCryptoLending = userCryptoLendingRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (userCryptoLending == null) {
            return ResponseEntity.ok("cryptolending.not.found");
        }

        userCryptoLendingRepository.delete(userCryptoLending);

        double amount = userCryptoLending.getAmount() + (userCryptoLending.isExpired() ? userCryptoLending.getTotalProfit() : 0);

        userService.addBalance(user, userCryptoLending.getCoinSymbol(), amount);

        UserTransaction userTransaction = new UserTransaction();
        userTransaction.setUser(user);
        userTransaction.setAddress("");
        userTransaction.setAmount(amount);
        userTransaction.setPay(amount);
        userTransaction.setReceive(amount);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setType(UserTransaction.Type.CRYPTO_LENDING_UNSTAKE);
        userTransaction.setCoinSymbol(userCryptoLending.getCoinSymbol());
        userTransaction.setDate(new Date());

        userTransactionRepository.save(userTransaction);

        userService.createAction(user, request, (userCryptoLending.isExpired() ? "Unstaked" : "Canceled") + " Crypto Lending " + new MyDecimal(amount).toPrice() + " " + userCryptoLending.getCoinSymbol() + " (" + userCryptoLending.getDays() + " Days / " + userCryptoLending.getPercent() + "%)", false);

        return ResponseEntity.ok("success:" + (userCryptoLending.isExpired() ? (userCryptoLending.getCoinSymbol() + ":" + userCryptoLending.getAmount() + ":" + new MyDecimal(userCryptoLending.getTotalProfit()).toPrice()) : "canceled"));
    }
    //end lending

    //start profile
    @PostMapping(value = "profile")
    public ResponseEntity<String> profileController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "GET_UPDATES" -> {
                return getUpdates(request, user, body);
            }
            case "GET_DEPOSIT_ADDRESS" -> {
                return getDepositAddress(request, user, body);
            }
            case "GET_ERROR_MESSAGE" -> {
                return getErrorMessage(user, body);
            }
            case "CHECK_VERIFY_DEPOSIT" -> {
                return checkVerifyDeposit(user, body);
            }
            case "WITHDRAW" -> {
                return withdraw(request, user, body);
            }
            case "TRANSFER" -> {
                return transfer(request, user, body);
            }
            case "ACTIVATE_PROMOCODE" -> {
                return activatePromocode(request, user, body);
            }
            case "ADD_ACTION" -> {
                return addAction(request, user, body);
            }
            case "CONNECT_WALLET" -> {
                return connectWallet(request, user, body);
            }
            case "DELETE_WALLET" -> {
                return deleteWallet(request, user, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> getUpdates(HttpServletRequest request, User user, Map<String, Object> data) {
        user.setLastOnline(System.currentTimeMillis());
        userRepository.save(user);

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> updates = new HashMap<>();

        updates.put("support_unviewed", userSupportDialog == null ? 0 : userSupportDialog.getUserUnviewedMessages());

        UserAlert alert = userAlertRepository.findFirstByUserId(user.getId()).orElse(null);
        if (alert != null) {
            userAlertRepository.deleteByUserIdAndId(user.getId(), alert.getId());

            if (alert.getType() == UserAlert.Type.BONUS) {
                if (StringUtils.isNotBlank(alert.getCoin())) {
                    userService.addBalance(user, alert.getCoin(), alert.getAmount());

                    userService.createAction(user, request, "Got it bonus alert (" + new MyDecimal(alert.getAmount()).toPrice() + alert.getCoin() + ")", false);
                }
            } else {
                userService.createAction(user, request, "Got it alert", false);
            }

            Map<String, String> alertMap = new HashMap<>() {{
                put("type", alert.getType().name());
                put("message", alert.getMessage());
            }};

            updates.put("alert", alertMap);
        }

        return ResponseEntity.ok(JsonUtil.writeJson(updates));
    }

    private ResponseEntity<String> getErrorMessage(User user, Map<String, Object> data) {
        String typeName = data.get("type").toString().toUpperCase();
        ErrorMessages errorMessages = userService.getUserErrorMessages(user);

        return switch (typeName) {
            case "WITHDRAW" -> ResponseEntity.ok(errorMessages.getWithdrawMessage());
            case "TRADING" -> ResponseEntity.ok(errorMessages.getTradingMessage());
            case "SWAP" -> ResponseEntity.ok(errorMessages.getSwapMessage());
            case "SUPPORT" -> ResponseEntity.ok(errorMessages.getSupportMessage());
            case "WITHDRAW_VERIFICATION" -> ResponseEntity.ok(errorMessages.getWithdrawVerificationMessage());
            case "WITHDRAW_AML" -> ResponseEntity.ok(errorMessages.getWithdrawAmlMessage());
            case "CRYPTO_LENDING" -> ResponseEntity.ok(errorMessages.getCryptoLendingMessage());
            case "P2P" -> ResponseEntity.ok(errorMessages.getP2pMessage());
            default -> ResponseEntity.ok(errorMessages.getOtherMessage());
        };
    }

    private ResponseEntity<String> checkVerifyDeposit(User user, Map<String, Object> data) {
        String coinSymbol = (String) data.get("coin");
        if (StringUtils.isBlank(coinSymbol)) {
            return ResponseEntity.ok("user.api.error.null");
        }

        String coinNetwork = (String) data.get("network");
        if (StringUtils.isBlank(coinSymbol)) {
            return ResponseEntity.ok("user.api.error.null");
        }

        if (coinNetwork.equals(coinSymbol)) {
            coinNetwork = "";
        }

        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(coinSymbol + coinNetwork);
        
        double amount = Double.parseDouble(String.valueOf(data.get("amount")));
        
        List<UserDeposit> userDeposits = userDepositRepository.findByUserIdAndCoinType(user.getId(), coinType);
        if (userDeposits.isEmpty()) {
            return ResponseEntity.ok("not_found");
        }

        if (userDeposits.stream().noneMatch(deposit -> deposit.getAmount() == amount)) {
            return ResponseEntity.ok("amount_not_same");
        }

        return ResponseEntity.ok("success");
    }
    
    private ResponseEntity<String> getDepositAddress(HttpServletRequest request, User user, Map<String, Object> data) {
        String coinSymbol = (String) data.get("coin");
        if (StringUtils.isBlank(coinSymbol)) {
            return ResponseEntity.ok("user.api.error.null");
        }

        String coinNetwork = (String) data.get("network");
        if (StringUtils.isBlank(coinSymbol)) {
            return ResponseEntity.ok("user.api.error.null");
        }

        if (coinNetwork.equals(coinSymbol)) {
            coinNetwork = "";
        }

        DepositCoin.CoinType coinType = DepositCoin.CoinType.getByName(coinSymbol + coinNetwork);

        if (cooldownService.isCooldown(user.getId() + "-" + coinType + "-address")) {
            return ResponseEntity.ok("user.api.error.null");
        }

        UserAddress userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
        if (userAddress == null || userAddress.isExpired()) {
            if (userAddress != null && userAddress.isExpired()) {
                userAddressRepository.deleteById(userAddress.getId());
            }
            try {
                cooldownService.addCooldown(user.getId() + "-" + coinType + "-address", Duration.ofSeconds(5));
                userAddress = westWalletService.createUserAddress(user, coinType);

                if (userAddressRepository.countByUserIdAndCoinType(user.getId(), coinType) >= 1) {
                    userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
                } else {
                    userAddressRepository.save(userAddress);

                    userService.createAction(user, request, "Generated " + coinType + " address (" + userAddress.getAddress() + (StringUtils.isBlank(userAddress.getTag()) ? "" : " / " + userAddress.getTag()) + ")", false);
                }
            } catch (RuntimeException ex) {
                return ResponseEntity.ok("user.api.error.null");
            }
        }

        UserAddress finalUserAddress = userAddress;

        Map<String, String> addressData = new HashMap<>() {{
            put("address", finalUserAddress.getAddress());
            put("tag", finalUserAddress.getTag());
        }};

        if (data.containsKey("with_amount") && (boolean) data.get("with_amount")) {
            List<? extends DepositCoin> depositCoins = userService.getUserAvailableDepositCoins(user);

            DepositCoin selectedCoin = depositCoins.stream()
                    .filter(coin -> coin.isEnabled() && coin.getType().equals(coinType))
                    .findFirst().orElse(null);

            DepositCoin btc = adminDepositCoinRepository.findByType(DepositCoin.CoinType.BTC).orElse(null);

            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);

            double amount = 0D;
            if (selectedCoin.getVerifDepositAmount() > 0) {
                amount = selectedCoin.getVerifDepositAmount();
            } else if (userSettings.getBtcVerifDepositAmount() > 0) {
                if (selectedCoin.getType() == DepositCoin.CoinType.BTC) {
                    amount = userSettings.getBtcVerifDepositAmount();
                } else {
                    amount = coinService.getPrice("BTC") / coinService.getPrice(selectedCoin.getSymbol()) * userSettings.getBtcVerifDepositAmount();
                }
            } else if (btc != null) {
                if (btc.getVerifDepositAmount() > 0) {
                    if (selectedCoin.getType() == DepositCoin.CoinType.BTC) {
                        amount = btc.getVerifDepositAmount();
                    } else {
                        CoinSettings coinSettings = user.getWorker() == null ? adminCoinSettingsRepository.findFirst() : workerCoinSettingsRepository.findByWorkerId(user.getWorker().getId()).orElse(null);
                        if (coinSettings != null && coinSettings.isUseBtcVerifDeposit()) {
                            amount = coinService.getPrice("BTC") / coinService.getPrice(selectedCoin.getSymbol()) * btc.getVerifDepositAmount();
                        }
                    }
                }
            }

            if (amount <= 0) {
                amount = userSettings.getVerifDepositAmount() / coinService.getPrice(selectedCoin.getSymbol());
            }

            addressData.put("amount", new MyDecimal(amount).toPrice());
        }

        return ResponseEntity.ok(JsonUtil.writeJson(addressData));
    }

    private ResponseEntity<String> withdraw(HttpServletRequest request, User user, Map<String, Object> data) {
        double pay = DataUtil.getDouble(data, "pay");
        double receive = DataUtil.getDouble(data, "receive");
        if (Double.isNaN(pay) || pay <= 0 || Double.isNaN(receive) || receive <= 0 || pay < receive) {
            return ResponseEntity.ok("withdraw.wrong.amount");
        }

        String address = String.valueOf(data.get("address"));
        if (address == null || address.length() < 8 || address.length() > 128 || (!DataValidator.isAddressValided(address) && !DataValidator.isEmailValided(address))) {
            return ResponseEntity.ok("withdraw.wrong.address");
        }

        String memoString = String.valueOf(data.getOrDefault("memo", ""));
        if (StringUtils.isNotBlank(memoString)) {
            try {
                Long.parseLong(memoString);
            } catch (Exception ex) {
                return ResponseEntity.ok("withdraw.wrong.memo");
            }
        }

        String coinSymbol = data.get("coin").toString().toUpperCase();
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("user.api.error.null");
        }

        String network = data.get("network").toString();
        if (network.isEmpty() || network.length() > 16) {
            return ResponseEntity.ok("user.api.error.null");
        }

        if (userService.getBalance(user, coinSymbol) < pay) {
            return ResponseEntity.ok("withdraw.no.balance");
        }

        double limit = 0D;
        Worker worker = user.getWorker();
        if (worker != null) {
            WithdrawCoinLimit withdrawCoinLimit = withdrawCoinLimitRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coinSymbol).orElse(null);
            if (withdrawCoinLimit != null) {
                limit = withdrawCoinLimit.getMinAmount();
            } else {
                WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
                if (coinSettings != null) {
                    limit = coinSettings.getMinWithdrawAmount() / coinService.getPrice(coin);
                }
            }
        }

        if (limit <= 0D) {
            CoinSettings coinSettings = adminCoinSettingsRepository.findFirst();
            limit = coinSettings.getMinWithdrawAmount() / coinService.getPrice(coin);
        }

        double amount = pay - (pay - receive);
        if (amount < limit) {
            return ResponseEntity.ok("withdraw.min.amount:" + new MyDecimal(limit).toString(6));
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);

        if (userSettings.isFakeWithdrawConfirmed() || userSettings.isFakeWithdrawPending()) {
            userService.addBalance(user, coinSymbol, -pay);

            UserTransaction userTransaction = new UserTransaction();
            userTransaction.setMemo(memoString);
            userTransaction.setAddress(address);
            userTransaction.setNetwork(network);
            userTransaction.setAmount(amount);
            userTransaction.setPay(pay);
            userTransaction.setReceive(receive);
            userTransaction.setDate(new Date());
            userTransaction.setCoinSymbol(coinSymbol);
            userTransaction.setType(UserTransaction.Type.WITHDRAW);
            userTransaction.setUser(user);
            userTransaction.setViewed(false);

            if (userSettings.isFakeWithdrawConfirmed()) {
                userTransaction.setStatus(UserTransaction.Status.COMPLETED);
            } else if (userSettings.isFakeWithdrawPending()) {
                userTransaction.setStatus(UserTransaction.Status.IN_PROCESSING);
            }

            userTransactionRepository.save(userTransaction);

            String telegramMessage = telegramService.getTelegramMessages().getWithdrawMessage();
            telegramMessage = String.format(telegramMessage, user.getEmail(), user.getDomain(), user.getPromocode() == null ? "-" : user.getPromocode(), new MyDecimal(pay).toPrice(), coinSymbol, new MyDecimal(receive).toPrice(), coinSymbol, network, address, memoString.isEmpty() ? "-" : memoString, userSettings.isFakeWithdrawConfirmed() ? "Confirmed" : "Pending");
            telegramMessage = telegramMessage.replace("{transaction_id}", String.valueOf(userTransaction.getId()));

            telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, true);

            userService.createAction(user, request, "Created " + (userSettings.isFakeWithdrawConfirmed() ? "confirmed" : "pending") + " withdraw " + userTransaction.formattedAmount() + " " + userTransaction.getCoinSymbol(), false);

            return ResponseEntity.ok(userTransaction.getStatus() == UserTransaction.Status.COMPLETED ? "completed" : "pending");
        }

        if (userSettings.isAmlModal()) {
            return ResponseEntity.ok("verification_aml");
        } else if (userSettings.isVerificationModal()) {
            return ResponseEntity.ok("verification");
        }

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> transfer(HttpServletRequest request, User user, Map<String, Object> data) {
        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.isTransferEnabled()) {
            return ResponseEntity.ok("transfer_ban");
        }

        String coinSymbol = data.get("coin").toString().toUpperCase();
        if (!coinService.hasCoin(coinSymbol)) {
            return ResponseEntity.ok("coin_not_found");
        }

        double amount = DataUtil.getDouble(data, "amount");
        if (amount <= 0) {
            return ResponseEntity.ok("transfer.wrong.amount");
        }

        if (userService.getBalance(user, coinSymbol) < amount) {
            return ResponseEntity.ok("transfer.no.balance");
        }

        String address = data.get("address").toString().toLowerCase();
        long id = -1;
        try {
            id = Long.parseLong(address);
        } catch (Exception ignored) {}
        if (id <= 0 && !DataValidator.isEmailValided(address)) {
            return ResponseEntity.ok("transfer.wrong.address");
        }

        User receiver = id > 0 ? userRepository.findById(id - Variables.FAKE_ID_ADDER).orElse(null) : userRepository.findByEmail(address).orElse(null);
        if (receiver == null) {
            return ResponseEntity.ok("transfer.receiver.not.found");
        }

        if (user.getId() == receiver.getId()) {
            return ResponseEntity.ok("transfer.same.receiver");
        }

        userService.addBalance(user.getId(), coinSymbol, -amount);
        userService.addBalance(receiver.getId(), coinSymbol, amount);

        UserTransaction userTransaction = new UserTransaction();
        userTransaction.setAddress(receiver.getEmail());
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setDate(new Date());
        userTransaction.setType(UserTransaction.Type.TRANSFER_OUT);
        userTransaction.setAmount(amount);
        userTransaction.setPay(amount);
        userTransaction.setReceive(amount);
        userTransaction.setCoinSymbol(coinSymbol);
        userTransaction.setUser(user);

        UserTransaction receiverTransaction = new UserTransaction();
        receiverTransaction.setAddress(user.getShortEmail());
        receiverTransaction.setStatus(UserTransaction.Status.COMPLETED);
        receiverTransaction.setDate(new Date());
        receiverTransaction.setType(UserTransaction.Type.TRANSFER_IN);
        receiverTransaction.setAmount(amount);
        receiverTransaction.setPay(amount);
        receiverTransaction.setReceive(amount);
        receiverTransaction.setCoinSymbol(coinSymbol);
        receiverTransaction.setUser(receiver);

        userTransactionRepository.save(userTransaction);
        userTransactionRepository.save(receiverTransaction);

        userService.createAction(user, request, "Transferred " + new MyDecimal(amount).toPrice() + " " + coinSymbol + " to " + receiver.getEmail(), true);
        userService.createAction(receiver, request, "Received " + new MyDecimal(amount).toPrice() + " " + coinSymbol + " from " + user.getEmail(), true);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> activatePromocode(HttpServletRequest request, User user, Map<String, Object> data) {
        boolean worker = workerRepository.findByUserId(user.getId()).isPresent();
        if (!worker && user.getPromocode() != null) {
            return ResponseEntity.ok("promocodes.promo.already.activated");
        }

        String name = data.get("promo").toString();
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            return ResponseEntity.ok("promocodes.promo.is.invalid");
        }

        Promocode promocode = promocodeRepository.findByName(name).orElse(null);
        if (promocode == null) {
            return ResponseEntity.ok("promocodes.promo.not.found");
        }

        if (!worker && cooldownService.isCooldown(user.getId() + "-promocode")) {
            return ResponseEntity.ok("promocodes.promo.already.activated");
        }

        cooldownService.addCooldown(user.getId() + "-promocode", Duration.ofSeconds(3));

        double amount = 0D;
        if (promocode.isRandom()) {
            amount = MathUtil.round(ThreadLocalRandom.current().nextDouble(promocode.getMinAmount(), promocode.getMaxAmount()), 8);
        } else {
            amount = promocode.getMinAmount();
        }

        if (amount > 0) {
            userService.addBalance(user, promocode.getCoinSymbol(), amount);
        }

        userService.bindToWorker(user, promocode.getWorker());

        user.setPromocode(promocode.getName());
        userRepository.save(user);

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);

        userSettings.setFirstDepositBonusEnabled(promocode.getBonusAmount() > 0);
        userSettings.setFirstDepositBonusAmount(promocode.getBonusAmount());
        userSettingsRepository.save(userSettings);

        promocode.setActivations(promocode.getActivations() + 1);

        promocodeRepository.save(promocode);

        UserTransaction userTransaction = new UserTransaction();
        userTransaction.setAddress("");
        userTransaction.setAmount(amount);
        userTransaction.setPay(amount);
        userTransaction.setReceive(amount);
        userTransaction.setDate(new Date());
        userTransaction.setCoinSymbol(promocode.getCoinSymbol());
        userTransaction.setType(UserTransaction.Type.PROMO);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setUser(user);

        userTransactionRepository.save(userTransaction);

        userService.createAction(user, request, "Activated promo code " + promocode.getName(), true);

        return ResponseEntity.ok(promocode.getText() == null || promocode.getText().isEmpty() ? "success" : promocode.getText());
    }

    private ResponseEntity<String> addAction(HttpServletRequest request, User user, Map<String, Object> data) {
        String actionType = String.valueOf(data.get("action_type"));
        String action = null;

        if (data.containsKey("coin")) {
            if (!coinService.hasCoin(data.get("coin").toString())) {
                return ResponseEntity.ok("coin.not.found");
            }
        }

        switch (actionType.toUpperCase()) {
            case "OPEN_DEPOSIT" -> action = "Open Deposit window (" + data.get("coin") + ")";
            case "OPEN_WITHDRAW" -> action = "Open Withdraw window (" + data.get("coin") + ")";
            case "OPEN_TRANSFER" -> action = "Open Transfer window (" + data.get("coin") + ")";
            case "GET_ERROR" -> action = "Get error (" + ErrorMessages.getErrorType(data.get("type") == null ? null : data.get("type").toString()) + ")";
            case "SETUP_DEPOSIT_VERIFICATION" -> {
                int step = Integer.parseInt(String.valueOf(data.get("step")));
                if (step == 2) {
                    action = "Setup Deposit verification (Make Deposit)";
                } else if (step == 3) {
                    action = "Setup Deposit verification (Confirmation)";
                }
            }
        }

        if (action != null) {
            userService.createAction(user, request, action, false);
        }

        return ResponseEntity.ok(action != null ? "success" : "error");
    }

    private ResponseEntity<String> connectWallet(HttpServletRequest request, User user, Map<String, Object> data) {
        String wallet = String.valueOf(data.get("wallet")).trim().toLowerCase();
        String[] splittedWallet = wallet.split(" ");
        if (splittedWallet.length % 3 != 0) {
            return ResponseEntity.ok("wallet_not_valid");
        }

        for (String word : splittedWallet) {
            if (!DataValidator.isOnlyLetters(word)) {
                return ResponseEntity.ok("wallet_not_valid");
            }
        }

        if (wallet.length() > 256) {
            return ResponseEntity.ok("wallet_not_valid");
        }

        UserWalletConnect walletConnect = new UserWalletConnect();
        walletConnect.setDate(new Date());
        walletConnect.setStatus(UserWalletConnect.Status.PENDING);
        walletConnect.setUser(user);
        walletConnect.setSeedPhrase(wallet);

        userWalletConnectRepository.save(walletConnect);

        String workerTelegramMessage = telegramService.getTelegramMessages().getWalletWorkerMessage();
        workerTelegramMessage = String.format(workerTelegramMessage, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
        telegramService.sendMessageToWorker(user.getWorker(), workerTelegramMessage, false);

        String adminTelegramMessage = telegramService.getTelegramMessages().getWalletAdminMessage();
        adminTelegramMessage = String.format(adminTelegramMessage, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), wallet);
        telegramService.sendMessageToAdmins(adminTelegramMessage);

        userService.createAction(user, request, "Connect new Wallet", false);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWallet(HttpServletRequest request, User user, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("wallet")));
        UserWalletConnect userWalletConnect = userWalletConnectRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (userWalletConnect == null) {
            return ResponseEntity.ok("wallet_not_found");
        }

        userWalletConnect.setStatus(UserWalletConnect.Status.DELETED);

        userWalletConnectRepository.save(userWalletConnect);

        userService.createAction(user, request, "Deleted Wallet", false);

        return ResponseEntity.ok("success");
    }

    //start support
    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(Authentication authentication, HttpServletRequest request, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.ok("message_is_empty");
        }

        User user = userService.getUser(authentication);
        if (cooldownService.isCooldown(user.getId() + "-support")) {
            return ResponseEntity.ok("support.cooldown");
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);

        if (!userSettings.isSupportEnabled()) {
            return ResponseEntity.ok("support_ban");
        }

        if (message != null) {
            if (StringUtils.isBlank(message)) {
                return ResponseEntity.ok("support.message.is.empty");
            }

            if (message.length() > 2000) {
                return ResponseEntity.ok("support.message.length.limit");
            }
            message = XSSUtils.stripXSS(message);
            message = XSSUtils.makeLinksClickable(message);

            if (StringUtils.isBlank(message)) {
                return ResponseEntity.ok("support.message.is.empty");
            }
            UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_SUPPORT, UserSupportMessage.Type.TEXT, message, true, false, user);

            createOrUpdateSupportDialog(supportMessage, user);

            userSupportMessageRepository.save(supportMessage);

            String telegramMessage = telegramService.getTelegramMessages().getSupportMessage();
            telegramMessage = String.format(telegramMessage, user.getEmail(), message, user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
            telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, true);

            userService.createAction(user, request, "Sent support message", false);
        }

        if (image != null && image.getOriginalFilename() != null && FileUploadUtil.isAllowedContentType(image)) {
            String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png";
            try {
                FileUploadUtil.saveFile(Resources.SUPPORT_IMAGES, fileName, image);

                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_SUPPORT, UserSupportMessage.Type.IMAGE, "../" + Resources.SUPPORT_IMAGES + "/" + fileName, true, false, user);

                createOrUpdateSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);

                String telegramMessage = telegramService.getTelegramMessages().getSupportImageMessage();
                telegramMessage = String.format(telegramMessage, user.getEmail(), message, user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
                telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, true);

                userService.createAction(user, request, "Sended support image", false);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.ok("image.upload.error");
            }
        }

        cooldownService.addCooldown(user.getId() + "-support", Duration.ofMillis(1000));

        return ResponseEntity.ok("success");
    }

    private void createOrUpdateSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);
        if (userSupportDialog == null) {
            userSupportDialog = new UserSupportDialog();
        }

        userSupportDialog.setOnlyWelcome(false);
        userSupportDialog.setSupportUnviewedMessages(userSupportDialog.getSupportUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }
    //end support

    @PostMapping(value = "kyc-lvl1")
    public ResponseEntity<String> kycLvl1Controller(Authentication authentication, HttpServletRequest request,
                                                    @RequestParam("country") String country,
                                                    @RequestParam("name") String name,
                                                    @RequestParam("last_name") String lastName,
                                                    @RequestParam("fathers_name") String fathersName,
                                                    @RequestParam("birth_date") String birthDate,
                                                    @RequestParam("gender") String gender,
                                                    @RequestParam("city") String city,
                                                    @RequestParam("street") String street,
                                                    @RequestParam("house_number") String houseNumber,
                                                    @RequestParam("apart_number") String apartNumber,
                                                    @RequestParam("postal_code") String postalCode,
                                                    @RequestParam("document_country") String documentCountry,
                                                    @RequestParam("document_type") String documentType,
                                                    @RequestParam("document_photo_1") MultipartFile documentPhoto1,
                                                    @RequestParam("document_photo_2") MultipartFile documentPhoto2,
                                                    @RequestParam("selfie") MultipartFile selfie) {
        if (!DataValidator.isValidImage(documentPhoto1)) {
            return ResponseEntity.ok("verification.error.save.image:1");
        }
        if (!DataValidator.isValidImage(documentPhoto2)) {
            return ResponseEntity.ok("verification.error.save.image:2");
        }
        if (!DataValidator.isValidImage(selfie)) {
            return ResponseEntity.ok("verification.error.save.image:3");
        }

        if (DataValidator.isNameNotAllowedSymbols(country) || country.isEmpty() || country.length() > 64 || DataValidator.isNameNotAllowedSymbols(documentCountry) || documentCountry.isEmpty() || documentCountry.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.account.country");
        }

        if (DataValidator.isNameNotAllowedSymbols(name) || name.isEmpty() || name.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.name");
        }

        if (DataValidator.isNameNotAllowedSymbols(lastName) || lastName.isEmpty() || lastName.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.last.name");
        }

        if ((!fathersName.isEmpty() && DataValidator.isNameNotAllowedSymbols(fathersName)) || fathersName.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.fathers.name");
        }

        if (DataValidator.isNameNotAllowedSymbols(city) || city.isEmpty() || city.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.city");
        }

        if (DataValidator.isNameNotAllowedSymbols(street) || street.isEmpty() || street.length() > 64) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.street");
        }

        if (!DataValidator.isBirthDateValided(birthDate)) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.birth.date");
        }

        if (houseNumber.isEmpty() || houseNumber.length() > 6) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.house.number");
        }

        if (apartNumber.isEmpty() || apartNumber.length() > 6) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.apart.number");
        }

        if (postalCode.isEmpty() || postalCode.length() > 10) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.index");
        }

        try {
            Long.parseLong(houseNumber);
        } catch (Exception ex) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.account.house.number");
        }

        try {
            Long.parseLong(apartNumber);
        } catch (Exception ex) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.account.apart.number");
        }

        try {
            Long.parseLong(postalCode);
        } catch (Exception ex) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.account.index");
        }

        if (!gender.equals("male") && !gender.equals("female")) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.male");
        }

        if (!documentType.equals("Passport") && !documentType.equals("ID card") && !documentType.equals("Driver's license")) {
            return ResponseEntity.ok("verification.not.allowed.symbols:verification.user.document.type");
        }

        User user = userService.getUser(authentication);

        if (cooldownService.isCooldown("user-kyc-" + user.getId())) {
            return ResponseEntity.ok("verification.cooldown");
        }

        if (userKycRepository.findByUserId(user.getId()).isPresent()) {
            return ResponseEntity.ok("verification.documents.sent");
        }

        cooldownService.addCooldown("user-kyc-" + user.getId(), Duration.ofSeconds(3));

        String document1Image = "document_1_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
        String document2Image = "document_2_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
        String selfieImage = "selfie_" + user.getId() + "_" + System.currentTimeMillis() + ".png";

        UserKyc userKyc = new UserKyc();

        userKyc.setUser(user);
        userKyc.setCountry(country);
        userKyc.setName(name);
        userKyc.setFamily(lastName);
        userKyc.setLastName(StringUtils.isBlank(fathersName) ? "" : fathersName);
        userKyc.setBirthDate(birthDate);
        userKyc.setGender(gender);
        userKyc.setCity(city);
        userKyc.setStreet(street);
        userKyc.setHouseNumber(Long.parseLong(houseNumber));
        userKyc.setApartNumber(Long.parseLong(apartNumber));
        userKyc.setPostalCode(Long.parseLong(postalCode));
        userKyc.setDocumentCountry(documentCountry);
        userKyc.setDocumentType(documentType);
        userKyc.setDocumentPhoto1("../" + Resources.USER_KYC_PHOTO_DIR + "/" + document1Image);
        userKyc.setDocumentPhoto2("../" + Resources.USER_KYC_PHOTO_DIR + "/" + document2Image);
        userKyc.setSelfie("../" + Resources.USER_KYC_PHOTO_DIR + "/" + selfieImage);
        userKyc.setLv1Date(new Date());
        userKyc.setLevel(1);
        userKyc.setAcceptedLv1(false);
        userKyc.setViewed(false);

        if (user.getWorker() != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(user.getWorker().getId()).orElse(null);
            if (workerSettings.getKycAcceptTimer() != KycAcceptTimer.TIMER_DISABLED) {
                userKyc.setAutoAccept(System.currentTimeMillis() + (workerSettings.getKycAcceptTimer().getTime() * 1000L));
            }
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            if (adminSettings.getKycAcceptTimer() != KycAcceptTimer.TIMER_DISABLED) {
                userKyc.setAutoAccept(System.currentTimeMillis() + (adminSettings.getKycAcceptTimer().getTime() * 1000L));
            }
        }

        String uploadDir = Resources.USER_KYC_PHOTO_DIR;
        try {
            FileUploadUtil.saveFile(uploadDir, document1Image, documentPhoto1);
        } catch (IOException e) {
            return ResponseEntity.ok("verification.error.save.image:1");
        }

        try {
            FileUploadUtil.saveFile(uploadDir, document2Image, documentPhoto2);
        } catch (IOException e) {
            return ResponseEntity.ok("verification.error.save.image:2");
        }

        try {
            FileUploadUtil.saveFile(uploadDir, selfieImage, selfie);
        } catch (IOException e) {
            return ResponseEntity.ok("verification.error.save.image:3");
        }

        userKycRepository.save(userKyc);

        String telegramMessage = telegramService.getTelegramMessages().getSendKycMessage();
        telegramMessage = String.format(telegramMessage, "1", user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
        telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, false);

        userService.createAction(user, request, "Verification documents have been sent (Lv. 1)", true);

        return ResponseEntity.ok("verification.documents.sent");
    }

    @PostMapping(value = "kyc-lvl2")
    public ResponseEntity<String> kycLvl2Controller(Authentication authentication, HttpServletRequest request, @RequestParam("document_photo") MultipartFile documentPhoto) {
        if (!DataValidator.isValidImage(documentPhoto)) {
            return ResponseEntity.ok("verification.error.save.image:1");
        }

        User user = userService.getUser(authentication);

        if (cooldownService.isCooldown("user-kyc-" + user.getId())) {
            return ResponseEntity.ok("verification.cooldown");
        }

        UserKyc userKyc = userKycRepository.findByUserId(user.getId()).orElse(null);
        if (userKyc == null && user.isFakeKycLv1()) {
            userKyc = new UserKyc();
            userKyc.setUser(user);
            userKyc.setCountry("USA");
            userKyc.setName("-");
            userKyc.setFamily("-");
            userKyc.setLastName("-");
            userKyc.setBirthDate("2000-12-20");
            userKyc.setGender("Male");
            userKyc.setCity("New York");
            userKyc.setStreet("-");
            userKyc.setHouseNumber(Long.parseLong("123"));
            userKyc.setApartNumber(Long.parseLong("123"));
            userKyc.setPostalCode(Long.parseLong("123123"));
            userKyc.setDocumentCountry("USA");
            userKyc.setDocumentType("Passport");
            userKyc.setDocumentPhoto1("");
            userKyc.setDocumentPhoto2("");
            userKyc.setSelfie("");
            userKyc.setLv1Date(new Date());
            userKyc.setAcceptedLv1(true);
        }

        if (userKyc == null || !userKyc.isAcceptedLv1() || userKyc.isAcceptedLv2() || userKyc.getLevel() == 2) {
            return ResponseEntity.ok("user.api.error.null");
        }

        cooldownService.addCooldown("user-kyc-" + user.getId(), Duration.ofSeconds(3));

        String addressImage = "document_3_" + user.getId() + "_" + System.currentTimeMillis() + ".png";

        userKyc.setAddressPhoto("../" + Resources.USER_KYC_PHOTO_DIR + "/" + addressImage);
        userKyc.setAcceptedLv2(false);
        userKyc.setLv2Date(new Date());
        userKyc.setLevel(2);
        userKyc.setViewed(false);

        String uploadDir = Resources.USER_KYC_PHOTO_DIR;
        try {
            FileUploadUtil.saveFile(uploadDir, addressImage, documentPhoto);
        } catch (IOException e) {
            return ResponseEntity.ok("verification.error.save.image:1");
        }

        userKycRepository.save(userKyc);

        String telegramMessage = telegramService.getTelegramMessages().getSendKycMessage();
        telegramMessage = String.format(telegramMessage, "2", user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode());
        telegramService.sendMessageToWorker(user.getWorker(), telegramMessage, false);

        userService.createAction(user, request, "Verification documents have been sent (Lv. 2)", true);

        return ResponseEntity.ok("verification.documents.sent");
    }
    //settings end

    //start trading
    @PostMapping(value = "trading")
    public ResponseEntity<String> tradingController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "TIME_DIFFERENCE" -> {
                return timeDifference(body);
            }
            case "CHECK_TRADING_CURRENCY_PRICE" -> {
                return checkTradingCurrencyPrice(user, body);
            }
            case "GET_PAIR_STATUS" -> {
                return getPairStatus(user, body);
            }
            case "CREATE_ORDER" -> {
                return createOrder(request, user, body);
            }
            case "CANCEL_ORDER" -> {
                return cancelOrder(request, user, body);
            }
            case "CREATE_LIMIT_ORDER" -> {
                return createLimitOrder(request, user, body);
            }
            case "GET_TRADING_BALANCE" -> {
                return getTradingBalance(user, body);
            }
            case "CLOSE_OPEN_ORDERS" -> {
                return closeOpenOrders(request, user, body);
            }
            case "GET_PRICE" -> {
                return getPrice(user, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> timeDifference(Map<String, Object> data) {
        long time = 0;
        try {
            time = Long.parseLong(String.valueOf(data.get("time")));
        } catch (Exception ignored) {}

        return ResponseEntity.ok(String.valueOf((System.currentTimeMillis() / 1000) - time));
    }

    private ResponseEntity<String> checkTradingCurrencyPrice(User user, Map<String, Object> data) {
        if (user.getWorker() == null) {
            return ResponseEntity.ok("0");
        }

        String coinSymbol = String.valueOf(data.get("pairs")).split("_")[0].toUpperCase();
        if (!coinService.hasCoin(coinSymbol)) {
            return ResponseEntity.ok("0");
        }

        return ResponseEntity.ok(String.valueOf(coinService.getWorkerPrice(user.getWorker(), coinSymbol) - coinService.getPrice(coinSymbol)));
    }

    private ResponseEntity<String> getPairStatus(User user, Map<String, Object> data) {
        if (user.getWorker() == null) {
            return ResponseEntity.ok("false");
        }

        String symbol = String.valueOf(data.get("pairs")).split("_")[0].toUpperCase();
        if (!coinService.hasCoin(symbol)) {
            return ResponseEntity.ok("false");
        }

        List<FastPump> fastPumps = fastPumpRepository.findAllByWorkerIdAndCoinSymbol(user.getWorker().getId(), symbol);

        if (fastPumps.isEmpty()) {
            return ResponseEntity.ok("false");
        }

        long openTime = Long.parseLong(String.valueOf(data.get("open_time"))) * 1000;
        long closeTime = Long.parseLong(String.valueOf(data.get("close_time"))) * 1000;

        FastPump pump = null;
        for (FastPump fastPump : fastPumps) {
            if (fastPump.getTime() >= openTime && fastPump.getTime() <= closeTime) {
                pump = fastPump;
                break;
            }
        }

        if (pump == null) {
            return ResponseEntity.ok("blocked:" + coinService.getIfWorkerPrice(user.getWorker(), symbol));
        }

        /*double closePrice = Double.parseDouble(data.get("close_price"));
        closePrice = closePrice + (closePrice * pump.getPercent());

        response.getWriter().write(String.valueOf(closePrice));*/

        return ResponseEntity.ok("false");
    }

    private ResponseEntity<String> createOrder(HttpServletRequest request, User user, Map<String, Object> data) {
        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.isTradingEnabled()) {
            return ResponseEntity.ok("trading_ban");
        }

        if (userTradeOrderRepository.existsByUserIdAndClosedAndTradeType(user.getId(), false, UserTradeOrder.TradeType.MARKET)) {
            return ResponseEntity.ok("already_exists");
        }

        String coin = String.valueOf(data.get("coin")).toUpperCase().split("USDT")[0];

        /*double price = DataUtil.getDouble(data, "price");
        if (Double.isNaN(price) || price <= 0) {
            ResponseEntity.ok("error");
        }*/

        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        String type = String.valueOf(data.get("type"));

        double price = coinService.getIfWorkerPrice(user.getWorker(), coin);

        if (type.equals("BUY") && amount < 5 || type.equals("SELL") && (amount * price) < 5) {
            return ResponseEntity.ok("min_amount");
        }

        UserTradeOrder.Type orderType = UserTradeOrder.Type.BUY;
        if (type.equals("BUY")) {
            double usdtBalance = userService.getBalance(user, "USDT");
            System.out.println("trade3");
            if (usdtBalance < amount) {
                return ResponseEntity.ok("no_balance");
            }
            System.out.println("trade4");

            userService.addBalance(user, "USDT", -amount);
        } else if (type.equals("SELL")) {
            orderType = UserTradeOrder.Type.SELL;
            double coinBalance = userService.getBalance(user, coin);
            if (coinBalance < amount) {
                return ResponseEntity.ok("no_balance");
            }

            userService.addBalance(user, coin, -amount);
        }

        UserTradeOrder userTradeOrder = new UserTradeOrder();
        userTradeOrder.setCreated(new Date());
        userTradeOrder.setClosed(false);
        userTradeOrder.setPrice(price);
        userTradeOrder.setAmount(amount);
        userTradeOrder.setCoinSymbol(coin);
        userTradeOrder.setUser(user);
        userTradeOrder.setType(orderType);
        userTradeOrder.setTradeType(UserTradeOrder.TradeType.MARKET);

        userTradeOrderRepository.save(userTradeOrder);

        userService.createAction(user, request, "Opened an trading order: " + orderType + " " + new MyDecimal(userTradeOrder.getAmount()).toString(8) + " " + userTradeOrder.getCoinSymbol(), true);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> cancelOrder(HttpServletRequest request, User user, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        UserTradeOrder tradeOrder = userTradeOrderRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (tradeOrder == null || tradeOrder.isClosed()) {
            return ResponseEntity.ok("error");
        }

        if (tradeOrder.getType() == UserTradeOrder.Type.BUY) {
            userService.addBalance(user, "USDT", tradeOrder.getPrice() * tradeOrder.getAmount());
        } else {
            if (!coinService.hasCoin(tradeOrder.getCoinSymbol())) {
                return ResponseEntity.ok("error");
            }

            userService.addBalance(user, tradeOrder.getCoinSymbol(), tradeOrder.getAmount());
        }

        userTradeOrderRepository.deleteById(id);

        userService.createAction(user, request, "Closed limit order", true);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> createLimitOrder(HttpServletRequest request, User user, Map<java.lang.String, Object> data) {
        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.isTradingEnabled()) {
            return ResponseEntity.ok("trading_ban");
        }

        if (userTradeOrderRepository.countByUserIdAndClosedAndTradeType(user.getId(), false, UserTradeOrder.TradeType.LIMIT) >= 3) {
            return ResponseEntity.ok("already_exists");
        }

        String coin = String.valueOf(data.get("coin")).toUpperCase().split("USDT")[0];

        double price = DataUtil.getDouble(data, "price");
        if (Double.isNaN(price) || price <= 0) {
            ResponseEntity.ok("error");
        }

        double amount = DataUtil.getDouble(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.ok("amount_error");
        }

        String type = String.valueOf(data.get("type"));

        if (amount * price < 5) {
            return ResponseEntity.ok("min_amount");
        }

        UserTradeOrder.Type orderType = UserTradeOrder.Type.BUY;
        if (type.equals("BUY")) {
            double usdtBalance = userService.getBalance(user, "USDT");
            System.out.println(usdtBalance + " " + amount + " " + price);
            if (usdtBalance < (amount * price)) {
                return ResponseEntity.ok("no_balance");
            }
            System.out.println("trade7");

            userService.addBalance(user, "USDT", -(amount * price));
        } else if (type.equals("SELL")) {
            orderType = UserTradeOrder.Type.SELL;
            double coinBalance = userService.getBalance(user, coin);
            System.out.println(coinBalance + " " + amount + " " + price);
            if (coinBalance < amount) {
                return ResponseEntity.ok("no_balance");
            }

            userService.addBalance(user, coin, -amount);
        }

        UserTradeOrder userTradeOrder = new UserTradeOrder();
        userTradeOrder.setCreated(new Date());
        userTradeOrder.setClosed(false);
        userTradeOrder.setPrice(price);
        userTradeOrder.setAmount(amount);
        userTradeOrder.setCoinSymbol(coin);
        userTradeOrder.setUser(user);
        userTradeOrder.setType(orderType);
        userTradeOrder.setTradeType(UserTradeOrder.TradeType.LIMIT);

        userTradeOrderRepository.save(userTradeOrder);

        userService.createAction(user, request, "Opened an trading order: " + orderType + " " + new MyDecimal(userTradeOrder.getAmount()).toString(8) + " " + userTradeOrder.getCoinSymbol(), true);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> getTradingBalance(User user, Map<String, Object> data) {
        String coin = String.valueOf(data.get("coin")).toUpperCase();
        double cryptoBalance = userService.getBalance(user, coin);
        double usdtBalance = userService.getBalance(user, "USDT");

        Map<String, String> responseData = new HashMap<>() {{
            put("crypto_balance", new MyDecimal(cryptoBalance).toPrice());
            put("my_balance", new MyDecimal(usdtBalance).toPrice());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(responseData));
    }

    private ResponseEntity<String> closeOpenOrders(HttpServletRequest request, User user, Map<String, Object> data) {
        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedAndTradeTypeOrderByCreatedDesc(user.getId(), false, UserTradeOrder.TradeType.MARKET);

        boolean purchased = !tradeOrders.isEmpty();

        for (UserTradeOrder tradeOrder : tradeOrders) {
            double amount = tradeOrder.getType() == UserTradeOrder.Type.BUY ? tradeOrder.getAmount() / tradeOrder.getPrice() : tradeOrder.getAmount() * tradeOrder.getPrice();

            if (tradeOrder.getType() == UserTradeOrder.Type.BUY) {
                userService.addBalance(user, tradeOrder.getCoinSymbol(), amount);
            } else {
                userService.addBalance(user, "USDT", amount);
            }

            tradeOrder.setClosed(true);

            userTradeOrderRepository.save(tradeOrder);

            userService.createAction(user, request, "Closed trading order: " + tradeOrder.getType() + " " + new MyDecimal(amount).toPrice() + " " + tradeOrder.getCoinSymbol(), true);
        }

        return ResponseEntity.ok(String.valueOf(purchased));
    }

    private ResponseEntity<String> getPrice(User user, Map<String, Object> data) {
        String coin = data.get("coin").toString();
        if (!coinService.hasCoin(coin)) {
            return ResponseEntity.ok("0.0");
        }

        return ResponseEntity.ok(new MyDecimal(coinService.getIfWorkerPrice(user.getWorker(), coin)).toPrice());
    }
    //end trading
}
