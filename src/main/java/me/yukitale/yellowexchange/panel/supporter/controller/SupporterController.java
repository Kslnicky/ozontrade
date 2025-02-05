package me.yukitale.yellowexchange.panel.supporter.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.*;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.*;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.supporter.model.Supporter;
import me.yukitale.yellowexchange.panel.supporter.model.SupporterSupportPreset;
import me.yukitale.yellowexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.yellowexchange.panel.supporter.repository.SupporterSupportPresetsRepository;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@Controller
@RequestMapping(value = "/supporter")
@PreAuthorize("hasRole('ROLE_SUPPORTER')")
public class SupporterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private UserService userService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/supporter/users");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/supporter/users");
    }

    @GetMapping(value = "deposits")
    public String depositsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        userDepositRepository.markAllAsViewed();

        model.addAttribute("deposits", userDepositRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));

        return "panel/supporter/deposits";
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

        return "panel/supporter/withdraw";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        userKycRepository.markAllAsViewed();

        List<UserKyc> userKyc = userKycRepository.findAllOrderByDate();

        model.addAttribute("user_kyc", userKyc);

        return "panel/supporter/allkyc";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        List<UserLog> userLogs = userLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 200));

        model.addAttribute("user_logs", userLogs);

        return "panel/supporter/logs";
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
                user = userRepository.findByRoleTypeAndEmail(UserRole.UserRoleType.ROLE_USER, email.toLowerCase()).orElse(null);
            }
            if (user != null) {
                users.add(user);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByRoleTypeAndLastOnlineGreaterThan(UserRole.UserRoleType.ROLE_USER, lastOnline) / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByRoleTypeAndLastOnlineGreaterThanOrderByLastActivityDesc(UserRole.UserRoleType.ROLE_USER, lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.countByRoleType(UserRole.UserRoleType.ROLE_USER) / size);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, (int) size);
                users = userRepository.findAllByRoleTypeOrderByLastActivityDesc(UserRole.UserRoleType.ROLE_USER, pageable);
            }
        }

        model.addAttribute("users", users);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "panel/supporter/users";
    }

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        Supporter supporter = supporterRepository.findByUserId(user.getId()).orElseThrow();

        List<SupporterSupportPreset> supportPresets = supporterSupportPresetsRepository.findAllBySupporterId(supporter.getId());

        model.addAttribute("supporter", supporter);

        model.addAttribute("support_presets", supportPresets);

        return "panel/supporter/support-presets";
    }

    //todo: оптимизация
    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model, @RequestParam("id") long userId, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleType() != UserRole.UserRoleType.ROLE_USER) {
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

        return "panel/supporter/user-edit";
    }

    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email, HttpServletRequest request, @RequestParam(name = "panel_lang", required = false) String panelLang) {
        User user = userService.addUserAttribute(authentication, model);
        userService.addPanelLangAttribute(model, request, panelLang);

        addUnviewedCounterAttributes(model);

        Supporter supporter = supporterRepository.findByUserId(user.getId()).orElse(null);

        if (supporter.isSupportPresetsEnabled()) {
            List<SupporterSupportPreset> supportPresets = supporterSupportPresetsRepository.findAllBySupporterId(supporter.getId());

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

        return "panel/supporter/support";
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
