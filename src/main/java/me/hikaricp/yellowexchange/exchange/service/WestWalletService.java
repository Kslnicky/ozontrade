package me.hikaricp.yellowexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.SneakyThrows;
import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.exchange.model.user.*;
import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.*;
import me.hikaricp.yellowexchange.panel.admin.model.AdminCoinSettings;
import me.hikaricp.yellowexchange.panel.admin.model.AdminSettings;
import me.hikaricp.yellowexchange.panel.admin.model.AdminTelegramSettings;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminCoinSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminDepositCoinRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminTelegramSettingsRepository;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;
import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.common.model.Promocode;
import me.hikaricp.yellowexchange.panel.common.repository.DomainRepository;
import me.hikaricp.yellowexchange.panel.common.repository.PromocodeRepository;
import me.hikaricp.yellowexchange.panel.common.service.TelegramService;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.panel.worker.model.WorkerCoinSettings;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerCoinSettingsRepository;
import me.hikaricp.yellowexchange.panel.worker.repository.WorkerRepository;
import me.hikaricp.yellowexchange.utils.HttpUtil;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@DependsOn(value = {"userService", "coinService", "telegramService"})
public class WestWalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WestWalletService.class);

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminDepositCoinRepository depositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CoinService coinService;
    
    @Autowired
    private TelegramService telegramService;

    @Getter
    private String westProtect;

    @SneakyThrows
    @PostConstruct
    public void init() {
        Path path = Paths.get("secret.key");
        File file = path.toFile();
        if (file.exists()) {
            this.westProtect = Files.readString(path).trim();
        } else {
            this.westProtect = RandomStringUtils.random(32, true, true);
            Files.write(path, this.westProtect.getBytes());
        }

        startMonitoring();
    }

    private void startMonitoring() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                AdminSettings adminSettings = adminSettingsRepository.findFirst();

                String publicKey = adminSettings.getWestWalletPublicKey();
                String privateKey = adminSettings.getWestWalletPrivateKey();
                if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey)) {
                    LOGGER.error("Настройте Payments в админ-панели");
                    return;
                }

                Map<String, Object> responseMap = getTransactions();

                if (!responseMap.containsKey("error") || !responseMap.get("error").equals("ok")) {
                    LOGGER.warn("Возможна ошибка при получении последних транзакций WestWallet.io");
                }

                if (!responseMap.containsKey("result")) {
                    LOGGER.error("Ошибка получения получения последних транзакций WestWallet.io");
                    return;
                }

                if ((int) responseMap.get("count") == 0) {
                    return;
                }

                List<Map<String, Object>> transactions = (List<Map<String, Object>>) responseMap.get("result");
                for (Map<String, Object> transaction : transactions) {
                    try {
                        if (transaction.get("id") == null || transaction.get("status") == null || transaction.get("amount") == null) {
                            continue;
                        }

                        long transactionId = Long.parseLong(String.valueOf(transaction.get("id")));
                        boolean completed = transaction.get("status").equals("completed");
                        double amount = Double.parseDouble(String.valueOf(transaction.get("amount")));

                        UserDeposit userDeposit = userDepositRepository.findByTxId(transactionId).orElse(null);
                        if (userDeposit != null) {
                            if (!userDeposit.isCompleted() && completed) {
                                userDeposit.setCompleted(true);

                                userDepositRepository.save(userDeposit);

                                User user = userRepository.findById(userDeposit.getUserId()).orElse(null);
                                if (user == null) {
                                    continue;
                                }

                                Worker worker = user.getWorker() == null ? null : workerRepository.findById(user.getWorker().getId()).orElse(null);

                                UserTransaction userTransaction = userTransactionRepository.findById(userDeposit.getTransactionId()).orElse(null);
                                if (userTransaction != null && userTransaction.getStatus() != UserTransaction.Status.COMPLETED) {
                                    userTransaction.setStatus(UserTransaction.Status.COMPLETED);
                                    userTransactionRepository.save(userTransaction);
                                    userService.addBalanceLazyBypass(user, userTransaction.getCoinSymbol(), userTransaction.getReceive());
                                }

                                userRepository.save(user);

                                if (user.getPromocode() != null) {
                                    Promocode promocode = promocodeRepository.findByName(user.getPromocode()).orElse(null);
                                    if (promocode != null) {
                                        promocode.setDeposits(promocode.getDeposits() + 1);
                                        promocode.setDepositsPrice(promocode.getDepositsPrice() + userDeposit.getPrice());

                                        promocodeRepository.save(promocode);
                                    }
                                }

                                addUserDeposits(user, userDeposit.getPrice());
                                addWorkerDeposits(worker, userDeposit.getPrice());
                                addDomainDeposits(user.getDomain(), userDeposit.getPrice());

                                String messageConfirmed = telegramService.getTelegramMessages().getDepositConfirmedMessage();
                                messageConfirmed = String.format(messageConfirmed, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), userTransaction.getAddress(), userDeposit.getHash());
                                telegramService.sendMessageToWorker(worker, messageConfirmed, true);

                                try {
                                    AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
                                    if (adminTelegramSettings.isChannelNotification() && adminTelegramSettings.getChannelId() != -1 && adminTelegramSettings.getChannelId() != 0 && !StringUtils.isBlank(adminTelegramSettings.getChannelMessage())) {
                                        User workerUser = worker == null ? null : userRepository.findById(worker.getUser().getId()).orElse(null);
                                        String message = String.format(adminTelegramSettings.getChannelMessage(), workerUser == null ? "-" : workerUser.getShortEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name());
                                        telegramService.sendMessageToChannel(message, adminTelegramSettings.getChannelId(), message.contains("`"));
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                userService.createAction(user, "Completed deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)", false);
                            }
                            continue;
                        }

                        if (transaction.get("address") == null) {
                            continue;
                        }

                        String address = (String) transaction.get("address");
                        String tag = transaction.get("dest_tag") == null ? "" : (String) transaction.get("dest_tag");
                        UserAddress userAddress;
                        if (StringUtils.isBlank(tag)) {
                            userAddress = userAddressRepository.findByAddressIgnoreCase(address.toLowerCase()).orElse(null);
                        } else {
                            userAddress = userAddressRepository.findByAddressIgnoreCaseAndTagIgnoreCase(address.toLowerCase(), tag).orElse(null);
                        }

                        if (userAddress == null) {
                            continue;
                        }

                        DepositCoin.CoinType coinType = userAddress.getCoinType();
                        String coinSymbol = getCoinSymbol(coinType);

                        double price = coinService.getPrice(coinSymbol);
                        if (price <= 0) {
                            LOGGER.warn("Ошибка получения курса в депозите для валюты " + coinSymbol);
                            continue;
                        }

                        User user = userRepository.findById(userAddress.getUserId()).orElse(null);
                        if (user == null) {
                            continue;
                        }

                        String hash = String.valueOf(transaction.get("blockchain_hash"));

                        Worker worker = user.getWorker() == null ? null : workerRepository.findById(user.getWorker().getId()).orElse(null);

                        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
                        String commissionLine = "0%";
                        if (!userSettings.getDepositCommission().startsWith("-1")) {
                            commissionLine = userSettings.getDepositCommission();
                        } else if (worker != null) {
                            WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
                            commissionLine = coinSettings.getDepositCommission();
                        } else {
                            AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();
                            commissionLine = adminCoinSettings.getDepositCommission();
                        }

                        double commissionAmount = 0;
                        if (!commissionLine.startsWith("-1") && !commissionLine.equals("0%") && !commissionLine.equals("0")) {
                            double comm = Double.parseDouble(commissionLine.replace("%", ""));
                            if (comm > 0) {
                                if (commissionLine.contains("%")) {
                                    commissionAmount = amount * (comm / 100);
                                } else {
                                    commissionAmount = comm / price;
                                }
                            }
                        }

                        UserTransaction userTransaction = new UserTransaction();
                        userTransaction.setUser(user);
                        userTransaction.setAmount(amount - commissionAmount);
                        userTransaction.setPay(amount);
                        userTransaction.setReceive(amount - commissionAmount);
                        userTransaction.setType(UserTransaction.Type.DEPOSIT);
                        userTransaction.setStatus(completed ? UserTransaction.Status.COMPLETED : UserTransaction.Status.IN_PROCESSING);
                        userTransaction.setDate(new Date());
                        userTransaction.setCoinSymbol(coinSymbol);
                        userTransaction.setAddress(userAddress.getAddress());
                        userTransaction.setMemo(userAddress.getTag());

                        userTransactionRepository.save(userTransaction);

                        userDeposit = new UserDeposit();
                        userDeposit.setCountryCode(user.getLastCountryCode());
                        userDeposit.setBotReceived(false);
                        userDeposit.setTransaction(userTransaction);
                        userDeposit.setTransactionId(userTransaction.getId());
                        userDeposit.setHash(hash);
                        userDeposit.setCoinType(coinType);
                        userDeposit.setDate(new Date());
                        userDeposit.setAmount(amount);
                        userDeposit.setPrice(amount * price);
                        userDeposit.setUser(user);
                        userDeposit.setUserId(user.getId());
                        userDeposit.setWorker(worker);
                        userDeposit.setTxId(transactionId);
                        userDeposit.setCompleted(completed);
                        userDeposit.setViewed(false);

                        userDepositRepository.save(userDeposit);

                        if (completed) {
                            userService.addBalanceLazyBypass(user, userTransaction.getCoinSymbol(), userTransaction.getReceive());

                            //todo: проверить
                            if (user.getPromocode() != null) {
                                Promocode promocode = promocodeRepository.findByName(user.getPromocode()).orElse(null);
                                if (promocode != null) {
                                    promocode.setDeposits(promocode.getDeposits() + 1);
                                    promocode.setDepositsPrice(promocode.getDepositsPrice() + userDeposit.getPrice());

                                    promocodeRepository.save(promocode);
                                }
                            }

                            addUserDeposits(user, userDeposit.getPrice());
                            addWorkerDeposits(worker, userDeposit.getPrice());
                            addDomainDeposits(user.getDomain(), userDeposit.getPrice());

                            String messageConfirmed = telegramService.getTelegramMessages().getDepositConfirmedMessage();
                            messageConfirmed = String.format(messageConfirmed, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), address, hash);
                            telegramService.sendMessageToWorker(worker, messageConfirmed, true);

                            try {
                                AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
                                if (adminTelegramSettings.isChannelNotification() && adminTelegramSettings.getChannelId() != -1 && adminTelegramSettings.getChannelId() != 0 && !StringUtils.isBlank(adminTelegramSettings.getChannelMessage())) {
                                    User workerUser = worker == null ? null : userRepository.findById(worker.getUser().getId()).orElse(null);
                                    String message = String.format(adminTelegramSettings.getChannelMessage(), workerUser == null ? "-" : workerUser.getShortEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name());
                                    telegramService.sendMessageToChannel(message, adminTelegramSettings.getChannelId(), message.contains("`"));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            userService.createAction(user, "Completed deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)", false);
                        } else {
                            String messagePending = telegramService.getTelegramMessages().getDepositPendingMessage();
                            messagePending = String.format(messagePending, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocode()) ? "-" : user.getPromocode(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), address, hash);
                            telegramService.sendMessageToWorker(worker, messagePending, true);

                            userService.createAction(user, "Pending deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)", false);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 15, 20, TimeUnit.SECONDS);
    }

    public Coin getCoin(String coinSymbol) {
        return coinRepository.findBySymbol(coinSymbol).orElse(null);
    }

    public void addUserDeposits(User user, double depositAmount) {
        user.setDepositsPrice(user.getDepositsPrice() + depositAmount);
        user.setDepositsCount(user.getDepositsCount() + 1);

        userRepository.save(user);
    }

    public void addWorkerDeposits(Worker worker, double depositAmount) {
        if (worker != null) {
            worker.setDepositsPrice(worker.getDepositsPrice() + depositAmount);
            worker.setDepositsCount(worker.getDepositsCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void addDomainDeposits(String domainName, double depositAmount) {
        Domain domain = domainRepository.findByName(domainName).orElse(null);
        if (domain != null) {
            domain.setDepositsPrice(domain.getDepositsPrice() + depositAmount);
            domain.setDepositsCount(domain.getDepositsCount() + 1);

            domainRepository.save(domain);
        }
    }

    private String getCoinSymbol(DepositCoin.CoinType coinType) {
        DepositCoin depositCoin = depositCoinRepository.findByType(coinType).orElse(null);
        if (depositCoin == null) {
            return null;
        }

        return depositCoin.getSymbol();
    }

    public Map<String, Object> getTransactions() throws RuntimeException {
        Map<String, Object> data = new HashMap<>() {{
            put("type", "receive");
            put("order", "desc");
            put("limit", 30);
        }};

        String dataJson = JsonUtil.writeJson(data);

        HttpPost httpPost = HttpUtil.createPost("https://api.westwallet.io/wallet/transactions", dataJson);

        signRequest(httpPost, dataJson);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpPost);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            return JsonUtil.readJson(responseJson, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка получения последних транзакций WestWallet.io");
        }
    }

    public UserAddress createUserAddress(User user, DepositCoin.CoinType coinType) throws RuntimeException {
        Map<String, Object> data = new HashMap<>() {{
            put("currency", coinType.name());
            put("ipn_url", "");
            put("label", "");
        }};

        String dataJson = JsonUtil.writeJson(data);

        HttpPost httpPost = HttpUtil.createPost("https://api.westwallet.io/address/generate", dataJson);

        signRequest(httpPost, dataJson);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpPost);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            Map<String, String> responseData = JsonUtil.readJson(responseJson, Map.class);

            if (!responseData.containsKey("address") || !responseData.containsKey("currency") || !responseData.containsKey("error") || !responseData.get("error").equals("ok")) {
                throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
            }

            String address = responseData.get("address");
            String destTag = responseData.get("dest_tag");

            if (address == null || address.isEmpty()) {
                throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
            }

            UserAddress userAddress = new UserAddress();
            userAddress.setUser(user);
            userAddress.setUserId(user.getId());
            userAddress.setTag(destTag);
            userAddress.setAddress(address);
            userAddress.setCoinType(coinType);
            userAddress.setCreated(System.currentTimeMillis());

            return userAddress;
        } catch (Exception e) {
            throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
        }
    }

    private void signRequest(HttpRequest httpRequest, String data) throws RuntimeException {
        AdminSettings paymentSettings = adminSettingsRepository.findFirst();
        if (paymentSettings == null || StringUtils.isBlank(paymentSettings.getWestWalletPublicKey()) || StringUtils.isBlank(paymentSettings.getWestWalletPrivateKey())) {
            throw new RuntimeException("WestWallet public and private keys not found");
        }

        long timestamp = Instant.now().getEpochSecond();

        String sign = Hex.encodeHexString(HmacUtils.hmacSha256(paymentSettings.getWestWalletPrivateKey().getBytes(),
                (timestamp + (data == null || data.isEmpty() ? "" : data)).getBytes()));

        httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpRequest.addHeader("X-API-KEY", paymentSettings.getWestWalletPublicKey());
        httpRequest.addHeader("X-ACCESS-SIGN", sign);
        httpRequest.addHeader("X-ACCESS-TIMESTAMP", String.valueOf(timestamp));
    }
}
