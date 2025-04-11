package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.types.KycAcceptTimer;

@Entity
@Table(name = "worker_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long telegramId;

    @Size(max = 512)
    private String supportWelcomeMessage;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean promoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean buyCryptoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportWelcomeEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportPresetsEnabled;

    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private KycAcceptTimer kycAcceptTimer;

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
    private boolean fiatWithdrawEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean vipEnabled;

    @Size(max = 512)
    private String bonusText;

    private String bonusCoin;

    private double bonusAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
