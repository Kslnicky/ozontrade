package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.utils.MyDecimal;
import me.hikaricp.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_trade_orders")
@Getter
@Setter
@NoArgsConstructor
public class UserTradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private TradeType tradeType;

    @Column(nullable = false)
    private String coinSymbol;

    private double amount;

    private double price;

    private boolean closed;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public String getFormattedCreated() {
        return StringUtil.formatDate(this.created);
    }

    public MyDecimal getFormattedAmount() {
        return new MyDecimal(this.amount);
    }

    public MyDecimal getFormattedPrice() {
        return new MyDecimal(this.price, true);
    }

    public String getBuyAmount() {
        return new MyDecimal(tradeType == TradeType.LIMIT ? this.amount * this.price : this.amount / this.price).toPrice();
    }

    public enum Type {

        BUY,
        SELL;
    }

    @AllArgsConstructor
    @Getter
    public enum TradeType {

        LIMIT("Limit"),
        MARKET("Market"),
        TRIGGER("Trigger");

        private final String title;
    }
}
