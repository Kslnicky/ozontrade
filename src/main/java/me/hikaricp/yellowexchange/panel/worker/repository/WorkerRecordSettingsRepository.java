package me.hikaricp.yellowexchange.panel.worker.repository;

import me.hikaricp.yellowexchange.panel.worker.model.WorkerRecordSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRecordSettingsRepository extends JpaRepository<WorkerRecordSettings, Long> {

    List<WorkerRecordSettings> findByWorkerId(long workerId);

    Optional<WorkerRecordSettings> findByIdAndWorkerId(long id, long workerId);

    @Override
    @CacheEvict(value = "record_settings", key = "#result.emailEnd")
    <T extends WorkerRecordSettings> T save(T result);

    @CacheEvict(value = "record_settings", key = "#emailEnd")
    default void deleteByIdAndEmailEnd(long id, long emailEnd) {
        deleteById(id);
    }

    @Cacheable(value = "record_settings", key = "#emailEnd")
    Optional<WorkerRecordSettings> findByEmailEnd(long emailEnd);

    long countByWorkerId(long workerId);

    boolean existsByEmailEnd(long emailEnd);
}
