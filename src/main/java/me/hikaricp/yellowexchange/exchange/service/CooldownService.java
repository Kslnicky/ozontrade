package me.hikaricp.yellowexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class CooldownService {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        startCleanTask();
    }

    public void startCleanTask() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            List<String> keysToDelete = new ArrayList<>();
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, Long> entry : this.cooldowns.entrySet()) {
                if (entry.getValue() < currentTime) {
                    keysToDelete.add(entry.getKey());
                }
            }

            keysToDelete.forEach(this.cooldowns::remove);
        }, 5, 1, TimeUnit.SECONDS);
    }

    public void addCooldown(String key, Duration duration) {
        this.cooldowns.put(key, System.currentTimeMillis() + duration.toMillis());
    }

    public void removeCooldown(String key) {
        this.cooldowns.remove(key);
    }

    public boolean isCooldown(String key) {
        return this.cooldowns.containsKey(key);
    }

    public String getCooldownLeft(String key) {
        long time = this.cooldowns.get(key);
        long seconds = (time - System.currentTimeMillis()) / 1000;

        if (seconds <= 0) {
            return "0 s.";
        } else if (seconds >= 86400) {
            return seconds / 86400 + " d. " + (seconds % 86400) / 3600 + " h.";
        } else if (seconds > 3600) {
            return seconds / 3600 + " h. " + (seconds % 3600) / 60 + " min. ";
        } else if (seconds > 60) {
            return seconds / 60 + " min. " + (seconds % 60) + " s.";
        } else {
            return seconds + " s.";
        }
    }
}
