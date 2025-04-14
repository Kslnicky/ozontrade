package me.hikaricp.yellowexchange.panel.supporter.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.common.model.SupportPreset;

@Entity
@Table(name = "supporters_support_presets")
@Getter
@Setter
@NoArgsConstructor
public class SupporterSupportPreset extends SupportPreset {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_id", nullable = false)
    private Supporter supporter;
}
