package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.common.model.ErrorMessages;

@Entity
@Table(name = "user_error_messages")
@Getter
@Setter
@NoArgsConstructor
public class UserErrorMessages extends ErrorMessages {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
