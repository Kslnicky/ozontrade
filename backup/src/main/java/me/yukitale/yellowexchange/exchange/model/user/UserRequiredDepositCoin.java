package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.worker.model.Worker;

@Entity
@Table(name = "user_required_deposit_coins",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"symbol", "user_id"})
        })
@Getter
@Setter
public class UserRequiredDepositCoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private DepositCoin.CoinType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
