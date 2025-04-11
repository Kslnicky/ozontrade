package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "user_support_dialogs")
@Getter
@Setter
@NoArgsConstructor
public class UserSupportDialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int totalMessages;

    private int userUnviewedMessages;

    private int supportUnviewedMessages;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean onlyWelcome;
    
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date lastMessageDate;

    public String getFormattedLastMessageDate() {
        return StringUtil.formatDateWithoutYears(this.lastMessageDate);
    }
}
