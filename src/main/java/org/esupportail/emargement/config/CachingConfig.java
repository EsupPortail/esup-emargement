package org.esupportail.emargement.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(APPLI_CONFIG_CACHE);
    }
}