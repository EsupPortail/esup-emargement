package org.esupportail.emargement.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Active le mécanisme de cache Spring pour les valeurs de configuration applicative
 * (cf. {@link org.esupportail.emargement.services.AppliConfigService}).
 *
 * Le cache "appliConfig" évite des centaines de SELECT redondants lors de l'import
 * ADE (plusieurs lookups par session × N sessions).
 */
@Configuration
@EnableCaching
public class CachingConfig {
    public static final String APPLI_CONFIG_CACHE = "appliConfig";
    public static final String PHOTOS_CACHE = "photosCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(APPLI_CONFIG_CACHE, PHOTOS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(1000));
        return manager;
    }
}