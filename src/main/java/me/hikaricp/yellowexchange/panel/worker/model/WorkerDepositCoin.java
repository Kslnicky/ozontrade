package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;

@Entity
@Table(name = "worker_deposit_coins",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_type", "worker_id"})
        })
@Getter
@Setter
public class WorkerDepositCoin extends DepositCoin {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
