package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.common.model.CoinSettings;

@Entity
@Table(name = "worker_coin_settings")
@Getter
@Setter
public class WorkerCoinSettings extends CoinSettings {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
