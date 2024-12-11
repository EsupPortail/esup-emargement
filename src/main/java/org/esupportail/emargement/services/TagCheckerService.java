package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.utils.ParamUtil;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	DataEmitterService dataEmitterService;
	
	@Resource
	LdapUserRepository userLdapRepository;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Resource
	UserAppService userAppService;
	
	@Autowired
	ParamUtil paramUtil;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	UserAppRepository userAppRepository;
	
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
		SessionEpreuve se = tc.getSessionEpreuve();
		SessionLocation sl = tc.getSessionLocation();
		finalString = htmltemplate.replace(wrapChar.concat("civilite").concat(wrapChar), "M");
		finalString = finalString.replace(wrapChar.concat("prenom").concat(wrapChar), tc.getUserApp().getPrenom());
		finalString = finalString.replace(wrapChar.concat("nom").concat(wrapChar), tc.getUserApp().getNom());
		finalString = finalString.replace(wrapChar.concat("nomSession").concat(wrapChar), se.getNomSessionEpreuve());
		finalString = finalString.replace(wrapChar.concat("dateExamen").concat(wrapChar), String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()));
		finalString = finalString.replace(wrapChar.concat("heureConvocation").concat(wrapChar), String.format("%1$tH:%1$tM", se.getHeureConvocation()));
		finalString = finalString.replace(wrapChar.concat("debutEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", se.getHeureEpreuve()));
		finalString = finalString.replace(wrapChar.concat("finEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", se.getFinEpreuve()));
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), toolUtil.getDureeEpreuve(se.getHeureEpreuve(), se.getFinEpreuve(), null));
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), sl.getLocation().getAdresse());
		finalString = finalString.replace(wrapChar.concat("site").concat(wrapChar), se.getCampus().getSite());
		finalString = finalString.replace(wrapChar.concat("salle").concat(wrapChar), sl.getLocation().getNom());
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), sl.getLocation().getAdresse());
		
		return finalString;
	}
	
	
	public void sendEmailConsignes (String subject, String bodyMsg, Long sessionEpreuveId, String htmltemplatePdf) throws Exception {
		
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
							log.error("Echec de l'envoi du mail de consignes  :  ",e);
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

	public List<TagChecker> updatePresentsTagCkeckers(String presence, SessionLocation validLocation, TypeEmargement type){
		List<TagChecker> tagCheckers = new ArrayList<>();
		String [] splitPresence = presence.split(",");
		if(splitPresence.length >2) {
			tagCheckers = tagCheckerRepository.findBySessionLocationIdAndUserAppEppn(Long.valueOf(splitPresence[3].trim()), splitPresence[2].trim());
			if(!tagCheckers.isEmpty()) {
				boolean isPresent = Boolean.valueOf(splitPresence[0].trim());
				for(TagChecker tc : tagCheckers) {
					Date date = (isPresent)? new Date() : null;
					TypeEmargement typeEmargement = (type != null) ? type : (isPresent ? TypeEmargement.MANUAL : null);
					SessionEpreuve se = tc.getSessionEpreuve();
					Authentication auth = SecurityContextHolder.getContext().getAuthentication();
					UserApp userApp = (isPresent)? userAppRepository.findByEppnAndContextKey(auth.getName(), se.getContext().getKey()) : null;
					if(userApp !=null) {
						List<UserApp> temp = new ArrayList<>();
						temp.add(userApp);
						List<UserApp> users =  userAppService.setNomPrenom(temp,false);
						if(se.getIsSecondTag()) {
							tc.setTagValidator(users.get(0));
						}else {
							tc.setTagValidator2(users.get(0));
						}
					}else{
						if(se.getIsSecondTag()) {
							tc.setTagValidator(null);
						}else {
							tc.setTagValidator2(null);
						}
					}
					if(se.getIsSecondTag()) {
						tc.setTagDate2(date);
						tc.setTypeEmargement2(typeEmargement);
					}else {
						tc.setTagDate(date);
						tc.setTypeEmargement(typeEmargement);
					}
					tagCheckerRepository.saveAndFlush(tc);
				}
			}
		}
		if(!tagCheckers.isEmpty()){
			dataEmitterService.sendTagChecker(tagCheckers.get(0));
		}
		return tagCheckers;
	}
}
