package me.hikaricp.yellowexchange.panel.worker.repository;

import me.hikaricp.yellowexchange.panel.worker.model.WorkerSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface WorkerSettingsRepository extends JpaRepository<WorkerSettings, Long> {

    @Cacheable(value = "worker_settings", key = "#workerId")
    Optional<WorkerSettings> findByWorkerId(long workerId);

    @CacheEvict(value = "worker_settings", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_settings", key = "#entity.worker.id")
    @Override
    <S extends WorkerSettings> S save(S entity);

    @CacheEvict(value = "worker_settings", allEntries = true)
    @Modifying
    @Transactional
    @Query("UPDATE WorkerSettings SET bonusCoin = NULL, bonusAmount = 0 WHERE bonusCoin = :coin")
    void deleteAllByCoinSymbol(@Param("coin") String coin);

    @CacheEvict(value = "worker_settings", allEntries = true)
    @Modifying
    @Transactional
    @Query("UPDATE WorkerSettings w SET w.transferEnabled = true")
    void enableTransferForAll();

    @CacheEvict(value = "worker_settings", allEntries = true)
    @Modifying
    @Transactional
    @Query("UPDATE WorkerSettings w SET w.cryptoLendingEnabled = true")
    void enableCryptoLendingForAll();
}
