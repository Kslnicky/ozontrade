package me.hikaricp.yellowexchange.security.auth.captcha;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pig4cloud.captcha.base.Captcha;
import jakarta.annotation.PostConstruct;
import me.hikaricp.yellowexchange.utils.MyDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CaptchaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);

    private final CachedCaptcha[] cachedCaptchas = new CachedCaptcha[1000];
    private final LoadingCache<String, Optional<CachedCaptcha>> userCaptchaCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.of(1, ChronoUnit.MINUTES))
            .build(new CacheLoader<>() {
                @Override
                public Optional<CachedCaptcha> load(String key) throws Exception {
                    return Optional.ofNullable(cachedCaptchas[ThreadLocalRandom.current().nextInt(1000)]);
                }
            });

    public Optional<CachedCaptcha> getCaptcha(String key) {
        try {
            return this.userCaptchaCache.get(key);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public void removeCaptchaCache(String key) {
        this.userCaptchaCache.invalidate(key);
    }

    @PostConstruct
    public void init() {
        generatingCaptcha();
    }

    private void generatingCaptcha() {
        AtomicInteger completedCaptchas = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    CustomCaptcha specCaptcha = new CustomCaptcha(130, 48, 5);
                    specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
                    specCaptcha.setFont(Captcha.FONT_2);

                    cachedCaptchas[finalI] = new CachedCaptcha(finalI + 1, specCaptcha.text(), specCaptcha.toBase64());

                    completedCaptchas.incrementAndGet();
                } catch (Exception ex) {
                    throw new RuntimeException("Captcha generation error");
                }
            });
        }

        while (completedCaptchas.get() < 1000) {
            LOGGER.info("Captcha generation: " + new MyDecimal(completedCaptchas.get() / 100D, true) + "%");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        executor.shutdownNow();

        System.gc();

        LOGGER.info("Captcha generated");
    }
}
