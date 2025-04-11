package me.yukitale.yellowexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class LegalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String aml;

    @Column(columnDefinition = "MEDIUMTEXT", length = 196605)
    private String terms;
}
