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
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.Motif;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.StoredFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AbsenceController {
	
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
	StoredFileService storedFileService;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	PersonService personService;
	
	private final static String ITEM = "absence";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/absence")
	public String list(Model uiModel, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable) {
		Page<Absence> absences = absenceRepository.findAll(pageable);
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
        return "manager/absence/create";
    }
    
    
    @GetMapping(value = "/manager/absence/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable String emargementContext, @PathVariable("id") Absence absence, Model uiModel) {

    	uiModel.addAttribute("absence", absence);
    	uiModel.addAttribute("seId", absence.getId());
    	uiModel.addAttribute("typePj", "absence");
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
        			 tc.setAbsence(absence.getIsValidated()? Motif.JUSTIFIE : null);
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
        absence.setUserApp(userAppRepository.findByEppnAndContextKey(eppn, emargementContext));
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
    		@RequestParam("strDateDebut") String strDateDebut, @RequestParam("strDateFin") String strDateFin, BindingResult bindingResult, 
    		Model uiModel) throws ParseException, IOException{
    	Date dateDebut=new SimpleDateFormat("yyyy-MM-dd").parse(strDateDebut);
    	Date dateFin=new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin);
    	DateFormat df = new SimpleDateFormat("HH:mm");
    	absence.setDateDebut(dateDebut);
        absence.setDateFin(dateFin);
        personRepository.save(absence.getPerson());
    	absenceRepository.save(absence);
    	LocalTime heureDebut = LocalTime.parse(df.format(absence.getHeureDebut()));
		LocalTime heureFin = LocalTime.parse(df.format(absence.getHeureFin()));
		List<TagCheck> tcs = tagCheckRepository.findByDates(absence.getPerson().getEppn(), dateDebut, dateFin,
				heureDebut, heureFin);
		if (!tcs.isEmpty()) {
			for (TagCheck tc : tcs) {
				tc.setAbsence(absence.getIsValidated()? Motif.JUSTIFIE : null);
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
    
    @PostMapping(value = "/manager/absence/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Absence absence,final RedirectAttributes redirectAttributes) {
    	
    	storedFileService.deleteAllStoredFiles(absence);
    	absenceRepository.delete(absence);
    	//logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.FAILED, "Nom : " + nom, auth.getName(), null, emargementContext, null);
    	
        return String.format("redirect:/%s/manager/absence", emargementContext);
    }
}

