package me.hikaricp.yellowexchange.exchange.repository;

import me.hikaricp.yellowexchange.exchange.model.Coin;
import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoinRepository extends JpaRepository<Coin, Long> {

    default Coin findFirst() {
        return findAll().get(0);
    }

    @Cacheable(value = "coins_json")
    default String findCoinsAsJson() {
        return JsonUtil.writeJsonPretty(findAll());
    }

    @Override
    default List<Coin> findAll() {
        return findByOrderByPosition();
    }

    @Cacheable(value = "all_coins")
    List<Coin> findByOrderByPosition();

    @Cacheable(value = "coins_by_id", key = "#id")
    Optional<Coin> findById(long id);

    @Cacheable(value = "coins_by_symbol", key = "#symbol")
    Optional<Coin> findBySymbol(String symbol);

    @Caching(evict = {
            @CacheEvict(value = "coins_by_id", key = "#entity.id"),
            @CacheEvict(value = "coins_by_symbol", key = "#entity.symbol"),
            @CacheEvict(value = "all_coins", allEntries = true),
            @CacheEvict(value = "coins_json", allEntries = true)
    })
    @Override
    <S extends Coin> S save(S entity);

    @Caching(evict = {
            @CacheEvict(value = "coins_by_id", key = "#coin.id"),
            @CacheEvict(value = "coins_by_symbol", key = "#coin.symbol"),
            @CacheEvict(value = "all_coins", allEntries = true),
            @CacheEvict(value = "coins_json", allEntries = true)
    })
    default void delete(Coin coin) {
        deleteById(coin.getId());
    }
}
