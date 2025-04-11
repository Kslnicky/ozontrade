package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.worker.model.WorkerSupportPreset;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerSupportPresetRepository extends JpaRepository<WorkerSupportPreset, Long> {

    @CacheEvict(value = "worker_support_presets", key = "#result.worker.id")
    <T extends WorkerSupportPreset> T save(T result);

    @CacheEvict(value = "worker_support_presets", key = "#workerId")
    default <S extends WorkerSupportPreset> List<S> saveAllByWorkerId(Iterable<S> entities, long workerId) {
        return saveAll(entities);
    }

    @Cacheable(value = "worker_support_presets", key = "#workerId")
    List<WorkerSupportPreset> findAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_support_presets", key = "#workerId")
    default void deleteById(long id, long workerId) {
        deleteById(id);
    }

    @CacheEvict(value = "worker_support_presets", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    long countByWorkerId(long workerId);

    boolean existsByIdAndWorkerId(long id, long workerId);
}
