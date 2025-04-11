package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.utils.MyDecimal;

@Entity
@Table(name = "user_balances",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_symbol", "user_id"})
        })
@NoArgsConstructor
@Getter
@Setter
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(min = 1, max = 10)
    private String coinSymbol;

    private double balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public MyDecimal getFormattedBalance() {
        return new MyDecimal(this.balance);
    }

    public double getInUsd(double pricePerOne) {
        return balance * pricePerOne;
    }
}
