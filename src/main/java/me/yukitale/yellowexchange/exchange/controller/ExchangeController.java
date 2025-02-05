package me.yukitale.yellowexchange.exchange.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.model.user.UserCryptoLending;
import me.yukitale.yellowexchange.exchange.model.user.UserFavoriteCoins;
import me.yukitale.yellowexchange.exchange.model.user.UserSettings;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserCryptoLendingRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserFavoriteCoinsRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserSettingsRepository;
import me.yukitale.yellowexchange.exchange.service.CoinService;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminCryptoLendingRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminLegalSettingsRepository;
import me.yukitale.yellowexchange.panel.common.model.CryptoLending;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.model.LegalSettings;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.service.DomainService;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.repository.*;
import me.yukitale.yellowexchange.panel.worker.service.WorkerService;
import me.yukitale.yellowexchange.utils.JsonUtil;
import me.yukitale.yellowexchange.utils.MyDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class ExchangeController {

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private WorkerLegalSettingsRepository workerLegalSettingsRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private UserFavoriteCoinsRepository userFavoriteCoinsRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private WorkerService workerService;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;
    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @GetMapping(value = "robots.txt")
    public ResponseEntity<String> indexController(@RequestHeader("host") String host) {
        Domain domain = domainRepository.findByName(host.toLowerCase()).orElse(null);
        if (domain == null) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body("");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .body(domain.getRobotsTxt());
    }

    @GetMapping(value = "")
    public String indexController(Model model, Authentication authentication, HttpServletRequest request, HttpServletResponse response,
                                  @RequestParam(value = "promo", required = false) String promo, @RequestParam(value = "fbpixel", required = false) String fbpixel,
                                  @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        Worker worker = workerService.addUserWorkerAttribute(user, host.toLowerCase(), model);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Home page", true);

        List<Coin> coins = coinRepository.findAll();

        List<Map<String, String>> newCoins = new ArrayList<>();

        int i = 0;
        for (Coin coin : coins) {
            if (i == 8) {
                break;
            }

            if (coin.getSymbol().equals("USDT") || coin.getSymbol().equals("USDC") || coin.getSymbol().equals("BUSD")) {
                continue;
            }

            Map<String, String> newCoin = new HashMap<>();

            newCoin.put("symbol", coin.getSymbol());
            newCoin.put("icon", coin.getIcon());
            newCoin.put("price", new MyDecimal(coinService.getIfWorkerPrice(worker, coin.getSymbol())).toPrice());

            double priceChangePercent = coinService.getWorkerPriceChangePercent(worker, coin.getSymbol());

            newCoin.put("up_price", String.valueOf(priceChangePercent >= 0));

            newCoin.put("price_change_percent", new MyDecimal(priceChangePercent).toString(2));

            newCoins.add(newCoin);

            i++;
        }

        model.addAttribute("coins", newCoins);

        if (fbpixel != null) {
            Cookie cookie = new Cookie("fbpixel", fbpixel);
            response.addCookie(cookie);
        }

        return "exchange/index";
    }

    @GetMapping(value = "/card")
    public String cardController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Our Card page", true);

        return "exchange/card";
    }

    @GetMapping(value = "/swap")
    public String swapController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        Worker worker = workerService.addUserWorkerAttribute(user, host.toLowerCase(), model);
        userService.addLangAttribute(model, request, lang);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.addBalancesJsonAttribute(user, model);

        List<Coin> coins = coinRepository.findAll();

        coinService.addCoinsJsonAttribute(model);

        double priceFrom = coinService.getIfWorkerPrice(worker, coins.get(0).getSymbol());
        double priceTo = coinService.getIfWorkerPrice(worker, coins.get(1).getSymbol());

        double price = priceFrom / priceTo;

        model.addAttribute("min_amount", new MyDecimal(1D / priceFrom).toString(8));

        model.addAttribute("price", new MyDecimal(price).toString(8));

        model.addAttribute("price_change_percent", coinService.getWorkerPriceChangePercentFormatted(worker, coins.get(0).getSymbol()));

        userService.createAction(user, request, "Visited the Swap page", true);

        return "exchange/swap";
    }

    @GetMapping(value = "/earn")
    public String earnController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Earn page", true);

        return "exchange/earn";
    }

    @GetMapping(value = "/token-listing")
    public String tokenListingController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Token Listing page", true);

        return "exchange/token-listing";
    }

    @GetMapping(value = "/tournament")
    public String tournamentController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Tournament page", true);

        return "exchange/tournament";
    }

    @GetMapping(value = "/tools/market-cap")
    public String marketCapController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Market Cap page", true);

        return "exchange/tools/market-cap";
    }

    @GetMapping(value = "/tools/market-screener")
    public String marketScreenerController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Market Screener page", true);

        return "exchange/tools/market-screener";
    }

    @GetMapping(value = "/tools/cross-rates")
    public String crossRatesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Cross Rates page", true);

        return "exchange/tools/cross-rates";
    }

    @GetMapping(value = "/tools/heat-map")
    public String heatMapController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Currency Heat map page", true);

        return "exchange/tools/heat-map";
    }

    @GetMapping(value = "/tools/technical-analysis")
    public String technicalAnalysisController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Technical Analysis page", true);

        return "exchange/tools/technical-analysis";
    }

    @GetMapping(value = "/privacy-policy")
    public String userAgreementController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the User Agreement page", true);

        return "exchange/privacy-policy";
    }

    @GetMapping(value = "/cookies-policy")
    public String cookiesPolicyController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Cookies Policy page", true);

        return "exchange/cookies-policy";
    }

    @GetMapping(value = "/risk")
    public String riskController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Risk Disclosure Statement page", true);

        return "exchange/risk";
    }

    @GetMapping(value = "/regulatory")
    public String regulatoryController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Regulatory License page", true);

        return "exchange/regulatory";
    }

    @GetMapping(value = "/treatment")
    public String treatmentController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Special Treatment page", true);

        return "exchange/treatment";
    }

    @GetMapping(value = "/law")
    public String lawController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Law Enforcement Requests page", true);

        return "exchange/law";
    }

    @GetMapping(value = "/user-agreement")
    public String privacyPolicyController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        LegalSettings legalSettings = getLegalSettings(domain, user);

        model.addAttribute("user_agreement", legalSettings.getTerms()
                .replace("{domain_url}", (String) model.getAttribute("site_domain"))
                .replace("{domain_name}", (String) model.getAttribute("site_name")));

        userService.createAction(user, request, "Visited the Privacy Policy page", true);

        return "exchange/user-agreement";
    }

    @GetMapping(value = "/aml-policy")
    public String amlPolicyController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        LegalSettings legalSettings = getLegalSettings(domain, user);

        model.addAttribute("aml_policy", legalSettings.getAml()
                .replace("{domain_url}", (String) model.getAttribute("site_domain"))
                .replace("{domain_name}", (String) model.getAttribute("site_name")));

        userService.createAction(user, request, "Visited the AML Policy page", true);

        return "exchange/aml-policy";
    }

    @GetMapping(value = "/about-us")
    public String aboutUsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the About Us page", true);

        return "exchange/about-us";
    }

    @GetMapping(value = "/channel-verification")
    public String channelVerificationController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Channel Verification page", true);

        return "exchange/channel-verification";
    }

    @GetMapping(value = "/fees")
    public String feesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        Worker worker = user == null ? null : user.getWorker();
        if (worker == null && domain != null) {
            worker = domain.getWorker();
        }

        String depositCommission = "1";
        UserSettings userSettings = user == null ? null : userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (userSettings != null && !userSettings.getDepositCommission().startsWith("-1")) {
            depositCommission = userSettings.getDepositCommission();
        } else if (worker != null) {
            depositCommission = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null).getDepositCommission();
        } else {
            depositCommission = adminCoinSettingsRepository.findFirst().getDepositCommission();
        }

        List<? extends DepositCoin> depositCoins;
        if (worker != null) {
            depositCoins = workerDepositCoinRepository.findAllByWorkerId(worker.getId()).stream().toList();
        } else {
            depositCoins = adminDepositCoinRepository.findAll();
        }

        List<Map<String, Object>> coins = new ArrayList<>();
        for (DepositCoin depositCoin : depositCoins) {
            if (coins.stream().anyMatch(map -> map.get("symbol").equals(depositCoin.getSymbol()))) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("symbol", depositCoin.getSymbol());
            map.put("title", depositCoin.getTitle());
            map.put("icon", depositCoin.getIcon());
            map.put("min_deposit", new MyDecimal(depositCoin.getMinDepositAmount()));

            double price = coinService.getPrice(depositCoin.getSymbol());
            if (price > 0) {
                if (depositCommission.endsWith("%")) {
                    map.put("commission", depositCommission);
                } else {
                    double comm = Double.parseDouble(depositCommission) / price;
                    map.put("commission", "~ " + new MyDecimal(comm).toPrice() + " " + depositCoin.getSymbol());
                }
            }

            coins.add(map);
        }

        model.addAttribute("deposit_coins", coins);

        userService.createAction(user, request, "Visited the Fees page", true);

        return "exchange/fees";
    }

    @GetMapping(value = "/corporate-identity")
    public String corporateIdentityController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        String siteName = (String) model.getAttribute("site_name");

        model.addAttribute("site_name_first_letter", siteName.toCharArray()[0]);

        userService.createAction(user, request, "Visited the Corporate Identity page", true);

        return "exchange/corporate-identity";
    }

    @GetMapping(value = "/institutional-services")
    public String institutionalServicesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Institutional Services page", true);

        return "exchange/institutional-services";
    }

    @GetMapping(value = "/bug-bounty")
    public String bugBountyController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        userService.createAction(user, request, "Visited the Bug Bounty page", true);

        return "exchange/bug-bounty";
    }

    @GetMapping(value = "/markets")
    public String marketsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host, @RequestParam(name = "lang", required = false) String lang) {
        User user = userService.addUserAttribute(authentication, model);
        Domain domain = domainService.addDomainAttribute(model, host);
        Worker worker = workerService.addUserWorkerAttribute(user, host.toLowerCase(), model);
        userService.addLangAttribute(model, request, lang);
        userService.addTotalUsdBalanceAttribute(user, model);
        userService.addPageSettingsAttribute(user, domain, model);

        String favoriteCoins = "";

        if (user != null) {
            UserFavoriteCoins userFavoriteCoins = userFavoriteCoinsRepository.findByUserId(user.getId()).orElse(null);

            if (userFavoriteCoins != null) {
                favoriteCoins = userFavoriteCoins.getFavorites();
            }
        }

        Map<String, Map<String, String>> coins = new LinkedHashMap<>();

        for (Coin coin : coinRepository.findAll()) {
            if (coin.getSymbol().equals("USDT")) {
                continue;
            }

            Map<String, String> coinMap = new HashMap<>();

            coinMap.put("symbol", coin.getSymbol());
            coinMap.put("title", coin.getTitle());
            coinMap.put("icon", coin.getIcon());

            double price = coinService.getIfWorkerPrice(worker, coin.getSymbol());

            String priceFormatted = new MyDecimal(coinService.getIfWorkerPrice(worker, coin.getSymbol()), true).toPrice();
            if (priceFormatted.equals("0")) {
                priceFormatted = new MyDecimal(price).toString(8);
            }

            coinMap.put("price", priceFormatted);
            coinMap.put("price_change_percent", coinService.getWorkerPriceChangePercentFormatted(worker, coin.getSymbol()));
            coinMap.put("price_high", new MyDecimal(coinService.getWorkerHighPrice(worker, coin.getSymbol())).toPrice());
            coinMap.put("price_low", new MyDecimal(coinService.getWorkerLowPrice(worker, coin.getSymbol())).toPrice());
            coinMap.put("volume", coinService.getWorkerQuoteVolumeFormatted(worker, coin.getSymbol()));

            coins.put(coin.getSymbol(), coinMap);
        }

        model.addAttribute("user_favorite_coins", favoriteCoins);

        model.addAttribute("coins", JsonUtil.writeJson(coins));

        userService.createAction(user, request, "Visited the Markets page", true);

        return "exchange/markets";
    }

    private LegalSettings getLegalSettings(Domain domain, User user) {
        Worker worker = workerService.getUserWorker(user, domain == null ? null : domain.getName());
        if (worker != null) {
            return workerLegalSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
        }

        return adminLegalSettingsRepository.findFirst();
    }
}
