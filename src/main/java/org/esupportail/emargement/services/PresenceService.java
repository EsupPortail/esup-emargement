package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import org.springframework.context.MessageSource;
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
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PresenceService {
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired	
	ContextRepository contextRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
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
	GroupeService groupeService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired	
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void getPdfPresence(Document document, HttpServletResponse response,  Long sessionLocationId, String emargementContext, ByteArrayOutputStream bos) {
        document.setMargins(10, 10, 10, 10);
       
        SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
        SessionEpreuve se = sl.getSessionEpreuve();
        List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(se.getId(), null).getContent();
    	tagCheckService.setNomPrenomTagChecks(list, false, false);
        String dateFin = (se.getDateFin()!=null)? "" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
        String date = String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).concat("\n"  + dateFin);
        String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").
    			concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin));
        String heures = String.format("%1$tH:%1$tM", se.getHeureEpreuve()) + " - " + String.format("%1$tH:%1$tM", se.getFinEpreuve());
        Long totalExpected = tagCheckRepository.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(se.getId());
    	Long totalPresent = tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNull(se.getId());
    	String total = String.valueOf(totalPresent) + " / " + String.valueOf(totalExpected);
    	String title = se.getNomSessionEpreuve() + " // " + sl.getLocation().getNom();
    	List<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
    	tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
    	String surveillants = tagCheckers.stream()
                .map(t-> (t.getUserApp().getPrenom()+ "-" + t.getUserApp().getNom())).distinct()
                .collect(Collectors.joining(","));
        try 
        {
			
			PdfWriter writer =   null;
			if(bos != null) {
				writer = PdfWriter.getInstance(document, bos);
			}else {
				response.setContentType("application/pdf");
				response.setHeader("Content-Disposition", "attachment; filename=".concat(nomFichier));
				writer = PdfWriter.getInstance(document, response.getOutputStream());
			}
			
			Image image = Image.getInstance(PresenceService.class.getResource("/static/images/logo.jpg"));
			image.scaleAbsolute(150f, 50f);//image width,height 

			PdfPTable headerTable = new PdfPTable(3);
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Présents"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Date"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Heure"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(total)); 
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(date)); 	
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(heures));	
			
			Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD);
			Paragraph name = new Paragraph(title, font);
			name.setSpacingBefore(2f);

			PdfPTable tagCheckerTable = new PdfPTable(1);
			tagCheckerTable.setWidthPercentage(100);
			tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell("Surveillants :"));
			tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell(surveillants));
			PdfPCell summaryL = new PdfPCell (tagCheckerTable);
			summaryL.setColspan (4);
			summaryL.setPadding (1.0f);	                   
			
			PdfPTable remarques = new PdfPTable(1);
			remarques.setWidthPercentage(100);
			remarques.addCell(pdfGenaratorUtil.getRemarquesCell("Remarques: " + se.getComment()));
			PdfPCell summaryR = new PdfPCell (remarques);
			summaryR.setColspan (3);         

			PdfPTable describer = new PdfPTable(1);
			describer.setWidthPercentage(100);
			describer.addCell(pdfGenaratorUtil.getDescCell(" "));
			describer.addCell(pdfGenaratorUtil.getDescCell("Esup-emargement - " + Year.now().getValue()));	

			document.open();
			
			headerTable.setTotalWidth(300f);
			headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(), document.top(),writer.getDirectContent());
			
			PdfPTable mainTable = new PdfPTable(7); 
			mainTable.setWidthPercentage(100);
			mainTable.setWidths(new float[] { 0.7f,1.5f,1.5f,2,0.8f,1.5f,1.4f});
			mainTable.setSpacingBefore(20.0f);
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("#"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Nom"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Prénom"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Identifiant"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Type"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Emargement"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Mode"));
			document.add(image);
			document.add(name);
			
	        if(!list.isEmpty()) {
	        	int i =0;int j =1;
	        	for(TagCheck tc : list) {
	        		String dateEmargement  = "";
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String typeemargement = "";
	        		String typeIndividu = "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getNumIdentifiant();
	        			if(identifiant == null) {
	        				identifiant = tc.getPerson().getEppn();
	        			}
	        			typeIndividu = messageSource.getMessage("person.type.".concat(tc.getPerson().getType()), null, null).substring(0,1);
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        			typeIndividu = "Ex";
	        		}
	        		if(BooleanUtils.isTrue(tc.getIsExempt())) {
        				dateEmargement = "Exempt";
	        		}else if(tc.getTagDate() != null) {
	        			dateEmargement = String.format("%1$tH:%1$tM", tc.getTagDate());
	        			if(tc.getIsUnknown()) {
	        				dateEmargement = "Inconnu";
	        			}
	        		}
	        		if(tc.getIsTiersTemps()) {
	        			dateEmargement += " \nTemps aménagé";
	        		}
	        		if(tc.getTypeEmargement()!=null) {
	        			typeemargement = messageSource.getMessage("typeEmargement.".concat(tc.getTypeEmargement().name().toLowerCase()), null, null) + "\n";
	        		}
	        		typeemargement += (tc.getProxyPerson()!=null)? "Proc : " + tc.getProxyPerson().getPrenom() + ' ' + tc.getProxyPerson().getNom(): "";
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(String.valueOf(j)));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(nom));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(prenom));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(identifiant));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(typeIndividu));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(dateEmargement));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(typeemargement));
					i++;
					if (i == 18) {
						mainTable.addCell(summaryL);
						mainTable.addCell(summaryR);
						document.add(mainTable);
						document.add(describer);
						document.newPage();
						document.add(image);
						headerTable.setTotalWidth(300f);
						headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(), document.top(),
								writer.getDirectContent());
						document.add(name);
						mainTable = new PdfPTable(7);
						mainTable.setWidthPercentage(100);
						mainTable.setWidths(new float[] { 0.7f,1.5f,1.5f,2,0.8f,1.5f,1.4f});
						mainTable.setSpacingBefore(20.0f);
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("#"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Nom"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Prénom"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Identifiant"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Type"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Emargement"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Mode"));
						i=0;
					}
					j++;
	        	}
	        }
			mainTable.addCell(summaryL);
			mainTable.addCell(summaryR);
			document.add(mainTable);
			document.add(describer);
			logService.log(ACTION.EXPORT_PDF, RETCODE.SUCCESS, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        } catch (DocumentException de) {
          de.printStackTrace();
          logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        } catch (IOException de) {
          de.printStackTrace();
          logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        }
    }
	
	//Pour emargement manuel et qrCode
	public List<TagCheck> updatePresents(String presence, SessionLocation validLocation) throws ParseException {
		String eppn = null;
		TagChecker tagChecker = null;
		boolean isTagCheckerNeeded = true;
		boolean isValid = true;
		boolean isQrCode = presence.startsWith("qrcode");
		boolean isQrCodeUser = presence.startsWith("qrcodeUser");
		boolean isQrcodeSession = presence.startsWith("qrcodeSession");
		List<TagCheck> list = new ArrayList<TagCheck>();
		Context ctx = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(isQrCode) {
			if(presence.contains("@@@")) {
				isTagCheckerNeeded = false;
				String splitPresence [] = presence.split("@@@");
				presence = splitPresence[0];
				eppn = toolUtil.decodeFromBase64(splitPresence[1]);
			}
			String temp = toolUtil.decodeFromBase64(presence.replace("qrcodeUser", "").replace("qrcodeSession", ""));
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
	    		if(splitPresence.length >4 && "qrcode".equals(splitPresence[4].trim())) {
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
		    	if(splitPresence.length >2) {
			    	if(eppn == null) {
			    		eppn = splitPresence[1].trim();
			    	}
			    	if(isQrCodeUser) {
			    		sessionLocationId = validLocation.getId();
			    	}else {
			    		sessionLocationId = Long.valueOf(splitPresence[2].trim());
			    	}
			    	if(ctx!=null) {
				    	List<SessionLocation> sls = sessionLocationRepository.findByContextAndId(ctx, sessionLocationId);
				    	sessionLocation = sls.get(0);
				    	sessionEpreuve = sessionLocation.getSessionEpreuve();
				    	Long seId = null;
				    	List<TagCheck> tcs = new ArrayList<TagCheck>();
				    	if(!sessionEpreuve.isSessionLibre) {
							try {
								tcs = tagCheckRepository.findTagCheckByPersonEppnAndSessionEpreuve(eppn, sessionEpreuve);
								if(tcs.isEmpty()) {
									//comparaison avec les heures de début et de fin
						    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						    		Date today = dateFormat.parse(dateFormat.format(new Date()));
									LocalTime now = LocalTime.now();
									seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, today, now); 
									String lieu = " Autre session dans la journée";
									if(seId != null) {
										SessionEpreuve otherSe = sessionEpreuveRepository.findById(seId).get();
										lieu = " Attendu --> Session : "+ otherSe.getNomSessionEpreuve();
										List<TagCheck> list2 = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(otherSe.getId(), eppn, null).getContent();
										if(!list2.isEmpty()) {
											if(list2.get(0).getSessionLocationExpected() !=null) {
												String location = list2.get(0).getSessionLocationExpected() .getLocation().getNom();
												lieu = lieu.concat(" , lieu : ").concat(location);
											}
										}
										comment = "Inconnu dans cette session " + lieu;
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
					    		Long countSe = tagCheckRepository.countByPersonEppnAndSessionEpreuveDateExamen(eppn, date);
								List <TagCheck> checkOtherLocation = tagCheckRepository.findBySessionEpreuveAndPersonEppnAndIsUnknownFalse(sessionEpreuve, eppn);
								SessionLocation sessionLocationExpected = checkOtherLocation.get(0).getSessionLocationExpected();
					    		if(!tcs.isEmpty()) {
									if(sessionEpreuve.getTypeBadgeage().equals(TypeBadgeage.SALLE)) {
										List <TagCheck> tcsTemp = tagCheckRepository.findBySessionLocationExpectedAndPersonEppnAndIsUnknownFalse(sessionLocation, eppn);
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
									String lieu = " Autre session dans la journée";
									if(seId != null) {
										SessionEpreuve otherSe = sessionEpreuveRepository.findById(seId).get();
										lieu = " Attendu --> Session : "+ otherSe.getNomSessionEpreuve();
										List<TagCheck> list2 = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(otherSe.getId(), eppn, null).getContent();
										if(!list2.isEmpty()) {
											if(list2.get(0).getSessionLocationExpected() !=null) {
												String location = list2.get(0).getSessionLocationExpected() .getLocation().getNom();
												lieu = lieu.concat(" , lieu : ").concat(location);
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
			    	}else {
			    		sessionLocationBadged = null;
			    	}
		    	}
		    	if(isUnknown) {
					TagCheck newTc = tagCheckService.saveUnknownTagCheck(comment, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker, false, typeEmargement);
					dataEmitterService.sendData(newTc, Float.valueOf("0"), Long.valueOf("0"), new SessionLocation(), "");
				}else if(sessionLocation != null) {
		    		Date date = (isPresent)? new Date() : null;
		    		SessionEpreuve se = sessionLocation.getSessionEpreuve();
			    	Groupe gpe = se.getBlackListGroupe();
					boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);	
			    	String email = (splitPresence.length >3)? splitPresence[3] : "";
			    	TagCheck presentTagCheck = null;
			    	if(isQrCode && se.getIsSessionLibre()){
			    		presentTagCheck = tagCheckService.saveUnknownTagCheck("", ctx, eppn, se, sessionLocationBadged, tagChecker, true, typeEmargement);
			    	}else {
				    	if(!eppn.isEmpty()) {
				    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn).get(0);
				    	}else if(!email.isEmpty()) {
				    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndGuestEmailEquals(sessionLocationId, email).get(0);
				    	}
				    	presentTagCheck.setSessionEpreuve(se);
				    	presentTagCheck.setTypeEmargement(typeEmargement);
				    	presentTagCheck.setNbBadgeage(tagCheckService.getNbBadgeage(presentTagCheck, isPresent));
				    	
				    	if(tagChecker != null) {
				    		presentTagCheck.setTagChecker(tagChecker);
				    	}else if(isTagCheckerNeeded) {
							tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0) : null;
							presentTagCheck.setTagChecker(tagChecker);
				    	}
						if(!isPresent) {
							presentTagCheck.setProxyPerson(null);
							presentTagCheck.setTypeEmargement(null);
							presentTagCheck.setNbBadgeage(null);
						}
				    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
				    	presentTagCheck.setTagDate(date);
				    	presentTagCheck.setContext((ctx != null)? ctx : contextService.getcurrentContext());
				    	if(!isBlackListed && isPresent || !isBlackListed && !isPresent || isBlackListed && !isPresent) {
				    		tagCheckRepository.save(presentTagCheck);
				    	}
			    	}
			    	list.add(presentTagCheck);
			    	tagCheckService.setNomPrenomTagChecks(list, false, false);
			    	if(presentTagCheck.getTagChecker() != null) {
			        	List<TagChecker> tcList = new ArrayList<TagChecker>();
			        	tcList.add(presentTagCheck.getTagChecker());
				    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
				    	presentTagCheck.setTagChecker(tcList.get(0));
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
		log.info(list.get(0).getValue());
		if(!list.isEmpty() && list.get(0).getValue().equals("true")) {
			String eppn = taglog.getEppn();
			RestTemplate template = new RestTemplate();
			String uri = null;
			byte[] photo = null;
			Boolean noPhoto = true;
			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);
			if(!"inconnu".equals(eppn)) {
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
				MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
				HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
				uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
					noPhoto = false;
					httpResponse = template.exchange(uri, HttpMethod.GET, request, byte[].class);
					if(httpResponse.getBody() == null) noPhoto = true;
			}
			if (noPhoto) {
				ClassPathResource noImg = new ClassPathResource("NoPhoto.png");
				try {
					photo = IOUtils.toByteArray(noImg.getInputStream());
					httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);
				} catch (IOException e) {
					log.info("IOException reading ", e);
				}
			}
			photo64 = Base64Utils.encodeToString(httpResponse.getBody());
		}
		return photo64;
	}

}
