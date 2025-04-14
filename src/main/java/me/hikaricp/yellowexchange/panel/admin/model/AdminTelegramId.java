package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_telegram_ids")
@Getter
@Setter
@NoArgsConstructor
public class AdminTelegramId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private long telegramId;
}
