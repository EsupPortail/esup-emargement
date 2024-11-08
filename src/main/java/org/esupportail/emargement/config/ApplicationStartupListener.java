package org.esupportail.emargement.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener {

    private LocalDateTime startupTime;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        this.startupTime = LocalDateTime.now();
    }

    public LocalDateTime getStartupTime() {
        return startupTime;
    }
}
