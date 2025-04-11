package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminErrorMessages;
import me.yukitale.yellowexchange.panel.admin.model.AdminLegalSettings;
import me.yukitale.yellowexchange.panel.common.model.ErrorMessages;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminErrorMessagesRepository extends JpaRepository<AdminErrorMessages, Long> {

    @CachePut("admin_error_messages")
    @Override
    <T extends AdminErrorMessages> T save(T value);

    @Cacheable("admin_error_messages")
    default AdminErrorMessages findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin error messages not found");
        }
        return findAll().get(0);
    }
}
