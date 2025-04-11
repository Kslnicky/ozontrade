package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserRequiredDepositCoin;
import me.yukitale.yellowexchange.panel.common.model.DepositCoin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRequiredDepositCoinRepository extends JpaRepository<UserRequiredDepositCoin, Long> {

    @Cacheable(value = "user_required_deposit_coins", key = "#userId")
    List<UserRequiredDepositCoin> findByUserId(long userId);

    @Transactional
    @CacheEvict(value = "user_required_deposit_coins", key = "#userId")
    void deleteByUserIdAndType(long userId, DepositCoin.CoinType type);

    Optional<UserRequiredDepositCoin> findByUserIdAndType(long userId, DepositCoin.CoinType type);

    @CacheEvict(value = "user_required_deposit_coins", key = "#entity.user.id")
    @Override
    <S extends UserRequiredDepositCoin> S save(S entity);
}
