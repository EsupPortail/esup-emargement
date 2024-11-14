package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.services.StoredFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class StorefFileController {
	
	@Autowired	
	StoredFileRepository storedFileRepository;

	@Resource
	StoredFileService storedFileService;

    @Transactional
    @PostMapping("/manager/storedFile/delete")
    @ResponseBody
    public String  deleteStoredfile(@RequestParam("key") StoredFile storedFile){
    	return storedFileService.deleteStoredfile(storedFile);
    }
    
	@Transactional
	@RequestMapping(value = "/manager/storedFile/{id}/photo")
	public void getPhoto(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		storedFileService.getPhoto(id, response);
	}
	
    @GetMapping("/manager/storedFile/{type}/{id}")
    @ResponseBody
    public List<StoredFile> getStoredfiles(@PathVariable("type") String type, @PathVariable("id") Long id){
		return storedFileService.getStoredfiles(type, id);
    }
}
