package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.EmailBan;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EmailBanRepository extends JpaRepository<EmailBan, Long> {

    @CacheEvict(value = "email_bans", key = "#result.email")
    @Override
    <T extends EmailBan> T save(T result);

    @Transactional
    @CacheEvict(value = "email_bans", key = "#email")
    void deleteByEmail(String email);

    @Cacheable(value = "email_bans", key = "#email")
    boolean existsByEmail(String email);
}

