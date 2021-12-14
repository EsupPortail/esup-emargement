package org.esupportail.emargement.web.superadmin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.repositories.custom.LogsRepositoryCustom;
import org.esupportail.emargement.services.HelpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class LogsSuperAdminController {
	
	@Autowired
	LogsRepository logsRepository;
	
	@Autowired
	LogsRepositoryCustom logsRepositoryCustom;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "logs";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/superadmin/logs")
	public String list(Model model, @PageableDefault(size = 30, direction = Direction.DESC, sort = "logDate")  Pageable pageable) {
		Page<Log> logsPage = logsRepository.findLogByContextIsNull(pageable);
		addAttribute(model, new Log(), logsPage, null);
		return "superadmin/logs/list";
	}

    @PostMapping("/superadmin/logs/search")
    public String search(@PathVariable String emargementContext,@Valid Log logObject, Model model,  @PageableDefault(size = 10, direction = Direction.DESC, sort = "logDate")  Pageable pageable,
    					@RequestParam(value="stringDate") String stringDate)throws ParseException {
    	Page<Log> logsPage= logsRepositoryCustom.findAll(logObject, stringDate, pageable);
    	Date date = null;
    	if(!stringDate.isEmpty()) {
    		date = new SimpleDateFormat("yyyy-MM-dd").parse(stringDate);
    	}
    	addAttribute(model, logObject, logsPage, date);
		model.addAttribute("collapse", "show");
    	return "superadmin/logs/list";
    }
    
	@GetMapping(value = "/superadmin/logs/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("log",  logsRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "superadmin/logs/show";
    }
    
    void addAttribute(Model model, Log logObject, Page<Log> logsPage, Date stringDate) {
        model.addAttribute("eppns", logsRepositoryCustom.findDistinctEppn());
        model.addAttribute("actions", logsRepositoryCustom.findDistinctAction());
        model.addAttribute("cibleLogins", logsRepositoryCustom.findDistinctCibleLogin());
        model.addAttribute("logsPage", logsPage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("logObject",logObject );
        model.addAttribute("stringDate",stringDate );
    }
}
