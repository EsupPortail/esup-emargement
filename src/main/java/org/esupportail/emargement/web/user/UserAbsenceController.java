package org.esupportail.emargement.web.user;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.services.AbsenceEtudiantService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor() or  @userAppService.isUser()")
public class UserAbsenceController {

	@Autowired
	AbsenceEtudiantService absenceEtudiantService;

	@Autowired
	ContextRepository contextRepository;

	@Autowired
	PersonRepository personRepository;

	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;

	@Resource
	LogService logService;

	@PostMapping("/user/absence")
	public String declarer(@PathVariable String emargementContext, @RequestParam Long motifAbsenceId, @RequestParam String dateDebut,
			@RequestParam String dateFin, @RequestParam(required = false) String commentaire,
			@RequestParam(required = false) List<MultipartFile> files, RedirectAttributes redirectAttributes) throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		Context ctx = contextRepository.findByKey(emargementContext);
		List<Person> persons = personRepository.findByEppnAndContext(eppn, ctx);
		Person person = persons.isEmpty() ? null : persons.get(0);
		MotifAbsence motif = motifAbsenceRepository.findById(motifAbsenceId).orElse(null);

		String erreur = null;
		Date debut = null;
		Date fin = null;
		try {
			debut = parseDate(dateDebut);
			fin = parseDate(dateFin);
		} catch (ParseException e) {
			erreur = "Date invalide.";
		}
		if(erreur == null) {
			erreur = absenceEtudiantService.verifierDeclaration(person, ctx, motif, debut, fin, files);
		}
		if(erreur == null) {
			Absence absence = absenceEtudiantService.declarer(person, ctx, motif, commentaire, debut, fin, files);
			absenceEtudiantService.notifierDeclaration(absence, ctx);
			logService.log(ACTION.DECLARE_ABSENCE, RETCODE.SUCCESS, "Période : " + dateDebut + " - " + dateFin, eppn, null, emargementContext, null);
			redirectAttributes.addFlashAttribute("absenceDeclared", "absenceDeclared");
		} else {
			redirectAttributes.addFlashAttribute("absenceDeclareError", erreur);
		}
		return String.format("redirect:/%s/user", emargementContext);
	}

	private Date parseDate(String date) throws ParseException {
		if(StringUtils.isBlank(date)) {
			return null;
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setLenient(false);
		return format.parse(date);
	}
}
