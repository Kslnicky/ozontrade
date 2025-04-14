package me.hikaricp.yellowexchange.panel.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.hikaricp.yellowexchange.utils.MyDecimal;

@AllArgsConstructor
@Getter
public class Stats {

    private final MyDecimal todayProfit;

    private final MyDecimal monthProfit;

    private final MyDecimal allTimeProfit;

    private final long bindedUsers;

    private final String depositsGraphic;

    private final String usersGraphic;

    public enum StatsType {

        TODAY,
        WEEK,
        MONTH,
        YEAR,
        ALL;
    }
}
