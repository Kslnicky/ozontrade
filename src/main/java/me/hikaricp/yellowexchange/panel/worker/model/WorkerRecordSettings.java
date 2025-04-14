package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "worker_record_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerRecordSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private long emailEnd;

    private boolean fakeWithdrawPending;

    private boolean fakeWithdrawConfirmed;

    private boolean vip;

    private boolean walletConnect;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeVerifiedLv1;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeVerifiedLv2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
