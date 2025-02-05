package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "worker_withdraw_coin_limits",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"worker_id", "coin_symbol"})
        })
@Getter
@Setter
@NoArgsConstructor
public class WithdrawCoinLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String coinSymbol;

    private double minAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
