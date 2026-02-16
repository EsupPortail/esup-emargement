package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.AppliConfigService.AppliConfigKey;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PresenceService {
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired	
	ContextRepository contextRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
		
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
    @Resource
    LdapUserRepository ldapUserRepository;
    
    @Resource    
    AppliConfigRepository appliConfigRepository;

	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
    @Resource
    ContextService contextService;
    
    @Resource
	DataEmitterService dataEmitterService;
    
    @Resource    
    AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	GroupeService groupeService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired	
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;
		
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void getPdfPresence(Document document, HttpServletResponse response,  Long sessionLocationId, String emargementContext, ByteArrayOutputStream bos) {
        SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
        SessionEpreuve se = sl.getSessionEpreuve();
        List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(se.getId(), null).getContent();
    	tagCheckService.setNomPrenomTagChecks(list, false, false);
        String dateFin = (se.getDateFin()!=null)? "" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
        String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").
    			concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin)).concat(".pdf");
        try 
        {			
			PdfWriter writer =   null;
			if (bos != null) {
				writer = PdfWriter.getInstance(document, bos);
			} else {
				response.setContentType("application/pdf");
				response.setHeader("Content-Disposition", "attachment; filename=".concat(nomFichier));
				writer = PdfWriter.getInstance(document, response.getOutputStream());
			}

			tagCheckService.getTagCheckListAsPDF(list, document, writer, se, emargementContext, sessionLocationId);
		} catch (DocumentException de) {
			de.printStackTrace();
			logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					  null, emargementContext, null);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					  null, emargementContext, null);
		}
    }
	
	//Pour emargement manuel et qrCode
	public List<TagCheck> updatePresents(String presence, SessionLocation validLocation) throws ParseException {
		String eppn = null;
		String email = null; 
		TagChecker tagChecker = null;
		boolean isTagCheckerNeeded = true;
		boolean isValid = true;
		boolean isQrCode = presence.startsWith("qrcode");
		boolean isQrCodeUser = presence.startsWith("qrcodeUser");
		boolean isQrcodeSession = presence.startsWith("qrcodeSession");
		boolean isQrcodeCarte = false;
		List<TagCheck> list = new ArrayList<>();
		Context ctx = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
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
					tagChecker = tagCheckerRepository.findByContextAndUserAppEppn(ctx, eppnTagChecker).get(0);
				}
				List<AppliConfig> configs  = appliConfigRepository.findByContextAndKey(ctx,"QRCODE_CHANGE");
				Long qrCodeValidtime = Long.valueOf(configs.get(0).getValue());
				Long now = System.currentTimeMillis() / 1000;
				if(now - Long.valueOf(qrCodetimestamp) > qrCodeValidtime) {
					isValid = false;
					Long tempsDepasse = now - Long.valueOf(qrCodetimestamp) + qrCodeValidtime;
					log.info("QrCode invalide pour " + eppn + ", temps dépassé de " + tempsDepasse + " secondes");
				}
			}else{
				tagChecker = tagCheckerRepository.findByContextAndUserAppEppn(ctx, auth.getName()).get(0);
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
		    		tagChecker = tagCheckerRepository.findByContextAndUserAppEppn(ctx, auth.getName()).get(0);
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
			    	List<SessionLocation> sls = sessionLocationRepository.findByContextAndId(ctx, sessionLocationId);
			    	sessionLocation = sls.get(0);
			    	sessionEpreuve = sessionLocation.getSessionEpreuve();
			    	List<Long> seId = null;
			    	List<TagCheck> tcs = new ArrayList<>();
			    	if(!sessionEpreuve.isSessionLibre) {
						try {
							if(!eppn.isEmpty()) {
								tcs = tagCheckRepository.findTagCheckByPersonEppnAndSessionEpreuve(eppn, sessionEpreuve);
							}else {
								tcs = tagCheckRepository.findTagCheckByGuestEmailAndSessionEpreuve(email, sessionEpreuve);
							}
							if(tcs.isEmpty()) {
								//comparaison avec les heures de début et de fin
					    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					    		Date today = dateFormat.parse(dateFormat.format(new Date()));
								LocalTime now = LocalTime.now();
								seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, today, now); 
								if(seId != null) {
									String lieu = "";
									for(Long sessionId : seId) {
										List<TagCheck> list2 = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionId, eppn, null).getContent();
										if(!list2.isEmpty()) {
											TagCheck tagCheck = list2.get(0);
											if(tagCheck.getSessionLocationExpected() !=null) {
												lieu = lieu.concat(" --> Session(s) : "+ tagCheck.getSessionEpreuve().getNomSessionEpreuve());
												String location = list.get(0).getSessionLocationExpected() .getLocation().getNom();
												lieu = lieu.concat(" , lieu : ").concat(location);
											}
										}	
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
				    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				    		Date date = dateFormat.parse(dateFormat.format(new Date()));
				    		Long countSe = 0L;
				    		List <TagCheck> checkOtherLocation = new ArrayList<>();
				    		if(eppn != null && !eppn.isEmpty()) {
				    			countSe = tagCheckRepository.countByPersonEppnAndSessionEpreuveDateExamen(eppn, date);
								checkOtherLocation = tagCheckRepository.findBySessionEpreuveAndPersonEppnAndIsUnknownFalse(sessionEpreuve, eppn);
				    		}else if(email != null && !email.isEmpty()) {
				    			countSe = tagCheckRepository.countByGuestEmailAndSessionEpreuveDateExamen(email, date);
				    			checkOtherLocation = tagCheckRepository.findBySessionEpreuveAndGuestEmailAndIsUnknownFalse(sessionEpreuve, email);
				    		}
							SessionLocation sessionLocationExpected = checkOtherLocation.get(0).getSessionLocationExpected();
				    		if(!tcs.isEmpty()) {
								if(sessionEpreuve.getTypeBadgeage().equals(TypeBadgeage.SALLE)) {
									List <TagCheck> tcsTemp = new ArrayList<>();
									if(eppn != null && !eppn.isEmpty()) {
										tcsTemp = tagCheckRepository.findBySessionLocationExpectedAndPersonEppnAndIsUnknownFalse(sessionLocation, eppn);
									}else if(email != null && !email.isEmpty()) {
										tcsTemp = tagCheckRepository.findBySessionLocationExpectedAndGuestEmailAndIsUnknownFalse(sessionLocation, email);
									}
									if(tcsTemp.isEmpty()) {
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
								//On regarde si il est est dans une autre session aujourd'hui
								LocalTime now = LocalTime.now();
								seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, date, now); 
								if(seId != null) {
									String lieu = "";
									for(Long sessionId : seId) {
										List<TagCheck> list3 = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionId, eppn, null).getContent();
										if(!list3.isEmpty()) {
											TagCheck tagCheck = list3.get(0);
											if(tagCheck.getSessionLocationExpected() !=null) {
												lieu = lieu.concat(" --> Session(s) : "+ tagCheck.getSessionEpreuve().getNomSessionEpreuve());
												String location = list.get(0).getSessionLocationExpected() .getLocation().getNom();
												lieu = lieu.concat(" , lieu : ").concat(location);
											}
										}	
									}
									comment = "Inconnu dans cette session " + lieu;
									isUnknown = true;
								}else {
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
					dataEmitterService.sendData(newTc, Float.valueOf("0"), Long.valueOf("0"), new SessionLocation(), "");
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
				    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn).get(0);
				    	}else if(!email.isEmpty()) {
				    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndGuestEmailEquals(sessionLocationId, email).get(0);
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
							tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0) : null;
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
					Long totalPresent = Long.valueOf("0");
					Long totalExpected = Long.valueOf("0");
					float percent = 0;
					if(ctx != null) {
				    	totalPresent = tagCheckRepository.countByContextAndSessionLocationExpectedIdAndTagDateIsNotNull(ctx, sessionLocationId);
				    	totalExpected = tagCheckRepository.countByContextAndSessionLocationExpectedId(ctx, sessionLocationId);	
					}else {
					    totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
					    totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
					}
			    	if(totalExpected!=0) {
			    		percent = 100*(totalPresent.floatValue()/totalExpected.floatValue());
			    	}
			        if(sessionLocationBadged != null) {
			    		sessionLocationBadged.setNbPresentsSessionLocation(totalPresent);
			    	}
					if(appliConfigService.isTagCheckerDisplayed() && !tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppn, null).getContent().isEmpty()) {
						String presence2 ="true,tagchecker," + eppn + "," + sessionLocationId;
						tagCheckerService.updatePresentsTagCkeckers(presence2, typeEmargement);
					}
			        dataEmitterService.sendData(presentTagCheck, percent, totalPresent, sessionLocationBadged, msgError);
	    		}
			}
		}
    	return list;
	}
	
	public boolean saveTagCheckSessionLibre(Long slId, String eppn, String emargementContext, SessionLocation sl) {
		
		Groupe gpe = sl.getSessionEpreuve().getBlackListGroupe();
		boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);
		
		if(!isBlackListed) {
			List<TagCheck> existingTc = tagCheckRepository.findTagCheckBySessionLocationBadgedIdAndPersonEppnEquals(slId, eppn);
    	
	    	if(existingTc.isEmpty()) {
	    		Long nbTc =  tagCheckRepository.countBySessionLocationExpectedId(slId);
	    		if(nbTc < sl.getCapacite()){
			    	List<Person> list = personRepository.findByEppn(eppn);
			    	Context context = contextRepository.findByContextKey(emargementContext);
			    	Person p = null;
			    	LdapUser user = ldapUserRepository.findByEppnEquals(eppn).get(0);
			    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			    	TagChecker tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0);
			    	if(!list.isEmpty()) {
			    		p = list.get(0);
			    	}else {
			    		p = new Person();
			    		p.setEppn(eppn);
			    		p.setContext(context);
			    		String type = (user.getNumEtudiant() == null)? "staff" : "student";
			    		p.setType(type);
			    		p.setNumIdentifiant(user.getNumEtudiant());
			    		personRepository.save(p);
			    	}
			    	TagCheck tc = new TagCheck();
			    	tc.setContext(context);
			    	tc.setTypeEmargement(TypeEmargement.MANUAL);
			    	tc.setPerson(p);
			    	tc.setSessionEpreuve(sl.getSessionEpreuve());
			    	tc.setSessionLocationBadged(sl);
			    	tc.setSessionLocationExpected(sl);
			    	tc.setTagChecker(tagChecker);
			    	tc.setTagDate(new Date());
			    	tc.setNbBadgeage(tagCheckService.getNbBadgeage(tc, true));
			    	tc.setIsTiersTemps(sl.getIsTiersTempsOnly());
			    	tagCheckRepository.save(tc);
	    		}
	    	}
    	}
		
		return isBlackListed;
	}
	
	public String getBase64Photo(EsupNfcTagLog taglog) {
		String photo64 = null;
		String locationNom = taglog.getLocation();
		String[] splitLocationNom = locationNom.split(" // ");
		String idSession = splitLocationNom[3];
		Long id = Long.valueOf(idSession);
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
		Context ctx = sessionEpreuve.getContext();
		List<AppliConfig> list = appliConfigRepository.findAppliConfigByKeyAndContext(AppliConfigKey.ENABLE_PHOTO_ESUPNFCTAG.name(), ctx);
        //Si c'est le surveillant qui a badgé la personne: on affiche la photo
		boolean isOk = false;
		String eppnTagChecker = taglog.getEppnInit();
		List<TagCheck> tcs = tagCheckRepository.findBySessionEpreuveIdAndPersonEppn(id, taglog.getEppn());
		if (!tcs.isEmpty()) {
           TagChecker tagChecker = (sessionEpreuve.getIsSecondTag() != null && sessionEpreuve.getIsSecondTag()) ? tcs.get(0).getTagChecker2() : tcs.get(0).getTagChecker();
           if (tagChecker != null && tagChecker.getUserApp() != null && eppnTagChecker.equals(tagChecker.getUserApp().getEppn())) {
               isOk = true;
           }
		}
		if(!list.isEmpty() && list.get(0).getValue().equals("true") && isOk) {
			String eppn = taglog.getEppn();
			RestTemplate template = new RestTemplate();
			String uri = null;
			byte[] photo = null;
			Boolean noPhoto = true;
			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> httpResponse = new ResponseEntity<>(photo, headers, HttpStatus.OK);
			if(!"inconnu".equals(eppn)) {
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
				MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<>();
				HttpEntity<Object> request = new HttpEntity<>(multipartMap, headers);
				uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
					noPhoto = false;
					httpResponse = template.exchange(uri, HttpMethod.GET, request, byte[].class);
					if(httpResponse.getBody() == null) noPhoto = true;
			}
			if (noPhoto) {
				ClassPathResource noImg = new ClassPathResource("NoPhoto.png");
				try {
					photo = IOUtils.toByteArray(noImg.getInputStream());
					httpResponse = new ResponseEntity<>(photo, headers, HttpStatus.OK);
				} catch (IOException e) {
					log.info("IOException reading ", e);
				}
			}
			photo64 = Base64Utils.encodeToString(httpResponse.getBody());
		}
		return photo64;
	}
	public static boolean isValidEppn(String eppn) {
        String eppnRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(eppnRegex);
        Matcher matcher = pattern.matcher(eppn);
        return matcher.matches();
    }
}
