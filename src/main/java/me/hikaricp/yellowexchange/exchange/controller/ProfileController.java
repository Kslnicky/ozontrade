package me.hikaricp.yellowexchange.exchange.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.service.CoinService;
import me.hikaricp.yellowexchange.exchange.service.UserService;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCryptoLendingRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.model.CryptoLending;
import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.common.model.Promocode;
import me.hikaricp.yellowexchange.panel.common.repository.PromocodeRepository;
import me.hikaricp.yellowexchange.panel.common.service.DomainService;
import me.hikaricp.yellowexchange.panel.worker.model.FastPump;
import me.hikaricp.yellowexchange.panel.worker.model.StablePump;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.panel.worker.repository.*;
import me.hikaricp.yellowexchange.panel.worker.service.WorkerService;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import me.hikaricp.yellowexchange.utils.MyDecimal;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping(value = "/profile")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER') || hasRole('ROLE_MANAGER')")
public class ProfileController {

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private WorkerCryptoLendingRepository workerCryptoLendingRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private UserCryptoLendingRepository userCryptoLendingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private DomainService domainService;

    private final List<Pair<Double, String>> limits = Collections.synchronizedList(new ArrayList<>());
    
    private long limitsLastUpdate;
    @Autowired
    private PromocodeRepository promocodeRepository;

    @GetMapping("/")
    public RedirectView indexController() {
        return new RedirectView("/profile/wallet");
    }

    @GetMapping("")
    public RedirectView emptyController() {
        return new RedirectView("/profile/wallet");
    }

