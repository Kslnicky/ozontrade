package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserFavoriteCoins;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFavoriteCoinsRepository extends JpaRepository<UserFavoriteCoins, Long> {

    @Cacheable(value = "user_favorite_coins", key = "#userId")
    Optional<UserFavoriteCoins> findByUserId(long userId);

    @CacheEvict(value = "user_favorite_coins", key = "#entity.user.id")
    @Override
    <S extends UserFavoriteCoins> S save(S entity);
}
