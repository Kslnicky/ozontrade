package me.yukitale.yellowexchange.panel.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.*;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.*;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.model.*;
import me.yukitale.yellowexchange.panel.admin.repository.*;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.model.Promocode;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.repository.PromocodeRepository;
import me.yukitale.yellowexchange.panel.common.service.StatsService;
import me.yukitale.yellowexchange.panel.supporter.model.Supporter;
import me.yukitale.yellowexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.yellowexchange.utils.DataValidator;
import me.yukitale.yellowexchange.utils.DateUtil;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Controller
@RequestMapping(value = "/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPanelController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

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
    private AdminErrorMessagesRepository adminErrorMessagesRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private TelegramMessagesRepository telegramMessagesRepository;

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
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StatsService statsService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/admin/statistics");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/admin/statistics");
    }

    @GetMapping(value = "statistics")
    public String statisticsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        model.addAttribute("stats", statsService.getAdminStats());

        return "panel/admin/statistics";
    }

    @GetMapping(value = "detailed-statistics")
    public String detailedStatisticsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        long startTime = user.getRegistered().getTime();
        long currentTime = System.currentTimeMillis();
        int daysPeriod = (int) ((currentTime - startTime) / (86400 * 1000));

        if (daysPeriod == 0) {
            daysPeriod = 1;
        }

        long registrations = userRepository.count();
        long deposits = userDepositRepository.countByCompleted(true);
        double depositsPrice = Optional.ofNullable(userDepositRepository.sumPrice()).orElse(0D);
        long addresses = userAddressRepository.count();
        //todo: support without welcome messages
        long dialogs = userSupportDialogRepository.count();
        long messages = userSupportMessageRepository.count();

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

        return "panel/admin/detailed-statistics";
    }

    @GetMapping(value = "users")
    public String usersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam, @RequestParam(name = "type", defaultValue = "offline", required = false) String type,
                                  @RequestParam(name = "email", defaultValue = "null", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        double size = 30D;

        List<User> users;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!email.equals("null")) {
            users = new ArrayList<>();
            User user = null;
            if (DataValidator.isEmailValided(email)) {
                user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
            }
            if (user != null) {
                users.add(user);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByLastOnlineGreaterThan(lastOnline) / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByLastOnlineGreaterThanOrderByLastActivityDesc(lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.count() / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByOrderByLastActivityDesc(pageable);
            }
        }

        model.addAttribute("users", users);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "panel/admin/users";
    }

    @GetMapping(value = "workers")
    public String workersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(name = "email", defaultValue = "null", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        List<Worker> workers;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!email.equals("null")) {
            workers = new ArrayList<>();
            Worker worker = null;
            if (DataValidator.isEmailValided(email)) {
                worker = workerRepository.findByUserEmail(email.toLowerCase()).orElse(null);
            }
            if (worker != null) {
                workers.add(worker);
            }
        } else {
            maxPage = (int) Math.ceil(workerRepository.count() / 30D);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, 30, Sort.by(Sort.Direction.DESC, "id"));
            workers = workerRepository.findAll(pageable).getContent();
        }

        model.addAttribute("workers", workers);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "panel/admin/workers";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        List<UserLog> userLogs = userLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 200));

        model.addAttribute("user_logs", userLogs);

        return "panel/admin/logs";
    }

    @GetMapping(value = "payments")
    public String paymentsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        model.addAttribute("payment_settings", adminSettingsRepository.findFirst());

        return "panel/admin/payments";
    }

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        List<AdminSupportPreset> supportPresets = adminSupportPresetRepository.findAll();

        model.addAttribute("settings", adminSettings);

        model.addAttribute("support_presets", supportPresets);

        return "panel/admin/support-presets";
    }

    @GetMapping(value = "aml")
    public String amlController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "panel/admin/aml";
    }

    @GetMapping(value = "terms")
    public String termsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "panel/admin/terms";
    }

    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model, @RequestParam("id") long userId, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:users";
        }

        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

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

        Map<Long, String> twinks = userRepository.findTwinksByIpAsMap(user.getRegIp());
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
        
        return "panel/admin/user-edit";
    }

    @GetMapping(value = "telegram")
    public String telegramController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();
        List<AdminTelegramId> telegramIds = adminTelegramIdRepository.findAll();
        TelegramMessages telegramMessages = telegramMessagesRepository.findFirst();

        model.addAttribute("telegram_settings", telegramSettings);
        model.addAttribute("telegram_ids", telegramIds);
        model.addAttribute("telegram_messages", telegramMessages);

        return "panel/admin/telegram";
    }

    @GetMapping(value = "supporters")
    public String supportersController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        List<Supporter> supporters = supporterRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("supporters", supporters);

        return "panel/admin/supporters";
    }

    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        if (adminSettings.isSupportPresetsEnabled()) {
            List<AdminSupportPreset> supportPresets = adminSupportPresetRepository.findAll();

            model.addAttribute("support_presets", supportPresets);
        }

        if (email != null) {
            email = email.toLowerCase();

            UserSupportDialog supportDialog = userSupportDialogRepository.findByUserEmail(email).orElse(null);

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
                    userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0) :
                    userSupportDialogRepository.countByOnlyWelcome(false);

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
                    userSupportDialogRepository.findByOnlyWelcomeAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(false, 0, pageable) :
                    userSupportDialogRepository.findByOnlyWelcomeOrderByLastMessageDateDesc(false, pageable);

            model.addAttribute("support_dialogs", supportDialogs);

            model.addAttribute("type", typeParam);

            model.addAttribute("current_page", page);

            model.addAttribute("max_pages", pages);

            model.addAttribute("pages", PaginationUtil.paginate(pages, page, 10));
        }

        return "panel/admin/support";
    }

    @GetMapping(value = "coins")
    public String coinsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();
        List<Coin> coins = coinRepository.findAll();
        List<AdminDepositCoin> depositCoins = adminDepositCoinRepository.findAll();

        model.addAttribute("coin_settings", adminCoinSettings);
        model.addAttribute("coins", coins);
        model.addAttribute("deposit_coins", depositCoins);

        return "panel/admin/coins";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        userKycRepository.markAllAsViewed();

        List<UserKyc> userKyc = userKycRepository.findAllOrderByDate();

        model.addAttribute("user_kyc", userKyc);

        return "panel/admin/allkyc";
    }

    @GetMapping(value = "admin-domains")
    public String adminDomainsController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                         @RequestParam(name = "name", defaultValue = "null", required = false) String name, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        double size = 30D;

        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        List<Domain> domains = null;
        if (name != null && !name.equals("null")) {
            domains = new ArrayList<>();

            Domain domain = domainRepository.findByName(name.toLowerCase()).orElse(null);
            if (domain != null) {
                domains.add(domain);
            }
        } else {
            maxPage = (int) Math.ceil(domainRepository.countByWorkerIdIsNull() / size);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, (int) size);
            domains = domainRepository.findByWorkerIdIsNullOrderByIdDesc(pageable);
        }

        model.addAttribute("domains", domains);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "panel/admin/admin-domains";
    }

    @GetMapping(value = "domains")
    public String domainsController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(name = "name", defaultValue = "null", required = false) String name, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        double size = 30D;

        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        List<Domain> domains = null;
        if (name != null && !name.equals("null")) {
            domains = new ArrayList<>();

            if (name.contains("@")) {
                domains = domainRepository.findByWorkerUserEmail(name);
            } else {
                Domain domain = domainRepository.findByWorkerIdIsNotNullAndName(name.toLowerCase()).orElse(null);
                if (domain != null) {
                    domains.add(domain);
                }
            }
        } else {
            maxPage = (int) Math.ceil(domainRepository.countByWorkerIdIsNotNull() / size);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, (int) size);
            domains = domainRepository.findByWorkerIdIsNotNullOrderByIdDesc(pageable);
        }

        model.addAttribute("domains", domains);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "panel/admin/domains";
    }

    @GetMapping(value = "domain-edit")
    public String domainEditController(Authentication authentication, Model model, @RequestParam("id") long domainId,
                                       @RequestParam(value = "type", required = false, defaultValue = "admin") String type, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        Domain domain = domainRepository.findById(domainId).orElse(null);
        if (domain == null) {
            return "redirect:" + (type.equals("admin") ? "admin-domains" : "domains");
        }

        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        model.addAttribute("domain", domain);

        model.addAttribute("domain_type", type);

        model.addAttribute("home_page", domain.getHomeDesign());

        return "panel/admin/domain-edit";
    }

    @GetMapping(value = "promocodes")
    public String promocodesController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                       @RequestParam(name = "name", defaultValue = "null", required = false) String promocodeName, @RequestHeader("host") String host, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        addCoinsAttribute(model);

        List<Promocode> promocodes;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!promocodeName.equals("null")) {
            promocodes = new ArrayList<>();
            Promocode promocode = promocodeRepository.findByName(promocodeName).orElse(null);
            if (promocode != null) {
                promocodes.add(promocode);
            }
        } else {
            maxPage = (int) Math.ceil(promocodeRepository.count() / 30D);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, 30);
            promocodes = promocodeRepository.findByOrderByIdDesc(pageable);
        }

        model.addAttribute("host", host);

        model.addAttribute("promocodes", promocodes);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "panel/admin/promocodes";
    }

    @GetMapping(value = "settings")
    public String settingsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        addCoinsAttribute(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
        AdminErrorMessages adminErrorMessages = adminErrorMessagesRepository.findFirst();
        List<AdminCryptoLending> adminCryptoLendings = adminCryptoLendingRepository.findAll();

        model.addAttribute("settings", adminSettings);
        model.addAttribute("email_settings", adminEmailSettings);
        model.addAttribute("error_messages", adminErrorMessages);
        model.addAttribute("crypto_lendings", adminCryptoLendings);

        return "panel/admin/settings";
    }

    @GetMapping(value = "deposits")
    public String depositsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        userDepositRepository.markAllAsViewed();

        model.addAttribute("deposits", userDepositRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));

        return "panel/admin/deposits";
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        userTransactionRepository.markAllAsViewed(UserTransaction.Type.WITHDRAW.ordinal());

        Pageable pageable = PageRequest.ofSize(300);

        List<UserTransaction> userTransactions = userTransactionRepository.findByTypeOrderByIdDesc(UserTransaction.Type.WITHDRAW, pageable);

        model.addAttribute("withdraws", userTransactions);

        return "panel/admin/withdraw";
    }

    @GetMapping(value = "wallet-connect")
    public String walletConnectController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        List<UserWalletConnect> walletConnects = userWalletConnectRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("wallets", walletConnects);

        return "panel/admin/wallet-connect";
    }

    private void addUnviewedCounterAttributes(Model model) {
        model.addAttribute("support_unviewed", userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0));
        model.addAttribute("deposits_unviewed", userDepositRepository.countByViewed(false));
        model.addAttribute("withdrawals_unviewed", userTransactionRepository.countByUnviewed(UserTransaction.Type.WITHDRAW.ordinal()));
        model.addAttribute("kyc_unviewed", userKycRepository.countByViewed(false));
    }

    private void addCoinsAttribute(Model model) {
        List<Coin> coins = coinRepository.findAll();
        model.addAttribute("coins", coins);
    }

    private void addUserServiceAttribute(Model model) {
        model.addAttribute("user_service", userService);
    }
}
