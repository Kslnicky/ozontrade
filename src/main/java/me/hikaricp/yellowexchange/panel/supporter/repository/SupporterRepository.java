package me.hikaricp.yellowexchange.panel.supporter.repository;

import me.hikaricp.yellowexchange.panel.supporter.model.Supporter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SupporterRepository extends JpaRepository<Supporter, Long> {

    @CacheEvict(value = "supporters_by_user_id", key = "#entity.user.id")
    @Override
    <S extends Supporter> S save(S entity);

    @Cacheable(value = "supporters_by_user_id", key = "#userId")
    Optional<Supporter> findByUserId(long userId);

    @CacheEvict(value = "supporters_by_user_id", key = "#userId")
    @Transactional
    void deleteByUserId(long userId);
}
