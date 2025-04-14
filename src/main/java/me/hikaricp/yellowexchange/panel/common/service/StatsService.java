package me.hikaricp.yellowexchange.panel.common.service;
import me.hikaricp.yellowexchange.exchange.repository.user.UserDepositRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.hikaricp.yellowexchange.panel.common.data.DetailedStats;
import me.hikaricp.yellowexchange.panel.common.data.Stats;
import me.hikaricp.yellowexchange.panel.common.data.WorkerTopStats;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.utils.DateUtil;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import me.hikaricp.yellowexchange.utils.MyDecimal;
import org.antlr.v4.runtime.misc.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import  me.hikaricp.yellowexchange.panel.common.data.Stats.StatsType;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//todo: expiring
@Service
public class StatsService {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final Map<Long, Pair<Stats, Long>> workerStatsMap = new HashMap<>();

    private final Map<WorkerTopStats.Type, Pair<List<WorkerTopStats>, Long>> workerTopStatsMap =  new LinkedHashMap<>();

    private final Map<StatsType, Pair<DetailedStats, Long>> adminDetailedStats = new LinkedHashMap<>();

    private final Map<Long, Map<StatsType, Pair<DetailedStats, Long>>> workerDetailedStats = new LinkedHashMap<>();

    private Stats adminStats;

    private long adminStatsLastUpdate;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    public DetailedStats getAdminDetailedStats(StatsType type) {
        Pair<DetailedStats, Long> pair = adminDetailedStats.get(type);
        if (pair == null || pair.getSecond() < System.currentTimeMillis()) {
            Map<String, Long> regs;
            Map<String, Triple<Long, Long, String>> refers = new LinkedHashMap<>();
            Map<String, Pair<Long, String>> deposits = new LinkedHashMap<>();
            Map<DepositCoin, Pair<Long, String>> depositCoins = new LinkedHashMap<>();
            Map<String, Triple<Long, Long, Double>> refs;
            Map<String, Pair<Long, Double>> deps;
            Map<DepositCoin.CoinType, Pair<Long, Double>> depCoins;

            if (type == StatsType.ALL) {
                regs = userRepository.findRegistrationsByCountriesAsMap();
                refs = userRepository.findRegistrationsByRefersAsMap();
                deps = userDepositRepository.findDepositsByCountriesAsMap();
                depCoins = userDepositRepository.findDepositsByCoinTypeAsMap();
            } else {
                Date startDate = type == StatsType.YEAR ? DateUtil.getYearStartDate() : type == StatsType.MONTH ? DateUtil.getMonthStartDate() : type == StatsType.WEEK ? DateUtil.getWeekStartDate() : DateUtil.getTodayStartDate();
                regs = userRepository.findRegistrationsByCountriesAsMap(startDate);
                refs = userRepository.findRegistrationsByRefersAsMap(startDate);
                deps = userDepositRepository.findDepositsByCountriesAsMap(startDate);
                depCoins = userDepositRepository.findDepositsByCoinTypeAsMap(startDate);
            }

            for (Map.Entry<String, Triple<Long, Long, Double>> entry : refs.entrySet()) {
                refers.put(entry.getKey(), new Triple<>(entry.getValue().a, entry.getValue().b, new MyDecimal(entry.getValue().c, true).toString()));
            }

            for (Map.Entry<String, Pair<Long, Double>> entry : deps.entrySet()) {
                String country = entry.getKey();
                Pair<Long, Double> p = entry.getValue();

                deposits.put(country, Pair.of(p.getFirst(), new MyDecimal(p.getSecond(), true).toString()));
            }

            for (Map.Entry<DepositCoin.CoinType, Pair<Long, Double>> entry : depCoins.entrySet()) {
                DepositCoin.CoinType coinType = entry.getKey();
                Pair<Long, Double> p = entry.getValue();

                adminDepositCoinRepository.findByType(coinType).ifPresent(depositCoin -> depositCoins.put(depositCoin, Pair.of(p.getFirst(), new MyDecimal(p.getSecond(), true).toString())));
            }

            DetailedStats detailedStats = new DetailedStats(refers, regs, deposits, depositCoins);

            pair = Pair.of(detailedStats, System.currentTimeMillis() + 15000);
        }

        adminDetailedStats.put(type, pair);

        return pair.getFirst();
    }

