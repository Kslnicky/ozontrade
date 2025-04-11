package me.yukitale.yellowexchange.panel.worker.repository;

import me.yukitale.yellowexchange.exchange.model.user.UserErrorMessages;
import me.yukitale.yellowexchange.panel.common.model.ErrorMessages;
import me.yukitale.yellowexchange.panel.worker.model.WorkerErrorMessages;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerErrorMessagesRepository extends JpaRepository<WorkerErrorMessages, Long> {

    @CacheEvict(value = "worker_error_messages", key = "#result.worker.id")
    @Override
    <T extends WorkerErrorMessages> T save(T result);

    @Cacheable(value = "worker_error_messages", key = "#workerId")
    Optional<WorkerErrorMessages> findByWorkerId(long workerId);

    @CacheEvict(value = "worker_error_messages", key = "#workerId")
    void deleteByWorkerId(long workerId);

    @Transactional
    @Modifying
    @CacheEvict(value = "worker_error_messages", allEntries = true)
    @Query("UPDATE WorkerErrorMessages w SET w.transferMessage = :transferError")
    void setTransferError(@Param("transferError") String transferError);

    @Transactional
    @Modifying
    @CacheEvict(value = "worker_error_messages", allEntries = true)
    @Query("UPDATE WorkerErrorMessages w SET w.cryptoLendingMessage = :cryptoLendingError")
    void setCryptoLendingError(@Param("cryptoLendingError") String cryptoLendingError);

    @Transactional
    @Modifying
    @CacheEvict(value = "worker_error_messages", allEntries = true)
    @Query("UPDATE WorkerErrorMessages w SET w.p2pMessage = :p2pError")
    void setP2pError(@Param("p2pError") String p2pError);
}
