package org.esupportail.emargement.services;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.repositories.custom.PersonRepositoryCustom;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;

@Service
public class TagCheckService {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	UserLdapRepository userLdapRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Resource
	ImportExportService importExportService;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	PersonRepositoryCustom personRepositoryCustom;

	@Resource
	EmailService emailService;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	PersonService personService;
	
	@Resource	
	SessionEpreuveService sessionEpreuveService;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
    @Resource
	DataEmitterService dataEmitterService;
    
    @Resource   
    TagCheckerService tagCheckerService;
	
	@Resource
	LogService logService;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    public void resetSessionLocationExpected(Long sessionEpreuveId){
    	
    	List<TagCheck> tagCkecks = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);    
    	
    	if(!tagCkecks.isEmpty()) {
    		for (TagCheck tc : tagCkecks) {
    			tc.setTagDate(null);
				tc.setSessionLocationExpected(null);
				tc.setSessionLocationBadged(null);
				tc.setDateEnvoiConvocation(null);
				tc.setIsCheckedByCard(null);
				tc.setTagChecker(null);
				tc.setNumAnonymat(null);
				tagCheckRepository.save(tc);
    		}
    	}
    }
    
    public Long countNbTagCheckRepartitionNull(Long sessionEpreuveId, boolean isTiersTemps) {
    	if(isTiersTemps) {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(sessionEpreuveId);
    	}else {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(sessionEpreuveId);
    	}
    }
    
    public Long countNbTagCheckRepartitionNotNull(Long sessionEpreuveId,  boolean isTiersTemps) {
    	if(isTiersTemps) {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrue(sessionEpreuveId);
    	}else {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalse(sessionEpreuveId);
    	}
    }
    
    public Page<TagCheck> getListTagChecksBySessionLocationId(Long id, Pageable pageable, String eppn, boolean withUnknown){
    	
    	Page<TagCheck> allTagChecks =  null;
    	if(eppn != null) {
    		if(withUnknown) {
    			allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEqualsOrSessionLocationBadgedIdAndPersonEppnEquals(id, eppn, id, eppn, pageable);
    		}else {
    			allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(id, eppn, pageable);
    		}
    	}
    	else {
    		if(withUnknown) {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(id, id, pageable);
    		}else {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedId(id, pageable);
    		}
    	}
		if(!allTagChecks.getContent().isEmpty()) {
			for(TagCheck tc : allTagChecks.getContent()) {
				if(tc.getSessionLocationBadged() != null && tc.getSessionLocationExpected() == null) {
					tc.setIsUnknown(true);
				}else {
					tc.setIsUnknown(false);
				}
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getPerson().getEppn());
				if(!userLdaps.isEmpty()) {
					tc.getPerson().setNom(userLdaps.get(0).getUsername());
					tc.getPerson().setPrenom(userLdaps.get(0).getPrenom());
				}
				if(tc.getTagChecker() != null) {
					List<UserLdap>  userLdaps2 = userLdapRepository.findByEppnEquals(tc.getTagChecker().getUserApp().getEppn());
					if(!userLdaps2.isEmpty()) {
						tc.getTagChecker().getUserApp().setNom(userLdaps2.get(0).getUsername());
						tc.getTagChecker().getUserApp().setPrenom(userLdaps2.get(0).getPrenom());
					}
				}
			}
		}
    	
    	return allTagChecks;
    }
    
    public List<TagCheck> importTagCheckCsv(Reader reader,  List<List<String>> finalList, Long sessionEpreuveId, String emargementContext, Long groupeId) throws Exception {
    	List<List<String>> rows  = new ArrayList<List<String>>();
    	String dataType = null;
    	if(reader!=null) {
    		rows  = importExportService.readAll(reader);
    	}else if(finalList != null) {
    		rows = finalList;
    	}
    	if(rows != null) {
	    	List<TagCheck> bilanCsv = new ArrayList<TagCheck>();
	    	List<TagCheck> tcToSave = new ArrayList<TagCheck>();
	    	List<String> unknowns = new ArrayList<String>();
	    	//De la forme [NumEtu,CodeGestion]
	    	if(rows.size()>0) {
	    		int i = 0; int j = 0; int k = 0;
	    		SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
	    		for(List<String> row : rows) {
		    		if(se != null) {
		    			TagCheck tc = new TagCheck();
		    			Person person = new Person();
		    			List<UserLdap>  userLdaps = null;
		    			String line = row.get(0).trim();
		    			Long tcTest = new Long(1);
		    			try {
		    				if(line.chars().allMatch(Character::isDigit)){
		    					userLdaps = userLdapRepository.findByNumEtudiantEquals(line);
								if(!userLdaps.isEmpty()) {
									person.setNumIdentifiant(line);
									dataType = "student";
								}
		    				}else {
		    					userLdaps = userLdapRepository.findByEppnEquals(line);
		    					if(!userLdaps.isEmpty()) {
		    						if(userLdaps.get(0).getNumEtudiant()!=null && !userLdaps.get(0).getNumEtudiant().isEmpty()) {
										person.setNumIdentifiant(line);
										dataType = "student";
		    						}
		    						else {
										dataType = "staff";
									}
								}
		    				}
			    			person.setContext(contexteService.getcurrentContext());
			    			tc.setSessionEpreuve(se);
			    			tc.setContext(contexteService.getcurrentContext());
			    			if(!userLdaps.isEmpty()) {
			    				person.setEppn(userLdaps.get(0).getEppn());
			    			}
			    			person.setType(dataType);
			    			tc.setPerson(person);
			    			if(groupeId != null){
			    				Groupe groupe = groupeRepository.findById(groupeId).get();
			    				tc.setGroupe(groupe);
			    			}
			    			if(!userLdaps.isEmpty()) {
			    				tcTest = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionEpreuveId, userLdaps.get(0).getEppn());
			    			}
						} catch (Exception e) {
							tcTest = new Long(-1);
							log.error("Erreur sur le login ou numéro identifiant "  + line + " lors de la recherche LDAP", e);
						}
			    		if(!userLdaps.isEmpty() && tcTest == 0) {
		    				tc.setFlagCSv("success");
		    				bilanCsv.add(tc);
		    				tcToSave.add(tc);
		    				i++;
			    			//tagCheckRepository.save(tc);
		    			}else if(tcTest >0 && !userLdaps.isEmpty()){
		    				tc.setComment("Personne déjà inscrite dans la session");
		    				tc.setFlagCSv("warning");
		    				bilanCsv.add(tc);
		    				j++;
		    			}else{
		    				tc.setComment("Personne inconnue dans le ldap");
		    				if(userLdaps.isEmpty()) {
			    				if("student".equals(dataType)) {
			    					person.setNumIdentifiant(line);
								}else {
									person.setEppn(line);
								}
		    				}
		    				tc.setPerson(person);
		    				bilanCsv.add(tc);
		    				unknowns.add(line);
		    				tc.setFlagCSv("danger");
		    				k++;
		    			}
	    			}
	    		}
	    		tagCheckRepository.saveAll(tcToSave);
	    		String inconnus = "";
	    		if(!unknowns.isEmpty()) {
	    			inconnus = " (Identifiants : "  + StringUtils.join(unknowns, ",") + ")";
	    		}
	            log.info("import inscriptions : Succès : " + i + " - Déjà inscrits : " + j + " Inconnus dans le ldap : " +  k + inconnus);
	            logService.log(ACTION.IMPORT_INSCRIPTION, RETCODE.SUCCESS, 
	            				"import inscriptions " + se.getNomSessionEpreuve() + " : Succès : " + i + " - Déjà inscrits : " + j + " - Inconnus dans le ldap : " +  k + inconnus , null,
	            				null, emargementContext, null);
	    	}
	    	return bilanCsv;
    	}else {
    		return null;
    	}
    }
    
    
    public List<List<String>> getListForimport(List<String> usersGroupLdap) {
    	List<List<String>> rows  = new ArrayList<List<String>>();
    	for(String user: usersGroupLdap) {
    		List<String> subList = new ArrayList<String>();
    		subList.add(user);
    		rows.add(subList);
    	}
    	return rows;
    }
    
    
    public  List<List<String>> setAddList(TagCheck tc){
    	
        List<String> strings = new ArrayList<String>();
        if(!tc.getPerson().getNumIdentifiant().isEmpty()){
        	 strings.add(tc.getPerson().getNumIdentifiant());
        }else {
        	 strings.add(tc.getPerson().getEppn());
        }
        List<List<String>> finalList = new ArrayList<List<String>>();
        finalList.add(strings);
        return finalList;
    }
    
    public void deleteAllTagChecksBySessionEpreuveId(Long sessionEpreuveId) {
		List<TagCheck> tagChecks =  tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
		if(!tagChecks.isEmpty()) {
			for(TagCheck tc : tagChecks){
				tagCheckRepository.deleteById(tc.getId());
			}
		}
    }
    
	public int setNomPrenomTagChecks(List<TagCheck> tagChecks){
		
		int count = 0;
		
		if(!tagChecks.isEmpty()) {
			for(TagCheck tc : tagChecks) {
				if(tc.getPerson()!=null) {
					List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getPerson().getEppn());
					if(!userLdaps.isEmpty()) {
						tc.getPerson().setNom(userLdaps.get(0).getUsername());
						tc.getPerson().setPrenom(userLdaps.get(0).getPrenom());
					}else {
						count ++;
					}
				}
			}
		}
		
		return count;
	}
	
	public String snTagChecks(List<Long> tagCheckIds){
		
		List<String> snTagChecks = new ArrayList<String>();
		
		if(!tagCheckIds.isEmpty()) {
			for(Long id : tagCheckIds) {
				TagCheck tc =tagCheckRepository.findById(id).get();
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getPerson().getEppn());
				String sn = "";
				if(!userLdaps.isEmpty()) {
					sn = userLdaps.get(0).getPrenom().concat(" ").concat(userLdaps.get(0).getUsername());
				}
				snTagChecks.add(sn);
			}
		}
		
		return StringUtils.join(snTagChecks, ", ");
	}
	
	public void getPdfConvocation(HttpServletResponse response, String htmltemplate) throws Exception {
		String filePath = pdfGenaratorUtil.createPdf(htmltemplate);
		FileInputStream fis = null;
		response.setContentType("application/pdf");
		response.setHeader("Content-disposition", "attachment;filename=" + "convocation.pdf");
		try {
			File f = new File(filePath);
			fis = new FileInputStream(f);
			DataOutputStream os = new DataOutputStream(response.getOutputStream());
			response.setHeader("Content-Length", String.valueOf(f.length()));
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = fis.read(buffer)) >= 0) {
				os.write(buffer, 0, len);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}
	
	public String replaceFields(String htmltemplate, TagCheck tc) {
		
		String wrapChar = "@";
		String finalString= "";
		finalString = htmltemplate.replace(wrapChar.concat("civilite").concat(wrapChar), tc.getPerson().getCivilite());
		finalString = finalString.replace(wrapChar.concat("prenom").concat(wrapChar), tc.getPerson().getPrenom());
		finalString = finalString.replace(wrapChar.concat("nom").concat(wrapChar), tc.getPerson().getNom());
		finalString = finalString.replace(wrapChar.concat("nomSession").concat(wrapChar), tc.getSessionEpreuve().getNomSessionEpreuve());
		finalString = finalString.replace(wrapChar.concat("dateExamen").concat(wrapChar), String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateExamen()));
		finalString = finalString.replace(wrapChar.concat("heureConvocation").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getHeureConvocation()));
		finalString = finalString.replace(wrapChar.concat("debutEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getHeureEpreuve()));
		finalString = finalString.replace(wrapChar.concat("finEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", tc.getSessionEpreuve().getFinEpreuve()));
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), sessionEpreuveService.getDureeEpreuve(tc.getSessionEpreuve()));
		finalString = finalString.replace(wrapChar.concat("site").concat(wrapChar), tc.getSessionEpreuve().getCampus().getSite());
		finalString = finalString.replace(wrapChar.concat("salle").concat(wrapChar), tc.getSessionLocationExpected().getLocation().getNom());
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), tc.getSessionLocationExpected().getLocation().getAdresse());
		
		return finalString;
	}
	
	public void sendEmailConvocation (String subject, String bodyMsg, boolean isSendToManager, List<Long> listeIds, String htmltemplatePdf, String emargementContext, boolean isAll, Long seId) throws Exception {
		if(!listeIds.isEmpty() || isAll) {
			int i=0; int j=0; 
			ArrayList<String> errors = new ArrayList<String>();
			String[] ccArray = {};
			if(isSendToManager){
				List<String> managers = appliConfigService.getListeGestionnaires();
				ccArray = managers.stream().toArray(String[]::new);
			}
			//Get ListId from all
			if(isAll) {
				List<TagCheck> listTc = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(seId);
				listeIds = listTc.stream().map(tc -> tc.getId()).distinct().collect(Collectors.toList());
			}
			
			for(Long id : listeIds) {
				TagCheck tc = tagCheckRepository.findById(id).get();
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getPerson().getEppn());
				if(!userLdaps.isEmpty()) {
					try {
						tc.getPerson().setNom(userLdaps.get(0).getUsername());
						tc.getPerson().setPrenom(userLdaps.get(0).getPrenom());
						tc.getPerson().setCivilite(userLdaps.get(0).getCivilite());
						String filePath = pdfGenaratorUtil.createPdf(replaceFields(htmltemplatePdf,tc));
						String email = userLdaps.get(0).getEmail();
						if(!appliConfigService.getTestEmail().isEmpty()) {
							email = appliConfigService.getTestEmail();
						}
						if(appliConfigService.isSendEmails()){
							emailService.sendMessageWithAttachment(email, subject, bodyMsg, filePath, "convocation.pdf", ccArray);
						}

						tc.setDateEnvoiConvocation(new Date());
						tagCheckRepository.save(tc);
						i++;
					} catch (Exception e) {
						errors.add(userLdaps.get(0).getEppn());
						e.printStackTrace();
						j++;
					}
				}
			}
			if(appliConfigService.isSendEmails()){
				log.info("Envoi de convocations :  " + i  );
				logService.log(ACTION.SEND_CONVOCATIONS, RETCODE.SUCCESS, "Nombre envoyé : " + i , null,
								null, emargementContext, null);
			}else {
				log.info("Envoi de mail désactivé :  ");
			}
			if(j > 0) {
				log.error("Envois de convocations avortés :  " + j);
				logService.log(ACTION.SEND_CONVOCATIONS, RETCODE.FAILED, "Nombre en erreur : " + j , null,
						null, emargementContext, null);
				if(isSendToManager && ccArray.length>0){
					bodyMsg = "Liste des eppn en erreur : " + StringUtils.join(errors, ",");
					emailService.sendSimpleMessage(ccArray[0], "Erreurs d'envoi de convocations", bodyMsg, ccArray);
				}
			}
		}
	}
	
	public boolean tagAction(String eppn, EsupNfcTagLog esupNfcTagLog, String action) throws ParseException {
		boolean isOk = false;
		
		List<Person> persons = personRepositoryCustom.findByEppn(eppn);
		log.info("tagaction pour l'eppn : " + eppn);
		if (!persons.isEmpty()) {
			String locationNom = esupNfcTagLog.getLocation();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = dateFormat.parse(dateFormat.format(new Date()));
			String[] splitLocationNom = locationNom.split(" // "); 
			if("isTagable".equals(action)) {
				log.info("isTagable pour l'eppn : " + eppn);
				Long tc = tagCheckRepository.checkIsTagable(splitLocationNom[1], eppn, date);
				
				if (tc ==1) {
					isOk = true;
				}else {
					try {
						//on regarde si la personne est dans une autre salle de la session
						Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date);
						if(sessionLocationId != null) {
							log.info("Personne non reconnue dans cette salle : " +  eppn);
							SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
							List<SessionLocation> listSl =  sessionLocationRepository.findSessionLocationBySessionEpreuve(sl.getSessionEpreuve());
							List<String> nomsLocation  = listSl.stream().map(l -> l.getLocation().getNom()).distinct().collect(Collectors.toList());
							if(nomsLocation.contains(splitLocationNom[1])) {
								isOk = true;
							}
						}else {
							List<Location> locations = locationRepository.findByNom(splitLocationNom[1], null).getContent();
							if(!locations.isEmpty()) {
								String nomSessionEpreuve = splitLocationNom[0];
								List<SessionEpreuve> listSe =  sessionEpreuveRepository.findByNomSessionEpreuve(nomSessionEpreuve, null).getContent();
								if(!listSe.isEmpty()) {
									log.info("On enregistre l'inconnu dans la session : " +  eppn);
									List<SessionLocation> slList = sessionLocationRepository.findSessionLocationByLocationIdAndSessionEpreuveId(locations.get(0).getId(), listSe.get(0).getId());
									if(!slList.isEmpty()) {
										SessionLocation sl = slList.get(0);
										Long count = tagCheckRepository.countBySessionEpreuveIdAndPersonEppnEquals(sl.getSessionEpreuve().getId(), eppn);
										if(count==0) {
											TagChecker tagChecker = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(sl, esupNfcTagLog.getEppnInit());
											TagCheck unknownTc = new TagCheck();
											unknownTc.setComment("Personne inconnue dans la seesion");
											unknownTc.setSessionEpreuve(sl.getSessionEpreuve());
											unknownTc.setContext(sl.getContext());
											Person p = new Person();
											p.setContext(sl.getContext());
											p.setEppn(eppn);
											List<UserLdap> users = userLdapRepository.findByEppnEquals(eppn);
											if(!users.isEmpty()) {
												p.setType((users.get(0).getNumEtudiant()!=null)? "student" : "staff");
												p.setNumIdentifiant(users.get(0).getNumEtudiant());
											}
											unknownTc.setPerson(p);
											unknownTc.setTagChecker(tagChecker);
											unknownTc.setSessionLocationBadged(sl);
											unknownTc.setTagDate(new Date());
											unknownTc.setIsCheckedByCard(true);
											tagCheckRepository.save(unknownTc);
											dataEmitterService.sendData(new TagCheck(), new Float(0), new Long(0), 1, new SessionLocation());
										}
									}
								}
							}else {
								log.error("Impossible de récupérer l'id de ce lieu : " +  locationNom);
							}
						}
					} catch (Exception e) {
						log.error("Problème de carte pour l'eppn : "  + eppn, e);
					}
				}
			}else if("validateTag".equals(action)) {
				log.info("validateTag pour l'eppn : " + eppn);
				Long realSlId = null;
				log.info("Eppn : " + eppn + " Date " + date);
				Long sessionLocationId = tagCheckRepository.getSessionLocationId(splitLocationNom[1], eppn, date);
				log.info("SessionLocationId : " + sessionLocationId);
				TagCheck presentTagCheck = null;
				if(sessionLocationId!=null) {
					realSlId = sessionLocationId;
					presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, eppn);
				}else {
					log.info("Eppn : " + eppn + " Date " + date + " --> Badgeage dans le mauvais lieu");
					
					Long slId = tagCheckRepository.getSessionLocationId(splitLocationNom[1], date);
					realSlId = slId;
					if(slId!=null) {
						SessionLocation sl = sessionLocationRepository.findById(slId).get();
						presentTagCheck = tagCheckRepository.findTagCheckBySessionEpreuveIdAndEppn(sl.getSessionEpreuve().getId(), esupNfcTagLog.getEppn());
					}
				}
				
				if(presentTagCheck!=null) {
			    	presentTagCheck.setTagDate(new Date());
			    	presentTagCheck.setIsCheckedByCard(true);
			    	SessionLocation sl = sessionLocationRepository.findById(realSlId).get();
					TagChecker tagChecker = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(sl, esupNfcTagLog.getEppnInit());
			    	presentTagCheck.setTagChecker(tagChecker);
			    	presentTagCheck.setSessionLocationBadged(sl);
			    	Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sl);
			    	sl.setNbPresentsSessionLocation(countPresent);
			    	List<TagChecker> tcList = new ArrayList<TagChecker>();
		        	tcList.add(presentTagCheck.getTagChecker());
			    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
			    	tagCheckRepository.save(presentTagCheck);
			    	isOk = true;
			    	//On prend l'id du sl expected
			    	Long slExpected = presentTagCheck.getSessionLocationExpected().getId();
			    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(slExpected);
			    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(slExpected);
			    	float percent = 0;
			    	if(totalExpected!=0) {
			    		percent = 100*(new Long(totalPresent).floatValue()/ new Long(totalExpected).floatValue() );
			    	}
			    	dataEmitterService.sendData(presentTagCheck, percent, totalPresent, 0, sl);
				}
			}
		}
		return isOk;
	}
	
	public void save(TagCheck tagCheck, String emargementContext) {
        TagCheck tc = tagCheckRepository.findById(tagCheck.getId()).get();
        tc.setIsTiersTemps(tagCheck.getIsTiersTemps());
        tc.setComment(tagCheck.getComment());
        tagCheckRepository.save(tc);
        log.info("maj inscrit ok : " + tc.getPerson().getEppn());
        logService.log(ACTION.UPDATE_INSCRIPTION, RETCODE.SUCCESS, "maj inscrit : " + tc.getPerson().getEppn(), null,
				null, emargementContext, null);
	}
	
	public void exportTagChecks(String type, Long id, String tempsAmenage,HttpServletResponse response, String emargementContext) {
		Page<TagCheck> tagCheckPage = null;
        Long count = new Long(0);
        Long presentTotal = new Long(0);
        String tiers = "";
    	if("tiers".equals(tempsAmenage)) {
    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonEppn(id, null);
    		count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(id);
    		presentTotal = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsTrueAndTagDateIsNotNull(id);
    		tiers = " -- Temps aménagé";
    	}else if("notTiers".equals(tempsAmenage)) {
    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonEppn(id, null);
    		count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(id);
    		presentTotal = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsFalseAndTagDateIsNotNull(id);
    	}else {
    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(id, null);
    		count = tagCheckRepository.countTagCheckBySessionEpreuveId(id);
    		presentTotal = tagCheckRepository.countTagCheckBySessionEpreuveIdAndTagDateIsNotNull(id);
    	}
    	List<TagCheck> list = tagCheckPage.getContent();
		this.setNomPrenomTagChecks(tagCheckPage.getContent());
		if ("PDF".equals(type)) {
			
		   	PdfPTable table = new PdfPTable(7);
	    	
	    	table.setWidthPercentage(100);
	    	table.setHorizontalAlignment(Element.ALIGN_CENTER);

	        //On créer l'objet cellule.
	    	TagCheck tch = list.get(0);
	    	String libelleSe = tch.getSessionEpreuve().getNomSessionEpreuve().concat(" -- ").
	    			concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionEpreuve().getDateExamen())))
	    			.concat(" --  nb de présents :  ").concat(presentTotal.toString()).concat("/").concat(count.toString());

	        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
	        cell.setBackgroundColor(BaseColor.GREEN);
	        cell.setColspan(7);
	        table.addCell(cell);
	   
	        //contenu du tableau.
	        PdfPCell header1 = new PdfPCell(new Phrase("N] Identifiant")); header1.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header2 = new PdfPCell(new Phrase("Nom")); header2.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header3 = new PdfPCell(new Phrase("Prénom")); header3.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header4 = new PdfPCell(new Phrase("Présent")); header4.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header5 = new PdfPCell(new Phrase("Badgeage")); header5.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header6 = new PdfPCell(new Phrase("Lieu badgé")); header6.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header7 = new PdfPCell(new Phrase("Temps aménagé")); header7.setBackgroundColor(BaseColor.GRAY);
	        
	        table.addCell(header1);
	        table.addCell(header2);
	        table.addCell(header3);
	        table.addCell(header4);
	        table.addCell(header5);
	        table.addCell(header6);
	        table.addCell(header7);
	        
	        
	        if(!list.isEmpty()) {
	        	for(TagCheck tc : list) {
	        		String presence = "Absent";
	        		String date  = "--";
	        		String badged  = "--";
	        		String tiersTemps  = "--";
	        		BaseColor b = new BaseColor(232, 97, 97, 50);
	        		PdfPCell dateCell = null;
	        		if(tc.getTagDate() != null) {
	        			presence = "Présent";
	        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
	        			b = new BaseColor(19, 232, 148, 50);
	        		}
	        		if(tc.getSessionLocationBadged()!=null) {
	        			badged = tc.getSessionLocationBadged().getLocation().getNom();
	        		}
	        		if(tc.getIsTiersTemps()) {
	        			tiersTemps = "Oui";
	        		}
	        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getNumIdentifiant()));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getNom()));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getPrenom()));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(presence));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(date));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(badged));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(tiersTemps));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);	                
	        	}
	        }
	        
	        Document document = new Document();
	        document.setMargins(10, 10, 10, 10);
	        try 
	        {
	          response.setContentType("application/pdf");
	          PdfWriter.getInstance(document,  response.getOutputStream());
	          document.open();

	          document.add(table);

	        } catch (DocumentException de) {
	          de.printStackTrace();
	        } catch (IOException de) {
	          de.printStackTrace();
	        }
	       
	        document.close();
			
			
		}else if("CSV".equals(type)){
			try {
				
				String filename = "users.csv";

				response.setContentType("text/csv");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				        "attachment; filename=\"" + filename + "\"");

				//create a csv writer
				CSVWriter writer = new CSVWriter(response.getWriter()); 
				String [] headers = {"Session", "Eppn", "Nom", "Prénom", "Numéro Etu", "Tiers-temps"};
				writer.writeNext(headers);
				for(TagCheck tc : tagCheckPage.getContent()) {
					List <String> line = new ArrayList<String>();
					line.add(tc.getSessionEpreuve().getNomSessionEpreuve());
					line.add(tc.getPerson().getEppn());
					line.add(tc.getPerson().getNom());
					line.add(tc.getPerson().getPrenom());
					line.add(tc.getPerson().getNumIdentifiant());
					line.add(tc.getIsTiersTemps().toString());
					writer.writeNext(line.toArray(new String[1]));
				}
		        // closing writer connection 
		        writer.close(); 
				log.info("Extraction csv :  " + tagCheckPage.getContent().size() + "résultats" );
				logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction csv :" +  tagCheckPage.getContent().size() + " résultats" , null,
								null, emargementContext, null);
			} catch (Exception e) {
				log.error("Erreur lors de lxtraction csv");
				logService.log(ACTION.EXPORT_CSV, RETCODE.FAILED, "Erreur lors de lxtraction csv" , null, null, emargementContext, null);
				e.printStackTrace();
			}
		}
	}
	
	public String getContextWs(EsupNfcTagLog esupNfcTagLog) throws ParseException {
		String key = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		String locationNom = esupNfcTagLog.getLocation();
		String[] splitLocationNom = locationNom.split(" // ");
		String eppn = esupNfcTagLog.getEppn();
		key = tagCheckRepository.getContextId(splitLocationNom[1], eppn, date);
		if(key == null) {
			Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date);
			SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
			
			key = tagCheckRepository.getContextIdBySeId(sl.getSessionEpreuve().getId(), eppn, date);
		}

		
		
		return key;
	}
	
	public List<Location> searchRepartition(Long id){
		
		List<Location> searchRepartition = new ArrayList<Location>();
		
		List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(id);
		
		if(!tcs.isEmpty()) {
			for(TagCheck tc : tcs) {
				if(tc.getSessionLocationExpected()!=null) {
					Location l = tc.getSessionLocationExpected().getLocation();
					if(!searchRepartition.contains(l)) {
						searchRepartition.add(l);
					}
				}
			}
		}
		searchRepartition.sort(Comparator.comparing(Location::getNom).reversed());

		return searchRepartition;
	}
	
	public Long countTagchecks(String tempsAmenage, String eppn, Long id, Long repartitionId) {
		Long count = new Long (0);
    	if("tiers".equals(tempsAmenage)) {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(id);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsTrue(id, eppn);
    			}else {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(id, eppn, repartitionId);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(id, repartitionId);
    			}
    		}
    	}else if("notTiers".equals(tempsAmenage)) {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(id);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId ==null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsFalse(id, eppn);
    			}else {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(id, eppn, repartitionId);
    			}
    		}
    		else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(id, repartitionId);
    			}
    		}
    	}else {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		count = tagCheckRepository.countTagCheckBySessionEpreuveId(id);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEquals(id, eppn);
    			}else {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEquals(id, eppn, repartitionId);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEquals(id, repartitionId);
    			}
    		}
    	}
		return count;
	}
	
	public  Page<TagCheck> getTagCheckPage(String tempsAmenage, String eppn, Long id, Long repartitionId, Pageable pageable) {
		Page<TagCheck> tagCheckPage = null;
		if("tiers".equals(tempsAmenage)) {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonEppn(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsTrue(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(id, eppn, repartitionId, pageable);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrueOrderByPersonEppn(id, repartitionId, pageable);
    			}
    		}
    	}else if("notTiers".equals(tempsAmenage)) {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonEppn(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId ==null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsFalse(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(id, eppn, repartitionId, pageable);
    			}
    		}
    		else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalseOrderByPersonEppn(id, repartitionId, pageable);
    			}
    		}
    	}else {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEquals(id, eppn, repartitionId, pageable);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsOrderByPersonEppn(id, repartitionId, pageable);
    			}
    		}
    	}
		return tagCheckPage;
	}
	
	public List<String> getUsersForImport(Long id) {
		List<TagCheck> tcs = tagCheckRepository.findTagCheckByGroupeId(id);
		List<String> listEppn = new ArrayList<String>();
		for(TagCheck tc : tcs) {
			listEppn.add(tc.getPerson().getEppn());
		}
		return listEppn;
	}
}
