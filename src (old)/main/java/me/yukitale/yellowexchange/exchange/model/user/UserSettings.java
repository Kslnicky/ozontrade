package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean swapEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean stakingEnabled;

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

    private double verifDepositAmount;

    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private double btcVerifDepositAmount;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean verificationModal;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean amlModal;

    private boolean firstDepositBonusEnabled;

    private double firstDepositBonusAmount;

    private String withdrawCommission;

    private String depositCommission;

    @Column(columnDefinition = "VARCHAR(128) DEFAULT ''")
    private String note;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
