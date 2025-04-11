package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import me.yukitale.yellowexchange.panel.worker.model.WorkerDepositCoin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerDepositCoinRepository extends JpaRepository<WorkerDepositCoin, Long> {

    default List<WorkerDepositCoin> findAllByWorkerId(long workerId) {
        return findAllByWorkerIdOrderByPosition(workerId);
    }

    @Cacheable(value = "worker_deposit_coins", key = "#workerId")
    List<WorkerDepositCoin> findAllByWorkerIdOrderByPosition(long workerId);

    Optional<WorkerDepositCoin> findByIdAndWorkerId(long id, long workerId);

    Optional<WorkerDepositCoin> findByTypeAndWorkerId(DepositCoin.CoinType coinType, long workerId);

    @CacheEvict(value = "worker_deposit_coins", key = "#workerId")
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "worker_deposit_coins", key = "#entity.worker.id")
    @Override
    <S extends WorkerDepositCoin> S save(S entity);
}
