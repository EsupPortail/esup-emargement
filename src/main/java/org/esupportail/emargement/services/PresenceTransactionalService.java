package org.esupportail.emargement.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.esupportail.emargement.beans.UpdatePresenceResult;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresenceTransactionalService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	ContextService contextService;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	GroupeService groupeService;
	
	@Autowired
	ToolUtil toolUtil;

	@Transactional
	public UpdatePresenceResult doUpdatePresents(String presence, SessionLocation validLocation) throws ParseException {
		UpdatePresenceResult result = new UpdatePresenceResult();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = dateFormat.parse(dateFormat.format(new Date()));
		LocalTime currentTime = LocalTime.now();
		String eppn = null;
		String email = null; 
		TagChecker tagChecker = null;
		boolean isTagCheckerNeeded = true;
		boolean isValid = true;
		boolean isQrCode = presence.startsWith("qrcode");
		boolean isQrCodeUser = presence.startsWith("qrcodeUser");
		boolean isQrcodeSession = presence.startsWith("qrcodeSession");
		boolean isQrcodeCarte = false;
		boolean existsInSession = false;
		List<TagCheck> list = new ArrayList<>();
		Context ctx = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String currentUser = auth.getName();
		if(isValidEppn(presence)) {
			isQrcodeCarte = true;
			if(!appliConfigService.isCardQrCodeEnabled()) {
				isValid = false;
				log.error("Badgeage de " + presence + " non pris en compte car l'option ENABLE_CARD_QRCODE n'est pas activée ");
			}
		}else if(isQrCode) {
			if(presence.contains("@@@")) {
				isTagCheckerNeeded = false;
				String splitPresence [] = presence.split("@@@");
				presence = splitPresence[0];
				eppn = toolUtil.decodeFromBase64(splitPresence[1]);
			}
			String temp = toolUtil.decodeFromBase64(presence.replace("qrcodeUser", "").replace("qrcodeSession", "").replace("qrcode", ""));
			String [] splitTemp = temp.split("@@@");
			presence = splitTemp[0];
			String qrCodetimestamp = splitTemp[1];
			String ctxId = splitTemp[2];
			ctx = contextRepository.findByContextId(Long.valueOf(ctxId));
			if(!"notime".equals(qrCodetimestamp)) {
				if(splitTemp.length>3) {
					String eppnTagChecker = splitTemp[3];
					tagChecker = tagCheckerRepository.findFirstByContextAndUserAppEppn(ctx, eppnTagChecker).orElse(null);
				}
				Long qrCodeValidtime = Long.valueOf(appliConfigRepository.findFirstByContextAndKey(ctx,"QRCODE_CHANGE").orElse(null).getValue());
				Long now = System.currentTimeMillis() / 1000;
				if(now - Long.valueOf(qrCodetimestamp) > qrCodeValidtime) {
					isValid = false;
					Long tempsDepasse = now - Long.valueOf(qrCodetimestamp) + qrCodeValidtime;
					log.info("QrCode invalide pour " + eppn + ", temps dépassé de " + tempsDepasse + " secondes");
				}
			}else{
				tagChecker = tagCheckerRepository.findFirstByContextAndUserAppEppn(ctx, currentUser).orElse(null);
			}
		}
		if(isValid) {
			String [] splitPresence = presence.split(",");
			if(splitPresence.length>0) {
				String msgError = "";
				TypeEmargement typeEmargement = null;
				boolean isPresent = false;
				if(isQrcodeCarte) {
					typeEmargement = TypeEmargement.QRCODE_CARD;
				}else if(splitPresence.length >4 && "qrcode".equals(splitPresence[4].trim())) {
	    			if(isQrcodeSession) {
	    				typeEmargement = TypeEmargement.QRCODE_SESSION;
	    			}else if (isQrCodeUser) {
	    				typeEmargement = TypeEmargement.QRCODE_USER;
	    			}else {
	    				typeEmargement = TypeEmargement.QRCODE;
	    			}
		    	}else {
		    		typeEmargement = TypeEmargement.MANUAL;
		    		isPresent = Boolean.valueOf(splitPresence[0].trim());
		    	}
		    	SessionLocation sessionLocation = null;
		    	SessionLocation sessionLocationBadged = null;
		    	Long sessionLocationId = null;
		    	boolean isUnknown = false;
		    	String comment = "";
		    	SessionEpreuve sessionEpreuve =  null;
		    	if(isQrcodeCarte) {
		    		eppn = splitPresence[0];
		    		ctx = contextService.getcurrentContext();
		    		sessionLocationId = validLocation.getId();
		    		tagChecker = tagCheckerRepository.findFirstByContextAndUserAppEppn(ctx, currentUser).orElse(null);
		    	}else if(splitPresence.length >2) {
			    	if(eppn == null) {
			    		eppn = splitPresence[1].trim();
			    	}
			    	if(isQrCodeUser) {
			    		sessionLocationId = validLocation.getId();
			    	}else {
			    		sessionLocationId = Long.valueOf(splitPresence[2].trim());
			    	}
		    	}
		    	if(splitPresence.length >3) {
		    		email = (splitPresence.length >3)? splitPresence[3] : "";
		    	}
		    	if(ctx!=null) {
			    	sessionLocation = sessionLocationRepository.findFirstByContextAndId(ctx, sessionLocationId).orElse(null);
			    	sessionEpreuve = sessionLocation.getSessionEpreuve();
			    	List<Long> seId = null;
			    	if(!sessionEpreuve.isSessionLibre) {
						try {
							if(!eppn.isEmpty()) {
							    existsInSession = tagCheckRepository
							        .existsByPersonEppnAndSessionEpreuve(eppn, sessionEpreuve);
							}else {
							    existsInSession = tagCheckRepository
							        .existsByGuestEmailAndSessionEpreuve(email, sessionEpreuve);
							}
							if(!existsInSession) {
								//comparaison avec les heures de début et de fin
								seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, currentDate, currentTime); 
								if(seId != null && !seId.isEmpty()){
									String lieu = "";
									TagCheck tagCheck = tagCheckRepository.findFirstBySessionEpreuveIdInAndPersonEppn(seId, eppn).orElse(null);
									if(tagCheck.getSessionLocationExpected() !=null) {
										lieu = lieu.concat(" --> Session(s) : "+ tagCheck.getSessionEpreuve().getNomSessionEpreuve());
										String location = tagCheck.getSessionLocationExpected() .getLocation().getNom();
										lieu = lieu.concat(" , lieu : ").concat(location);
									}
									isUnknown = true;
								}else {
									comment = "Inconnu dans cette session, Attendu dans une autre session aujourd'hui";
									isUnknown = true;
								}		
							}
						} catch (Exception e) {
							isUnknown = true;
							comment = "Intrus";
						}
			    	}
					if(!isUnknown) {
			    		if(sessionEpreuve.isSessionLibre) {
			    			isPresent = true;
			    		}else{
				    		Long countSe = 0L;
				    		TagCheck checkOtherLocation = null;
				    		if(eppn != null && !eppn.isEmpty()) {
				    			countSe = tagCheckRepository.countByPersonEppnAndSessionEpreuveDateExamen(eppn, currentDate);
				    			checkOtherLocation = tagCheckRepository.findFirstBySessionEpreuveAndPersonEppnAndIsUnknownFalse(sessionEpreuve, eppn).orElse(null);
				    		}else if(email != null && !email.isEmpty()) {
				    			countSe = tagCheckRepository.countByGuestEmailAndSessionEpreuveDateExamen(email, currentDate);
				    			checkOtherLocation = tagCheckRepository.findFirstBySessionEpreuveAndGuestEmailAndIsUnknownFalse(sessionEpreuve, email).orElse(null);
				    		}
							SessionLocation sessionLocationExpected = checkOtherLocation.getSessionLocationExpected();
				    		if(existsInSession) {
								if(sessionEpreuve.getTypeBadgeage().equals(TypeBadgeage.SALLE)) {
									boolean exists;
									if(eppn != null && !eppn.isEmpty()) {
									    exists = tagCheckRepository
									        .existsBySessionLocationExpectedAndPersonEppnAndIsUnknownFalse(
									                sessionLocation,
									                eppn);
									}else {
									    exists = tagCheckRepository
									        .existsBySessionLocationExpectedAndGuestEmailAndIsUnknownFalse(
									                sessionLocation,
									                email);
									}
									if(!exists) {
										comment = "Inconnu dans cette salle, Salle attendue : " +  sessionLocationExpected.getLocation().getNom();
										isUnknown = true;
									}else {
										isPresent = true;
									}
								}else {
									isPresent = true;
									sessionLocation = sessionLocationExpected;
									sessionLocationId = sessionLocationExpected.getId();
								}
							}else if (countSe>0) {
								// On regarde si il est est dans une autre session aujourd'hui
								seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, currentDate, currentTime);
								if (seId != null) {
									String lieu = "";
									TagCheck tagCheck = tagCheckRepository
											.findFirstBySessionEpreuveIdInAndPersonEppn(seId, eppn).orElse(null);
									if (tagCheck.getSessionLocationExpected() != null) {
										lieu = lieu.concat(" --> Session(s) : "
												+ tagCheck.getSessionEpreuve().getNomSessionEpreuve());
										String location = tagCheck.getSessionLocationExpected().getLocation()
												.getNom();
										lieu = lieu.concat(" , lieu : ").concat(location);
									}
									comment = "Inconnu dans cette session " + lieu;
									isUnknown = true;
								} else {
									comment = "Inconnu dans cette session, Attendu dans une autre session aujourd'hui";
									isUnknown = true;
								}
							}else { //Il est vraiment inconnu!!!
								comment = "Inconnu";
								isUnknown = true;
							}
			    		}
					}
		    	}else {
		    		sessionLocation = sessionLocationRepository.findById(sessionLocationId).get();
		    	}
		    	if(isPresent && TypeEmargement.MANUAL.equals(typeEmargement) || typeEmargement.name().startsWith(TypeEmargement.QRCODE.name())) {
		    		sessionLocationBadged = sessionLocation;
		    	}
		    	if(isUnknown) {
					TagCheck newTc = tagCheckService.saveUnknownTagCheck(comment, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker, false, typeEmargement);
					list.add(newTc);
					result.setList(list);
					result.setPresentTagCheck(newTc);
					result.setPercent(0F);
					result.setTotalPresent(0L);
					result.setSessionLocationBadged(new SessionLocation());
					result.setMsgError("");
				}else if(sessionLocation != null) {
		    		Date date = (isPresent)? new Date() : null;
		    		SessionEpreuve se = sessionLocation.getSessionEpreuve();
			    	Groupe gpe = se.getBlackListGroupe();
					boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);	
			    	TagCheck presentTagCheck = null;
			    	if((isQrCode || isQrcodeCarte) && se.getIsSessionLibre()){
			    		presentTagCheck = tagCheckService.saveUnknownTagCheck("", ctx, eppn, se, sessionLocationBadged, tagChecker, true, typeEmargement);
			    	}else {
				    	if(!eppn.isEmpty()) {
				    		presentTagCheck = tagCheckRepository.findFirstBySessionLocationExpectedIdAndPersonEppn(sessionLocationId, eppn).orElse(null);
				    	}else if(!email.isEmpty()) {
				    		presentTagCheck = tagCheckRepository.findFirstBySessionLocationExpectedIdAndGuestEmail(sessionLocationId, email).orElse(null);
				    	}
				    	presentTagCheck.setSessionEpreuve(se);
				    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
				    		presentTagCheck.setTypeEmargement2(typeEmargement);
				    	}else {
				    		presentTagCheck.setTypeEmargement(typeEmargement);
				    	}
				    	presentTagCheck.setNbBadgeage(tagCheckService.getNbBadgeage(presentTagCheck, isPresent));
				    	
				    	if(tagChecker != null) {
					    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
					    		presentTagCheck.setTagChecker2(tagChecker);
					    	}else {
					    		presentTagCheck.setTagChecker(tagChecker);
					    	}
				    		presentTagCheck.setTagChecker(tagChecker);
				    	}else if(isTagCheckerNeeded) {
							tagChecker =  (isPresent)? tagCheckerRepository.findFirstByUserAppEppn(currentUser).orElse(null): null;
					    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
					    		presentTagCheck.setTagChecker2(tagChecker);
					    	}else {
					    		presentTagCheck.setTagChecker(tagChecker);
					    	}
				    	}
						if(!isPresent) {
							presentTagCheck.setProxyPerson(null);
					    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
					    		presentTagCheck.setTypeEmargement2(null);
					    	}else {
					    		presentTagCheck.setTypeEmargement(null);
					    	}
							presentTagCheck.setNbBadgeage(null);
						}
				    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
				    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
				    		presentTagCheck.setTagDate2(date);
				    	}else {
				    		presentTagCheck.setTagDate(date);
				    	}
				    	presentTagCheck.setContext((ctx != null)? ctx : contextService.getcurrentContext());
				    	if(!isBlackListed && isPresent || !isBlackListed && !isPresent || isBlackListed && !isPresent) {
				    		tagCheckRepository.save(presentTagCheck);
				    	}
			    	}
			    	list.add(presentTagCheck);
			    	tagCheckService.setNomPrenomTagChecks(list, false, false);
			    	if(presentTagCheck.getTagChecker() != null) {
			        	List<TagChecker> tcList = new ArrayList<>();
			        	tcList.add(presentTagCheck.getTagChecker());
				    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
				    	if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
				    		presentTagCheck.setTagChecker2(tcList.get(0));
				    	}else {
				    		presentTagCheck.setTagChecker(tcList.get(0));
				    	}
			    	}
					if(isBlackListed) {
						msgError = presentTagCheck.getNomPrenom();
					}
					float percent = 0;
					Object obj = tagCheckRepository.countPresenceStats(sessionLocationId, ctx!=null? ctx.getId():null);

					Object[] row = (Object[]) obj;

					Long totalExpected = ((Number) row[0]).longValue();
					Long totalPresent = ((Number) row[1]).longValue();
			    	if(totalExpected!=0) {
			    		percent = 100*(totalPresent.floatValue()/totalExpected.floatValue());
			    	}
			        if(sessionLocationBadged != null) {
			    		sessionLocationBadged.setNbPresentsSessionLocation(totalPresent);
			    	}
					if(appliConfigService.isTagCheckerDisplayed() && tagCheckerRepository.existsByUserAppEppn(eppn)) {
						String presence2 ="true,tagchecker," + eppn + "," + sessionLocationId;
						tagCheckerService.updatePresentsTagCkeckers(presence2, typeEmargement);
					}
					result.setList(list);
					result.setPresentTagCheck(presentTagCheck);
					result.setPercent(percent);
					result.setTotalPresent(totalPresent);
					result.setSessionLocationBadged(sessionLocationBadged);
					result.setMsgError(msgError);
	    		}
			}
		}
    	return result;
	}

	public static boolean isValidEppn(String eppn) {
        String eppnRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(eppnRegex);
        Matcher matcher = pattern.matcher(eppn);
        return matcher.matches();
    }
}
