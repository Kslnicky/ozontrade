package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminDepositCoin;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminDepositCoinRepository extends JpaRepository<AdminDepositCoin, Long> {

    @Override
    default List<AdminDepositCoin> findAll() {
        return findByOrderByPosition();
    }

    @Cacheable(value = "admin_deposit_coins")
    List<AdminDepositCoin> findByOrderByPosition();

    @Caching(evict = {
            @CacheEvict(value = "admin_deposit_coin_types", allEntries = true),
            @CacheEvict(value = "admin_deposit_coins", allEntries = true)
    })
    @Override
    <T extends AdminDepositCoin> T save(T value);

    @Cacheable(value = "admin_deposit_coin_types", key = "#type")
    Optional<AdminDepositCoin> findByType(DepositCoin.CoinType type);
}
