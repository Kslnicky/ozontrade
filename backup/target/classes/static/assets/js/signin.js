document.addEventListener('DOMContentLoaded', function() {
    const emailInputWrapper = $("#email-input-wrapper");
    const emailLabel = $("#email-label");
    const emailInput = $("#email-input");
    const passwordInputWrapper = $("#password-input-wrapper");
    const passwordLabel = $("#password-label");
    const passwordInput = $("#password-input");
    const captchaInputWrapper = $("#captcha-input-wrapper");
    const captchaLabel = $("#captcha-label");
    const captchaInput = $("#captcha-input");
    const errorEmail = $("#error-email");
    const errorPassword = $("#error-password");
    const error1 = $("#error-1");
    const error2 = $("#error-2");
    const error3 = $("#error-3");
    const showPassword = $("#show-password");

    var emailValided = false;
    var passwordValided = false;

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

        if (password.length > 0) {
            passwordValided = true;
            passwordInputWrapper.removeClass('error--text');
            errorPassword.css('display', 'none');
        } else {
            passwordValided = false;
            passwordInputWrapper.addClass('error--text');
            errorPassword.css('display', 'flex');
        }
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

    function validateEmail(email) {
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
        return emailPattern.test(email);
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

    $("#signin").on('click', function (event) {
        event.preventDefault();

        if (!emailValided || !passwordValided) {
            return;
        }

        const email = emailInput.val();
        const password = passwordInput.val();
        const captcha = captchaInput.val();

        $.ajax({
            url: "../api/auth/login",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                email: email,
                password: password,
                captcha: captcha
            }),
            success: function (response) {
                if (response === 'success') {
                    setTimeout(() => {
                        location.replace('../profile');
                    });
                    error1.css('display', 'none');
                    error2.css('display', 'none');
                } else if (response.startsWith('incorrect_captcha:;')) {
                    showError(error1);
                    const img = $("#captcha-img");
                    if (img.attr('src') !== response.split(":;")[1]) {
                        img.attr('src', response.split(":;")[1]);
                    }
                } else if (response === 'user_not_found') {
                    showError(error2)
                } else if (response === 'wrong_password') {
                    showError(error3);
                } else if (response.startsWith('jwt_two_factor: ')) {
                    location.replace("signin-2fa?token=" + response.split('jwt_two_factor: ')[1]);
                } else {
                    location.reload();
                }
            }
        });
    });
});