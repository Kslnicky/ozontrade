package me.yukitale.yellowexchange.exchange.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EmailPasswordRecovery {

    private final String email;
    private final String password;
}
