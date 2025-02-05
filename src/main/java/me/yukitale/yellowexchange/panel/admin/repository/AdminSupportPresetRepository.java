package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminSupportPreset;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminSupportPresetRepository extends JpaRepository<AdminSupportPreset, Long> {

    @Cacheable(value = "admin_support_presets")
    @Override
    List<AdminSupportPreset> findAll();

    @CacheEvict(value = "admin_support_presets", allEntries = true)
    @Override
    <T extends AdminSupportPreset> T save(T value);

    @CacheEvict(value = "admin_support_presets", allEntries = true)
    @Override
    void deleteById(Long aLong);
}
