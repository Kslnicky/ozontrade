package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.worker.model.WorkerLegalSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerLegalSettingsRepository extends JpaRepository<WorkerLegalSettings, Long> {

    @Cacheable(value = "worker_legal_settings", key = "#workerId")
    Optional<WorkerLegalSettings> findByWorkerId(long workerId);

    @CacheEvict(value = "worker_legal_settings", key = "#entity.worker.id")
    @Override
    <S extends WorkerLegalSettings> S save(S entity);

    @CacheEvict(value = "worker_legal_settings", key = "#workerId")
    void deleteByWorkerId(long workerId);
}
