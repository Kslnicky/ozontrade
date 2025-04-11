package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE UserTransaction t SET t.viewed = true WHERE t.transactionType = :transactionType")
    void markAllAsViewed(@Param("transactionType") int transactionType);

    @Modifying
    @Transactional
    @Query("UPDATE UserTransaction t SET t.viewed = true WHERE t.viewed = false AND t.transactionType = :transactionType AND t.user.worker IS NOT NULL AND t.user.worker.id = :workerId")
    void markWorkerAsViewed(@Param("transactionType") int transactionType, @Param("workerId") long workerId);

    @Query("SELECT count(t.id) from UserTransaction t WHERE t.viewed = false AND t.transactionType = :transactionType")
    long countByUnviewed(@Param("transactionType") int transactionType);

    @Query("SELECT count(t.id) from UserTransaction t WHERE t.viewed = false AND t.transactionType = :transactionType AND t.user.worker IS NOT NULL AND t.user.worker.id = :workerId")
    long countByUnviewedAndWorkerId(@Param("transactionType") int transactionType, @Param("workerId") long workerId);

    Optional<UserTransaction> findByIdAndUserIdAndUserWorkerId(long id, long userId, long workerId);

    Optional<UserTransaction> findByIdAndUserId(long id, long userId);

    default List<UserTransaction> findByTypeOrderByIdDesc(UserTransaction.Type type, Pageable pageable) {
        return findByTransactionTypeOrderByIdDesc(type.ordinal(), pageable);
    }

    List<UserTransaction> findByTransactionTypeOrderByIdDesc(int transactionType, Pageable pageable);

    default List<UserTransaction> findByTypeAndUserWorkerIdOrderByIdDesc(UserTransaction.Type type, long workerId) {
        return findByTransactionTypeAndUserWorkerIdOrderByIdDesc(type.ordinal(), workerId);
    }

    List<UserTransaction> findByTransactionTypeAndUserWorkerIdOrderByIdDesc(int transactionType, long workerId);

    List<UserTransaction> findByUserId(long userId);

    List<UserTransaction> findByUserIdOrderByIdDesc(long userId);
}
