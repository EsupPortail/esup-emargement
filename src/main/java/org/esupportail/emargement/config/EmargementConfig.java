package org.esupportail.emargement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="emargement")
@PropertySource(value = "classpath:/emargement.properties", encoding = "UTF-8")
@EnableGlobalMethodSecurity(prePostEnabled=true)
@EnableScheduling
public class EmargementConfig {

}

