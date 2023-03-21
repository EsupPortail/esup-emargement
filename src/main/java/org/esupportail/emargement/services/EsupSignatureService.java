package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.EsupSignature.StatutSignature;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EsupSignatureService {
	
	@Value("${emargement.esupsignature.url}")
	private String urlEsupsignature;
	
	@Value("${emargement.esupsignature.workflow.id}")
	private String workflowId;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired	
	EsupSignatureRepository esupSignatureRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());	
	
	public void sendPdfToEsupToWorkflow(String emargementContext, Long id, HttpServletResponse response) {
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		byte[] pdfBytes = tagCheckService.exportTagChecks("PDF", id, response, emargementContext, null, true);
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("createByEppn", "system");
		map.add("filename", setNomFichierFromSessionEpreuve(se));
	    ByteArrayResource contentsAsResource = new ByteArrayResource(pdfBytes) {
	        @Override
	        public String getFilename() {
	            return setNomFichierFromSessionEpreuve(se);
	        }
	    };
		map.add("multipartFiles", contentsAsResource);
		map.add("recipientEmails", getRecipientEmails());
		map.add("title", se.getNomSessionEpreuve());
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		RestTemplate restTemplate = new RestTemplate();
		String signRequestId;
		try {
			String urlPostWorkflow = String.format("%s/ws/workflows/%s/new", urlEsupsignature, workflowId);
			signRequestId = restTemplate.postForObject(urlPostWorkflow, requestEntity, String.class);
			if(signRequestId != null) {
				EsupSignature esupSignature = new EsupSignature();
				esupSignature.setSessionEpreuve(se);
				esupSignature.setDateModification(new Date());
				esupSignature.setSignRequestId(Long.valueOf(signRequestId));
				Context context = contextRepository.findByContextKey(emargementContext);
				esupSignature.setContext(context);
				esupSignatureRepository.save(esupSignature);
				setStatus(urlEsupsignature, esupSignature);
			}
		} catch (RestClientException e) {
			log.error("Erreur lors de l'envoi du Pdf à esup-signature", e);
		}
	}
	
	public void getLastPdf(String emargementContext, EsupSignature esupsignature,  Long signId, HttpServletResponse response) {
		
		RestTemplate restTemplate = new RestTemplate();
		String urlStatus = String.format("%s/ws/signrequests/get-last-file/%s", urlEsupsignature, signId);
		ResponseEntity<byte[]> bytes = restTemplate.getForEntity(urlStatus, byte[].class);
		byte[] pdf = bytes.getBody();
		StoredFile sf = new StoredFile();
		SessionEpreuve se = esupsignature.getSessionEpreuve();
		String name = setNomFichierFromSessionEpreuve(se);
		String originalFileName = setNomFichierFromSessionEpreuve(se);
		String contentType = "application/pdf";

		MultipartFile file = new MockMultipartFile(name,
		                     originalFileName, contentType, pdf);
		try {
			storedFileService.setStoredFile(sf, file, emargementContext, se);
			storedFileRepository.save(sf);
			esupsignature.setStoredFileId(sf.getId());
			esupsignature.setStatutSignature(StatutSignature.DOWNLOADED);
			esupSignatureRepository.save(esupsignature);
			log.info("Téléchargement ok du Pdf depuis esup-signature : id " + sf.getId());
			deletePDF(signId);
			log.info("Document supprimé d'esup-signature : id " + sf.getId());
			esupsignature.setStatutSignature(StatutSignature.ENDED);
			esupSignatureRepository.save(esupsignature);
		} catch (Exception e) {
			log.error("Erreur lors de l'enregistrement du PDF vent d'esup signature, signId : "  + signId );
		}
	}
	
	public String setNomFichierFromSessionEpreuve(SessionEpreuve se) {
		Date dateFin = se.getDateFin();
    	String fin = (dateFin != null)? "_" + String.format("%1$td-%1$tm-%1$tY", dateFin) : "" ;
		String nomFichier = "signed_".concat(se.getNomSessionEpreuve()).concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen())).concat(fin).concat(".pdf");
		return  nomFichier;
	}
	
	//@Scheduled()
	public void checkStatus() {
		//Boucle
	}
	
	public void setStatus(String urlEsupsignature, EsupSignature esupSignature) {
		
		RestTemplate restTemplate = new RestTemplate();
		String urlStatus = String.format("%s/ws/signrequests/status/%s", urlEsupsignature, esupSignature.getSignRequestId());
		ResponseEntity<String> status = restTemplate.getForEntity(urlStatus, String.class);
		String statut = status.getBody();
		esupSignature.setStatutSignature(StatutSignature.valueOf(statut.toUpperCase()));
		esupSignature.setDateModification(new Date());
		esupSignatureRepository.save(esupSignature);
		log.info("Vérification statut, SignRequestId : " + esupSignature.getSignRequestId());
	}

	public String getRecipientEmails() {
		
		String strEmails = "";
		List<String> emails = appliConfigService.getEsupSignatureEmails();
		if(!emails.isEmpty()) {
			List<String> formattedEmails = new ArrayList<String>();
			for(String email : emails) {
				formattedEmails.add("1*".concat(email));
			}
			strEmails = StringUtils.join(formattedEmails, ",");
		}
		
		return strEmails;
	}
	
	public void deletePDF(Long signId) {
		String urlDeletePdf = String.format("%s/ws/signrequests/%s", urlEsupsignature, signId);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(urlDeletePdf);
	}
}