    @GetMapping("/wallet")
    public String profileController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang, @RequestParam(name = "action", required = false) String action) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.addBalancesJsonAttribute(user, model);

        coinService.addCoinsJsonAttribute(model);

        coinService.addDepositCoinsJsonAttribute(model, user);

        String commission = "0%";

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (!userSettings.getWithdrawCommission().startsWith("-1")) {
            commission = userSettings.getWithdrawCommission();
        } else if (user.getWorker() != null) {
            commission = workerCoinSettingsRepository.findByWorkerId(user.getWorker().getId()).orElse(null).getWithdrawCommission();
        } else {
            commission = adminCoinSettingsRepository.findFirst().getWithdrawCommission();
        }

        boolean fiatWithdraw = false;
        if (domain != null) {
            fiatWithdraw = domain.isFiatWithdrawEnabled();
        } else if (user.getWorker() != null) {
            fiatWithdraw = workerSettingsRepository.findByWorkerId(user.getWorker().getId()).get().isFiatWithdrawEnabled();
        } else {
            fiatWithdraw = adminSettingsRepository.findFirst().isFiatWithdrawEnabled();
        }

        model.addAttribute("withdraw_commission", commission);

        model.addAttribute("fiat_withdraw", fiatWithdraw);

        model.addAttribute("action", action);

        model.addAttribute("promo_show_enabled", domain != null && domain.isPromoPopupEnabled());

        if (!user.isPromoActivatedShowed()) {
            Promocode promocode = promocodeRepository.findByName(user.getPromocode()).orElse(null);

            if (promocode != null) {
                model.addAttribute("show_promo_activated", true);

                model.addAttribute("show_promo_text", promocode.getText());
            } else {
                model.addAttribute("show_promo_activated", false);
            }

            user.setPromoActivatedShowed(true);

            userRepository.save(user);
        } else {
            model.addAttribute("show_promo_activated", false);
        }

        userService.createAction(user, request, "Visited the Profile (Wallet) page", true);

        return "exchange/profile/profile";
    }

    @GetMapping(value = "trading")
    public String tradingController(Model model, Authentication authentication, HttpServletRequest request, @RequestParam(value = "currency", required = false) String coinSymbol,
                                    @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        Coin selectedCoin = StringUtils.isBlank(coinSymbol) ? coinRepository.findFirst() : coinRepository.findBySymbol(coinSymbol).orElseGet(() -> coinRepository.findFirst());

        if (selectedCoin.getSymbol().equals("USDT")) {
            selectedCoin = coinRepository.findFirst();
        }

        Coin usdtCoin = coinRepository.findBySymbol("USDT").get();

        MyDecimal availableCoin = new MyDecimal(userService.getBalance(user, selectedCoin.getSymbol()));
        MyDecimal availableUsdt = new MyDecimal(userService.getBalance(user, usdtCoin.getSymbol()), true);

        model.addAttribute("available_coin", availableCoin.toString(8));

        model.addAttribute("available_usdt", availableUsdt.getValue().doubleValue() < 0.01 ? "0.00" : availableUsdt.toString(2));

        model.addAttribute("coins", coinRepository.findAll());

        model.addAttribute("selected_coin", selectedCoin);

        model.addAttribute("usdt", usdtCoin);

        model.addAttribute("coin_service", coinService);

        Worker worker = user.getWorker();
        List<FastPump> workerFastPumps = worker == null ? null : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), selectedCoin.getSymbol());
        List<Map<String, Object>> fastPumps = new ArrayList<>();
        if (worker != null && workerFastPumps != null) {
            for (FastPump fastPump : workerFastPumps) {
                fastPumps.add(new HashMap<>() {{
                    put("time", fastPump.getTime() / 1000);
                    put("percent", fastPump.getPercent());
                }});
            }
        }

        model.addAttribute("fast_pumps_json", fastPumps.isEmpty() ? "" : JsonUtil.writeJson(fastPumps));

        long time = -1;
        List<Double> activePumps = new ArrayList<>();
        if (workerFastPumps != null && !workerFastPumps.isEmpty()) {
            time = workerFastPumps.get(workerFastPumps.size() - 1).getTime();
            workerFastPumps.forEach(pump -> activePumps.add(pump.getPercent()));
        }

        model.addAttribute("fast_pumps_active_json", JsonUtil.writeJson(activePumps));

        model.addAttribute("fast_pumps_end_time", time);

        double stablePumpPercent = 0;
        StablePump stablePump = worker == null ? null : stablePumpRepository.findByWorkerIdAndCoinSymbol(worker.getId(), selectedCoin.getSymbol()).orElse(null);
        if (stablePump != null) {
            stablePumpPercent = stablePump.getPercent();
        }

        model.addAttribute("stable_pump_percent", stablePumpPercent);

        userService.createAction(user, request, "Visited the Trading page", true);

        return "exchange/trading";
    }

    @GetMapping("/security")
    public String securityController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Security page", true);

        return "exchange/profile/security";
    }


    @GetMapping("/verification")
    public String verificationController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        UserKyc userKyc = userKycRepository.findByUserId(user.getId()).orElse(null);

        model.addAttribute("domain", domain);

        model.addAttribute("user_kyc", userKyc);

        userService.createAction(user, request, "Visited the Verification page", true);

        return "exchange/profile/verification";
    }

    @GetMapping("/verification-lvl1")
    public String verificationLvl1Controller(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        if (user.isFakeKycLv1() || user.isFakeKycLv2() || userKycRepository.findByUserId(user.getId()).isPresent()) {
            return "redirect:../profile/verification";
        }

        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Verification Lv. 1 page", true);

        return "exchange/profile/verification-lvl1";
    }

    @GetMapping("/verification-lvl2")
    public String verificationLvl2Controller(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        if (user.isFakeKycLv2()) {
            return "redirect:../profile/verification";
        }

        UserKyc userKyc = userKycRepository.findByUserId(user.getId()).orElse(null);
        if (!user.isFakeKycLv1() && (userKyc == null || !userKyc.isAcceptedLv1() || userKyc.isAcceptedLv2() || userKyc.getLevel() == 2)) {
            return "redirect:../profile/verification";
        }

        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        model.addAttribute("user_kyc", userKyc);

        userService.createAction(user, request, "Visited the Verification Lv. 2 page", true);

        return "exchange/profile/verification-lvl2";
    }

    @GetMapping("/settings")
    public String settingsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang, @RequestParam(name = "logs_page", required = false) String logsPage) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        int page = 1;
        try {
            page = StringUtils.isBlank(logsPage) ? 1 : Integer.parseInt(logsPage);
        } catch (Exception ignored) {}

        long logsCount = userLogRepository.countByUserIdAndForUser(user.getId(), true);
        int maxPage = (int) Math.ceil(logsCount / 20D);

        if (page < 1) {
            page = 1;
        } else if (page > maxPage) {
            page = maxPage;
        }

        //List<UserLog> userLogList = userLogRepository.findByUserIdAndForUserOrderByIdDesc(user.getId(), true, PageRequest.of(page - 1, 20));

        //model.addAttribute("user_logs", userLogList);

        model.addAttribute("page", page);

        model.addAttribute("max_page", maxPage);

        userService.createAction(user, request, "Visited the Settings page", true);

        return "exchange/profile/settings";
    }

    @GetMapping("/api")
    public String apiController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        model.addAttribute("api_keys", userApiKeyRepository.findByUserIdOrderByIdDesc(user.getId()));

        userService.createAction(user, request, "Visited the API Management page", true);

        return "exchange/profile/api";
    }

    @GetMapping("/promo")
    public String promocodesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Promo Codes page", true);

        return "exchange/profile/promocodes";
    }

    @GetMapping("/referral")
    public String referralController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Referral Program page", true);

        return "exchange/profile/referral";
    }

    @GetMapping("/wallet-connect")
    public String walletConnectController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        List<UserWalletConnect> userWalletConnects = userWalletConnectRepository.findByUserIdOrderByIdDesc(user.getId());

        model.addAttribute("user_wallet_connects", userWalletConnects);

        userService.createAction(user, request, "Visited the Wallet Connect page", true);

        return "exchange/profile/wallet-connect";
    }

    @GetMapping(value = "/staking")
    public String stakingController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addBalancesJsonAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        List<? extends CryptoLending> cryptoLendings = user != null && user.getWorker() != null ? workerCryptoLendingRepository.findAllByWorkerId(user.getWorker().getId()) : adminCryptoLendingRepository.findAll();
        List<Map<String, Object>> lendings = new ArrayList<>();
        for (CryptoLending cryptoLending : cryptoLendings) {
            Coin coin = coinRepository.findBySymbol(cryptoLending.getCoinSymbol()).orElse(null);
            if (coin == null) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", cryptoLending.getId());
            map.put("symbol", cryptoLending.getCoinSymbol());
            map.put("icon", coin.getIcon());
            map.put("title", coin.getTitle());
            map.put("percent_7", cryptoLending.getPercent7days());
            map.put("percent_14", cryptoLending.getPercent14days());
            map.put("percent_30", cryptoLending.getPercent30days());
            map.put("percent_90", cryptoLending.getPercent90days());
            map.put("percent_180", cryptoLending.getPercent180days());
            map.put("percent_360", cryptoLending.getPercent360days());
            map.put("min", cryptoLending.getMinAmount());
            map.put("max", cryptoLending.getMaxAmount());

            lendings.add(map);
        }

        List<Map<String, Object>> userLendings = new ArrayList<>();

        if (user != null) {
            for (UserCryptoLending lending : userCryptoLendingRepository.findAllByUserIdOrderByIdDesc(user.getId())) {
                Coin coin = coinRepository.findBySymbol(lending.getCoinSymbol()).orElse(null);
                if (coin == null) {
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("id", lending.getId());
                map.put("coin", lending.getCoinSymbol());
                map.put("icon", coin.getIcon());
                map.put("title", coin.getTitle());
                map.put("days", lending.getDays());
                map.put("percent", lending.getPercent());
                map.put("open_time", lending.formattedOpenTime());
                map.put("close_time", lending.formattedCloseTime());
                map.put("amount", lending.formattedAmount());
                map.put("realtime_profit", lending.formattedRealtimeProfit());
                map.put("is_expired", lending.isExpired());

                userLendings.add(map);
            }
        }

        model.addAttribute("user_crypto_lendings", userLendings);

        model.addAttribute("crypto_lendings_json", JsonUtil.writeJson(lendings));

        userService.createAction(user, request, "Visited the Staking page", true);

        return "exchange/staking";
    }

    @GetMapping(value = "/p2p")
    public String p2pController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        double price = this.coinService.getPrice("BTC");
        if (price > 0) {
            if ((System.currentTimeMillis() - limitsLastUpdate) / 1000 / 60 > 3) {
                this.limitsLastUpdate = System.currentTimeMillis();
                this.limits.clear();
                for (int i = 0; i < 10; i++) {
                    this.limits.add(Pair.of(price + (ThreadLocalRandom.current().nextDouble(-(price / 100D), price / 100D)), new MyDecimal(ThreadLocalRandom.current().nextDouble(0, 0.3)).toString(8)));
                }
            }
        }

        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the P2P page", true);

        Worker worker = workerService.getUserWorker(user, host);

        double priceDifference = worker == null ? 0 : this.coinService.getIfWorkerPrice(worker, "BTC") - price;

        List<Pair<MyDecimal, String>> userLimits = Collections.synchronizedList(new ArrayList<>());

        for (Pair<Double, String> limit : this.limits) {
            userLimits.add(Pair.of(new MyDecimal(limit.getFirst() + priceDifference, true), limit.getSecond()));
        }

        model.addAttribute("p2p_limits", userLimits);

        return "exchange/p2p";
    }

    @GetMapping("/history")
    public String historyController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang, @RequestParam(name = "type", required = false, defaultValue = "all") String type) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        if (!type.equals("withdraw") && !type.equals("deposit") && !type.equals("transfer") && !type.equals("earning")) {
            type = "all";
        }

        List<UserTransaction> userTransactions = userTransactionRepository.findByUserIdOrderByIdDesc(user.getId());
        List<UserTransaction> transactions = new ArrayList<>();

        for (UserTransaction userTransaction : userTransactions) {
            if (type.equals("withdraw") && userTransaction.getType() == UserTransaction.Type.WITHDRAW) {
                transactions.add(userTransaction);
            } else if (type.equals("deposit") && userTransaction.getType() == UserTransaction.Type.DEPOSIT) {
                transactions.add(userTransaction);
            } else if (type.equals("transfer") && (userTransaction.getType() == UserTransaction.Type.TRANSFER_IN || userTransaction.getType() == UserTransaction.Type.TRANSFER_OUT)) {
                transactions.add(userTransaction);
            } else if (type.equals("earning") && (userTransaction.getType() == UserTransaction.Type.BONUS || userTransaction.getType() == UserTransaction.Type.PROMO
                    || userTransaction.getType() == UserTransaction.Type.STAKE || userTransaction.getType() == UserTransaction.Type.UNSTAKE
                    || userTransaction.getType() == UserTransaction.Type.CRYPTO_LENDING_STAKE || userTransaction.getType() == UserTransaction.Type.CRYPTO_LENDING_UNSTAKE)) {
                transactions.add(userTransaction);
            } else if (type.equals("all")) {
                transactions.add(userTransaction);
            }
        }

        model.addAttribute("type", type);

        model.addAttribute("transactions", transactions);

        userService.createAction(user, request, "Visited the Transactions history page", true);

        return "exchange/profile/history";
    }

    @GetMapping("/deposit-verification")
    public String depositVerificationController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang, @RequestParam(name = "action", required = false) String action) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        coinService.addDepositCoinsJsonAttribute(model, user);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Setup Deposit verification (Select Coin)", false);

        return "exchange/profile/deposit-verification";
    }
}
