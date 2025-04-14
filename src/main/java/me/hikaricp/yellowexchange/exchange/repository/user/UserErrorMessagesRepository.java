package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserErrorMessages;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

//todo: deleteById
@Repository
public interface UserErrorMessagesRepository extends JpaRepository<UserErrorMessages, Long> {

    @CacheEvict(value = "user_error_messages", key = "#result.user.id")
    @Override
    <T extends UserErrorMessages> T save(T result);

    @Cacheable(value = "user_error_messages", key = "#userId")
    Optional<UserErrorMessages> findByUserId(long userId);

    @Transactional
    @Modifying
    @CacheEvict(value = "user_error_messages", allEntries = true)
    @Query("UPDATE UserErrorMessages u SET u.transferMessage = :transferError")
    void setTransferError(@Param("transferError") String transferError);

    @Transactional
    @Modifying
    @CacheEvict(value = "user_error_messages", allEntries = true)
    @Query("UPDATE UserErrorMessages u SET u.cryptoLendingMessage = :cryptoLendingError")
    void setCryptoLendingError(@Param("cryptoLendingError") String cryptoLendingError);

    @Transactional
    @Modifying
    @CacheEvict(value = "user_error_messages", allEntries = true)
    @Query("UPDATE UserErrorMessages u SET u.p2pMessage = :p2pError")
    void setP2pError(@Param("p2pError") String p2pError);
}
