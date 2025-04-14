package me.hikaricp.yellowexchange.panel.worker.repository;

import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.panel.worker.model.FastPump;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FastPumpRepository extends JpaRepository<FastPump, Long> {

    @CacheEvict(value = "fast_pumps", key = "#result.worker.id + '-' + #result.coin.symbol")
    <T extends FastPump> T save(T result);

    @Cacheable(value = "fast_pumps", key = "#workerId + '-' + #coinSymbol")
    List<FastPump> findAllByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    boolean existsByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    @Transactional
    @CacheEvict(value = "fast_pumps", key = "#workerId + '-' + #coinSymbol")
    void deleteAllByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    @Transactional
    @CacheEvict(value = "fast_pumps", allEntries = true)
    void deleteAllByWorkerId(long workerId);

    @Transactional
    @CacheEvict(value = "fast_pumps", allEntries = true)
    void deleteAllByCoin(Coin coin);
}
