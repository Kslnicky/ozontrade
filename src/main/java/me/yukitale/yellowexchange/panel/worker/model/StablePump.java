package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.exchange.model.Coin;

@Entity
@Table(name = "worker_stable_pumps",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_symbol", "worker_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class StablePump {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double percent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    public StablePump(Coin coin, double percent, Worker worker) {
        this.coin = coin;
        this.percent = percent;
        this.worker = worker;
    }
}
