package me.yukitale.yellowexchange.exchange.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EmailRegistration {

    private final String referrer;
    private final String email;
    private final String password;
    private final String domainName;
    private final String platform;
    private final String regIp;
    private final String promocodeName;
    private final String refCode;
}
