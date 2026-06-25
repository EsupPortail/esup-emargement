package org.esupportail.emargement.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AssiduiteBean2;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class AssiduiteService {
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	EsupSignatureRepository esupSignatureRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
    SessionEpreuveService sessionEpreuveService;
	
	@Resource
	ContextService contextService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	// ----------------------------------------------------------------
	// Utilitaire date
	// ----------------------------------------------------------------

	public Date toDate(LocalDate ld) {
	    return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	// ----------------------------------------------------------------
	// Branche surveillants
	// ----------------------------------------------------------------

	public void populateModelForSurveillants(Model model,
	                                           AssiduiteBean2 bean,
	                                           Date debut, Date fin) {
	    List<TagChecker> tcers = "absence".equalsIgnoreCase(bean.getSituation())
	        ? tagCheckerRepository
	            .findByTagDateIsNullAndSessionLocationSessionEpreuveDateExamenBetween(debut, fin)
	        : tagCheckerRepository
	            .findByTagDateIsNotNullAndSessionLocationSessionEpreuveDateExamenBetween(debut, fin);

	    List<TagChecker> result = tcers.stream()
	        .collect(Collectors.toMap(
	            t -> t.getSessionLocation().getSessionEpreuve().getId(),
	            Function.identity(),
	            (a, b) -> a,
	            LinkedHashMap::new
	        ))
	        .values()
	        .stream()
	        .collect(Collectors.toList());

	    tagCheckerService.setNomPrenom4TagCheckers(result);
	    model.addAttribute("tagCheckers", result);
	}

	// ----------------------------------------------------------------
	// Branche étudiants
	// ----------------------------------------------------------------

	public List<TagCheck> populateTagChecksForEtudiants(Model model,
	                                                      AssiduiteBean2 bean,
	                                                      Date debut, Date fin) {
	    if (bean.getSituation() == null) {
	        bean.setSituation("absence");
	    }

	    List<TagCheck> tcs = loadTagChecks(bean, debut, fin);

	    if (!"absence".equalsIgnoreCase(bean.getSituation())) {
	        populateEsupSignatureMap(model, tcs);
	    }

	    tcs = applyFilters(tcs, bean);
	    tagCheckService.setNomPrenomTagChecks(tcs, true, true, true);
	    return tcs;
	}

	private List<TagCheck> loadTagChecks(AssiduiteBean2 bean, Date debut, Date fin) {
	    if ("absence".equalsIgnoreCase(bean.getSituation())) {
	        return tagCheckRepository
	            .findByTagDateIsNullAndSessionEpreuveDateExamenBetweenOrTagDateIsNullAndSessionEpreuveDateFinBetweenOrTagDateIsNullAndSessionEpreuveDateExamenLessThanEqualAndSessionEpreuveDateFinGreaterThanEqual(
	                debut, fin, debut, fin, debut, fin);
	    } else {
	        return tagCheckRepository
	            .findByTagDateIsNotNullAndSessionEpreuveDateExamenBetweenOrTagDateIsNotNullAndSessionEpreuveDateFinBetweenOrTagDateIsNotNullAndSessionEpreuveDateExamenLessThanEqualAndSessionEpreuveDateFinGreaterThanEqual(
	                debut, fin, debut, fin, debut, fin);
	    }
	}

	private void populateEsupSignatureMap(Model model, List<TagCheck> tcs) {
	    List<EsupSignature> signList = esupSignatureRepository.findByTagCheckIn(tcs);
	    Map<Long, EsupSignature> mapTc = new HashMap<>();
	    for (EsupSignature sign : signList) {
	        if (sign.getTagCheck() != null) {
	            mapTc.put(sign.getTagCheck().getId(), sign);
	        }
	    }
	    model.addAttribute("mapTc", mapTc);
	}

	// ----------------------------------------------------------------
	// Filtres en pipeline
	// ----------------------------------------------------------------

	private List<TagCheck> applyFilters(List<TagCheck> tcs, AssiduiteBean2 bean) {
	    Stream<TagCheck> stream = tcs.stream();

	    if (bean.getMotifType() != null && !bean.getMotifType().isEmpty()) {
	        String motifType = bean.getMotifType();
	        stream = stream.filter(tc ->
	            tc.getAbsence() != null
	            && tc.getAbsence().getMotifAbsence() != null
	            && tc.getAbsence().getMotifAbsence().getTypeAbsence().name()
	                  .equalsIgnoreCase(motifType));
	    }

	    // CORRECTIF BUG : était getMotifType() != null dans le code original
	    if (bean.getMotifStatut() != null && !bean.getMotifStatut().isEmpty()) {
	        String motifStatut = bean.getMotifStatut();
	        stream = stream.filter(tc ->
	            tc.getAbsence() != null
	            && tc.getAbsence().getMotifAbsence() != null
	            && tc.getAbsence().getMotifAbsence().getStatutAbsence().name()
	                  .equalsIgnoreCase(motifStatut));
	    }

	    if (bean.getMotifAbsenceId() != null) {
	        Long id = bean.getMotifAbsenceId();
	        stream = stream.filter(tc ->
	            tc.getAbsence() != null
	            && tc.getAbsence().getMotifAbsence() != null
	            && Objects.equals(tc.getAbsence().getMotifAbsence().getId(), id));
	    }

	    if (bean.getSearchValue() != null && !bean.getSearchValue().isEmpty()) {
	        String eppn = bean.getSearchValue();
	        stream = stream.filter(tc ->
	            tc.getPerson() != null
	            && Objects.equals(tc.getPerson().getEppn(), eppn));
	    }

	    if (bean.getGroupe() != null) {
	        stream = stream.filter(tc ->
	            tc.getPerson() != null
	            && tc.getPerson().getGroupes().contains(bean.getGroupe()));
	    }

	    if (bean.getSessionEpreuve() != null) {
	        stream = stream.filter(tc ->
	            Objects.equals(tc.getSessionEpreuve(), bean.getSessionEpreuve()));
	    }

	    if (bean.getIdAdeBranch() != null) {
	        Long idBranch = bean.getIdAdeBranch();
	        stream = stream.filter(tc ->
	            tc.getSessionEpreuve() != null
	            && tc.getSessionEpreuve().getAdeBranch() != null
	            && Objects.equals(
	                tc.getSessionEpreuve().getAdeBranch().getId(), idBranch));
	    }

	    return stream.collect(Collectors.toList());
	}

	// ----------------------------------------------------------------
	// Modèle commun
	// ----------------------------------------------------------------

	public void populateCommonModel(Model model,
	                                  AssiduiteBean2 bean,
	                                  List<TagCheck> tcs,
	                                  Date debut, Date fin,
	                                  String resolvedRange) {
	    model.addAttribute("datesRangeSelect",      resolvedRange);
	    model.addAttribute("isTagCheckerDisplayed", appliConfigService.isTagCheckerDisplayed());
	    model.addAttribute("absencesPage",          tcs);
	    model.addAttribute("tcs",                   tcs);
	    model.addAttribute("mapTcs",                tagCheckService.getPersonWithTotalDuration(tcs));
	    model.addAttribute("mapDays",               tagCheckService.getPersonWithTotalDays(tcs));
	    model.addAttribute("mapSessions",           tagCheckService.getPersonWithTotalSessionCount(tcs));
	    model.addAttribute("assiduiteBean",         bean);
	    model.addAttribute("groupes",               groupeRepository.findAllByOrderByNom());
	    model.addAttribute("sessions",              sessionEpreuveRepository
	        .getAllSessionEpreuveForAssiduiteByContext(
	            debut, fin, contextService.getcurrentContext().getId()));
	    model.addAttribute("motifAbsences",         motifAbsenceRepository.findByIsActifTrueOrderByLibelle());
	    model.addAttribute("absence",               new Absence());
	    model.addAttribute("adeBranches",           sessionEpreuveService.getAdeBranches());
	}

}
