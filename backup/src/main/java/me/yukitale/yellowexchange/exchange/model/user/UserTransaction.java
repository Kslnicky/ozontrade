package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.config.Variables;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_transactions")
@Getter
@Setter
@NoArgsConstructor
public class UserTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, columnDefinition = "INT(2)")
    private int transactionType;

    @Column(nullable = false)
    private Status status;

    private String address;

    private String memo;

    @Size(max = 6)
    private String network;

    @Column(nullable = false)
    private String coinSymbol;

    private double amount;

    private double pay;

    private double receive;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean viewed;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public String formattedAmount() {
        return new MyDecimal(this.amount).toPrice();
    }

    public String formattedCommission() {
        return new MyDecimal(this.pay > 0 && this.receive > 0 ? this.pay - this.receive : 0).toPrice();
    }

    public String formattedDate() {
        return StringUtil.formatDate(date);
    }

    public Type getType() {
        return Type.values()[this.transactionType];
    }

    public void setType(Type type) {
        this.transactionType = type.ordinal();
    }

    public long getFakeTxId() {
        return this.id * 10 + Variables.FAKE_TXID_ADDER;
    }

    @AllArgsConstructor
    @Getter
    public enum Type {

        DEPOSIT("Deposit", true),
        BONUS("Bonus", true),
        PROMO("Promo", true),
        WITHDRAW("Withdraw", false),
        TRANSFER_IN("Transfer (In)", true),
        TRANSFER_OUT("Transfer (Out)", false),
        STAKE("Stake", false),
        UNSTAKE("Unstake", true),
        CRYPTO_LENDING_STAKE("Crypto Lending (Stake)", false),
        CRYPTO_LENDING_UNSTAKE("Crypto Lending (Unstake)", true);

        private final String title;
        private final boolean incrementBalance;
    }

    @AllArgsConstructor
    @Getter
    public enum Status {

        IN_PROCESSING("In processing"),
        CANCELED("Canceled"),
        COMPLETED("Completed");

        private final String title;
    }
}
