package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AssiduiteBean2;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.AssiduiteService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.TagCheckService;
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

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AssiduiteController {
	
	private final static String ITEM = "assiduite";
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	AbsenceService absenceService;
	
	@Resource
	LogService logService;
	
	@Resource
	AssiduiteService assiduiteService;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/assiduite")
	public String list(Model model,
	                   @Valid AssiduiteBean2 assiduiteBean,
	                   @RequestParam(required = false) String datesRange) {

	    String resolvedRange = datesRange != null ? datesRange
	                         : LocalDate.now() + "@" + LocalDate.now();

	    String[] parts = resolvedRange.split("@");
	    Date debut = assiduiteService.toDate(LocalDate.parse(parts[0]));
	    Date fin   = assiduiteService.toDate(LocalDate.parse(parts[1]));

	    assiduiteBean.setTypeIndividu(
	        assiduiteBean.getTypeIndividu() == null ? "etu" : assiduiteBean.getTypeIndividu()
	    );

	    List<TagCheck> tcs = new ArrayList<>();
	    if ("surv".equalsIgnoreCase(assiduiteBean.getTypeIndividu())) {
	    	assiduiteService.populateModelForSurveillants(model, assiduiteBean, debut, fin);
	    } else {
	        tcs = assiduiteService.populateTagChecksForEtudiants(model, assiduiteBean, debut, fin);
	    }

	    assiduiteService.populateCommonModel(model, assiduiteBean, tcs, debut, fin, resolvedRange);
	    return "manager/assiduite/index";
	}
	
	@Transactional
	@PostMapping("/manager/assiduite/createAbsence")
    public String updateAbsence(@PathVariable String emargementContext, @Valid Absence absence, @RequestParam("idListAbsences") List<TagCheck >tcs,
    		@RequestParam String searchUrl) throws IOException {
		for(TagCheck tc : tcs) {
			Absence newAbsence = absenceService.createAbsence(tc, absence) ;
			tc.setAbsence(newAbsence);
	    	tagCheckService.save(tc, emargementContext);
		}
    	return String.format("redirect:/%s/manager/assiduite%s", emargementContext, searchUrl);
    }
	
	@Transactional
	@PostMapping("/manager/assiduite/deleteAbsence")
    public String deleteAbsence(@PathVariable String emargementContext,  @RequestParam("idListAbsences") List<TagCheck >tcs, @RequestParam String searchUrl){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		int i= 0;
		for(TagCheck tc : tcs) {
			Absence absence = tc.getAbsence();
			if(absence != null) {
				tc.setAbsence(null);
				tagCheckService.save(tc, emargementContext);
				List<StoredFile> files = storedFileRepository.findByAbsence(absence);
				if(!files.isEmpty()) {
					storedFileRepository.deleteAll(files);
				}
				absenceRepository.delete(absence);
		    	i++;
			}
		}
		logService.log(ACTION.DELETE_ABSENCE, RETCODE.SUCCESS, "nb : " + i, auth.getName(), null, emargementContext, null);
		return String.format("redirect:/%s/manager/assiduite%s", emargementContext, searchUrl);
    }
}
