package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.Date;

import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {
	
	public StoredFile setStoredFile(StoredFile storedFile, MultipartFile file, String emargementContext) throws IOException {
		
		storedFile.setSendTime(new Date());
		storedFile.setFile(file);
		storedFile.setBigFile(new BigFile());
		storedFile.getBigFile().setBinaryFile(file.getBytes());
		storedFile.setContentType(file.getContentType());
		storedFile.setFilename(file.getName());
		storedFile.setFileSize(file.getSize());
		
		return storedFile;
	}
}
