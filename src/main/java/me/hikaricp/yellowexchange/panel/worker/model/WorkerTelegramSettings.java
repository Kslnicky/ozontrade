package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "worker_telegram_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerTelegramSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long telegramId;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean depositEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean withdrawEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean walletConnectEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean enable2faEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean sendKycEnabled;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
