package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.MotifAbsence.StatutAbsence;
import org.esupportail.emargement.domain.MotifAbsence.TypeAbsence;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.StoredFileService;
import org.esupportail.emargement.services.TagCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AbsenceController {
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	LdapUserRepository ldapUserRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	PersonService personService;
	
	@Resource
	TagCheckService tagCheckService;
	
	private final static String ITEM = "absence";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/absence")
	public String list(Model uiModel) {
		List<Absence> absences = absenceRepository.findAll();
		List<Person> persons = absenceRepository.findAll().stream().map(abs -> abs.getPerson())
				.collect(Collectors.toList());
		personService.setNomPrenom(persons);
		for (Absence abs : absences) {
			abs.setNbStoredFiles(storedFileRepository.countByAbsence(abs));
		}
		uiModel.addAttribute("absences", absences);
		return "manager/absence/list";
	}
	
    @GetMapping(value = "/manager/absence", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Absence absence = new Absence();
    	uiModel.addAttribute("absence", absence);
    	uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrue());
        return "manager/absence/create";
    }
    
    @GetMapping(value = "/manager/absence/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Absence absence, Model uiModel) {

    	uiModel.addAttribute("absence", absence);
    	uiModel.addAttribute("nomPrenom", ldapService.getPrenomNom(absence.getPerson().getEppn()));
    	uiModel.addAttribute("seId", absence.getId());
    	uiModel.addAttribute("typePj", "absence");
    	uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrue());
        return "manager/absence/update";
    }
    
    @Transactional
    @PostMapping("/manager/absence/create")
    public String create(@PathVariable String emargementContext, @Valid Absence absence, BindingResult bindingResult, 
    		Model uiModel,  @RequestParam("strDateDebut") String strDateDebut, 
    		@RequestParam("strDateFin") String strDateFin) throws ParseException, IOException {
    	Date dateDebut=new SimpleDateFormat("yyyy-MM-dd").parse(strDateDebut);
    	Date dateFin=new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin);
    	DateFormat df = new SimpleDateFormat("HH:mm");
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (bindingResult.hasErrors()) {
          //  populateEditForm(uiModel, person);
            return "manager/person/create";
        }
        uiModel.asMap().clear();
        String eppn = absence.getPerson().getEppn();
        List<Person> persons = personRepository.findByEppn(eppn);
        if(!persons.isEmpty()) {
        	 absence.setPerson(persons.get(0));
        	 LocalTime heureDebut = LocalTime.parse(df.format(absence.getHeureDebut()));
        	 LocalTime heureFin = LocalTime.parse(df.format(absence.getHeureFin()));
        	 List<TagCheck> tcs = tagCheckRepository.findByDates(eppn, dateDebut, dateFin, heureDebut, heureFin);
        	 if(!tcs.isEmpty()) {
        		 for(TagCheck tc : tcs) {
        			 tagCheckRepository.save(tc);
        		 }
        	 }
        }else {
        	List<LdapUser> ldapUsers = ldapUserRepository.findByEppnEquals(eppn);
        	if(!ldapUsers.isEmpty()) {
        		LdapUser ldapUser = ldapUsers.get(0);
            	Person person = new Person();
            	person.setEppn(eppn);
            	if(ldapUser.getNumEtudiant() != null) {
                	person.setNumIdentifiant(ldapUser.getNumEtudiant());
                	person.setType("student");
            	}else {
            		person.setType("staff");
            	}
            	personRepository.save(person);
            	absence.setPerson(person);
        	}
        }

        absence.setDateDebut(dateDebut);
        absence.setDateFin(dateFin);
        absence.setContext(contexteService.getcurrentContext());
        absence.setDateModification(new Date());
        absence.setUserApp(userAppRepository.findByEppnAndContextKey(auth.getName(), emargementContext));
        absenceRepository.save(absence);
        if(absence.getFiles() != null && !absence.getFiles().isEmpty()) {
			for(MultipartFile file : absence.getFiles()) {
				if(file.getSize()>0) {
					StoredFile sf = new StoredFile();
					sf.setAbsence(absence);
					storedFileService.setStoredFile(sf, file, emargementContext, null);
					storedFileRepository.save(sf);
				}
			}
		}
        return String.format("redirect:/%s/manager/absence", emargementContext);
    }
    
    @PostMapping("/manager/absence/update/{id}")
    public String update(@PathVariable String emargementContext, @Valid Absence absence, 
    		@RequestParam("strDateDebut") String strDateDebut, @RequestParam String strDateFin) throws ParseException, IOException{
    	Date dateDebut=new SimpleDateFormat("yyyy-MM-dd").parse(strDateDebut);
    	Date dateFin= !strDateFin.isEmpty() ? new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin) : new SimpleDateFormat("yyyy-MM-dd").parse(strDateDebut);
    	DateFormat df = new SimpleDateFormat("HH:mm");
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	absence.setDateDebut(dateDebut);
        absence.setDateFin(dateFin);
        absence.setDateModification(new Date());
        absence.setUserApp(userAppRepository.findByEppnAndContextKey(auth.getName(), emargementContext));
        personRepository.save(absence.getPerson());
    	absenceRepository.save(absence);
    	LocalTime heureDebut = LocalTime.parse(df.format(absence.getHeureDebut()));
		LocalTime heureFin = LocalTime.parse(df.format(absence.getHeureFin()));
		List<TagCheck> tcs = tagCheckRepository.findByDates(absence.getPerson().getEppn(), dateDebut, dateFin,
				heureDebut, heureFin);
		if (!tcs.isEmpty()) {
			for (TagCheck tc : tcs) {
				tagCheckRepository.save(tc);
			}
		}
        if(absence.getFiles() != null && !absence.getFiles().isEmpty()) {
			for(MultipartFile file : absence.getFiles()) {
				if(file.getSize()>0) {
					StoredFile sf = new StoredFile();
					sf.setAbsence(absence);
					storedFileService.setStoredFile(sf, file, emargementContext, null);
					storedFileRepository.save(sf);
				}
			}
		}
    	return String.format("redirect:/%s/manager/absence", emargementContext);
    }
    
    @Transactional
    @PostMapping(value = "/manager/absence/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Absence absence) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	DateFormat df = new SimpleDateFormat("HH:mm");
		LocalTime heureDebut = LocalTime.parse(df.format(absence.getHeureDebut()));
		LocalTime heureFin = LocalTime.parse(df.format(absence.getHeureFin()));
		List<TagCheck> tcs = tagCheckRepository.findByDates(absence.getPerson().getEppn(), absence.getDateDebut(), absence.getDateFin(), heureDebut, heureFin);
		if (!tcs.isEmpty()) {
			for (TagCheck tc : tcs) {
				tc.setAbsence(null);
				tagCheckRepository.save(tc);
			}
		}
    	storedFileService.deleteAllStoredFiles(absence);
    	tagCheckService.deleteAbsence(absence);
    	absenceRepository.delete(absence);
    	logService.log(ACTION.DELETE_ABSENCE, RETCODE.SUCCESS, absence.getPerson().getEppn(), auth.getName(), null, emargementContext, null);

        return String.format("redirect:/%s/manager/absence", emargementContext);
    }
    
    @GetMapping(value = "/manager/absence/motifs", produces = "text/html")
    public String search(Model uiModel, @RequestParam(required=false) String statut, @RequestParam(required=false) String type) {
    	
    	if(statut!= null && type != null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndStatutAbsenceAndTypeAbsence(StatutAbsence.valueOf(statut), TypeAbsence.valueOf(type)));
    	}else if(statut == null && type != null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndTypeAbsence(TypeAbsence.valueOf(type)));
    	}else if(statut != null && type == null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndStatutAbsence(StatutAbsence.valueOf(statut)));
    	}
    	return "manager/absence/selectMotifs";
    	
    }
}

