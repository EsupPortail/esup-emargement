package org.esupportail.emargement.web.manager;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class PersonController {
	
	@Autowired
	PersonRepository personRepository;
	
	@Resource
	PersonService personService;
	
	@Resource
	ContextService contexteService;
	
	private final static String ITEM = "person";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/manager/person")
	public String list(Model model, @PageableDefault(size = 10, direction = Direction.ASC, sort = "eppn")  Pageable pageable) {

        Page<Person> personPage = personRepository.findAll(pageable);
        personService.setNomPrenom(personPage.getContent());

        model.addAttribute("personPage", personPage);
	
		return "manager/person/list";
	}
	
	@GetMapping(value = "/manager/person/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("persons",  personRepository.findById(id).get());
        return "manager/person/show";
    }
	
    @GetMapping(value = "/manager/person", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Person person = new Person();
    	populateEditForm(uiModel, person);
        return "manager/person/create";
    }
    
    @GetMapping(value = "/manager/person/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Person person = personRepository.findById(id).get();
    	populateEditForm(uiModel, person);
        return "manager/person/update";
    }
    
    void populateEditForm(Model uiModel, Person Person) {
        uiModel.addAttribute("person", Person);
    }
    
    @PostMapping("/manager/person/create")
    public String create(@PathVariable String emargementContext, @Valid Person person, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, person);
            return "manager/person/create";
        }
        uiModel.asMap().clear();
        person.setContext(contexteService.getcurrentContext());
        personRepository.save(person);
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
    @PutMapping("/manager/person/update")
    public String update(@PathVariable String emargementContext, @Valid Person person, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, person);
            return "manager/person/update";
        }
        uiModel.asMap().clear();
        person.setContext(contexteService.getcurrentContext());
        personRepository.save(person);
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
    @DeleteMapping(value = "/manager/person/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
    	Person Person = personRepository.findById(id).get();
    	personRepository.delete(Person);
        return String.format("redirect:/%s/manager/person", emargementContext);
    }
    
}
