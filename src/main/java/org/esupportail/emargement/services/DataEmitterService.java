package org.esupportail.emargement.services;

import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import ch.rasc.sse.eventbus.SseEvent;

@Service
public class DataEmitterService {

	private final ApplicationEventPublisher eventPublisher;


	public DataEmitterService(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	//@Scheduled(initialDelay = 2000, fixedRate = 5_000)
	public void sendData(TagCheck tc, float percent, Long totalPresent, int refresh, SessionLocation sl) {
		this.eventPublisher.publishEvent(SseEvent.of("tc", tc));
		this.eventPublisher.publishEvent(SseEvent.of("sl", sl));
		String suffixe = (tc.getSessionLocationExpected()!=null )? tc.getSessionLocationExpected().getId().toString().concat("@@") : "";
		this.eventPublisher.publishEvent(SseEvent.ofData(suffixe.concat(String.valueOf(percent))));
		this.eventPublisher.publishEvent(SseEvent.of("total", suffixe.concat(String.valueOf(totalPresent))));
		this.eventPublisher.publishEvent(SseEvent.of("refresh", refresh));
	}

}