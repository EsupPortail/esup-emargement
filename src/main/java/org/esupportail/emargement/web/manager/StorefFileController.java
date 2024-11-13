package org.esupportail.emargement.web.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import flexjson.JSONSerializer;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class StorefFileController {
	
	@Autowired	
	StoredFileRepository storedFileRepository;

	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;

    @Transactional
    @PostMapping("/manager/storedFile/delete")
    @ResponseBody
    public String  deleteStoredfile(@RequestParam("key") StoredFile storedFile){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		storedFileRepository.delete(storedFile);
		JSONSerializer serializer = new JSONSerializer();
		String flexJsonString = serializer.deepSerialize("zezez");
		return flexJsonString;
    }
    
	@Transactional
	@RequestMapping(value = "/manager/storedFile/{id}/photo")
	public void getPhoto(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		StoredFile sf = storedFileRepository.findById(id).get();
		if(sf != null) {
			Long size = sf.getFileSize();
			String contentType = sf.getContentType();
			response.setContentType(contentType);
			response.setContentLength(size.intValue());
			InputStream targetStream = new ByteArrayInputStream(sf.getBigFile().getBinaryFile());
			IOUtils.copy(targetStream, response.getOutputStream());
			///regarder les droits
		}
	}
	
    @GetMapping("/manager/storedFile/{type}/{id}")
    @ResponseBody
    public List<StoredFile> getStoredfiles(@PathVariable("type") String type, @PathVariable("id") Long id){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		if("session".equals(type)) {
			SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
			return storedFileRepository.findBySessionEpreuve(se);
		}
		Absence absence = absenceRepository.findById(id).get();
		return storedFileRepository.findByAbsence(absence);
    }
}
