package me.hikaricp.yellowexchange.panel.common.repository;

import me.hikaricp.yellowexchange.panel.common.model.Domain;
import me.hikaricp.yellowexchange.panel.worker.model.Worker;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    @Cacheable(value = "domains_by_id", key = "#id")
    @Override
    Optional<Domain> findById(Long id);

    @Cacheable(value = "domains_by_name", key = "#name")
    Optional<Domain> findByName(String name);

    //todo: cache start
    Optional<Domain> findByIdAndWorkerId(long id, long workerId);

    Optional<Domain> findByWorkerIdAndName(long workerId, String name);

    long countByWorkerId(long workerId);

    List<Domain> findByWorkerIdOrderByIdDesc(long workerId, Pageable pageable);
    //cache end

    @Caching(evict = {
            @CacheEvict(value = "domains_by_name", key = "#domain.name"),
            @CacheEvict(value = "domains_by_id", key = "#domain.id")
    })
    @Override
    <T extends Domain> T save(T domain);

    @Caching(evict = {
            @CacheEvict(value = "domains_by_name", key = "#domain.name"),
            @CacheEvict(value = "domains_by_id", key = "#domain.id")
    })
    default void delete(Domain domain) {
        deleteById(domain.getId());
    }

    @Caching(evict = {
            @CacheEvict(value = "domains_by_name", allEntries = true),
            @CacheEvict(value = "domains_by_id", allEntries = true)
    })
    void deleteAllByWorkerId(long workerId);

    List<Domain> findByWorkerIdIsNullOrderByIdDesc(Pageable pageable);

    List<Domain> findByWorkerIdIsNotNullOrderByIdDesc(Pageable pageable);

    long countByWorkerIdIsNull();

    long countByWorkerIdIsNotNull();

    List<Domain> findByWorkerUserEmail(String name);

    Optional<Domain> findByWorkerIdIsNotNullAndName(String name);

    @Query("SELECT d.worker FROM Domain d WHERE d.name = :name")
    Optional<Worker> findWorkerByName(@Param("name") String name);
}

