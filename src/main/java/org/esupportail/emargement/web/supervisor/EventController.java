package org.esupportail.emargement.web.supervisor;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.services.AdeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xml.sax.SAXException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor() or  @userAppService.isUser()")
public class EventController {
	
	@Resource
	AdeService adeService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired
	CampusRepository campusRepository;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ITEM = "events";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/supervisor/events")
	public String index(Model uiModel) {
		uiModel.addAttribute("campuses", campusRepository.findAll());
		uiModel.addAttribute("existingSe",true);
		return "supervisor/events/index";
	}

	@GetMapping(value = "/supervisor/events/adeCampus")
	public String getTableEvents(@PathVariable String emargementContext, Model uiModel, 
			@RequestParam(value="existingSe", required = false) String existingSe,
			@RequestParam(value="idList", required = false) List<String> idList,
			@RequestParam(value="strDateMin", required = false) String strDateMin,
			@RequestParam(value="strDateMax", required = false) String strDateMax){
	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			String sessionId = adeService.getSessionId(false, emargementContext);
			String idProject = appliConfigService.getProjetAde();
			if(adeService.getConnectionProject(idProject, sessionId)==null) {
				sessionId = adeService.getSessionId(true, emargementContext);
				adeService.getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
			uiModel.addAttribute("listEvents", adeService.getAdeBeans(sessionId, strDateMin, strDateMax, null, existingSe, "myEvents", idList));
			uiModel.addAttribute("strDateMin", strDateMin);
			uiModel.addAttribute("strDateMax", strDateMax);
			uiModel.addAttribute("existingSe", (existingSe!=null)? true : false);
			uiModel.addAttribute("campuses", campusRepository.findAll());
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des évènements", e);
		}
		return "supervisor/events/table";
	}
	
	@Transactional
	@PostMapping(value = "/supervisor/events/adeCampus/importEvents")
	@ResponseBody
	public String importEvent(@PathVariable String emargementContext, @RequestParam(value="btSelectItem", required = false) List<Long> idEvents, 
			@RequestParam(value="campus", required = false) Campus campus,
			@RequestParam(value="strDateMin", required = false) String strDateMin,
			@RequestParam(value="existingSe", required = false) String existingSe,
			@RequestParam(value="idList", required = false) List<String> idList,
			@RequestParam(value="strDateMax", required = false) String strDateMax,
			@RequestParam(value="existingGroupe", required = false) List<Long> existingGroupe,
			@RequestParam(value="newGroupe", required = false) String newGroupe) throws IOException, ParserConfigurationException, SAXException, ParseException {
			adeService.importEvents(idEvents, emargementContext, strDateMin, strDateMax,newGroupe, existingGroupe, existingSe, 
					"myEvents",	campus,  idList, null, null, null);
		
		return String.format("strDateMin=%s&strDateMax=%s&existingSe=true&idList=%s", 
			    			emargementContext, strDateMin, strDateMax, StringUtils.join(idList, ","));
	}
}
