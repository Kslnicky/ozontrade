package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminCoinSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCoinSettingsRepository extends JpaRepository<AdminCoinSettings, Long> {

    @CacheEvict("admin_coin_settings")
    @Override
    <T extends AdminCoinSettings> T save(T value);

    @Cacheable("admin_coin_settings")
    default AdminCoinSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin coin settings not found");
        }
        return findAll().get(0);
    }
}
