package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.utils.ParamUtil;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
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
	LdapService ldapService;
	
	@Resource
	LdapUserRepository userLdapRepository;;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Autowired
	ParamUtil paramUtil;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public Page<TagChecker> getListTagCheckerBySessionEpreuve(SessionEpreuve sessionEpreuve, Pageable pageable) {
		
		List<SessionLocation> sessionLocations = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve);
		Page<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationIn(sessionLocations, pageable);
		List<String> tcList = tagCheckers.stream().filter(tc->tc.getUserApp()!=null).map(tagChecker -> tagChecker.getUserApp().getEppn())
				.collect(Collectors.toList());
		Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
		
		for(TagChecker tagChecker: tagCheckers.getContent()) {
			LdapUser ldapUser = mapLdapUsers.get(tagChecker.getUserApp().getEppn());
			if(ldapUser!=null) {
				tagChecker.getUserApp().setNom(ldapUser.getName());
				tagChecker.getUserApp().setPrenom(ldapUser.getPrenom());
			}
			if(ldapUser==null && tagChecker.getUserApp().getEppn().startsWith(paramUtil.getGenericUser())) {
				tagChecker.getUserApp().setNom(tagChecker.getUserApp().getContext().getKey());
				tagChecker.getUserApp().setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
			}
		}
		return tagCheckers;
	}
	
	public void setNomPrenom4TagCheckers(List<TagChecker> allTagCheckers) {		
		if(!allTagCheckers.isEmpty()) {
			List<String> tcList = allTagCheckers.stream().filter(tc->tc.getUserApp()!=null).map(tagChecker -> tagChecker.getUserApp().getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
			for(TagChecker tagChecker: allTagCheckers) {
				LdapUser ldapUser = mapLdapUsers.get(tagChecker.getUserApp().getEppn());
				if(ldapUser!=null) {
					tagChecker.getUserApp().setNom(ldapUser.getName());
					tagChecker.getUserApp().setPrenom(ldapUser.getPrenom());
				}
				if(ldapUser==null && tagChecker.getUserApp().getEppn().startsWith(paramUtil.getGenericUser())) {
					tagChecker.getUserApp().setNom(tagChecker.getContext().getKey());
					tagChecker.getUserApp().setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
				}
			}
		}
	}
	
	public String getSnTagCheckers(SessionEpreuve se){
		List<String> snTagChecks = new ArrayList<>();
		String listTc = "";
		List<TagChecker> allTagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
		if(!allTagCheckers.isEmpty()) {
			List<String> tcList = allTagCheckers.stream().filter(tc->tc.getUserApp()!=null).map(tagChecker -> tagChecker.getUserApp().getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
			for(TagChecker tc : allTagCheckers) {
				LdapUser ldapUser = mapLdapUsers.get(tc.getUserApp().getEppn());
				String sn = "";
				if(ldapUser!=null) {
					sn = ldapUser.getPrenom().concat(" ").concat(ldapUser.getName());
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
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), toolUtil.getDureeEpreuve(tc.getSessionEpreuve()));
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
				List<String> tcList = tcs.stream().filter(tc->tc.getUserApp()!=null).map(tagChecker -> tagChecker.getUserApp().getEppn())
						.collect(Collectors.toList());
				Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
				for(TagChecker tc : tcs) {
					LdapUser ldapUser = mapLdapUsers.get(tc.getUserApp().getEppn());
					if(ldapUser!=null) {
						try {
							tc.getUserApp().setNom(ldapUser.getName());
							tc.getUserApp().setPrenom(ldapUser.getPrenom());
							tc.getUserApp().setCivilite(ldapUser.getCivilite());
							String filePath = pdfGenaratorUtil.createPdf(replaceFields(htmltemplatePdf,tc));
							if(appliConfigService.isSendEmails()){
								emailService.sendMessageWithAttachment(ldapUser.getEmail(), subject, bodyMsg, filePath, "consignes.pdf", new String[0], null);
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
		tagCheckerRepository.deleteAll(tcs);
	}

}
