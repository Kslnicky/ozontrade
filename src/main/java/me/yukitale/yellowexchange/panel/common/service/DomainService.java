package me.yukitale.yellowexchange.panel.common.service;

import me.yukitale.yellowexchange.panel.admin.model.AdminEmailSettings;
import me.yukitale.yellowexchange.panel.admin.model.AdminSettings;
import me.yukitale.yellowexchange.panel.admin.repository.AdminEmailSettingsRepository;
import me.yukitale.yellowexchange.panel.admin.repository.AdminSettingsRepository;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.panel.common.repository.DomainRepository;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.panel.worker.model.WorkerSettings;
import me.yukitale.yellowexchange.panel.worker.repository.WorkerSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Service
public class DomainService {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    public Domain addDomainAttribute(Model model, String host) {
        if (host != null) {
            host = host.toLowerCase();
        }

        Domain domain = host == null ? null : domainRepository.findByName(host.startsWith("www.") ? host.replaceFirst("www\\.", "") : host).orElse(null);

        String siteName;
        String siteIcon;
        String siteTitle = "";
        String siteDescription;
        String siteKeywords;

        if (domain == null) {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();

            siteName = adminSettings.getSiteName();
            siteIcon = adminSettings.getSiteIcon();
            siteDescription = adminSettings.getSiteDescription();
            siteKeywords = adminSettings.getSiteKeywords();

            model.addAttribute("site_social_networks", Collections.emptyList());
        } else {
            siteName = domain.getExchangeName();
            siteIcon = domain.getIcon();
            siteTitle = domain.getTitle();
            siteDescription = domain.getDescription();
            siteKeywords = domain.getKeywords();

            model.addAttribute("site_social_networks", domain.getSocialNetworks());
        }

        model.addAttribute("site_name", siteName);
        model.addAttribute("site_icon", siteIcon);
        model.addAttribute("site_title", siteTitle);
        model.addAttribute("site_description", siteDescription);
        model.addAttribute("site_keywords", siteKeywords);
        model.addAttribute("site_domain", host == null ? siteName : host);

        return domain;
    }

    public void createDomain(Worker worker, String name) {
        AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = adminSettings.getSiteName();
        String icon = adminSettings.getSiteIcon();

        boolean promoEnabled = true;
        boolean buyCryptoEnabled = true;
        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            promoEnabled = workerSettings.isPromoEnabled();
            buyCryptoEnabled = workerSettings.isBuyCryptoEnabled();
        } else {
            promoEnabled = adminSettings.isPromoEnabled();
            buyCryptoEnabled = adminSettings.isBuyCryptoEnabled();
        }

        Domain domain = new Domain();

        domain.setWorker(worker);
        domain.setName(name.toLowerCase());
        domain.setExchangeName(exchangeName);
        domain.setIcon(icon);
        domain.setCreated(new Date());
        domain.setServer(adminEmailSettings.getDefaultServer());
        domain.setPort(adminEmailSettings.getDefaultPort());
        domain.setTitle(adminSettings.getSiteTitle());
        domain.setKeywords(adminSettings.getSiteKeywords());
        domain.setDescription(adminSettings.getSiteDescription());
        domain.setPromoEnabled(promoEnabled);
        domain.setBuyCryptoEnabled(buyCryptoEnabled);
        domain.setSignupRefEnabled(adminSettings.isSignupRefEnabled());
        domain.setSignupPromoEnabled(adminSettings.isSignupPromoEnabled());
        domain.setFiatWithdrawEnabled(adminSettings.isFiatWithdrawEnabled());
        domain.setVerif2Enabled(true);
        domain.setVerif2Balance(10000);

        domainRepository.save(domain);
    }
}
