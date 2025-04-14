package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserApiKey;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {

    @Cacheable(value = "user_api_keys", key = "#userId")
    List<UserApiKey> findByUserIdOrderByIdDesc(long userId);

    @CacheEvict(value = "user_api_keys", key = "#userId")
    default void deleteById(long id, long userId) {
        deleteById(id);
    }

    @CacheEvict(value = "user_api_keys", key = "#entity.user.id")
    @Override
    <S extends UserApiKey> S save(S entity);

    boolean existsByIdAndUserId(long id, long userId);

    long countByUserId(long userId);
}
