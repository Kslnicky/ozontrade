package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.utils.MyDecimal;
import me.hikaricp.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_crypto_lendings")
@Getter
@Setter
@NoArgsConstructor
public class UserCryptoLending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String coinSymbol;

    private int days;

    private double percent;

    private double amount;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date openTime;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date closeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public String formattedAmount() {
        return new MyDecimal(this.amount).toString();
    }

    public double getTotalProfit() {
        return this.amount * (this.percent / 100D);
    }

    public double getRealtimeProfit() {
        long diffMillis = System.currentTimeMillis() - this.openTime.getTime();
        int days = (int) (diffMillis / 1000D / 86400D);
        double dayPercent = this.percent / this.days * days;
        double realtimeProfit = this.amount * (dayPercent / 100D);

        return Math.min(realtimeProfit, getTotalProfit());
    }

    public String formattedTotalProfit() {
        return new MyDecimal(getTotalProfit()).toPrice();
    }

    public String formattedRealtimeProfit() {
        return new MyDecimal(getRealtimeProfit()).toPrice();
    }

    public String formattedOpenTime() {
        return StringUtil.formatDate(this.openTime);
    }

    public String formattedCloseTime() {
        return StringUtil.formatDate(this.closeTime);
    }

    public boolean isExpired() {
        return this.closeTime.getTime() < System.currentTimeMillis();
    }
}
