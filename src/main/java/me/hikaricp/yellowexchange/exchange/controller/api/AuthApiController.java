package me.hikaricp.yellowexchange.exchange.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.hikaricp.yellowexchange.exchange.model.user.User;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRepository;
import me.hikaricp.yellowexchange.exchange.service.EmailService;
import me.hikaricp.yellowexchange.exchange.service.UserDetailsImpl;
import me.hikaricp.yellowexchange.exchange.service.UserService;
import me.hikaricp.yellowexchange.panel.admin.model.AdminEmailSettings;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminEmailSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.common.repository.DomainRepository;
import me.hikaricp.yellowexchange.security.auth.captcha.CachedCaptcha;
import me.hikaricp.yellowexchange.security.auth.captcha.CaptchaService;
import me.hikaricp.yellowexchange.security.auth.utils.JwtUtils;
import me.hikaricp.yellowexchange.security.auth.utils.ServletUtil;
import me.hikaricp.yellowexchange.utils.DataValidator;
import me.hikaricp.yellowexchange.utils.GeoUtil;
import me.hikaricp.yellowexchange.utils.GoogleUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login-2fa")
    public ResponseEntity<?> loginUser(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String token = body.get("token").toString();
        String email;
        String password;
        try {
            Pair<String, String> userDataPair = jwtUtils.getEmailAndPasswordFromJwtToken(token);
            email = userDataPair.getFirst();
            password = userDataPair.getSecond();
        } catch (Exception ex) {
            return ResponseEntity.ok("error");
        }

        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (!user.getPassword().equals(password)) {
            return ResponseEntity.ok("wrong_password");
        }

        String code = body.get("code").toString();
        if (!code.equals(GoogleUtil.getTOTPCode(user.getTwoFactorCode()))) {
            return ResponseEntity.ok("wrong_code");
        }

        return authenticate(user, request);
    }

    private ResponseEntity<String> authenticate(User user, HttpServletRequest request) {
        Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(user.getEmail().toLowerCase(), user.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        user.setAuthCount(user.getAuthCount() + 1);
        user.setLastLogin(System.currentTimeMillis());

        userRepository.save(user);

        userService.createAction(user, request, "Authorized", true);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString()).body("success");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> body, HttpServletRequest request, @RequestHeader(value = "host") String domainName) {
        String sessionKey = request.getSession().getId();
        Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
        if (captchaOptional.isEmpty()) {
            return ResponseEntity.ok("bad_captcha");
        }

        String captchaAnswer = (String) body.get("captcha");
        if (!captchaOptional.get().getAnswer().equals(captchaAnswer)) {
            return ResponseEntity.ok("incorrect_captcha:;" + captchaOptional.get().getBase64());
        }

        String email = (String) body.get("email");
        email = email.toLowerCase();

        if (!DataValidator.isEmailValided(email)) {
            return ResponseEntity.ok("invalid_email");
        }

        String password = (String) body.get("password");
        if (!DataValidator.isPasswordValided(password)) {
            return ResponseEntity.ok("invalid_password");
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.ok("email_already_exists");
        }

        domainName = domainName.toLowerCase();
        domainName = domainName.startsWith("www.") ? domainName.replaceFirst("www\\.", "") : domainName;

        String promocodeName = (String) body.get("promocode");
        if (!DataValidator.isPromocodeValided(promocodeName)) {
            promocodeName = null;
        }

        String refCode = String.valueOf(body.get("referral_code"));

        String platform = ServletUtil.getPlatform(request);
        String regIp = ServletUtil.getIpAddress(request);
        String countryCode = "NO";
        try {
            GeoUtil.GeoData geoData = GeoUtil.getGeo(regIp);
            if (geoData != null && !StringUtils.isBlank(countryCode)) {
                countryCode = geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode();
            }
        } catch (Exception ignored) {}

        if (countryCode.equalsIgnoreCase("RU") || countryCode.equalsIgnoreCase("BY") || countryCode.equalsIgnoreCase("KZ")) {
            return ResponseEntity.ok("blocked_country");
        }

        Domain domain = domainRepository.findByName(domainName).orElse(null);

        String referrer = WebUtils.getCookie(request, "referrer") == null ? "" : WebUtils.getCookie(request, "referrer").getValue();

        User user = null;
        boolean emailRequiredConfirm = false;
        boolean registered = false;
        //todo: нормально сделать тут все
        if (domain != null && domain.isEmailEnabled() && domain.isEmailValid()) {
            if (domain.isEmailRequiredEnabled()) {
                emailService.createEmailRegistration(referrer, domain, email, password, domainName, platform, regIp, promocodeName, refCode);
                emailRequiredConfirm = true;
                registered = true;
            } else {
                user = userService.createUser(referrer, domain, domainName, email, password, regIp, platform, promocodeName, refCode, false, countryCode);
                emailService.createEmailConfirmation(domain, email, domainName, user);
                registered = true;
            }
        } else if (domain == null) {
            AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
            if (adminEmailSettings.isEmailEnabled() && adminEmailSettings.isEmailValid()) {
                if (adminEmailSettings.isEmailRequiredEnabled()) {
                    emailService.createEmailRegistration(referrer, null, email, password, domainName, platform, regIp, promocodeName, refCode);
                    emailRequiredConfirm = true;
                    registered = true;
                } else {
                    user = userService.createUser(referrer, null, domainName, email, password, regIp, platform, promocodeName, refCode, true, countryCode);
                    emailService.createEmailConfirmation(null, email, domainName, user);
                    registered = true;
                }
            }
        }

        if (!registered) {
            user = userService.createUser(referrer, domain, domainName, email, password, regIp, platform, promocodeName, refCode, true, countryCode);
        }

        captchaService.removeCaptchaCache(sessionKey);

        if (emailRequiredConfirm) {
            return ResponseEntity.ok("email_confirm");
        }

        return authenticate(user, request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String sessionKey = request.getSession().getId();
        Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
        if (captchaOptional.isEmpty()) {
            return ResponseEntity.ok("bad_captcha");
        }

        String captchaAnswer = (String) body.get("captcha");
        if (!captchaOptional.get().getAnswer().equals(captchaAnswer)) {
            return ResponseEntity.ok("incorrect_captcha:;" + captchaOptional.get().getBase64());
        }

        String email = (String) body.get("email");
        email = email.toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (!user.getPassword().equals(body.get("password"))) {
            return ResponseEntity.ok("wrong_password");
        }

        if (user.isTwoFactorEnabled()) {
            String token = jwtUtils.generateTokenFromEmailAndPassword(user.getEmail(), user.getPassword());
            return ResponseEntity.ok("jwt_two_factor: " + token);
        }

        captchaService.removeCaptchaCache(sessionKey);

        return authenticate(user, request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(HttpServletRequest request, @RequestBody Map<String, String> data) {
        String sessionKey = request.getSession().getId();
        Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
        if (captchaOptional.isEmpty()) {
            return ResponseEntity.ok("bad_captcha");
        }

        String captchaAnswer = String.valueOf(data.get("captcha"));
        if (!captchaOptional.get().getAnswer().equals(captchaAnswer)) {
            return ResponseEntity.ok("incorrect_captcha:;" + captchaOptional.get().getBase64());
        }

        String email = String.valueOf(data.get("email")).toLowerCase();
        if (!DataValidator.isEmailValided(email)) {
            return ResponseEntity.ok("user_not_found");
        }

        if (emailService.hasEmailPasswordRecovery(email)) {
            return ResponseEntity.ok("already_exists");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok("user_not_found");
        }

        if (!user.isEmailConfirmed()) {
            return ResponseEntity.ok("email_not_confirmed");
        }

        emailService.createEmailPasswordRecovery(user);

        captchaService.removeCaptchaCache(sessionKey);

        return ResponseEntity.ok("success");
    }

    @GetMapping("/logout")
    public RedirectView logoutUser(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(jwtUtils.getJwtCookie())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
        request.getSession().invalidate();
        return new RedirectView("/signin");
    }
}
