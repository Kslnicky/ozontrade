package me.yukitale.yellowexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import me.yukitale.yellowexchange.exchange.model.Coin;
import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.worker.model.FastPump;
import me.yukitale.yellowexchange.panel.worker.model.StablePump;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.model.WorkerCoinSettings;
import me.yukitale.yellowexchange.panel.worker.repository.FastPumpRepository;
import me.yukitale.yellowexchange.panel.worker.repository.StablePumpRepository;
import me.yukitale.yellowexchange.panel.worker.repository.WorkerCoinSettingsRepository;
import me.yukitale.yellowexchange.panel.worker.repository.WorkerDepositCoinRepository;
import me.yukitale.yellowexchange.utils.JsonUtil;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Service
public class CoinService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoinService.class);

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    private final Map<String, Map<String, Object>> prices = new ConcurrentHashMap<>() {{
        put("USDT", new HashMap<>() {{
            put("price", 1D);
            put("price_change", 0D);
            put("price_change_percent", 0D);
            put("volume", 0L);
            put("quote_volume", 0L);
            put("high_price", 1D);
            put("low_price", 1D);
        }});
    }};

    @Getter
    private final Map<String, MyDecimal> onlyPrices = new ConcurrentHashMap<>() {{
        put("USDT", new MyDecimal(1D));
    }};

    public boolean hasCoin(String coinSymbol) {
        return this.onlyPrices.containsKey(coinSymbol);
    }

    @PostConstruct
    public void init() {
        startMonitoring();
    }

    public void addCoinsJsonAttribute(Model model) {
        model.addAttribute("coins_json", coinRepository.findCoinsAsJson());
    }

    //todo: optimize
    public void addDepositCoinsJsonAttribute(Model model, User user) {
        Worker worker = user == null ? null : user.getWorker();

        List<? extends DepositCoin> depositCoins;
        if (worker != null) {
            depositCoins = workerDepositCoinRepository.findAllByWorkerId(worker.getId()).stream().toList();
        } else {
            depositCoins = adminDepositCoinRepository.findAll();
        }

        depositCoins = depositCoins.stream().filter(DepositCoin::isEnabled).toList();

        Map<String, Map<String, Object>> coinsMap = new LinkedHashMap<>();

        for (DepositCoin depositCoin : depositCoins) {
            String symbol = depositCoin.getSymbol();
            String network = depositCoin.getType().name().replaceFirst(symbol, "");
            if (network.isEmpty()) {
                network = symbol;
            }

            Map<String, Object> map = coinsMap.get(symbol);
            if (map == null) {
                map = new HashMap<>();

                map.put("icon", depositCoin.getIcon());
                map.put("title", depositCoin.getTitle());

                Map<String, Object> networks = new HashMap<>();
                networks.put(network, depositCoin.getMinDepositAmount());

                map.put("networks", networks);

                coinsMap.put(symbol, map);
            } else {
                Map<String, Object> networks = (Map<String, Object>) map.get("networks");
                networks.put(network, depositCoin.getMinDepositAmount());
            }
        }

        model.addAttribute("deposit_coins_json", JsonUtil.writeJson(coinsMap));
    }

    public MyDecimal getMinDepositAmount(Worker worker, String symbol) {
        double minDepositAmount = 0D;
        if (worker == null) {
            minDepositAmount = adminCoinSettingsRepository.findFirst().getMinDepositAmount() / getPrice(symbol);
        } else {
            WorkerCoinSettings workerCoinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            minDepositAmount = workerCoinSettings == null ? adminCoinSettingsRepository.findFirst().getMinDepositAmount() / getPrice(symbol) : workerCoinSettings.getMinDepositAmount() / getPrice(symbol);
        }

        return new MyDecimal(minDepositAmount);
    }

    public double getIfWorkerPrice(Worker worker, Coin coin) {
        return getIfWorkerPrice(worker, coin.getSymbol());
    }

    public double getIfWorkerPrice(Worker worker, String symbol) {
        return worker == null ? getPrice(symbol) : getWorkerPrice(worker, symbol);
    }

    public double getPrice(Coin coin) {
        return getPrice(coin.getSymbol());
    }

    public double getPrice(String symbol) {
        return onlyPrices.containsKey(symbol) ? onlyPrices.get(symbol).getValue().doubleValue() : 0D;
    }

    public double getPriceChange(String symbol) {
        return prices.containsKey(symbol) ? (double) prices.get(symbol).get("price_change") : 0D;
    }

    public double getPriceChangePercent(String symbol) {
        return prices.containsKey(symbol) ? (double) prices.get(symbol).get("price_change_percent") : 0D;
    }

    public long getVolume(String symbol) {
        return prices.containsKey(symbol) ? (long) prices.get(symbol).get("volume") : 0L;
    }

    public long getQuoteVolume(String symbol) {
        return prices.containsKey(symbol) ? (long) prices.get(symbol).get("quote_volume") : 0L;
    }

    public double getHighPrice(String symbol) {
        return prices.containsKey(symbol) ? (double) prices.get(symbol).get("high_price") : 0D;
    }

    public double getLowPrice(String symbol) {
        return prices.containsKey(symbol) ? (double) prices.get(symbol).get("low_price") : 0D;
    }

    public double getWorkerPrice(Worker worker, String symbol) {
        return getWorkerPrice(worker, symbol, getPrice(symbol));
    }

    public double getWorkerPriceZeroTime(Worker worker, String symbol) {
        return getWorkerPriceZeroTime(worker, symbol, getPrice(symbol));
    }

    public String getWorkerPriceFormatted(Worker worker, String symbol) {
        double price = getWorkerPrice(worker, symbol, getPrice(symbol));
        int decimals = 8;
        if (price > 100) {
            decimals = 2;
        }
        if (price > 1 && price < 100) {
            decimals = 4;
        }
        if (price <= 1 && price > 0.001) {
            decimals = 5;
        }
        return new MyDecimal(price).toString(decimals);
    }

    public double getWorkerPriceChange(Worker worker, String symbol) {
        double priceChange = getPriceChange(symbol);
        if (worker != null) {
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                priceChange += priceChange * (activeFastPump.getPercent());
            }
        }

        return priceChange;
    }

    public double getWorkerPriceChangePercent(Worker worker, String symbol) {
        double priceChangePercent = getPriceChangePercent(symbol);
        if (worker != null) {
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                priceChangePercent += activeFastPump.getPercent() * 100;
            }
        }

        return priceChangePercent;
    }

    public String getWorkerPriceChangePercentFormatted(Worker worker, String symbol) {
        return new MyDecimal(getWorkerPriceChangePercent(worker, symbol)).toString(2);
    }

    public String getPriceChangePercentFormatted(String symbol) {
        return new MyDecimal(getPriceChangePercent(symbol)).toString();
    }

    public String getPriceChangeFormatted(String symbol) {
        return new MyDecimal(getPriceChange(symbol)).toString();
    }

    public MyDecimal format(double value) {
        return new MyDecimal(value);
    }

    public double getWorkerPriceChangePercentZeroTime(Worker worker, String symbol) {
        double priceChangePercent = getPriceChangePercent(symbol);
        if (worker != null) {
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                priceChangePercent = priceChangePercent + (activeFastPump.getPercent() * 100);
            }
        }

        return priceChangePercent;
    }

    private double getStablePumpPercent(Worker worker, String symbol) {
        StablePump stablePump = stablePumpRepository.findByWorkerIdAndCoinSymbol(worker.getId(), symbol).orElse(null);
        return stablePump == null ? 0D : stablePump.getPercent();
    }

    public long getWorkerQuoteVolume(Worker worker, String symbol) {
        long quoteVolume = getQuoteVolume(symbol);
        if (worker != null) {
            quoteVolume = (long) (quoteVolume + (quoteVolume * getStablePumpPercent(worker, symbol)));
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                if (activeFastPump.getTime() > System.currentTimeMillis()) {
                    break;
                }
                quoteVolume = (long) (quoteVolume + (quoteVolume * activeFastPump.getPercent()));
            }
        }

        return quoteVolume;
    }

    public String getWorkerQuoteVolumeFormatted(Worker worker, String symbol) {
        return StringUtil.formatDecimal(getWorkerQuoteVolume(worker, symbol));
    }

    public long getWorkerVolume(Worker worker, String symbol) {
        long volume = getVolume(symbol);
        if (worker != null) {
            volume = (long) (volume + (volume * getStablePumpPercent(worker, symbol)));
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                if (activeFastPump.getTime() > System.currentTimeMillis()) {
                    break;
                }
                volume = (long) (volume + (volume * activeFastPump.getPercent()));
            }
        }

        return volume;
    }

    public double getWorkerHighPrice(Worker worker, String symbol) {
        return getWorkerPrice(worker, symbol, getHighPrice(symbol));
    }

    public double getWorkerLowPrice(Worker worker, String symbol) {
        return getWorkerPrice(worker, symbol, getLowPrice(symbol));
    }

    private double getWorkerPrice(Worker worker, String symbol, double price) {
        if (worker != null) {
            price = price + (price * getStablePumpPercent(worker, symbol));
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                if (activeFastPump.getTime() <= System.currentTimeMillis()) {
                    price = price + (price * activeFastPump.getPercent());
                }
            }
        }

        return price;
    }

    private double getWorkerPriceZeroTime(Worker worker, String symbol, double price) {
        if (worker != null) {
            price = price + (price * getStablePumpPercent(worker, symbol));
            for (FastPump activeFastPump : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), symbol)) {
                price = price + (price * activeFastPump.getPercent());
            }
        }

        return price;
    }

    public MyDecimal toDecimal(double value) {
        return new MyDecimal(value);
    }

    public MyDecimal toDecimalUsd(double value) {
        return new MyDecimal(value, true);
    }

    private String getSymbolsLine() {
        StringBuilder symbolsLineBuilder = new StringBuilder("[");
        for (Coin coin : coinRepository.findAll()) {
            if (coin.getSymbol().equals("USDT")) {
                continue;
            }
            symbolsLineBuilder.append("\"");
            symbolsLineBuilder.append(coin.getSymbol());
            symbolsLineBuilder.append("USDT");
            symbolsLineBuilder.append("\",");
        }

        symbolsLineBuilder.deleteCharAt(symbolsLineBuilder.length() - 1);
        symbolsLineBuilder.append("]");

        return symbolsLineBuilder.toString();

    }

    public void startMonitoring() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            long ticks = Long.MAX_VALUE;
            final long updateSeconds = 5;

            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (true) {
                    try {
                        if (ticks >= (14400D / updateSeconds)) {
                            updateTickers(getSymbolsLine());
                            ticks = 0;
                        } else {
                            updatePrices(getSymbolsLine());
                        }
                        Thread.sleep(updateSeconds * 1000);
                        ticks++;
                    } catch (Exception ex) {
                        LOGGER.error("Ошибка получения курсов. Возможно, вы добавили не существующую на бинансе монету: ", ex);
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
    }

    private void updatePrices(String symbols) throws IOException {
        String response = sendGetRequestHttp("https://api.binance.com/api/v3/ticker/price?symbols=" + symbols);
        List<Map<String, String>> priceAnswer = JsonUtil.readJson(response, List.class);
        for (Map<String, String> entry : priceAnswer) {
            String symbol = entry.get("symbol");
            String currency = symbol.substring(0, symbol.length() - 4);
            double price = Double.parseDouble(entry.get("price"));
            Map<String, Object> priceMap = prices.get(currency);
            if (priceMap != null) {
                priceMap.put("price", price);
            }
            onlyPrices.put(currency, new MyDecimal(price));
        }
    }

    private void updateTickers(String symbols) {
        try {
            String response = sendGetRequestHttp("https://api.binance.com/api/v3/ticker/24hr?symbols=" + symbols);
            List<Map<String, String>> priceAnswer = JsonUtil.readJson(response, List.class);
            if (priceAnswer.isEmpty()) {
                Thread.sleep(30000);
                updateTickers(symbols);
                return;
            }
            for (Map<String, String> entry : priceAnswer) {
                String symbol = entry.get("symbol");
                String currency = symbol.substring(0, symbol.length() - 4);
                String volumeLine = entry.get("volume").substring(0, entry.get("volume").indexOf("."));
                String quoteVolumeLine = entry.get("quoteVolume").substring(0, entry.get("quoteVolume").indexOf("."));
                long volume = Long.parseLong(volumeLine) / 10;
                long quoteVolume = Long.parseLong(quoteVolumeLine) / 10;
                prices.put(currency, new HashMap<>() {{
                    put("price", Double.parseDouble(entry.get("lastPrice")));
                    put("price_change", Double.parseDouble(entry.get("priceChange")));
                    put("price_change_percent", Double.parseDouble(entry.get("priceChangePercent")));
                    put("volume", volume);
                    put("quote_volume", quoteVolume);
                    put("high_price", Double.parseDouble(entry.get("highPrice")));
                    put("low_price", Double.parseDouble(entry.get("lowPrice")));
                }});
            }

        } catch (Exception ex) {
            try {
                Thread.sleep(30000);
                updateTickers(symbols);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String sendGetRequestHttp(String urlLine) throws IOException {
        URL url = new URL(urlLine);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
