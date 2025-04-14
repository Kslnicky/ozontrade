package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserCryptoLending;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCryptoLendingRepository extends JpaRepository<UserCryptoLending, Long> {

    Optional<UserCryptoLending> findByIdAndUserId(long id, long userId);

    @Cacheable(value = "user_crypto_lendings", key = "#userId")
    List<UserCryptoLending> findAllByUserIdOrderByIdDesc(long userId);

    @CacheEvict(value = "user_crypto_lendings", key = "#entity.user.id")
    @Override
    <S extends UserCryptoLending> S save(S entity);

    @CacheEvict(value = "user_crypto_lendings", key = "#entity.user.id")
    default void delete(UserCryptoLending entity) {
        deleteById(entity.getId());
    }
}
