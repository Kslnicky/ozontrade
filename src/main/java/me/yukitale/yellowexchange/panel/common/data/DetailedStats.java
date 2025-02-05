package me.yukitale.yellowexchange.panel.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import org.antlr.v4.runtime.misc.Triple;
import org.springframework.data.util.Pair;

import java.util.Map;

@AllArgsConstructor
@Getter
public class DetailedStats {

    private final Map<String, Triple<Long, Long, String>> userRefers;

    private final Map<String, Long> userCountries;

    private final Map<String, Pair<Long, String>> userDeposits;

    private final Map<DepositCoin, Pair<Long, String>> coinDeposits;
}
