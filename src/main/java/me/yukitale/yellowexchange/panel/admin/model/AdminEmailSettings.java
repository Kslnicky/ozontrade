package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

@Entity
@Table(name = "admin_email_settings")
@Getter
@Setter
@NoArgsConstructor
public class AdminEmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 64)
    private String defaultServer;

    private int defaultPort;

    @Size(max = 256)
    private String registrationTitle;

    @Size(max = 256)
    private String passwordRecoveryTitle;

    @Column(columnDefinition = "TEXT")
    private String registrationMessage;

    @Column(columnDefinition = "TEXT")
    private String passwordRecoveryMessage;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailRequiredEnabled;

    @Size(max = 64)
    private String server;

    private int port;

    @Size(max = 64)
    private String email;

    @Size(max = 64)
    private String password;

    public boolean isEmailValid() {
        return StringUtils.isNotBlank(this.server) && port > 0 && port < 65535 && StringUtils.isNotBlank(this.email) && StringUtils.isNotBlank(this.password);
    }
}
