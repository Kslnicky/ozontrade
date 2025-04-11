package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_wallet_connects")
@Getter
@Setter
@NoArgsConstructor
public class UserWalletConnect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 256)
    @Column(nullable = false)
    private String seedPhrase;

    @Column(nullable = false)
    private Status status;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    public String getFormattedDate() {
        return StringUtil.formatDate(this.date);
    }

    public String getHiddenSeedPhrase() {
        return seedPhrase.length() <= 9 ? "******" : seedPhrase.substring(0, 3) + "***" + seedPhrase.substring(seedPhrase.length() - 3);
    }

    public enum Status {

        PENDING,
        VERIFIED,
        NOT_VERIFIED,
        DELETED;
    }
}
