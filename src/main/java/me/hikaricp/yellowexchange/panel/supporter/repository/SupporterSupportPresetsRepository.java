package me.hikaricp.yellowexchange.panel.supporter.repository;

import me.hikaricp.yellowexchange.panel.supporter.model.SupporterSupportPreset;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupporterSupportPresetsRepository extends JpaRepository<SupporterSupportPreset, Long> {

    Optional<SupporterSupportPreset> findByIdAndSupporterId(long id, long supporterId);

    @CacheEvict(value = "supporter_support_presets", key = "#result.supporter.id")
    <T extends SupporterSupportPreset> T save(T result);

    @CacheEvict(value = "supporter_support_presets", key = "#supporterId")
    default <S extends SupporterSupportPreset> List<S> saveAllBySupporterId(Iterable<S> entities, long supporterId) {
        return saveAll(entities);
    }

    @Cacheable(value = "supporter_support_presets", key = "#supporterId")
    List<SupporterSupportPreset> findAllBySupporterId(long supporterId);

    @CacheEvict(value = "supporter_support_presets", key = "#supporterId")
    default void deleteById(long id, long supporterId) {
        deleteById(id);
    }

    @CacheEvict(value = "supporter_support_presets", key = "#supporterId")
    void deleteAllBySupporterId(long supporterId);

    long countBySupporterId(long supporterId);

    boolean existsByIdAndSupporterId(long id, long supporterId);
}
