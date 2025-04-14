package me.hikaricp.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.common.model.SupportPreset;

@Entity
@Table(name = "worker_support_presets")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSupportPreset extends SupportPreset {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
