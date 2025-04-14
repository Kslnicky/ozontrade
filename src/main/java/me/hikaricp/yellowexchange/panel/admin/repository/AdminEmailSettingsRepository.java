package me.hikaricp.yellowexchange.panel.admin.repository;

import me.hikaricp.yellowexchange.panel.admin.model.AdminEmailSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminEmailSettingsRepository extends JpaRepository<AdminEmailSettings, Long> {

    @CacheEvict(value = "admin_email_settings", allEntries = true)
    @Override
    <T extends AdminEmailSettings> T save(T value);

    @Cacheable(value = "admin_email_settings")
    default AdminEmailSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin email settings not found");
        }
        return findAll().get(0);
    }
}
