package org.esupportail.emargement.web.supervisor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.services.DataEmitterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class SseController {
	
	private final SseEventBus eventBus;

	public SseController(SseEventBus eventBus) {
		this.eventBus = eventBus;
	}
	
	@Resource
	DataEmitterService dataEmitterService;

    @GetMapping("/supervisor/register/{id}")
    public SseEmitter register(@PathVariable("id") String id, HttpServletResponse response) {
    	response.setHeader("Cache-Control", "no-store");
        return this.eventBus.createSseEmitter(id, 30_000L, SseEvent.DEFAULT_EVENT, "tc", "total", "refresh", "sl", "customMsg");
	}
}