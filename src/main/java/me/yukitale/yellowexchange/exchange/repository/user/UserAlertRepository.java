package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserAlert;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

//todo: caching
@Repository
public interface UserAlertRepository extends JpaRepository<UserAlert, Long> {

    @Cacheable(value = "user_alerts", key = "#userId")
    Optional<UserAlert> findFirstByUserId(long userId);

    @CacheEvict(value = "user_alerts", key = "#entity.user.id")
    @Override
    <S extends UserAlert> S save(S entity);

    @CacheEvict(value = "user_alerts", key = "#userId")
    default void deleteByUserIdAndId(long userId, long id) {
        deleteById(id);
    }

    @CacheEvict(value = "user_alerts", allEntries = true)
    @Transactional
    void deleteAllByCoin(String coin);
}
