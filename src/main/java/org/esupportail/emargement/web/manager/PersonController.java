package org.esupportail.emargement.web.manager;

import java.util.Collections;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
@HelpPage("person")
public class PersonController {
	
	@Resource
	PersonService personService;
	
	@Resource
	ContextService contextService;

    @Resource
	AppliConfigService appliConfigService;

	@Resource
	UserAppService userAppService;
	
	private final static String ITEM = "person";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/manager/person")
    public String list(@PathVariable String emargementContext, Model model,
                    @RequestParam(value = "search", required = false) String search,
                    @RequestParam(value = "regime", required = false) String regime,
                    @RequestParam(value = "boursier", required = false) Boolean boursier,
                    @RequestParam(value = "rse", required = false) Boolean rse,
                    @RequestParam(value = "tiersTemps", required = false) Boolean tiersTemps,
                    @RequestParam(value = "formaSup", required = false) Boolean formaSup,
                    @PageableDefault(size = 10, direction = Direction.ASC, sort = "eppn") Pageable pageable,
                    @RequestHeader(value = "HX-Request", defaultValue = "false") boolean isHtmxRequest) {

        String searchValue = StringUtils.hasText(search) ? search.trim() : "";
        String regimeValue = StringUtils.hasText(regime) ? regime.trim() : "";
        boolean onlyBoursier = Boolean.TRUE.equals(boursier);
        boolean onlyRse = Boolean.TRUE.equals(rse);
        boolean onlyTiersTemps = Boolean.TRUE.equals(tiersTemps);
        boolean onlyFormaSup = Boolean.TRUE.equals(formaSup);
        String searchFilter = searchValue.length() >= 3 ? searchValue : null;

        Page<Person> personPage = personService.getPersonsPage(contextService.getcurrentContext(), searchFilter, regimeValue, onlyBoursier, onlyRse, onlyTiersTemps, onlyFormaSup, pageable);
        personService.setNomPrenom(personPage.getContent());

        model.addAttribute("personPage", personPage);
        model.addAttribute("search", searchValue);
        model.addAttribute("regime", regimeValue);
        model.addAttribute("boursier", onlyBoursier);
        model.addAttribute("rse", onlyRse);
        model.addAttribute("tiersTemps", onlyTiersTemps);
        model.addAttribute("formaSup", onlyFormaSup);

        if (isHtmxRequest) {
            return "manager/person/list :: table-fragment";
        }

        model.addAttribute("formationTypes",
                personService.getRegimeChoices(emargementContext, contextService.getcurrentContext()));
        return "manager/person/list";
    }
	
	@GetMapping(value = "/manager/person/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
        Person person = personService.getPersonById(id);
        personService.setNomPrenom(Collections.singletonList(person));
        uiModel.addAttribute("person", person);
        return "manager/person/show";
    }
	
    @GetMapping(value = "/manager/person", params = "form", produces = "text/html")
    public String createForm(@PathVariable String emargementContext, Model uiModel) {
    	Person person = new Person();
    	populateEditForm(uiModel, person, emargementContext);
        return "manager/person/create";
    }
    
    @GetMapping(value = "/manager/person/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable Long id, @PathVariable String emargementContext, Model uiModel) {
    	Person person = personService.getPersonById(id);
    	populateEditForm(uiModel, person, emargementContext);
        return "manager/person/update";
    }
    
    void populateEditForm(Model uiModel, Person person, String emargementContext) {
        uiModel.addAttribute("person", person);
        uiModel.addAttribute("formationTypes", appliConfigService.getAdeRegimeChoice(emargementContext));
    }
    
    @PostMapping("/manager/person/create")
    public String create(@PathVariable String emargementContext, @Valid Person person, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, person, emargementContext);
            return "manager/person/create";
        }
        personService.createPerson(person, contextService.getcurrentContext(), userAppService.getCurrentUserApp(contextService.getcurrentContext()));
        uiModel.asMap().clear();
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
    @PostMapping("/manager/person/update")
    public String update(@PathVariable String emargementContext, @Valid Person person, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, person, emargementContext);
            return "manager/person/update";
        }
        personService.updatePerson(person, contextService.getcurrentContext(), userAppService.getCurrentUserApp(contextService.getcurrentContext()));
        uiModel.asMap().clear();
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
    @PostMapping(value = "/manager/person/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable Long id) {
    	Person Person = personService.getPersonById(id);
    	if (Person != null) {
    		personService.deletePerson(Person);
    	}
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
}
