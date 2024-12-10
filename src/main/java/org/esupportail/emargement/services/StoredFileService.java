package org.esupportail.emargement.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import flexjson.JSONSerializer;

@Service
public class StoredFileService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	public StoredFile setStoredFile(StoredFile storedFile, MultipartFile file, String emargementContext, SessionEpreuve sessionEpreuve) throws IOException {
		
		storedFile.setSendTime(new Date());
		storedFile.setFile(file);
		storedFile.setBigFile(new BigFile());
		storedFile.getBigFile().setBinaryFile(file.getBytes());
		Context context = contextRepository.findByKey(emargementContext);
		storedFile.getBigFile().setContext(context);
		storedFile.setContentType(file.getContentType());
		storedFile.setFilename(file.getOriginalFilename());
		storedFile.setFileSize(file.getSize());
		storedFile.setContext(context);
		storedFile.setSessionEpreuve(sessionEpreuve);
		return storedFile;
	}
	
	public void deleteAllStoredFiles(SessionEpreuve se) {
		List<StoredFile> sfs = storedFileRepository.findBySessionEpreuve(se);
		storedFileRepository.deleteAll(sfs);
	}
	
	public void deleteAllStoredFiles(Absence absence) {
		List<StoredFile> sfs = storedFileRepository.findByAbsence(absence);
		storedFileRepository.deleteAll(sfs);
	}
	
	public void getPhoto(Long id, HttpServletResponse response) throws IOException {
		StoredFile sf = storedFileRepository.findById(id).get();
		if(sf != null) {
			Long size = sf.getFileSize();
			String contentType = sf.getContentType();
			response.setContentType(contentType);
			response.setContentLength(size.intValue());
			InputStream targetStream = new ByteArrayInputStream(sf.getBigFile().getBinaryFile());
			IOUtils.copy(targetStream, response.getOutputStream());
		}
	}
	
	public String deleteStoredfile(StoredFile storedFile){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		storedFileRepository.delete(storedFile);
		JSONSerializer serializer = new JSONSerializer();
		String flexJsonString = serializer.deepSerialize("zezez");
		return flexJsonString;
    }
	
    public List<StoredFile> getStoredfiles(String type, Long id){
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
