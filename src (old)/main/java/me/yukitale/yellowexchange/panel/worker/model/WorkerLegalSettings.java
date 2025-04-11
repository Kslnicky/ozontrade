package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.model.LegalSettings;

@Entity
@Table(name = "worker_legal_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerLegalSettings extends LegalSettings {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
