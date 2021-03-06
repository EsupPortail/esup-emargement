package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TagCheckerService {
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Resource
	EmailService emailService;
	
	@Resource
	UserLdapRepository userLdapRepository;;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Resource
	UserAppService userAppService;
	
	@Resource	
	SessionEpreuveService sessionEpreuveService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public Page<TagChecker> getListTagCheckerBySessionEpreuve(SessionEpreuve sessionEpreuve, Pageable pageable) {
		
		List<SessionLocation> sessionLocations = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve);
		Page<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationIn(sessionLocations, pageable);
		
		for(TagChecker tagChecker: tagCheckers.getContent()) {
			List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tagChecker.getUserApp().getEppn());
			if(!userLdaps.isEmpty()) {
				tagChecker.getUserApp().setNom(userLdaps.get(0).getUsername());
				tagChecker.getUserApp().setPrenom(userLdaps.get(0).getPrenom());
			}
			if(userLdaps.isEmpty() && tagChecker.getUserApp().getEppn().startsWith(userAppService.getGenericUser())) {
				tagChecker.getUserApp().setNom(tagChecker.getUserApp().getContext().getKey());
				tagChecker.getUserApp().setPrenom(StringUtils.capitalize(userAppService.getGenericUser()));
			}
		}
		return tagCheckers;
	}
	
	public void setNomPrenom4TagCheckers(List<TagChecker> allTagCheckers) {		
		if(!allTagCheckers.isEmpty()) {
			for(TagChecker tagChecker: allTagCheckers) {
				List<UserLdap> userLdaps= userLdapRepository.findByEppnEquals(tagChecker.getUserApp().getEppn());
				if(!userLdaps.isEmpty()) {
					tagChecker.getUserApp().setNom(userLdaps.get(0).getUsername());
					tagChecker.getUserApp().setPrenom(userLdaps.get(0).getPrenom());
				}
				if(userLdaps.isEmpty() && tagChecker.getUserApp().getEppn().startsWith(userAppService.getGenericUser())) {
					tagChecker.getUserApp().setNom(tagChecker.getContext().getKey());
					tagChecker.getUserApp().setPrenom(StringUtils.capitalize(userAppService.getGenericUser()));

				}
			}
		}
	}
	
	public String getSnTagCheckers(SessionEpreuve se){
		List<String> snTagChecks = new ArrayList<String>();
		String listTc = "";
		List<TagChecker> tcList = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
		if(!tcList.isEmpty()) {
			for(TagChecker tc : tcList) {
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getUserApp().getEppn());
				String sn = "";
				if(!userLdaps.isEmpty()) {
					sn = userLdaps.get(0).getPrenom().concat(" ").concat(userLdaps.get(0).getUsername());
					snTagChecks.add(sn);
				}
				
			}
			listTc = StringUtils.join(snTagChecks, ", ");
		}
		
		return listTc;
	}
	
	public String replaceFields(String htmltemplate, TagChecker tc) {
		
		String wrapChar = "@";
		String finalString= "";
		finalString = htmltemplate.replace(wrapChar.concat("civilite").concat(wrapChar), "M");
		finalString = finalString.replace(wrapChar.concat("prenom").concat(wrapChar), tc.getUserApp().getPrenom());
		finalString = finalString.replace(wrapChar.concat("nom").concat(wrapChar), tc.getUserApp().getNom());
		finalString = finalString.replace(wrapChar.concat("nomSession").concat(wrapChar), tc.getSessionEpreuve().getNomSessionEpreuve());
		finalString = finalString.replace(wrapChar.concat("dateExamen").concat(wrapChar), String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateExamen()));
		finalString = finalString.replace(wrapChar.concat("heureConvocation").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getHeureConvocation()));
		finalString = finalString.replace(wrapChar.concat("debutEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getHeureEpreuve()));
		finalString = finalString.replace(wrapChar.concat("finEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getFinEpreuve()));
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), sessionEpreuveService.getDureeEpreuve(tc.getSessionEpreuve()));
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), tc.getSessionLocation().getLocation().getAdresse());
		finalString = finalString.replace(wrapChar.concat("site").concat(wrapChar), tc.getSessionEpreuve().getCampus().getSite());
		finalString = finalString.replace(wrapChar.concat("salle").concat(wrapChar), tc.getSessionLocation().getLocation().getNom());
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), tc.getSessionLocation().getLocation().getAdresse());
		
		return finalString;
	}
	
	
	public void sendEmailConsignes (String subject, String bodyMsg, Long sessionEpreuveId, String htmltemplatePdf, String emargementContext) throws Exception {
		
		if(sessionEpreuveId!= null) {
			List<TagChecker> tcs =  tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(sessionEpreuveId);
			
			if(!tcs.isEmpty()) {
				for(TagChecker tc : tcs) {
					List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getUserApp().getEppn());
					if(!userLdaps.isEmpty()) {
						try {
							tc.getUserApp().setNom(userLdaps.get(0).getUsername());
							tc.getUserApp().setPrenom(userLdaps.get(0).getPrenom());
							tc.getUserApp().setCivilite(userLdaps.get(0).getCivilite());
							String filePath = pdfGenaratorUtil.createPdf(replaceFields(htmltemplatePdf,tc));
							String email = userLdaps.get(0).getEmail();
							if(!appliConfigService.getTestEmail().isEmpty()) {
								email = appliConfigService.getTestEmail();
							}
							if(appliConfigService.isSendEmails()){
								emailService.sendMessageWithAttachment(appliConfigService.getNoReplyAdress(), email, subject, bodyMsg, filePath, "consignes.pdf", new String[0], null);
							}else {
								log.info("Envoi de mail désactivé :  ");
							}
						}catch(Exception e){
							
						}

					}
				}
			}
		}
	}
	
	public void deleteAllTagCheckersBySessionEpreuveId(Long id) {
		List<TagChecker> tcs = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(id);
		
		if(!tcs.isEmpty()) {
			for(TagChecker tc : tcs) {
				tagCheckerRepository.delete(tc);
			}
		}
	}

}
