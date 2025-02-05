package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AssiduiteBean2;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.TagCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AssiduiteController {
	
	private final static String ITEM = "assiduite";
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Autowired
	EsupSignatureRepository esupSignatureRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	AbsenceService absenceService;
	
	@Resource
	ContextService contextService;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/assiduite")
	public String list(Model model, @Valid AssiduiteBean2 assiduiteBean,
			@RequestParam(value = "datesRange", required = false) String datesRange) throws ParseException {
		List<TagCheck> tcs = new ArrayList<>();
		if(datesRange == null) {
			 LocalDate today = LocalDate.now();
			 datesRange = today + "@" + today;
		}
		String[] splitDates = datesRange.split("@");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
		Date dateDebut = df.parse(splitDates[0]);
		Date dateFin = df.parse(splitDates[1]);
		if(assiduiteBean.getSituation()==null){
			assiduiteBean.setSituation("absence");
		}
		if("absence".equalsIgnoreCase(assiduiteBean.getSituation())){
			tcs = tagCheckRepository
					.findByTagDateIsNullAndSessionEpreuveDateExamenBetweenOrTagDateIsNullAndSessionEpreuveDateFinBetweenOrTagDateIsNullAndSessionEpreuveDateExamenLessThanEqualAndSessionEpreuveDateFinGreaterThanEqual(
							dateDebut, dateFin, dateDebut, dateFin, dateDebut, dateFin);
		}else {
			tcs = tagCheckRepository
					.findByTagDateIsNotNullAndSessionEpreuveDateExamenBetweenOrTagDateIsNotNullAndSessionEpreuveDateFinBetweenOrTagDateIsNotNullAndSessionEpreuveDateExamenLessThanEqualAndSessionEpreuveDateFinGreaterThanEqual(
							dateDebut, dateFin, dateDebut, dateFin, dateDebut, dateFin);
			List<EsupSignature> signList = esupSignatureRepository.findByTagCheckIn(tcs);
			Map<Long, EsupSignature> mapTc = new HashMap<>();
			if(!signList.isEmpty()) {
				for(EsupSignature sign : signList) {
					if(sign.getTagCheck() != null) {
						mapTc.put(sign.getTagCheck().getId(), sign);
					}
				}
			}
			model.addAttribute("mapTc", mapTc);
		}
		if(assiduiteBean.getMotifType()!=null && !assiduiteBean.getMotifType().isEmpty()) {
			List<TagCheck> temp = tcs.stream()
				    .filter(tagCheck -> tagCheck.getAbsence() != null && tagCheck.getAbsence().getMotifAbsence() != null 
				    	&& tagCheck.getAbsence().getMotifAbsence().getTypeAbsence().name().equalsIgnoreCase(assiduiteBean.getMotifType()))
				    .collect(Collectors.toList());
			tcs.removeAll(tcs);
			tcs .addAll(temp);
		}
		if(assiduiteBean.getMotifType()!=null && !assiduiteBean.getMotifStatut().isEmpty()) {
			List<TagCheck> temp = tcs.stream()
				    .filter(tagCheck -> tagCheck.getAbsence() != null && tagCheck.getAbsence().getMotifAbsence() != null 
				    	&& tagCheck.getAbsence().getMotifAbsence().getStatutAbsence().name().equalsIgnoreCase(assiduiteBean.getMotifStatut()))
				    .collect(Collectors.toList());
			tcs.removeAll(tcs);
			tcs .addAll(temp);
		}
		if(assiduiteBean.getSearchValue()!=null && !assiduiteBean.getSearchValue().isEmpty()) {
			List<TagCheck> temp = tcs.stream()
				    .filter(tagCheck -> tagCheck.getPerson() != null && tagCheck.getPerson().getEppn().equals(assiduiteBean.getSearchValue()))
				    .collect(Collectors.toList());
			tcs.removeAll(tcs);
			tcs .addAll(temp);
		}
		if(assiduiteBean.getGroupe()!=null) {
			List<TagCheck> temp = tcs.stream()
				    .filter(tagCheck -> tagCheck.getPerson() != null && tagCheck.getPerson().getGroupes().contains(assiduiteBean.getGroupe()))
				    .collect(Collectors.toList());
			tcs.removeAll(tcs);
			tcs .addAll(temp);
		}
		if(assiduiteBean.getSessionEpreuve()!=null) {
			List<TagCheck> temp = tcs.stream()
				    .filter(tagCheck -> tagCheck.getPerson() != null && tagCheck.getSessionEpreuve().equals(assiduiteBean.getSessionEpreuve()))
				    .collect(Collectors.toList());
			tcs.removeAll(tcs);
			tcs .addAll(temp);
		}
		tagCheckService.setNomPrenomTagChecks(tcs, true, true);
		Map<String, String> mapTcs = tagCheckService.getPersonWithTotalDuration(tcs);
		Map<String, Long> mapDays = tagCheckService.getPersonWithTotalDays(tcs);
		Map<String, Long> mapSessions = tagCheckService.getPersonWithTotalSessionCount(tcs);
		model.addAttribute("datesRangeSelect", datesRange);
		model.addAttribute("absencesPage", tcs);
		model.addAttribute("mapTcs", mapTcs);
		model.addAttribute("mapDays", mapDays);
		model.addAttribute("mapSessions", mapSessions);
		model.addAttribute("tcs", tcs);
		model.addAttribute("assiduiteBean", assiduiteBean);
		model.addAttribute("groupes", groupeRepository.findAllByOrderByNom());
		model.addAttribute("sessions", sessionEpreuveRepository.getAllSessionEpreuveForAssiduiteByContext(dateDebut, dateFin, contextService.getcurrentContext().getId()));
		model.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrue());
		model.addAttribute("absence", new Absence());
		return "manager/assiduite/index";
	}
	
	@Transactional
	@PostMapping("/manager/assiduite/createAbsence")
    public String updateAbsence(@PathVariable String emargementContext, @Valid Absence absence, @RequestParam("idListAbsences") List<TagCheck >tcs,
    		@RequestParam("searchUrl") String searchUrl) throws IOException {
		for(TagCheck tc : tcs) {
			absenceService.createAbsence(absence.getMotifAbsence(), tc, absence) ;
			tc.setAbsence(absence);
	    	tagCheckService.save(tc, emargementContext);
		}
    	return String.format("redirect:/%s/manager/assiduite%s", emargementContext, searchUrl);
    }
}
