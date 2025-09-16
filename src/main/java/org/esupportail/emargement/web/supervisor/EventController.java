package org.esupportail.emargement.web.supervisor;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.exceptions.AdeApiRequestException;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
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
	
	@Autowired	
	ContextRepository contextRepository;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ITEM = "events";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/supervisor/events")
	public String index(@PathVariable String emargementContext, Model uiModel, @RequestParam(required = false) String projet) throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, XPathExpressionException{
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn =  auth.getName();
		// Si projet est null alors on récupère l'id du projet en cours
		// sinon on utilise projet et on l'enregistre en tant que projet en cours
		String idProject =  adeService.getCurrentProject(projet, eppn, emargementContext);
		uiModel.addAttribute("campuses", campusRepository.findAll());
		uiModel.addAttribute("existingSe", true);
		uiModel.addAttribute("isAdeConfigOk", appliConfigService.getProjetAde().isEmpty()? false : true);
		uiModel.addAttribute("idProject", idProject);
		String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
		uiModel.addAttribute("projects", adeService.getProjectLists(sessionId));
		return "supervisor/events/index";
	}

	@GetMapping(value = "/supervisor/events/adeCampus")
	public String getTableEvents(@PathVariable String emargementContext, Model uiModel, 
			@RequestParam(required = false) String existingSe,
			@RequestParam(required = false) List<String> idList,
			@RequestParam(required = false) String strDateMin,
			@RequestParam(required = false) String strDateMax,
			@RequestParam(required = false) String projet){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			// Si projet est null alors on récupère l'id du projet en cours
			// sinon on utilise projet et on l'enregistre en tant que projet en cours
			String idProject = adeService.getCurrentProject(projet, auth.getName(), emargementContext) ;
			String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
			Context ctx = contextRepository.findByKey(emargementContext);
			uiModel.addAttribute("listEvents", adeService.getAdeBeans(sessionId, strDateMin, strDateMax, null, existingSe, "myEvents", idList, ctx, false));
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
			@RequestParam(required = false) Campus campus,
			@RequestParam(required = false) String strDateMin,
			@RequestParam(required = false) String existingSe,
			@RequestParam(required = false) List<String> idList,
			@RequestParam(required = false) String strDateMax,
			@RequestParam(required = false) List<Long> existingGroupe,
			@RequestParam(required = false) String newGroupe) throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
			adeService.importEvents(idEvents, emargementContext, strDateMin, strDateMax,newGroupe, existingGroupe, existingSe, 
					"myEvents",	campus, idList, null, null, null, false);
		
		return String.format("strDateMin=%s&strDateMax=%s&existingSe=true&idList=%s", 
			    			emargementContext, strDateMin, strDateMax, StringUtils.join(idList, ","));
	}
}
