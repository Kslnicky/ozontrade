package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.panel.worker.model.WithdrawCoinLimit;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawCoinLimitRepository extends JpaRepository<WithdrawCoinLimit, Long> {

    @Cacheable(value = "worker_all_withdraw_coin_limits", key = "#workerId")
    List<WithdrawCoinLimit> findAllByWorkerId(long workerId);

    @Cacheable(value = "worker_withdraw_coin_limits", key = "#workerId + '-' + #coinSymbol")
    Optional<WithdrawCoinLimit> findByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    @Caching(evict = {
            @CacheEvict(value = "worker_all_withdraw_coin_limits", key = "#entity.worker.id"),
            @CacheEvict(value = "worker_withdraw_coin_limits", key = "#entity.worker.id + '-' + #entity.coinSymbol")
    })
    @Override
    <S extends WithdrawCoinLimit> S save(S entity);

    @Caching(evict = {
            @CacheEvict(value = "worker_all_withdraw_coin_limits", key = "#withdrawCoinLimit.worker.id"),
            @CacheEvict(value = "worker_withdraw_coin_limits", key = "#withdrawCoinLimit.worker.id + '-' + #withdrawCoinLimit.coinSymbol")
    })
    default void delete(WithdrawCoinLimit withdrawCoinLimit) {
        deleteById(withdrawCoinLimit.getId());
    }

    @Caching(evict = {
            @CacheEvict(value = "worker_all_withdraw_coin_limits", key = "#workerId"),
            @CacheEvict(value = "worker_withdraw_coin_limits", allEntries = true)
    })
    @Transactional
    void deleteAllByWorkerId(long workerId);

    @Caching(evict = {
            @CacheEvict(value = "worker_all_withdraw_coin_limits", allEntries = true),
            @CacheEvict(value = "worker_withdraw_coin_limits", allEntries = true)
    })
    @Transactional
    void deleteAllByCoinSymbol(String coinSymbol);
}
