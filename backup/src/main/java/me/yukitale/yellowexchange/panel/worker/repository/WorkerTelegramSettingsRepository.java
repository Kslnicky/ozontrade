package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.worker.model.WorkerSettings;
import me.yukitale.yellowexchange.panel.worker.model.WorkerTelegramSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerTelegramSettingsRepository extends JpaRepository<WorkerTelegramSettings, Long> {

    @Cacheable(value = "worker_telegram_settings", key = "#workerId")
    Optional<WorkerTelegramSettings> findByWorkerId(long workerId);

    @CacheEvict(value = "worker_telegram_settings", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_telegram_settings", key = "#entity.worker.id")
    @Override
    <S extends WorkerTelegramSettings> S save(S entity);
}
