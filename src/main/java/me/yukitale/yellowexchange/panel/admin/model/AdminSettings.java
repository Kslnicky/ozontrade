package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.types.KycAcceptTimer;

@Entity
@Table(name = "admin_settings")
@Getter
@Setter
@NoArgsConstructor
public class AdminSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 64)
    private String siteName;

    @NotBlank
    @Size(max = 128)
    private String siteIcon;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String siteDescription;

    @Size(max = 128)
    @Column(columnDefinition = "VARCHAR(128) DEFAULT ''", length = 128)
    private String siteTitle;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String siteKeywords;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String blockedCountries;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String robotsTxt;

    private String westWalletPrivateKey;

    private String westWalletPublicKey;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean promoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean buyCryptoEnabled;

    private boolean verif2Enabled;

    @Column(columnDefinition = "DOUBLE DEFAULT 10000")
    private double verif2Balance;

    @Size(min = 32, max = 32)
    private String apiKey;

    @Size(max = 512)
    private String supportWelcomeMessage;

    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private KycAcceptTimer kycAcceptTimer;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportWelcomeEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportPresetsEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean workerTopStats;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean swapEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean transferEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean tradingEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean cryptoLendingEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean walletConnectEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeWithdrawPending;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeWithdrawConfirmed;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean vipEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean signupPromoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fiatWithdrawEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean signupRefEnabled;
}
