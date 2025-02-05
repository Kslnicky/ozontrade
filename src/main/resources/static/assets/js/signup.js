document.addEventListener('DOMContentLoaded', function() {
    const emailInputWrapper = $("#email-input-wrapper");
    const emailLabel = $("#email-label");
    const emailInput = $("#email-input");
    const passwordInputWrapper = $("#password-input-wrapper");
    const passwordLabel = $("#password-label");
    const passwordInput = $("#password-input");

    const passwordInputWrapper2 = $("#password-input-wrapper-2");
    const passwordLabel2 = $("#password-label-2");
    const passwordInput2 = $("#password-input-2");
    const showPassword2 = $("#show-password-2");
    const passwordRepeatError = $("#error-password-repeat");

    const promocodeInputWrapper = $("#promo-input-wrapper");
    const promocodeLabel = $("#promo-label");
    const promocodeInput = $("#promo-input");
    const referralInputWrapper = $("#referral-input-wrapper");
    const referralLabel = $("#referral-label");
    const referralInput = $("#referral-input");
    const captchaInputWrapper = $("#captcha-input-wrapper");
    const captchaLabel = $("#captcha-label");
    const captchaInput = $("#captcha-input");
    const allowedSymbolsTrigger = $("#allowed-symbols-trigger");
    const allowedSymbolsPopup = $("#allowed-symbols-popup");
    const errorsSection = $("#errors-section");
    const errorSymbols = $("#error-symbols");
    const errorUpper = $("#error-upper");
    const errorNumber = $("#error-number");
    const errorBlockedSymbols = $("#error-blocked-symbols");
    const errorEmail = $("#error-email");
    const error1 = $("#error-1");
    const error2 = $("#error-2");
    const successPassword = $("#success-password");
    const checkboxPolicyWrapper = $("#checkbox-policy");
    const checkboxCountriesWrapper = $("#checkbox-countries");
    const checkboxPolicy = $("#checkbox-policy-input");
    const checkboxCountries = $("#checkbox-countries-input");
    const showPassword = $("#show-password");
    const promoInputShow = $("#promo-input-show");
    const referralInputShow = $("#referral-input-show");

    var emailValided = false;
    var passwordValided = false;
    var promocodeValided = true;
    var referralValided = true;

    var policyAccepted = false;
    var countriesAccepted = checkboxCountries.closest('.register__form-check__wrapper').css('display') === 'none';

    emailInput.on('focus', function () {
        emailInputWrapper.addClass('v-input--is-focused');
        emailLabel.addClass('v-label--active');
    });

    emailInput.on('blur', function () {
        emailInputWrapper.removeClass('v-input--is-focused');

        const email = emailInput.val();
        if (email.length === 0) {
            emailLabel.removeClass('v-label--active');
        }
    });

    emailInput.on('input', function () {
        const email = emailInput.val();
        if (validateEmail(email)) {
            emailValided = true;
            emailInputWrapper.removeClass('error--text');
            errorEmail.css('display', 'none');
        } else {
            emailValided = false;
            emailInputWrapper.addClass('error--text');
            errorEmail.css('display', 'flex');
        }
    });

    passwordInput.on('focus', function () {
        passwordInputWrapper.addClass('v-input--is-focused');
        passwordLabel.addClass('v-label--active');
    });

    passwordInput.on('blur', function () {
        passwordInputWrapper.removeClass('v-input--is-focused');

        const password = passwordInput.val();
        if (password.length === 0) {
            passwordLabel.removeClass('v-label--active');
        }
    });

    passwordInput.on('input', function () {
        const password = passwordInput.val();

        if (validatePassword(password)) {
            passwordValided = true;
            passwordInputWrapper.removeClass('error--text');
            errorsSection.removeClass('error-red');
            errorsSection.css('display', 'none');
            successPassword.css('display', 'flex');
            errorBlockedSymbols.css('display', 'none');
        } else {
            passwordValided = false;
            passwordInputWrapper.addClass('error--text');
            errorsSection.addClass('error-red');
            errorsSection.css('display',  'flex');
            successPassword.css('display', 'none');

            var oneOfErrors = false;
            if (validatePasswordLength(password)) {
                errorSymbols.css('display', 'none');
            } else {
                oneOfErrors = true;
                errorSymbols.css('display', 'flex');
            }

            if (validatePasswordUppercase(password)) {
                errorUpper.css('display', 'none');
            } else {
                oneOfErrors = true;
                errorUpper.css('display', 'flex');
            }

            if (validateDigit(password)) {
                errorNumber.css('display', 'none');
            } else {
                oneOfErrors = true;
                errorNumber.css('display', 'flex');
            }

            if (!oneOfErrors) {
                errorBlockedSymbols.css('display', 'flex');
            } else {
                errorBlockedSymbols.css('display', 'none');
            }
        }

        const password1 = passwordInput.val();
        const password2 = passwordInput2.val();

        if (password1 !== password2) {
            passwordInputWrapper2.addClass('error--text');
            passwordRepeatError.css('display', '');
        } else {
            passwordInputWrapper2.removeClass('error--text');
            passwordRepeatError.css('display', 'none');
        }
    });

    //password repeat start
    passwordInput2.on('focus', function () {
        passwordInputWrapper2.addClass('v-input--is-focused');
        passwordLabel2.addClass('v-label--active');
    });

    passwordInput2.on('blur', function () {
        passwordInputWrapper2.removeClass('v-input--is-focused');

        const password = passwordInput2.val();
        if (password.length === 0) {
            passwordLabel2.removeClass('v-label--active');
        }
    });

    passwordInput2.on('input', function () {
        const password1 = passwordInput.val();
        const password2 = passwordInput2.val();

        if (password1 !== password2) {
            passwordInputWrapper2.addClass('error--text');
            passwordRepeatError.css('display', '');
        } else {
            passwordInputWrapper2.removeClass('error--text');
            passwordRepeatError.css('display', 'none');
        }
    });
    //password repeat end

    allowedSymbolsTrigger.on('mouseenter', function () {
        allowedSymbolsTrigger.attr('aria-expanded', 'true');
        allowedSymbolsPopup.css('display', 'flex');
    });

    allowedSymbolsTrigger.on('mouseleave', function () {
        allowedSymbolsTrigger.attr('aria-expanded', 'false');
        allowedSymbolsPopup.css('display', 'none');
    });

    captchaInput.on('focus', function () {
        captchaInputWrapper.addClass('v-input--is-focused');
        captchaLabel.addClass('v-label--active');
    });

    captchaInput.on('blur', function () {
        captchaInputWrapper.removeClass('v-input--is-focused');

        const captcha = captchaInput.val();
        if (captcha.length === 0) {
            captchaLabel.removeClass('v-label--active');
        }
    });

    promocodeInput.on('focus', function () {
        promocodeInputWrapper.addClass('v-input--is-focused');
        promocodeLabel.addClass('v-label--active');
    });

    promocodeInput.on('blur', function () {
        promocodeInputWrapper.removeClass('v-input--is-focused');

        const promocode = promocodeInput.val();
        if (promocode.length === 0) {
            promocodeLabel.removeClass('v-label--active');
        }
    });

    promocodeInput.on('input', function () {
        const promocode = promocodeInput.val();
        if (promocode.length === 0 || validatePromocode(promocode)) {
            promocodeValided = true;
            promocodeInputWrapper.removeClass('error--text');
        } else {
            promocodeValided = false;
            promocodeInputWrapper.addClass('error--text');
        }
    });

    promoInputShow.on('click', function () {
        const i = $(promoInputShow).find('i');
        if (promocodeInputWrapper.css('display') === 'none') {
            promocodeInputWrapper.css('display', 'flex');
            promoInputShow.css('color', '#ffc014');
            i.removeClass('mdi-chevron-down');
            i.addClass('mdi-chevron-up');
            i.css('color', '#ffc014');
        } else {
            promocodeInputWrapper.css('display', 'none');
            promoInputShow.css('color', 'hsla(0,0%,100%,.6)');
            i.addClass('mdi-chevron-down');
            i.removeClass('mdi-chevron-up');
            i.css('color', 'hsla(0,0%,100%,.6)');
        }
    });

    referralInput.on('focus', function () {
        referralInputWrapper.addClass('v-input--is-focused');
        referralLabel.addClass('v-label--active');
    });

    referralInput.on('blur', function () {
        referralInputWrapper.removeClass('v-input--is-focused');

        const referral = referralInput.val();
        if (referral.length === 0) {
            referralLabel.removeClass('v-label--active');
        }
    });

    referralInput.on('input', function () {
        const referral = referralInput.val();
        if (referral.length === 0 || validateReferral(referral)) {
            referralValided = true;
            referralInputWrapper.removeClass('error--text');
        } else {
            referralValided = false;
            referralInputWrapper.addClass('error--text');
        }
    });

    referralInputShow.on('click', function () {
        const i = $(referralInputShow).find('i');
        if (referralInputWrapper.css('display') === 'none') {
            referralInputWrapper.css('display', 'flex');
            referralInputShow.css('color', '#ffc014');
            i.removeClass('mdi-chevron-down');
            i.addClass('mdi-chevron-up');
            i.css('color', '#ffc014');
        } else {
            referralInputWrapper.css('display', 'none');
            referralInputShow.css('color', 'hsla(0,0%,100%,.6)');
            i.addClass('mdi-chevron-down');
            i.removeClass('mdi-chevron-up');
            i.css('color', 'hsla(0,0%,100%,.6)');
        }
    });
    
    checkboxPolicy.on('click', function () {
        const i = checkboxPolicyWrapper.find('i');
        if (checkboxPolicy.is(':checked')) {
            policyAccepted = true;
            checkboxPolicyWrapper.removeClass('error--text');
            checkboxPolicyWrapper.removeClass('v-input--is-dirty');
            checkboxPolicyWrapper.addClass('primary--text');
            i.removeClass('material-icons');
            i.removeClass('error--text');
            i.addClass('mdi');
            i.addClass('mdi-check');
            i.addClass('primary--text');
        } else {
            policyAccepted = false;
            checkboxPolicyWrapper.addClass('error--text');
            checkboxPolicyWrapper.addClass('v-input--is-dirty');
            checkboxPolicyWrapper.removeClass('primary--text');
            i.addClass('material-icons');
            i.addClass('error--text');
            i.removeClass('mdi');
            i.removeClass('mdi-check');
            i.removeClass('primary--text');
        }
    });

    checkboxPolicy.on('focus', function () {
        checkboxPolicyWrapper.addClass('v-input--is-focused');
    });

    checkboxPolicy.on('blur', function () {
        checkboxPolicyWrapper.removeClass('v-input--is-focused');
    });


    checkboxCountries.on('click', function () {
        const i = checkboxCountriesWrapper.find('i');
        if (checkboxCountries.is(':checked')) {
            countriesAccepted = true;
            checkboxCountriesWrapper.removeClass('error--text');
            checkboxCountriesWrapper.removeClass('v-input--is-dirty');
            checkboxCountriesWrapper.addClass('primary--text');
            i.removeClass('material-icons');
            i.removeClass('error--text');
            i.addClass('mdi');
            i.addClass('mdi-check');
            i.addClass('primary--text');
        } else {
            countriesAccepted = false;
            checkboxCountriesWrapper.addClass('error--text');
            checkboxCountriesWrapper.addClass('v-input--is-dirty');
            checkboxCountriesWrapper.removeClass('primary--text');
            i.addClass('material-icons');
            i.addClass('error--text');
            i.removeClass('mdi');
            i.removeClass('mdi-check');
            i.removeClass('primary--text');
        }
    });

    checkboxCountries.on('focus', function () {
        checkboxCountriesWrapper.addClass('v-input--is-focused');
    });

    checkboxCountries.on('blur', function () {
        checkboxCountriesWrapper.removeClass('v-input--is-focused');
    });

    showPassword.on('click', function () {
        if (passwordInput.attr('type') === 'password') {
            passwordInput.attr('type', 'text');
            showPassword.removeClass('mdi-eye-off');
            showPassword.addClass('mdi-eye');
        } else {
            passwordInput.attr('type', 'password');
            showPassword.removeClass('mdi-eye');
            showPassword.addClass('mdi-eye-off');
        }
    });

    showPassword2.on('click', function () {
        if (passwordInput2.attr('type') === 'password') {
            passwordInput2.attr('type', 'text');
            showPassword2.removeClass('mdi-eye-off');
            showPassword2.addClass('mdi-eye');
        } else {
            passwordInput2.attr('type', 'password');
            showPassword2.removeClass('mdi-eye');
            showPassword2.addClass('mdi-eye-off');
        }
    });

    function validateEmail(email) {
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
        return emailPattern.test(email);
    }

    function validatePassword(password) {
        const passwordPattern = /^(?=.*[A-Z])(?=.*\d)[A-Za-z\d_!@#$%^&*(),.?":{}|<>]{8,30}$/;
        return passwordPattern.test(password);
    }


    function validatePasswordLength(password) {
        return password.length >= 8 && password.length <= 30;
    }

    function validatePasswordUppercase(password) {
        const uppercasePattern = /[A-Z]/;
        return uppercasePattern.test(password);
    }

    function validateDigit(password) {
        const digitPattern = /\d/;
        return digitPattern.test(password);
    }

    function validatePromocode(promocode) {
        const regex = /^[A-Za-z0-9_]{4,16}$/;
        return regex.test(promocode);
    }

    function validateReferral(refCode) {
        const regex = /^[A-Za-z0-9_]{8}$/;
        return regex.test(refCode);
    }

    function showError(error) {
        if (error.css('display') === 'flex') {
            return;
        }

        error.css('display', 'flex');
        setTimeout(() => {
            error.css('display', 'none');
        }, 5000);
    }

    $("#signup").on('click', function (event) {
        event.preventDefault();

        if (!emailValided || !passwordValided || !promocodeValided || !referralValided || !policyAccepted || !countriesAccepted) {
            return;
        }

        const password1 = passwordInput.val();
        const password2 = passwordInput2.val();

        if (password1 !== password2) {
            passwordInputWrapper2.addClass('error--text');
            passwordRepeatError.css('display', '');
            return;
        }

        const email = emailInput.val();
        const password = passwordInput.val();
        const promocode = promocodeInput.val();
        const referralCode = referralInput.val();
        const captcha = captchaInput.val();

        $.ajax({
            url: "../api/auth/register",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                email: email,
                password: password,
                captcha: captcha,
                promocode: promocode,
                referral_code: referralCode
            }),
            success: function (response) {
                if (response === 'success') {
                    setTimeout(() => {
                        location.replace('../profile');
                    }, 500);

                    if (cookie !== null && cookie !== 'none' && cookie !== '') {
                        fbq('track', 'Lead');
                    }

                    error1.css('display', 'none');
                    error2.css('display', 'none');
                } else if (response === 'email_confirm') {
                    noti("Registration is almost complete, please check your email", "success");
                    popup(getMessage("signup.email.confirm.popup.title"), getMessage("signup.email.confirm.popup.description", [`<strong style='color: dodgerblue'>" + email + "</strong>`]), "../assets/img/success.svg", 'success', true, function () {
                        location.replace('../signin');
                    });
                } else if (response === 'email_already_exists') {
                    showError(error1)
                } else if (response.startsWith('incorrect_captcha:;')) {
                    showError(error2);
                    const img = $("#captcha-img");
                    if (img.attr('src') !== response.split(":;")[1]) {
                        img.attr('src', response.split(":;")[1]);
                    }
                } else {
                    location.reload();
                }
            }
        });
    });
});