package me.hikaricp.yellowexchange.panel.common.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum KycAcceptTimer {

    TIMER_DISABLED(0),
    TIMER_2(120),
    TIMER_5(300),
    TIMER_10(600),
    TIMER_15(900);

    private final long time;

    public static KycAcceptTimer getByName(String name) {
        return Arrays.stream(values()).filter(kycAcceptTimer -> kycAcceptTimer.name().equals(name)).findFirst().orElse(null);
    }
}
