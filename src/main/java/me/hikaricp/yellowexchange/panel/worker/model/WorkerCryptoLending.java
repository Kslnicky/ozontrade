package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.common.model.CryptoLending;

@Entity
@Table(name = "worker_crypto_lendings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_symbol", "worker_id"})
        })
@Getter
@Setter
public class WorkerCryptoLending extends CryptoLending {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
