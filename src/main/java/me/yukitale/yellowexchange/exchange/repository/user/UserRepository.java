package me.yukitale.yellowexchange.exchange.repository.user;

import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.model.user.UserRole;
import org.antlr.v4.runtime.misc.Triple;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(value = "users_by_id", key = "#id")
    @Override
    Optional<User> findById(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Cacheable(value = "users_by_email", key = "#email")
    Optional<User> findByEmail(String email);

    @Caching(evict = {
            @CacheEvict(value = "users_by_id", key = "#entity.id"),
            @CacheEvict(value = "users_by_email", key = "#entity.email")
    })
    @Override
    <S extends User> S save(S entity);

    boolean existsByEmail(String email);

    @Caching(evict = {
            @CacheEvict(value = "users_by_id", allEntries = true),
            @CacheEvict(value = "users_by_email", allEntries = true)
    })
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.vip = :enable WHERE u.worker IS NOT NULL AND u.worker.id = :workerId")
    void enableVipForAll(long workerId, boolean enable);

    //worker start
    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByCountries(@Param("workerId") long workerId);

    default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("workerId") long workerId) {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByCountries(workerId)) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId AND u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByCountries(@Param("workerId") long workerId, @Param("startDate") Date startDate);

    default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByCountries(workerId, startDate)) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    //refers start
    @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.depositsPrice) FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId GROUP BY u.referrer ORDER by registrations DESC")
    List<Object[]> findRegistrationsByRefers(@Param("workerId") long workerId);

    default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("workerId") long workerId) {
        Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByRefers(workerId)) {
            String referrer = (String) objects[0];
            if (referrer.isEmpty()) {
                referrer = "N/A";
            }

            long users = (long) objects[1];
            long depositsCount = (long) objects[2];
            double depositsPrice = (double) objects[3];

            registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
        }

        return registrations;
    }

    @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.depositsPrice) FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId AND u.registered >= :startDate GROUP BY u.referrer ORDER by registrations DESC")
    List<Object[]> findRegistrationsByRefers(@Param("workerId") long workerId, @Param("startDate") Date startDate);

    default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
        Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByRefers(workerId, startDate)) {
            String referrer = (String) objects[0];
            if (referrer.isEmpty()) {
                referrer = "N/A";
            }

            long users = (long) objects[1];
            long depositsCount = (long) objects[2];
            double depositsPrice = (double) objects[3];

            registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
        }

        return registrations;
    }
    //refers end
    //worker end

    //refers start
    @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.depositsPrice) FROM User u GROUP BY u.referrer ORDER by registrations DESC")
    List<Object[]> findRegistrationsByRefers();

    default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap() {
        Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByRefers()) {
            String referrer = (String) objects[0];
            if (referrer.isEmpty()) {
                referrer = "N/A";
            }

            long users = (long) objects[1];
            long depositsCount = (long) objects[2];
            double depositsPrice = (double) objects[3];

            registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
        }

        return registrations;
    }

    @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.depositsPrice) FROM User u WHERE u.registered >= :startDate GROUP BY u.referrer ORDER by registrations DESC")
    List<Object[]> findRegistrationsByRefers(@Param("startDate") Date startDate);

    default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("startDate") Date startDate) {
        Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByRefers(startDate)) {
            String referrer = (String) objects[0];
            if (referrer.isEmpty()) {
                referrer = "N/A";
            }

            long users = (long) objects[1];
            long depositsCount = (long) objects[2];
            double depositsPrice = (double) objects[3];

            registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
        }

        return registrations;
    }
    //refers end

    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByCountries();

    default Map<String, Long> findRegistrationsByCountriesAsMap() {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByCountries()) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByCountries(@Param("startDate") Date startDate);

    default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("startDate") Date startDate) {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByCountries(startDate)) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.promocode = :promocode AND u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByPromocode(@Param("promocode") String promocode, @Param("startDate") Date startDate);

    default Map<String, Long> findRegistrationsByPromocodeAsMap(String promocode, Date startDate) {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByPromocode(promocode, startDate)) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.promocode = :promocode GROUP BY u.regCountryCode ORDER by registrations DESC")
    List<Object[]> findRegistrationsByPromocode(@Param("promocode") String promocode);

    default Map<String, Long> findRegistrationsByPromocodeAsMap(String promocode) {
        Map<String, Long> registrations = new LinkedHashMap<>();
        for (Object[] objects : findRegistrationsByPromocode(promocode)) {
            registrations.put((String) objects[0], (Long) objects[1]);
        }

        return registrations;
    }

    List<User> findAllByLastIpOrRegIpOrderByIdDesc(String lastIp, String regIp);

    List<User> findAllByRoleTypeOrderByLastActivityDesc(UserRole.UserRoleType roleType, Pageable pageable);

    List<User> findAllByOrderByLastActivityDesc(Pageable pageable);

    List<User> findAllByWorkerIdOrderByLastActivityDesc(long workerId, Pageable pageable);

    List<User> findAllByLastOnlineGreaterThan(long lastActivity);

    List<User> findAllByRoleTypeAndLastOnlineGreaterThanOrderByLastActivityDesc(UserRole.UserRoleType roleType, long lastActivity, Pageable pageable);

    List<User> findAllByLastOnlineGreaterThanOrderByLastActivityDesc(long lastActivity, Pageable pageable);

    List<User> findAllByWorkerIdAndLastOnlineGreaterThanOrderByLastActivityDesc(long workerId, long lastActivity, Pageable pageable);

    Optional<User> findByRoleTypeAndEmail(UserRole.UserRoleType roleType, String email);

    Optional<User> findByEmailAndWorkerId(String email, long workerId);

    Optional<User> findByIdAndWorkerId(long id, long workerId);

    long countByRoleType(UserRole.UserRoleType roleType);

    long countByLastOnlineGreaterThan(long lastActivity);

    long countByRoleTypeAndLastOnlineGreaterThan(UserRole.UserRoleType roleType, long lastActivity);

    long countByWorkerIdAndLastOnlineGreaterThan(long workerId, long lastActivity);

    long countByWorkerId(long workerId);

    boolean existsByIdAndWorkerId(long id, long workerId);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.worker = null WHERE u.worker IS NOT NULL AND u.worker.id = :workerId")
    void removeWorkerFromUsers(@Param("workerId") long workerId);

    //start admin stats
    @Query("SELECT DATE(u.registered), COUNT(u) " +
            "FROM User u " +
            "GROUP BY DATE(u.registered)")
    List<Object[]> getUsersCountPerDay();

    default Map<Date, Long> getUsersCountPerDayAsMap() {
        Map<Date, Long> map = new LinkedHashMap<>();
        for (Object[] objects : getUsersCountPerDay()) {
            map.put((Date) objects[0], (Long) objects[1]);
        }

        return map;
    }
    //end admin stats

    //start worker stats
    @Query("SELECT DATE(u.registered), COUNT(u) " +
            "FROM User u " +
            "WHERE u.worker IS NOT NULL AND u.worker.id = :workerId " +
            "GROUP BY DATE(u.registered)")
    List<Object[]> getUsersCountPerDayByWorkerId(@Param("workerId") long workerId);

    default Map<Date, Long> getUsersCountPerDayByWorkerIdAsMap(long workerId) {
        Map<Date, Long> map = new LinkedHashMap<>();
        for (Object[] objects : getUsersCountPerDayByWorkerId(workerId)) {
            map.put((Date) objects[0], (Long) objects[1]);
        }

        return map;
    }

    @Query("SELECT u.worker.id, COUNT(u) " +
            "FROM User u " +
            "WHERE u.worker IS NOT NULL AND u.registered >= :startDate GROUP BY u.worker")
    List<Object[]> getWorkerUsersCountByDateGreaterThan(@Param("startDate") Date startDate);

    default Map<Long, Long> getWorkerUsersCountByDateGreaterThanAsMap(Date startDate) {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Object[] objects : getWorkerUsersCountByDateGreaterThan(startDate)) {
            map.put((Long) objects[0], (Long) objects[1]);
        }

        return map;
    }

    @Query("SELECT u.worker.id, COUNT(u) " +
            "FROM User u " +
            "WHERE u.worker IS NOT NULL " +
            "GROUP BY u.worker")
    List<Object[]> getWorkerUsersCount();

    default Map<Long, Long> getWorkerUsersCountAsMap() {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Object[] objects : getWorkerUsersCount()) {
            map.put((Long) objects[0], (Long) objects[1]);
        }

        return map;
    }
    //end worker stats

    @Query("SELECT u.id, u.email FROM User u WHERE u.regIp = :ip ORDER BY u.id DESC")
    List<Object[]> findTwinksByIp(@Param("ip") String ip);

    default Map<Long, String> findTwinksByIpAsMap(String ip) {
        Map<Long, String> map = new LinkedHashMap<>();

        for (Object[] objects : findTwinksByIp(ip)) {
            map.put((Long) objects[0], (String) objects[1]);
        }

        return map;
    }

    @Query("SELECT u.id, u.email FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId AND u.regIp = :ip ORDER BY u.id DESC")
    List<Object[]> findTwinksByIpAndWorker(@Param("ip") String ip, @Param("workerId") long workerId);

    default Map<Long, String> findTwinksByIpAndWorkerAsMap(String ip, long workerId) {
        Map<Long, String> map = new LinkedHashMap<>();

        for (Object[] objects : findTwinksByIpAndWorker(ip, workerId)) {
            map.put((Long) objects[0], (String) objects[1]);
        }

        return map;
    }
}
