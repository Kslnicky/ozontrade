package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserSupportDialog;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSupportDialogRepository extends JpaRepository<UserSupportDialog, Long> {

    long countByUserWorkerId(long workerId);

    @Cacheable(value = "user_support_dialogs", key = "#userId")
    Optional<UserSupportDialog> findByUserId(long userId);

    Optional<UserSupportDialog> findByUserEmail(String email);

    Optional<UserSupportDialog> findByUserWorkerIdAndUserEmail(long workerId, String email);

    @CacheEvict(value = "user_support_dialogs", key = "#result.user.id")
    @Override
    <T extends UserSupportDialog> T save(T result);

    @Transactional
    @CacheEvict(value = "user_support_dialogs", key = "#userId")
    void deleteByUserId(long userId);

    List<UserSupportDialog> findByOnlyWelcomeAndUserWorkerIdOrderByLastMessageDateDesc(boolean onlyWelcome, long workerId, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeOrderByLastMessageDateDesc(boolean onlyWelcome, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(boolean onlyWelcome, long workerId, int supportUnviewedMessages, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(boolean onlyWelcome, int supportUnviewedMessages, Pageable pageable);

    long countByOnlyWelcomeAndUserWorkerId(boolean onlyWelcome, long workerId);

    long countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(boolean onlyWelcome, long workerId, int supportUnviewedMessages);

    long countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(boolean onlyWelcome, int supportUnviewedMessages);

    long countByOnlyWelcome(boolean onlyWelcome);
}
