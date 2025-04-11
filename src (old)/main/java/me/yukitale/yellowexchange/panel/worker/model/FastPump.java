package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.exchange.model.Coin;

@Entity
@Table(name = "worker_fast_pumps")
@Getter
@Setter
@NoArgsConstructor
public class FastPump {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double percent;

    private long time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    public FastPump(Coin coin, double percent, long time, Worker worker) {
        this.coin = coin;
        this.percent = percent;
        this.time = time;
        this.worker = worker;
    }
}
