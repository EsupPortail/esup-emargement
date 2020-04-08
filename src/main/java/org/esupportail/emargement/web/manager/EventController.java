package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Event;
import org.esupportail.emargement.repositories.EventRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EventService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.fortuna.ical4j.data.ParserException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class EventController {
	
	@Autowired
	EventRepository eventRepository;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;
	
	@Resource
	EventService eventService;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "event";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/event")
	public String list(Model model,  Pageable pageable) throws IOException, ParserException {
		Page<Event> eventPage = eventRepository.findAll(pageable);
		eventService.setNbEvent(eventPage.getContent());
		model.addAttribute("eventPage", eventPage);
		eventService.getAllUrlList();
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "manager/event/list";
	}
	
	@PostMapping("/manager/event/create")
    public String create(@PathVariable String emargementContext, @Valid Event event, BindingResult bindingResult, Model uiModel) {
    	
    	if (bindingResult.hasErrors()) {
            return "manager/event/create";
        }
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	event.setDateCreation(new Date());
    	event.setContext(contexteService.getcurrentContext());
    	eventRepository.save(event);
        log.info("Ajout évènement : " + event.getNom());
        logService.log(ACTION.AJOUT_EVENT, RETCODE.SUCCESS, "Evènement : ".concat(event.getNom()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
        return String.format("redirect:/%s/manager/event", emargementContext);
    }
	
	@PostMapping("/manager/event/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid Event event, BindingResult bindingResult, Model uiModel) {
    	
    	if (bindingResult.hasErrors()) {
            return "manager/event/update";
        }
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	Event oldEvent = eventRepository.findById(id).get();
    	oldEvent.setIsEnabled(event.getIsEnabled());
    	oldEvent.setNom(event.getNom());
    	oldEvent.setCommentaire(event.getCommentaire());
    	oldEvent.setUrl(event.getUrl());
    	eventRepository.save(oldEvent);
        log.info("Modification évènement : " + event.getNom());
        logService.log(ACTION.UPDATE_EVENT, RETCODE.SUCCESS, "Evènement : ".concat(event.getNom()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
        return String.format("redirect:/%s/manager/event", emargementContext);
    }
	
    @GetMapping(value = "/manager/event", params = "form", produces = "text/html")
    public String createForm(@PathVariable String emargementContext, Model uiModel){
    	Event event = new Event();
    	uiModel.addAttribute("event", event);
        return "manager/event/create";
    }
    
    @GetMapping(value = "/manager/event/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Event event = eventRepository.findById(id).get();
    	uiModel.addAttribute("event", event);
        return "manager/event/update";
    }
    
    @PostMapping(value = "/manager/event/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	Event event = eventRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
    		eventRepository.delete(event);
			log.info("Suppression de l'évènement  : " + event.getNom());
			logService.log(ACTION.DELETE_EVENT, RETCODE.SUCCESS, "Evènement : ".concat(event.getNom()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
		} catch (Exception e) {
			log.error("Erreur lors de la suppression de l'évènement " + event.getNom(), e );
		}
    	return String.format("redirect:/%s/manager/event", emargementContext);
    }
    
    @GetMapping("/manager/event/searchEvent")
    @ResponseBody
    public Event getEvent(@PathVariable String emargementContext, @RequestParam("uid") String uid) throws IOException, ParserException, ParseException{
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	
        return eventService.searchEventByUid(uid, emargementContext, eventService.getAllUrlList());
    }
    
    @GetMapping("/manager/event/selectEvent/{id}")
    public String getEventByid(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) throws IOException, ParserException, ParseException{
    	
    	uiModel.addAttribute("events", eventService.geteventsById(emargementContext, id));
    	return "manager/event/select";
    }

}
