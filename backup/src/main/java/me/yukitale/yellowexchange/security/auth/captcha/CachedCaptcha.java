package me.yukitale.yellowexchange.security.auth.captcha;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CachedCaptcha {

    private final int id;

    private final String answer;

    private final String base64;
}
