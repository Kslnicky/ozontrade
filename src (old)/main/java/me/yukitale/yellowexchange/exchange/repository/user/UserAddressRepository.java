package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserAddress;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    long countByUserWorkerId(long workerId);

    List<UserAddress> findByUserId(long userId);

    Optional<UserAddress> findByAddressIgnoreCaseAndTagIgnoreCase(String address, String tag);

    Optional<UserAddress> findByAddressIgnoreCase(String address);

    @Cacheable(value = "user_addresses", key = "#userId + '-' + #coinType.name")
    Optional<UserAddress> findByUserIdAndCoinType(long userId, DepositCoin.CoinType coinType);

    long countByUserIdAndCoinType(long userId, DepositCoin.CoinType coinType);

    @CacheEvict(value = "user_addresses", key = "#entity.user.id + '-' + #entity.coinType.name")
    @Override
    <S extends UserAddress> S save(S entity);
}
