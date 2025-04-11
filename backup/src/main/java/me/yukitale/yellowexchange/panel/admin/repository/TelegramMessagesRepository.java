package me.yukitale.yellowexchange.panel.admin.repository;

import me.yukitale.yellowexchange.panel.admin.model.TelegramMessages;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramMessagesRepository extends JpaRepository<TelegramMessages, Long> {

    @CacheEvict(value = "telegram_messages", allEntries = true)
    @Override
    <T extends TelegramMessages> T save(T adminSettings);

    @Cacheable("telegram_messages")
    default TelegramMessages findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Telegram Messages not found");
        }
        return findAll().get(0);
    }
}
