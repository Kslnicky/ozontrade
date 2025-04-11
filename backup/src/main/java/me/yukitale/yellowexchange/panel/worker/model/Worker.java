package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.utils.MyDecimal;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long usersCount;

    private long depositsCount;

    private double depositsPrice;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public MyDecimal formattedDeposits() {
        return new MyDecimal(this.depositsPrice, true);
    }
}
