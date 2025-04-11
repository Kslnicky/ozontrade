package me.yukitale.yellowexchange.panel.common.repository;

import me.yukitale.yellowexchange.panel.common.model.Promocode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromocodeRepository extends JpaRepository<Promocode, Long> {

    @Cacheable(value = "promocodes_by_name", key = "#name")
    Optional<Promocode> findByName(String name);

    @CacheEvict(value = "promocodes_by_name", key = "#entity.name")
    @Override
    <S extends Promocode> S save(S entity);

    @CacheEvict(value = "promocodes_by_name", key = "#promocode.name")
    default void delete(Promocode promocode) {
        deleteById(promocode.getId());
    }

    List<Promocode> findByOrderByIdDesc(Pageable pageable);

    List<Promocode> findByWorkerIdOrderByIdDesc(long workerId);

    Optional<Promocode> findByNameIgnoreCaseAndWorkerId(String name, long workerId);

    @CacheEvict(value = "promocodes_by_name", allEntries = true)
    void deleteAllByWorkerId(long workerId);

    boolean existsByNameIgnoreCase(String name);

    Optional<Promocode> findByIdAndWorkerId(long id, long workerId);

    long countByWorkerId(long workerId);

    @Transactional
    @CacheEvict(value = "promocodes_by_name", allEntries = true)
    void deleteAllByCoinSymbol(String coinSymbol);
}
