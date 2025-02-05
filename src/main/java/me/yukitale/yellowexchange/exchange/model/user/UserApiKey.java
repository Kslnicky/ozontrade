package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user_api_keys")
@Getter
@Setter
@NoArgsConstructor
public class UserApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 16)
    private String secretKey;

    private boolean readDataEnabled;

    private boolean writeDataEnabled;

    private boolean tradingEnabled;

    private boolean exchangeEnabled;

    private boolean transferEnabled;

    private boolean withdrawEnabled;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public String getFormattedPermissions() {
        List<String> permissions = new ArrayList<>();
        if (readDataEnabled) {
            permissions.add("Read Data");
        }
        if (writeDataEnabled) {
            permissions.add("Write Data");
        }
        if (tradingEnabled) {
            permissions.add("Trading");
        }
        if (exchangeEnabled) {
            permissions.add("Exchange");
        }
        if (transferEnabled) {
            permissions.add("Transfer");
        }
        if (withdrawEnabled) {
            permissions.add("Withdraw");
        }

        return StringUtils.join(permissions, ", ");
    }

    public String getFormattedCreated() {
        return StringUtil.formatDate(this.created);
    }
}
