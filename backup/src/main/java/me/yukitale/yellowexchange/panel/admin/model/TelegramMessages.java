package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telegram_messages")
@Getter
@Setter
@NoArgsConstructor
public class TelegramMessages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT", length = 512)
    private String supportMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String supportImageMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String enable2faMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String sendKycMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String withdrawMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String depositPendingMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String depositConfirmedMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String walletWorkerMessage;

    @Column(columnDefinition = "TEXT", length = 512)
    private String walletAdminMessage;

    @AllArgsConstructor
    @Getter
    public enum MessageType {

        USER_SEND_SUPPORT_MESSAGE("User send support message"),
        USER_SEND_SUPPORT_IMAGE("User send support image"),
        USER_ENABLE_2FA("User enable 2FA"),
        USER_SEND_KYC("User send KYC"),
        USER_WITHDRAW("User withdraw"),
        USER_DEPOSIT_PENDING("User deposit pending"),
        USER_DEPOSIT_CONFIRMED("User deposit confirmed"),
        USER_CONNECT_WALLET_FOR_WORKER("User connect wallet (for Worker)"),
        USER_CONNECT_WALLET_FOR_ADMIN("User connect wallet (for Admin)");
        //SUPPORT_SEND_ANSWER("Support send answer (from Telegram bot)");

        private final String title;
    }
}
