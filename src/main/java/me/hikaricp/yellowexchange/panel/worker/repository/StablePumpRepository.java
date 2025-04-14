package me.hikaricp.yellowexchange.panel.worker.repository;

import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.panel.worker.model.StablePump;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StablePumpRepository extends JpaRepository<StablePump, Long> {

    @CacheEvict(value = "stable_pumps", key = "#result.worker.id + '-' + #result.coin.symbol")
    @Override
    <T extends StablePump> T save(T result);

    List<StablePump> findAllByWorkerId(long workerId);

    @Cacheable(value = "stable_pumps", key = "#workerId + '-' + #coinSymbol")
    Optional<StablePump> findByWorkerIdAndCoinSymbol(long workerId, String coinSymbol);

    Optional<StablePump> findByWorkerIdAndId(long workerId, long id);

    @CacheEvict(value = "stable_pumps", key = "#workerId + '-' + #coinSymbol")
    default void deleteByIdAndWorkerIdAndCoinSymbol(long id, long workerId, String coinSymbol) {
        deleteById(id);
    }

    @CacheEvict(value = "stable_pumps", allEntries = true)
    @Transactional
    void deleteAllByWorkerId(long workerId);

    @CacheEvict(value = "stable_pumps", allEntries = true)
    @Transactional
    void deleteAllByCoin(Coin coin);
}
