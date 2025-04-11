package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.worker.model.WorkerCryptoLending;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerCryptoLendingRepository extends JpaRepository<WorkerCryptoLending, Long> {

    @Cacheable(value = "worker_crypto_lendings", key = "#workerId")
    List<WorkerCryptoLending> findAllByWorkerId(long workerId);

    Optional<WorkerCryptoLending> findByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    Optional<WorkerCryptoLending> findByIdAndWorkerId(long id, long workerId);

    @CacheEvict(value = "worker_crypto_lendings", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_crypto_lendings", key = "#workerCryptoLending.worker.id")
    void delete(WorkerCryptoLending workerCryptoLending);

    @CacheEvict(value = "worker_crypto_lendings", key = "#entity.worker.id")
    @Override
    <S extends WorkerCryptoLending> S save(S entity);

    @CacheEvict(value = "worker_crypto_lendings", allEntries = true)
    void deleteByCoinSymbol(String coinSymbol);
}
