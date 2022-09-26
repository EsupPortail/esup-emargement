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
	public void sendData(TagCheck tc, float percent, Long totalPresent, SessionLocation sl, String customMsg) {
		this.eventPublisher.publishEvent(SseEvent.of("tc", tc));
		if(sl !=null) {
			this.eventPublisher.publishEvent(SseEvent.of("sl", sl));
		}
		String suffixe = (tc.getSessionLocationExpected()!=null )? tc.getSessionLocationExpected().getId().toString().concat("@@") : "";
		this.eventPublisher.publishEvent(SseEvent.ofData(suffixe.concat(String.valueOf(percent))));
		this.eventPublisher.publishEvent(SseEvent.of("total", suffixe.concat(String.valueOf(totalPresent))));
		this.eventPublisher.publishEvent(SseEvent.of("customMsg", customMsg));
	}
	
	public void sendDataImport(String nbImportSession) {
		this.eventPublisher.publishEvent(SseEvent.of("nbImportSession", nbImportSession));
	}

}