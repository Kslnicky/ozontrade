package me.yukitale.yellowexchange.panel.worker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.model.ErrorMessages;

@Entity
@Table(name = "worker_error_messages",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"error_type", "worker_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class WorkerErrorMessages extends ErrorMessages {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
