package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByUserEmail(String email);

    Optional<Worker> findByUserOwnRefCode(String refCode);

    @Cacheable(value = "workers_by_user_id", key = "#userId")
    Optional<Worker> findByUserId(long userId);

    @CacheEvict(value = "workers_by_user_id", key = "#entity.user.id")
    @Override
    <S extends Worker> S save(S entity);

    @CacheEvict(value = "workers_by_user_id", key = "#user.id")
    default void deleteById(long workerId, User user) {
        deleteById(workerId);
    }
}
