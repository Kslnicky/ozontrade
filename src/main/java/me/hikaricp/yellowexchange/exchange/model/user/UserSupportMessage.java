package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_support_messages")
@Getter
@Setter
@NoArgsConstructor
public class UserSupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Target target;

    private Type type;

    @NotBlank
    @Size(min = 1, max = 2000)
    private String message;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean userViewed;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean supportViewed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    public UserSupportMessage(Target target, Type type, String message, boolean userViewed, boolean supportViewed, User user) {
        this.target = target;
        this.type = type;
        this.message = message;
        this.userViewed = userViewed;
        this.supportViewed = supportViewed;
        this.user = user;
        this.created = new Date();
    }

    public enum Target {

        TO_USER,
        TO_SUPPORT;
    }

    public enum Type {

        TEXT,
        IMAGE;
    }

    @Transient
    public String getFormattedDate() {
        return StringUtil.formatDateWithoutSeconds(this.created);
    }

    @Transient
    public boolean toSupport() {
        return target == Target.TO_SUPPORT;
    }

    @Transient
    public boolean isImage() {
        return type == Type.IMAGE;
    }
}
