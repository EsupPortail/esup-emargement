package org.esupportail.emargement.web.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.repositories.custom.LogsRepositoryCustom;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LogService;
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
@PreAuthorize(value="@userAppService.isAdmin()")
public class LogsController {
	
	@Autowired
	LogsRepository logsRepository;
	
	@Autowired
	LogsRepositoryCustom logsRepositoryCustom;
	
	@Resource
	HelpService helpService;
	
	@Resource
	LogService logService;
	
	private final static String ITEM = "logs";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/logs")
	public String list(Model model, @PageableDefault(size = 20, direction = Direction.DESC, sort = "logDate")  Pageable pageable) {
		Page<Log> logsPage = logsRepository.findAll(pageable);
		addAttribute(model, new Log(), logsPage, null);
		return "admin/logs/list";
	}

    @PostMapping("/admin/logs/search")
    public String search(@PathVariable String emargementContext,@Valid Log logObject, Model model,  @PageableDefault(size = 10, direction = Direction.DESC, sort = "logDate")  Pageable pageable,
    					@RequestParam String stringDate)throws ParseException {
    	Page<Log> logsPage= logsRepositoryCustom.findAll(logObject, stringDate, pageable);
    	Date date = null;
    	if(!stringDate.isEmpty()) {
    		date = new SimpleDateFormat("yyyy-MM-dd").parse(stringDate);
    	}
    	addAttribute(model, logObject, logsPage, date);
		model.addAttribute("collapse", "show");
    	return "admin/logs/list";
    }
    
    void addAttribute(Model model, Log logObject, Page<Log> logsPage, Date date) {
        model.addAttribute("eppns", logsRepositoryCustom.findDistinctEppn());
        model.addAttribute("actions", logsRepositoryCustom.findDistinctAction());
        model.addAttribute("cibleLogins", logsRepositoryCustom.findDistinctCibleLogin());
        model.addAttribute("logsPage", logsPage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("logObject",logObject );
        model.addAttribute("stringDate",date );
    }
    
	@GetMapping(value = "/admin/logs/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
        uiModel.addAttribute("log",  logsRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "admin/logs/show";
    }
	
}
