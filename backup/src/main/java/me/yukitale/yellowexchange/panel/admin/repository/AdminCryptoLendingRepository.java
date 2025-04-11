package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.AdminCryptoLending;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminCryptoLendingRepository extends JpaRepository<AdminCryptoLending, Long> {

    @Cacheable(value = "admin_crypto_lendings")
    @Override
    List<AdminCryptoLending> findAll();

    @Caching(evict = {
            @CacheEvict(value = "admin_crypto_lendings", allEntries = true),
            @CacheEvict(value = "admin_crypto_lendings_by_coin", key = "#adminCryptoLending.coinSymbol")
    })
    void delete(AdminCryptoLending adminCryptoLending);

    @Caching(evict = {
            @CacheEvict(value = "admin_crypto_lendings", allEntries = true),
            @CacheEvict(value = "admin_crypto_lendings_by_coin", key = "#entity.coinSymbol")
    })
    @Override
    <S extends AdminCryptoLending> S save(S entity);

    @Cacheable(value = "admin_crypto_lendings_by_coin", key = "#coinSymbol")
    Optional<AdminCryptoLending> findByCoinSymbol(String coinSymbol);

    @Caching(evict = {
            @CacheEvict(value = "admin_crypto_lendings", allEntries = true),
            @CacheEvict(value = "admin_crypto_lendings_by_coin", allEntries = true)
    })
    void deleteByCoinSymbol(String coinSymbol);
}
