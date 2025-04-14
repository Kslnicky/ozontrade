package me.hikaricp.yellowexchange.panel.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import me.hikaricp.yellowexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "promocodes")
@Getter
@Setter
@NoArgsConstructor
public class Promocode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(min = 2, max = 64)
    @Column(unique = true, nullable = false)
    private String name;

    @Size(min = 1, max = 256)
    private String text;

    private String coinSymbol;

    private double minAmount;

    private double maxAmount;

    private double bonusAmount;

    private int activations;

    private int deposits;

    private double depositsPrice;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = true)
    private Worker worker;

    @Transient
    public String getFormattedDate() {
        return StringUtil.formatDateToTimeAgo(this.created.getTime());
    }

    @Transient
    public boolean isRandom() {
        return this.maxAmount > this.minAmount;
    }
}

