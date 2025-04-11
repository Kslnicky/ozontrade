package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings, Long> {

    @CacheEvict(value = "admin_settings_main", allEntries = true)
    @Override
    <T extends AdminSettings> T save(T adminSettings);

    @Cacheable("admin_settings_main")
    default AdminSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin settings not found");
        }
        return findAll().get(0);
    }
}
