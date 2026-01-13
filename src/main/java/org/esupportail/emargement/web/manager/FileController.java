package org.esupportail.emargement.web.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
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
public class FileController {
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource
	ContextService contexteService;
	
	private final static String ITEM = "file";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/file")
	public String list(Model model, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable) {
		model.addAttribute("files", storedFileRepository.findAll(pageable));
		return "manager/file/list";
	}
	
	@Transactional
	@PostMapping(value = "/manager/file/uploadFile")
	public String uploadFile(@RequestParam("uploadingFile") MultipartFile file) throws IOException {
		
		StoredFile storedfile= new StoredFile();
		storedfile.setSendTime(new Date());
		storedfile.setFile(file);
		storedfile.setBigFile(new BigFile());
		storedfile.getBigFile().setBinaryFile(file.getBytes());
		storedfile.setContentType(file.getContentType());
		storedfile.setFilename(file.getName());
		storedfile.setFileSize(file.getSize());
		storedfile.setContext(contexteService.getcurrentContext());
		storedFileRepository.save(storedfile);
		return "manager/file/list";
	}
	
	@RequestMapping(value="/manager/file/{id}")
	public void getPhoto(@PathVariable Long id, HttpServletResponse response) throws IOException {
		StoredFile storedFile = storedFileRepository.findById(id).get();
		if(storedFile != null) {
			Long size = storedFile.getFileSize();
			String contentType = storedFile.getContentType();
			response.setContentType(contentType);
			response.setContentLength(size.intValue());
			InputStream targetStream = new ByteArrayInputStream(storedFile.getBigFile().getBinaryFile());
			IOUtils.copy(targetStream, response.getOutputStream());
		}
	}

}