    public DetailedStats getWorkerDetailedStats(Worker worker, StatsType type) {
        if (worker == null) {
            return null;
        }

        Map<StatsType, Pair<DetailedStats, Long>> map = workerDetailedStats.getOrDefault(worker.getId(), new ConcurrentHashMap<>());
        Pair<DetailedStats, Long> pair = map.get(type);
        if (pair == null || pair.getSecond() < System.currentTimeMillis()) {
            Map<String, Long> regs;
            Map<String, Triple<Long, Long, String>> refers = new LinkedHashMap<>();
            Map<String, Pair<Long, String>> deposits = new LinkedHashMap<>();
            Map<DepositCoin, Pair<Long, String>> depositCoins = new LinkedHashMap<>();
            Map<String, Triple<Long, Long, Double>> refs;
            Map<String, Pair<Long, Double>> deps;
            Map<DepositCoin.CoinType, Pair<Long, Double>> depCoins;

            if (type == StatsType.ALL) {
                regs = userRepository.findRegistrationsByCountriesAsMap(worker.getId());
                refs = userRepository.findRegistrationsByRefersAsMap(worker.getId());
                deps = userDepositRepository.findDepositsByCountriesAsMap(worker.getId());
                depCoins = userDepositRepository.findDepositsByCoinTypeAsMap(worker.getId());
            } else {
                Date startDate = type == StatsType.YEAR ? DateUtil.getYearStartDate() : type == StatsType.MONTH ? DateUtil.getMonthStartDate() : type == StatsType.WEEK ? DateUtil.getWeekStartDate() : DateUtil.getTodayStartDate();
                regs = userRepository.findRegistrationsByCountriesAsMap(worker.getId(), startDate);
                refs = userRepository.findRegistrationsByRefersAsMap(worker.getId(), startDate);
                deps = userDepositRepository.findDepositsByCountriesAsMap(worker.getId(), startDate);
                depCoins = userDepositRepository.findDepositsByCoinTypeAsMap(worker.getId(), startDate);
            }

            for (Map.Entry<String, Triple<Long, Long, Double>> entry : refs.entrySet()) {
                refers.put(entry.getKey(), new Triple<>(entry.getValue().a, entry.getValue().b, new MyDecimal(entry.getValue().c, true).toString()));
            }

            for (Map.Entry<String, Pair<Long, Double>> entry : deps.entrySet()) {
                String country = entry.getKey();
                Pair<Long, Double> p = entry.getValue();

                deposits.put(country, Pair.of(p.getFirst(), new MyDecimal(p.getSecond(), true).toString()));
            }

            for (Map.Entry<DepositCoin.CoinType, Pair<Long, Double>> entry : depCoins.entrySet()) {
                DepositCoin.CoinType coinType = entry.getKey();
                Pair<Long, Double> p = entry.getValue();

                adminDepositCoinRepository.findByType(coinType).ifPresent(depositCoin -> depositCoins.put(depositCoin, Pair.of(p.getFirst(), new MyDecimal(p.getSecond(), true).toString())));
            }

            DetailedStats detailedStats = new DetailedStats(refers, regs, deposits, depositCoins);

            pair = Pair.of(detailedStats, System.currentTimeMillis() + 15000);
        }

        map.put(type, pair);

        workerDetailedStats.put(worker.getId(), map);

        return pair.getFirst();
    }

