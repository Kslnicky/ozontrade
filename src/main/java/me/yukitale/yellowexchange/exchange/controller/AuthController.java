package me.yukitale.yellowexchange.exchange.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.yukitale.yellowexchange.exchange.data.EmailPasswordRecovery;
import me.yukitale.yellowexchange.exchange.data.EmailRegistration;
import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.model.user.UserEmailConfirm;
import me.yukitale.yellowexchange.exchange.repository.user.UserEmailConfirmRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserRepository;
import me.yukitale.yellowexchange.exchange.service.EmailService;
import me.yukitale.yellowexchange.exchange.service.UserDetailsServiceImpl;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.model.AdminSettings;
import me.yukitale.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.service.DomainService;
import me.yukitale.yellowexchange.security.auth.captcha.CachedCaptcha;
import me.yukitale.yellowexchange.security.auth.captcha.CaptchaService;
import me.yukitale.yellowexchange.security.auth.utils.JwtUtils;
import me.yukitale.yellowexchange.utils.DataValidator;
import me.yukitale.yellowexchange.utils.GeoUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEmailConfirmRepository userEmailConfirmRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping(value = "signup")
    public String signupController(HttpServletRequest request, HttpServletResponse response, Authentication authentication, Model model, @RequestParam(value = "ref", required = false) String ref,
                                   @RequestParam(value = "promo", required = false) String promo, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang, @RequestParam(value = "error", required = false) String error) {
        if (userService.isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        userService.addLangAttribute(model, request, lang);

        Domain domain = domainService.addDomainAttribute(model, host);

        addCaptcha(request, model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        model.addAttribute("blocked_countries", adminSettings.getBlockedCountries());

        model.addAttribute("ref", ref);

        model.addAttribute("promo", StringUtils.isBlank(promo) || !DataValidator.isPromocodeValided(promo) ? "" : promo);

        model.addAttribute("signup_promo_enabled", domain == null ? adminSettings.isSignupPromoEnabled() : domain.isSignupPromoEnabled());

        model.addAttribute("signup_ref_enabled", domain == null ? adminSettings.isSignupRefEnabled() : domain.isSignupRefEnabled());

        model.addAttribute("fbpixel", domain == null ? -1 : domain.getFbpixel());

        model.addAttribute("promo_show_enabled", domain != null && domain.isPromoPopupEnabled() && domain.isPromoEnabled());

        model.addAttribute("error", error);

        return "exchange/sign/signup";
    }

    @GetMapping(value = "signin")
    public String signinController(HttpServletRequest request, Authentication authentication, Model model, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        if (userService.isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }
        userService.addLangAttribute(model, request, lang);

        domainService.addDomainAttribute(model, host);

        addCaptcha(request, model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        model.addAttribute("blocked_countries", adminSettings.getBlockedCountries());

        return "exchange/sign/signin";
    }

    @GetMapping(value = "signin-2fa")
    public String signin2faController(HttpServletRequest request, Authentication authentication, @RequestParam(value = "token", required = false) String token, Model model, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        if (token == null || token.isEmpty()) {
            return "redirect:signin";
        }

        if (userService.isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        try {
            jwtUtils.getEmailAndPasswordFromJwtToken(token);
        } catch (Exception ex) {
            return "redirect:signin";
        }

        userService.addLangAttribute(model, request, lang);

        domainService.addDomainAttribute(model, host);

        model.addAttribute("token", token);

        return "exchange/sign/signin-2fa";
    }

    @GetMapping(value = "forgot-password")
    public String forgotPasswordController(Authentication authentication, Model model, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "lang", required = false) String lang) {
        if (userService.isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        userService.addLangAttribute(model, request, lang);

        domainService.addDomainAttribute(model, host);

        addCaptcha(request, model);

        return "exchange/sign/forgot-password";
    }

    @GetMapping(value = "email")
    public String confirmEmailController(@RequestParam("action") String action, @RequestParam("hash") String hash, @RequestParam(value = "user_id", required = false, defaultValue = "null") String userIdString) {
        if (action.equals("registration")) {
            EmailRegistration emailRegistration = emailService.getEmailRegistration(hash);
            if (emailRegistration == null) {
                return "redirect:signup?error=not_found";
            }

            if (userRepository.existsByEmail(emailRegistration.getEmail().toLowerCase())) {
                return "redirect:signup?error=already_exists";
            }

            String countryCode = "NO";
            try {
                GeoUtil.GeoData geoData = GeoUtil.getGeo(emailRegistration.getRegIp());
                if (geoData != null && !StringUtils.isBlank(countryCode)) {
                    countryCode = geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode();
                }
            } catch (Exception ignored) {}

            Domain domain = domainRepository.findByName(emailRegistration.getDomainName()).orElse(null);
            userService.createUser(emailRegistration.getReferrer(), domain, emailRegistration.getDomainName(), emailRegistration.getEmail(), emailRegistration.getPassword(),
                    emailRegistration.getRegIp(), emailRegistration.getPlatform(), emailRegistration.getPromocodeName(), emailRegistration.getRefCode(), true, countryCode);

            emailService.removeEmailRegistration(hash);

            return "redirect:signin";
        } else if (action.equals("confirmation")) {
            if (userIdString.equals("null")) {
                return "redirect:signup";
            }

            long userId = -1;
            try {
                userId = Long.parseLong(userIdString);
            } catch (Exception ex) {
                return "redirect:signup";
            }

            if (userId <= 0) {
                return "redirect:signup";
            }

            UserEmailConfirm emailConfirm = userEmailConfirmRepository.findByUserIdAndHash(userId, hash).orElse(null);
            if (emailConfirm == null) {
                return "redirect:profile/wallet";
            }

            User user = emailConfirm.getUser();
            if (!user.isEmailConfirmed()) {
                user.setEmailConfirmed(true);
                userRepository.save(user);
            }

            userEmailConfirmRepository.deleteById(emailConfirm.getId());

            emailService.removeEmailRegistration(hash);

            return "redirect:profile/wallet";
        } else if (action.equals("password_recovery")) {
            EmailPasswordRecovery emailPasswordRecovery = emailService.getEmailPasswordRecovery(hash);
            if (emailPasswordRecovery == null) {
                return "redirect:signin?error=password_recovery_not_found";
            }

            User user = userRepository.findByEmail(emailPasswordRecovery.getEmail()).orElse(null);
            if (user == null) {
                return "redirect:signin?error=user_not_found";
            }

            user.setPassword(emailPasswordRecovery.getPassword());

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());

            emailService.removeEmailPasswordRecovery(hash);

            return "redirect:signin";
        }

        return "redirect:signup";
    }

    private void addCaptcha(HttpServletRequest request, Model model) {
        String sessionKey = request.getSession().getId();

        Optional<CachedCaptcha> captcha = captchaService.getCaptcha(sessionKey);
        model.addAttribute("captcha", captcha.get().getBase64());
    }
}
