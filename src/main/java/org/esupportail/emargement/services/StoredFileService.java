package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
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
	
	@Transactional
	public void deleteAllStoredFiles(SessionEpreuve se) {
		List<StoredFile> sfs = storedFileRepository.findBySessionEpreuve(se);
		
		if(!sfs.isEmpty()) {
			for(StoredFile sf : sfs) {
				storedFileRepository.delete(sf);
			}
		}
	}
}