    public Stats getWorkerStats(Worker worker) {
        if (worker == null) {
            return null;
        }

        Pair<Stats, Long> statsPair = this.workerStatsMap.get(worker.getId());
        if (statsPair == null || statsPair.getSecond() < System.currentTimeMillis()) {
            MyDecimal todayProfit = new MyDecimal(userDepositRepository.sumPriceByWorkerIdDateGreaterThan(worker.getId(), DateUtil.getTodayStartDate()), true);
            MyDecimal monthProfit = new MyDecimal(userDepositRepository.sumPriceByWorkerIdDateGreaterThan(worker.getId(), DateUtil.getMonthStartDate()), true);
            MyDecimal allTimeProfit = new MyDecimal(userDepositRepository.sumPriceByWorkerId(worker.getId()), true);

            Long bindedUsers = userRepository.countByWorkerId(worker.getId());
            if (bindedUsers == null) {
                bindedUsers = 0L;
            }

            Map<Date, Long> usersCount = userRepository.getUsersCountPerDayByWorkerIdAsMap(worker.getId());
            Map<Date, Double> depositsPrices = userDepositRepository.getSumPricePerDayByWorkerIdAsMap(worker.getId());

            List<Map<String, Object>> usersGraphic = new ArrayList<>();
            for (Map.Entry<Date, Long> entry : usersCount.entrySet()) {
                usersGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(entry.getKey()));
                    put("deps", entry.getValue());
                }});
            }

            List<Map<String, Object>> depositsGraphic = new ArrayList<>();
            for (Map.Entry<Date, Double> entry : depositsPrices.entrySet()) {
                depositsGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(entry.getKey()));
                    put("deps", Double.parseDouble(new MyDecimal(entry.getValue(), true).toString()));
                }});
            }

            if (usersGraphic.isEmpty()) {
                usersGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(new Date()));
                    put("deps", 0);
                }});
            }

            if (depositsGraphic.isEmpty()) {
                depositsGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(new Date()));
                    put("deps", 0D);
                }});
            }

            String depositsGraphicJson = JsonUtil.writeJson(depositsGraphic);
            String usersGraphicJson = JsonUtil.writeJson(usersGraphic);

            Stats stats = new Stats(todayProfit, monthProfit, allTimeProfit, bindedUsers, depositsGraphicJson, usersGraphicJson);

            this.workerStatsMap.put(worker.getId(), Pair.of(stats, System.currentTimeMillis() + 180000));

            return stats;
        }

        return statsPair.getFirst();
    }

    public Stats getAdminStats() {
        if (adminStats == null || adminStatsLastUpdate < System.currentTimeMillis()) {
            MyDecimal todayProfit = new MyDecimal(userDepositRepository.sumPriceDateGreaterThan(DateUtil.getTodayStartDate()), true);
            MyDecimal monthProfit = new MyDecimal(userDepositRepository.sumPriceDateGreaterThan(DateUtil.getMonthStartDate()), true);
            MyDecimal allTimeProfit = new MyDecimal(userDepositRepository.sumPrice(), true);

            Long bindedUsers = userRepository.count();
            if (bindedUsers == null) {
                bindedUsers = 0L;
            }

            Map<Date, Long> usersCount = userRepository.getUsersCountPerDayAsMap();
            Map<Date, Double> depositsPrices = userDepositRepository.getSumPricePerDayAsMap();

            List<Map<String, Object>> usersGraphic = new ArrayList<>();
            for (Map.Entry<Date, Long> entry : usersCount.entrySet()) {
                usersGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(entry.getKey()));
                    put("deps", entry.getValue());
                }});
            }

            List<Map<String, Object>> depositsGraphic = new ArrayList<>();
            for (Map.Entry<Date, Double> entry : depositsPrices.entrySet()) {
                depositsGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(entry.getKey()));
                    put("deps", Double.parseDouble(new MyDecimal(entry.getValue(), true).toString()));
                }});
            }

            if (usersGraphic.isEmpty()) {
                usersGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(new Date()));
                    put("deps", 0);
                }});
            }

            if (depositsGraphic.isEmpty()) {
                depositsGraphic.add(new HashMap<>() {{
                    put("y", dateFormat.format(new Date()));
                    put("deps", 0D);
                }});
            }

            String depositsGraphicJson = JsonUtil.writeJson(depositsGraphic);
            String usersGraphicJson = JsonUtil.writeJson(usersGraphic);

            adminStats = new Stats(todayProfit, monthProfit, allTimeProfit, bindedUsers, depositsGraphicJson, usersGraphicJson);

            adminStatsLastUpdate = System.currentTimeMillis() + 15000;
        }

        return adminStats;
    }

    public List<WorkerTopStats> getAllWorkerStats(WorkerTopStats.Type type) {
        Pair<List<WorkerTopStats>, Long> statsPair = workerTopStatsMap.get(type);

        if (statsPair == null || statsPair.getSecond() < System.currentTimeMillis()) {
            Date startDate = getDateByType(type);

            Map<Worker, Pair<Double, Long>> deposits = startDate == null ? userDepositRepository.sumWorkerPriceAsMap() : userDepositRepository.sumWorkerPriceByDateGreaterThanAsMap(startDate);
            Map<Long, Long> users = startDate == null ? userRepository.getWorkerUsersCountAsMap() : userRepository.getWorkerUsersCountByDateGreaterThanAsMap(startDate);

            List<WorkerTopStats> workerTopStats = new ArrayList<>();
            int id = 1;
            for (Map.Entry<Worker, Pair<Double, Long>> entry : deposits.entrySet()) {
                Worker worker = entry.getKey();

                MyDecimal depositsPrice = new MyDecimal(entry.getValue().getFirst(), true);

                Long depositsCount = entry.getValue().getSecond();
                Long usersCount = users.get(worker.getId());

                if (usersCount == null) {
                    usersCount = 0L;
                }

                if (depositsCount == null) {
                    depositsCount = 0L;
                }

                WorkerTopStats workerTopStat = new WorkerTopStats(id, worker, depositsPrice, depositsCount, usersCount);
                workerTopStats.add(workerTopStat);

                id++;
            }

            statsPair = Pair.of(workerTopStats, System.currentTimeMillis() + 300000);

            workerTopStatsMap.put(type, statsPair);
        }

        return statsPair.getFirst();
    }

    private Date getDateByType(WorkerTopStats.Type type) {
        switch (type) {
            case TODAY -> {
                return DateUtil.getTodayStartDate();
            }
            case WEEK -> {
                return DateUtil.getWeekStartDate();
            }
            case MONTH -> {
                return DateUtil.getMonthStartDate();
            }
            case YEAR -> {
                return DateUtil.getYearStartDate();
            }
            default -> {
                return null;
            }
        }
    }
}
