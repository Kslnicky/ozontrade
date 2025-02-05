package me.yukitale.yellowexchange.panel.worker.service;

import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.model.user.UserRole;
import me.yukitale.yellowexchange.exchange.repository.user.UserDepositRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserRequiredDepositCoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserRoleRepository;
import me.yukitale.yellowexchange.exchange.service.UserDetailsServiceImpl;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.admin.model.*;
import me.yukitale.yellowexchange.panel.admin.repository.*;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.common.repository.PromocodeRepository;
import me.yukitale.yellowexchange.panel.worker.model.*;
import me.yukitale.yellowexchange.panel.worker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkerService {

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminCryptoLendingRepository adminCryptoLendingRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminErrorMessagesRepository adminErrorMessagesRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerCryptoLendingRepository workerCryptoLendingRepository;

    @Autowired
    private WorkerLegalSettingsRepository workerLegalSettingsRepository;

    @Autowired
    private WorkerErrorMessagesRepository workerErrorMessagesRepository;

    @Autowired
    private WorkerSupportPresetRepository workerSupportPresetsRepository;

    @Autowired
    private WorkerTelegramSettingsRepository workerTelegramSettingsRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    public Worker getWorker(Authentication authentication) {
        User user = userService.getUser(authentication);
        if (user == null) {
            throw new NullPointerException("User is null");
        }

        return getWorker(user);
    }

    public Worker getWorker(User user) {
        return workerRepository.findByUserId(user.getId()).orElseThrow(NullPointerException::new);
    }

    @Transactional
    public Worker createWorker(User user) {
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_USER).orElseThrow());
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_WORKER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRole.UserRoleType.ROLE_WORKER);

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        Worker worker = new Worker();
        worker.setUser(user);

        workerRepository.save(worker);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        WorkerSettings settings = new WorkerSettings();
        settings.setWorker(worker);
        settings.setSupportWelcomeEnabled(adminSettings.isSupportWelcomeEnabled());
        settings.setSupportWelcomeMessage(adminSettings.getSupportWelcomeMessage());
        settings.setSupportPresetsEnabled(adminSettings.isSupportPresetsEnabled());
        settings.setKycAcceptTimer(adminSettings.getKycAcceptTimer());
        settings.setSwapEnabled(adminSettings.isSwapEnabled());
        settings.setTradingEnabled(adminSettings.isTradingEnabled());
        settings.setSupportEnabled(adminSettings.isSupportEnabled());
        settings.setTransferEnabled(adminSettings.isTransferEnabled());
        settings.setCryptoLendingEnabled(adminSettings.isCryptoLendingEnabled());
        settings.setVipEnabled(adminSettings.isVipEnabled());
        settings.setWalletConnectEnabled(adminSettings.isWalletConnectEnabled());
        settings.setFakeWithdrawPending(adminSettings.isFakeWithdrawPending());
        settings.setFakeWithdrawConfirmed(adminSettings.isFakeWithdrawConfirmed());
        settings.setFiatWithdrawEnabled(adminSettings.isFiatWithdrawEnabled());
        settings.setPromoEnabled(adminSettings.isPromoEnabled());
        settings.setBuyCryptoEnabled(adminSettings.isBuyCryptoEnabled());
        workerSettingsRepository.save(settings);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        WorkerLegalSettings legalSettings = new WorkerLegalSettings();
        legalSettings.setWorker(worker);
        legalSettings.setAml(adminLegalSettings.getAml());
        legalSettings.setTerms(adminLegalSettings.getTerms());
        workerLegalSettingsRepository.save(legalSettings);

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();

        WorkerCoinSettings workerCoinSettings = new WorkerCoinSettings();
        workerCoinSettings.setWorker(worker);
        workerCoinSettings.setDepositCommission(adminCoinSettings.getDepositCommission());
        workerCoinSettings.setWithdrawCommission(adminCoinSettings.getWithdrawCommission());
        workerCoinSettings.setMinDepositAmount(adminCoinSettings.getMinDepositAmount());
        workerCoinSettings.setMinVerifAmount(adminCoinSettings.getMinVerifAmount());
        workerCoinSettings.setMinWithdrawAmount(adminCoinSettings.getMinWithdrawAmount());
        workerCoinSettings.setUseBtcVerifDeposit(adminCoinSettings.isUseBtcVerifDeposit());
        workerCoinSettings.setVerifAml(adminCoinSettings.isVerifAml());
        workerCoinSettings.setVerifRequirement(adminCoinSettings.isVerifRequirement());

        workerCoinSettingsRepository.save(workerCoinSettings);

        AdminErrorMessages adminErrorMessages = adminErrorMessagesRepository.findFirst();

        WorkerErrorMessages workerErrorMessages = new WorkerErrorMessages();
        workerErrorMessages.setWorker(worker);
        workerErrorMessages.setWithdrawMessage(adminErrorMessages.getWithdrawMessage());
        workerErrorMessages.setWithdrawVerificationMessage(adminErrorMessages.getWithdrawVerificationMessage());
        workerErrorMessages.setWithdrawAmlMessage(adminErrorMessages.getWithdrawAmlMessage());
        workerErrorMessages.setTradingMessage(adminErrorMessages.getTradingMessage());
        workerErrorMessages.setSupportMessage(adminErrorMessages.getSupportMessage());
        workerErrorMessages.setTransferMessage(adminErrorMessages.getTransferMessage());
        workerErrorMessages.setSwapMessage(adminErrorMessages.getSwapMessage());
        workerErrorMessages.setOtherMessage(adminErrorMessages.getOtherMessage());
        workerErrorMessages.setCryptoLendingMessage(adminErrorMessages.getCryptoLendingMessage());

        workerErrorMessagesRepository.save(workerErrorMessages);

        WorkerTelegramSettings workerTelegramSettings = new WorkerTelegramSettings();
        workerTelegramSettings.setWorker(worker);
        workerTelegramSettings.setDepositEnabled(true);
        workerTelegramSettings.setSupportEnabled(true);
        workerTelegramSettings.setEnable2faEnabled(true);
        workerTelegramSettings.setSendKycEnabled(true);
        workerTelegramSettings.setWithdrawEnabled(true);
        workerTelegramSettings.setWalletConnectEnabled(true);

        workerTelegramSettingsRepository.save(workerTelegramSettings);

        List<AdminDepositCoin> adminDepositCoins = adminDepositCoinRepository.findAll();

        for (AdminDepositCoin adminDepositCoin : adminDepositCoins) {
            WorkerDepositCoin workerDepositCoin = new WorkerDepositCoin();
            workerDepositCoin.setType(adminDepositCoin.getType());
            workerDepositCoin.setIcon(adminDepositCoin.getIcon());
            workerDepositCoin.setTitle(adminDepositCoin.getTitle());
            workerDepositCoin.setSymbol(adminDepositCoin.getSymbol());
            workerDepositCoin.setMinReceiveAmount(adminDepositCoin.getMinReceiveAmount());
            workerDepositCoin.setMinDepositAmount(adminDepositCoin.getMinDepositAmount());
            workerDepositCoin.setVerifDepositAmount(adminDepositCoin.getVerifDepositAmount());
            workerDepositCoin.setEnabled(adminDepositCoin.isEnabled());
            workerDepositCoin.setPosition(adminDepositCoin.getPosition());
            workerDepositCoin.setWorker(worker);
            workerDepositCoinRepository.save(workerDepositCoin);
        }

        List<AdminSupportPreset> adminSupportPresets = adminSupportPresetRepository.findAll();
        for (AdminSupportPreset adminSupportPreset : adminSupportPresets) {
            WorkerSupportPreset workerSupportPreset = new WorkerSupportPreset();
            workerSupportPreset.setTitle(adminSupportPreset.getTitle());
            workerSupportPreset.setMessage(adminSupportPreset.getMessage());
            workerSupportPreset.setWorker(worker);

            workerSupportPresetsRepository.save(workerSupportPreset);
        }

        List<AdminCryptoLending> adminCryptoLendings = adminCryptoLendingRepository.findAll();
        for (AdminCryptoLending adminCryptoLending : adminCryptoLendings) {
            WorkerCryptoLending workerCryptoLending = new WorkerCryptoLending();
            workerCryptoLending.setWorker(worker);
            workerCryptoLending.setCoinSymbol(adminCryptoLending.getCoinSymbol());
            workerCryptoLending.setMaxAmount(adminCryptoLending.getMaxAmount());
            workerCryptoLending.setMinAmount(adminCryptoLending.getMinAmount());
            workerCryptoLending.setPercent7days(adminCryptoLending.getPercent7days());
            workerCryptoLending.setPercent14days(adminCryptoLending.getPercent14days());
            workerCryptoLending.setPercent30days(adminCryptoLending.getPercent30days());
            workerCryptoLending.setPercent90days(adminCryptoLending.getPercent90days());
            workerCryptoLending.setPercent180days(adminCryptoLending.getPercent180days());
            workerCryptoLending.setPercent360days(adminCryptoLending.getPercent360days());

            workerCryptoLendingRepository.save(workerCryptoLending);
        }

        userService.bindToWorker0(user, worker, workerCoinSettings, settings);

        return worker;
    }

    @Transactional
    public void deleteWorker(Worker worker) {
        User user = worker.getUser();

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_USER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRole.UserRoleType.ROLE_USER);

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        userDepositRepository.removeWorkerForAll(worker.getId());

        withdrawCoinLimitRepository.deleteAllByWorkerId(worker.getId());
        workerCoinSettingsRepository.deleteAllByWorkerId(worker.getId());
        workerDepositCoinRepository.deleteAllByWorkerId(worker.getId());

        workerErrorMessagesRepository.deleteByWorkerId(worker.getId());
        workerLegalSettingsRepository.deleteByWorkerId(worker.getId());
        workerSettingsRepository.deleteAllByWorkerId(worker.getId());
        workerTelegramSettingsRepository.deleteAllByWorkerId(worker.getId());
        workerSupportPresetsRepository.deleteAllByWorkerId(worker.getId());

        domainRepository.deleteAllByWorkerId(worker.getId());
        fastPumpRepository.deleteAllByWorkerId(worker.getId());
        stablePumpRepository.deleteAllByWorkerId(worker.getId());
        promocodeRepository.deleteAllByWorkerId(worker.getId());

        userRepository.removeWorkerFromUsers(worker.getId());

        workerRepository.deleteById(worker.getId(), user);

        //todo: delete from all repositories
    }

    public Worker addWorkerAttribute(User user, Model model) {
        Worker worker = getWorker(user);
        model.addAttribute("worker", worker);
        return worker;
    }

    public Worker addUserWorkerAttribute(User user, String domain, Model model) {
        Worker worker = getUserWorker(user, domain);

        model.addAttribute("worker", worker);

        return worker;
    }

    public Worker getUserWorker(User user, String domain) {
        Worker worker = user == null ? null : user.getWorker();
        if (worker == null && domain != null) {
            worker = getWorkerByDomain(domain);
        }

        return worker;
    }

    public Worker getWorkerByDomain(String domain) {
        return domainRepository.findWorkerByName(domain.toLowerCase()).orElse(null);
    }
}
