package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserKyc;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKycRepository extends JpaRepository<UserKyc, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE UserKyc k SET k.viewed = true")
    void markAllAsViewed();

    @Modifying
    @Transactional
    @Query("UPDATE UserKyc k SET k.viewed = true WHERE k.viewed = false AND k.user.worker IS NOT NULL AND k.user.worker.id = :workerId")
    void markWorkerAsViewed(@Param("workerId") long workerId);

    long countByViewed(boolean viewed);

    long countByViewedAndUserWorkerId(boolean viewed, long workerId);

    @CacheEvict(value = "user_kyc", key = "#entity.user.id")
    @Override
    <S extends UserKyc> S save(S entity);

    @Transactional
    @CacheEvict(value = "user_kyc", key = "#userId")
    void deleteByUserId(long userId);

    @Cacheable(value = "user_kyc", key = "#userId")
    Optional<UserKyc> findByUserId(long userId);

    List<UserKyc> findAllByOrderByIdDesc();

    List<UserKyc> findAllByUserWorkerId(long workerId);

    @Query("SELECT uk FROM UserKyc uk ORDER BY CASE WHEN uk.level <= 1 THEN uk.lv1Date ELSE uk.lv2Date END DESC")
    List<UserKyc> findAllOrderByDate();
}
