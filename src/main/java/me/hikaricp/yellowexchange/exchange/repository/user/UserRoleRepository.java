package me.hikaricp.yellowexchange.exchange.repository.user;

import me.hikaricp.yellowexchange.exchange.model.user.UserRole;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

  @Cacheable(value = "roles", key = "#name")
  Optional<UserRole> findByName(UserRole.UserRoleType name);

  @CacheEvict(value = "roles", key = "#entity.name")
  @Override
  <S extends UserRole> S save(S entity);

  boolean existsByName(UserRole.UserRoleType name);
}
