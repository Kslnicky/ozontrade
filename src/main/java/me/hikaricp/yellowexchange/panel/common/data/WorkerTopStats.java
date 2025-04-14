package me.hikaricp.yellowexchange.panel.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.utils.MyDecimal;

@AllArgsConstructor
@Getter
public class WorkerTopStats {

    private final int id;

    private final Worker worker;

    private final MyDecimal depositsPrice;

    private final long depositsCount;

    private final long usersCount;

    public enum Type {

        TODAY,
        WEEK,
        MONTH,
        YEAR,
        ALL;
    }
}
