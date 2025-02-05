package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserWalletConnect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletConnectRepository extends JpaRepository<UserWalletConnect, Long> {

    Optional<UserWalletConnect> findByIdAndUserId(long id, long userId);

    List<UserWalletConnect> findByUserWorkerIdOrderByIdDesc(long workerId);

    @Cacheable(value = "user_wallet_connects", key = "#userId")
    List<UserWalletConnect> findByUserIdOrderByIdDesc(long userId);

    @CacheEvict(value = "user_wallet_connects", key = "#entity.user.id")
    @Override
    <S extends UserWalletConnect> S save(S entity);

    @CacheEvict(value = "user_wallet_connects", key = "#userWalletConnect.user.id")
    default void delete(UserWalletConnect userWalletConnect) {
        deleteById(userWalletConnect.getId());
    }

    long countByUserId(long userId);
}
