package me.yukitale.yellowexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class CoinSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private double minDepositAmount;

    @Column(columnDefinition = "VARCHAR(10) DEFAULT '1'")
    private String depositCommission;

    @Column(columnDefinition = "VARCHAR(10) DEFAULT '1'")
    private String withdrawCommission;

    private boolean verifRequirement;

    private boolean verifAml;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean useBtcVerifDeposit;

    private double minVerifAmount;

    private double minWithdrawAmount;
}
