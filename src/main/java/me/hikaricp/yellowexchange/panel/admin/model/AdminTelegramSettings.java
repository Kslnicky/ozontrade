package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

@Entity
@Table(name = "admin_telegram_settings")
@Getter
@Setter
@NoArgsConstructor
public class AdminTelegramSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 48)
    private String botUsername;

    @Size(max = 128)
    private String botToken;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean channelNotification;

    private long channelId;

    @Column(columnDefinition = "TEXT", length = 512)
    private String channelMessage;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean depositEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean withdrawEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean walletConnectEnabled;

    @Transient
    public boolean isEnabled() {
        return StringUtils.isNotBlank(this.botUsername) && StringUtils.isNotBlank(this.botToken);
    }
}
