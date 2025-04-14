package me.hikaricp.yellowexchange.panel.worker.repository;

import me.hikaricp.yellowexchange.panel.worker.model.WorkerCoinSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerCoinSettingsRepository extends JpaRepository<WorkerCoinSettings, Long> {

    @Cacheable(value = "worker_coin_settings", key = "#workerId")
    Optional<WorkerCoinSettings> findByWorkerId(long workerId);

    @CacheEvict(value = "worker_coin_settings", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_coin_settings", key = "#entity.worker.id")
    @Override
    <S extends WorkerCoinSettings> S save(S entity);
}
