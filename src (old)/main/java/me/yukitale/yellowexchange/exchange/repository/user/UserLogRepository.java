package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.UserLog;
import me.yukitale.yellowexchange.exchange.model.user.UserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    List<UserLog> findByUserWorkerIdOrderByIdDesc(long workerId, Pageable pageable);

    List<UserLog> findByUserIdOrderByIdDesc(long userId, Pageable pageable);

    List<UserLog> findAllByUserId(long userId);

    List<UserLog> findAllByOrderByIdDesc();

    List<UserLog> findAllByOrderByIdDesc(Pageable pageable);

    List<UserLog> findAllByUserRoleTypeOrderByIdDesc(UserRole.UserRoleType roleType, Pageable pageable);

    List<UserLog> findByUserIdAndForUserOrderByIdDesc(long userId, boolean forUser, Pageable pageable);

    long countByUserIdAndDateGreaterThan(long userId, Date startDate);

    long countByUserIdAndForUser(long userId, boolean forUser);
}
