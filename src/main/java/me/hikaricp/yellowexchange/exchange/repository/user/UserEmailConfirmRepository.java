package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserEmailConfirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEmailConfirmRepository extends JpaRepository<UserEmailConfirm, Long> {

    Optional<UserEmailConfirm> findByUserIdAndHash(long userId, String hash);
}
