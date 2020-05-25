package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.Date;

import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.ContextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {
	
	@Autowired
	ContextRepository contextRepository;
	
	public StoredFile setStoredFile(StoredFile storedFile, MultipartFile file, String emargementContext) throws IOException {
		
		storedFile.setSendTime(new Date());
		storedFile.setFile(file);
		storedFile.setBigFile(new BigFile());
		storedFile.getBigFile().setBinaryFile(file.getBytes());
		Context context = contextRepository.findByKey(emargementContext);
		storedFile.getBigFile().setContext(context);
		storedFile.setContentType(file.getContentType());
		storedFile.setFilename(file.getName());
		storedFile.setFileSize(file.getSize());
		storedFile.setContext(context);
		return storedFile;
	}
}
