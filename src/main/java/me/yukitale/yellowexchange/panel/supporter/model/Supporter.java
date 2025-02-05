package me.yukitale.yellowexchange.panel.supporter.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.exchange.model.user.User;

@Entity
@Table(name = "supporters")
@Getter
@Setter
@NoArgsConstructor
public class Supporter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportPresetsEnabled;
}
