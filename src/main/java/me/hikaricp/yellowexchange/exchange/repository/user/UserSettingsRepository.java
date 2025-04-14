package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    @Cacheable(value = "user_settings", key = "#userId")
    Optional<UserSettings> findByUserId(long userId);

    @CacheEvict(value = "user_settings", key = "#entity.user.id")
    @Override
    <S extends UserSettings> S save(S entity);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.tradingEnabled = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableTradingForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.swapEnabled = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableSwapForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.supportEnabled = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableSupportForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.fakeWithdrawPending = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableFakeWithdrawPendingForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.fakeWithdrawConfirmed = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableFakeWithdrawConfirmedForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.walletConnectEnabled = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableWalletConnectForAll(long workerId, boolean enable);

    @CacheEvict(value = "user_settings", allEntries = true)
    @Transactional
    @Modifying
    @Query("UPDATE UserSettings u SET u.cryptoLendingEnabled = :enable WHERE u.user.worker IS NOT NULL AND u.user.worker.id = :workerId")
    void enableCryptoLendingForAll(long workerId, boolean enable);

    @CacheEvict(value = "worker_settings", allEntries = true)
    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.transferEnabled = true")
    void enableTransferForAll();

    @CacheEvict(value = "worker_settings", allEntries = true)
    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.cryptoLendingEnabled = true")
    void enableCryptoLendingForAll();
}
