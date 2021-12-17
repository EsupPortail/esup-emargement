package org.esupportail.emargement.services;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ParamUtil;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	
	@Resource
	GroupeService groupeService;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	GuestRepository guestRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;

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
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
    @Resource
	DataEmitterService dataEmitterService;
    
    @Resource   
    TagCheckerService tagCheckerService;
    
	@Resource
	LdapService ldapService;
	
	@Resource
	LogService logService;
	
	@Resource
	UserAppService userAppService;
	
	@Autowired
	ParamUtil paramUtil;
	
	@Autowired
    private MessageSource messageSource;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	private static final String ANONYMOUS = "anonymous";

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    public void resetSessionLocationExpected(Long sessionEpreuveId){
    	
    	List<TagCheck> tagCkecks = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);    
    	
    	if(!tagCkecks.isEmpty()) {
    		for (TagCheck tc : tagCkecks) {
    			tc.setTagDate(null);
				tc.setSessionLocationExpected(null);
				tc.setSessionLocationBadged(null);
				tc.setDateEnvoiConvocation(null);
				tc.setTypeEmargement(null);
				tc.setTagChecker(null);
				tc.setNumAnonymat(null);
				tc.setNbBadgeage(null);
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
    
    public Page<TagCheck> getListTagChecksBySessionLocationId(Long id, Pageable pageable,  Long presentId, boolean withUnknown){
    	
    	Page<TagCheck> allTagChecks =  null;
    	if(presentId != null) {
    		allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndIdEquals(id, presentId, pageable);
    	}
    	else {
    		if(withUnknown) {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedIdOrderByPersonEppn(id, id, pageable);
    		}else {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedIdOrderByPersonEppn(id, pageable);
    		}
    	}
		if(!allTagChecks.getContent().isEmpty()) {
			for(TagCheck tc : allTagChecks.getContent()) {
				if(tc.getSessionLocationBadged() != null && tc.getSessionLocationExpected() == null) {
					tc.setIsUnknown(true);
				}else {
					tc.setIsUnknown(false);
				}
				if(tc.getPerson() != null) {
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(tc.getPerson().getEppn());
					if(!userLdaps.isEmpty()) {
						tc.getPerson().setNom(userLdaps.get(0).getUsername());
						tc.getPerson().setPrenom(userLdaps.get(0).getPrenom());
						tc.setNomPrenom(userLdaps.get(0).getUsername().concat(userLdaps.get(0).getPrenom()));
					}else {
						tc.setNomPrenom("");
					}
				}
				if(tc.getTagChecker() != null) {
					List<UserLdap>  userLdaps2 = userLdapRepository.findByEppnEquals(tc.getTagChecker().getUserApp().getEppn());
					if(!userLdaps2.isEmpty()) {
						tc.getTagChecker().getUserApp().setNom(userLdaps2.get(0).getUsername());
						tc.getTagChecker().getUserApp().setPrenom(userLdaps2.get(0).getPrenom());
					}
					if(userLdaps2.isEmpty() && tc.getTagChecker().getUserApp().getEppn().startsWith(paramUtil.getGenericUser())) {
						tc.getTagChecker().getUserApp().setNom(tc.getTagChecker().getUserApp().getContext().getKey());
						tc.getTagChecker().getUserApp().setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
					}
				}
			}
			List<TagCheck> sortedUsers = allTagChecks.stream()
			  .sorted(Comparator.comparing(TagCheck::getNomPrenom))
			  .collect(Collectors.toList());
			
			Page<TagCheck> page = new PageImpl<>(sortedUsers, pageable, sortedUsers.size());
			
			allTagChecks=page;

		}
    	
    	return allTagChecks;
    }
    
    public List<Integer> importTagCheckCsv(Reader reader,  List<List<String>> finalList, Long sessionEpreuveId, String emargementContext, String origine, Boolean checkLdap, Person formPerson, Long sessionLocationId, Guest formGuest) throws Exception {
    	List<List<String>> rows  = new ArrayList<List<String>>();
    	List<Integer> bilanCsv = new ArrayList<Integer>();
    	if(reader!=null) {
    		rows  = importExportService.readAll(reader);
    	}else if(finalList != null) {
    		rows = finalList;
    	}
    	if(rows != null) {	    	
	    	List<TagCheck> tcToSave = new ArrayList<TagCheck>();
	    	List<String> unknowns = new ArrayList<String>();
	    	List<String> missingData = new ArrayList<String>();
	    	//De la forme [NumEtu,CodeGestion]
    		boolean isRepartitionLocationOk = true;
    		if(sessionLocationId != null) {
				if(!checkImportIntoSessionLocations(sessionLocationId, rows.size())) {
					isRepartitionLocationOk = false;
				}
    		}
    		if(isRepartitionLocationOk) {
		    	if(rows.size()>0) {
		    		int i = 0; int j = 0; int k = 0; int l=0;
		    		SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
		    		for(List<String> row : rows) {
			    		if(se != null) {
			    			TagCheck tc = new TagCheck();
			    			Person person = null;
			    			Guest guest = null;
			    			String eppn  = null;
			    			List<UserLdap>  userLdaps = null;
			    			boolean isFromDomain = false;
			    			String line = row.get(0).trim();
			    			line = line.replace("\"","");
			    			Long tcTest = new Long(0);
			    			//Pour Guest
			    			String [] splitLine = null;
			    			if(line.contains(",")) {
			    				splitLine = line.split(",");
			    			}else if(line.contains(";")) {
			    				splitLine = line.split(";");
			    			}
			    			if(!line.startsWith("#")) {
				    			try {
				    				if(line.chars().allMatch(Character::isDigit)){
				    					userLdaps = userLdapRepository.findByNumEtudiantEquals(line);
										if(!userLdaps.isEmpty()) {
											eppn = userLdaps.get(0).getEppn();
											List<Person> existingPersons = personRepository.findByEppn(eppn);
											if(!existingPersons.isEmpty()) {
												person = existingPersons.get(0);
											}else {
												person = new Person();
												person.setNumIdentifiant(line);
												person.setType("student");
												person.setContext(contexteService.getcurrentContext());
												person.setEppn(eppn);
												person.setType("student");
												personRepository.save(person);
											}
										}
										tc.setCodeEtape(origine);
				    				}else {
				    					userLdaps = userLdapRepository.findByEppnEquals(line);
				    					if(!userLdaps.isEmpty() || !checkLdap) {
				    						eppn =(!userLdaps.isEmpty())? userLdaps.get(0).getEppn() : line;
				    						List<Person> existingPersons = personRepository.findByEppn(eppn);
				    						if(!existingPersons.isEmpty()) {
												person = existingPersons.get(0);
											}else {
												person = new Person();
												if(checkLdap) {
						    						if(userLdaps.get(0).getNumEtudiant()!=null && !userLdaps.get(0).getNumEtudiant().isEmpty()) {
														person.setNumIdentifiant(userLdaps.get(0).getNumEtudiant());
														person.setType("student");
						    						}
						    						else {
						    							person.setType("staff");
													}
												}else {
													person.setType("unknown");
												}
		
					    						person.setContext(contexteService.getcurrentContext());
					    						person.setEppn(eppn);
					    						personRepository.save(person);
											}
										}else if(line.contains("@".concat(nomDomaine))){
											isFromDomain = true;
										}
										else {
											List<Guest> existingGuests = new ArrayList<Guest>();
											if(splitLine != null && splitLine.length>1) {
												existingGuests = guestRepository.findByEmail(splitLine[0]);
												if(!existingGuests.isEmpty()) {
													guest = existingGuests.get(0);
												}else {
													guest = new Guest();
													guest.setEmail(splitLine[0]);
													guest.setNom(splitLine[1]);
													guest.setPrenom(splitLine[2]);
													guest.setContext(contexteService.getcurrentContext());
													guestRepository.save(guest);
												}
											}
										}
				    				}
					    			tc.setSessionEpreuve(se);
					    			tc.setContext(contexteService.getcurrentContext());
					    			tc.setPerson(person);
					    			tc.setGuest(guest);
					    			if(sessionLocationId != null) {
					    				if(checkImportIntoSessionLocations(sessionLocationId, rows.size())) {
					    					SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
					    					tc.setSessionLocationExpected(sl);
					    				}
					    			}
					    			if(se.getIsSessionLibre()) {
					    				SessionLocation slLibre = null;
			    						if(tc.getSessionLocationExpected()!=null) {
			    							slLibre = tc.getSessionLocationExpected();
			    						}else {
			    							List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuveId(se.getId());
			    							if(!sls.isEmpty()) {
			    								slLibre = sls.get(0);
			    							}
			    						}
		    							Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		    							TagChecker tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(ldapService.getEppn(auth.getName()), null).getContent().get(0); 
		    							tc.setTypeEmargement(TypeEmargement.MANUAL);
		    							tc.setTagDate(new Date());
		    							tc.setTagChecker(tagChecker);
	    								tc.setSessionLocationBadged(slLibre);
	    								tc.setSessionLocationExpected(slLibre);
					    			}
					    			if(!userLdaps.isEmpty() || !checkLdap && eppn!=null) {
					    				tcTest = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionEpreuveId, eppn);
					    			}else if(!isFromDomain && splitLine!=null) {
					    				tcTest = tagCheckRepository.countTagCheckBySessionEpreuveIdAndGuestEmailEquals(sessionEpreuveId, splitLine[0]);
					    			}

								} catch (Exception e) {
									tcTest = new Long(-1);
									log.error("Erreur sur le login ou numéro identifiant "  + line + " lors de la recherche LDAP", e);
								}
					    		if(!userLdaps.isEmpty() && tcTest == 0 || userLdaps.isEmpty() && tcTest == 0 && guest!=null || !checkLdap && tcTest == 0 && eppn!=null ){
				    				tcToSave.add(tc);
				    				i++;
				    			}else if(tcTest >0){
				    				tc.setComment("Personne déjà inscrite dans la session");
				    				j++;
				    			}else if(isFromDomain) {
				    				tc.setComment("Personne inconnue dans l'annuaire Ldap");
				    				unknowns.add(line);
				    				k++;
				    			}	
				    			else{
				    				tc.setComment("Données manquantes");
				    				missingData.add(line);
				    				l++;
				    			}
			    			}
			    		}
		    		}
		    		tagCheckRepository.saveAll(tcToSave);
		    		String inconnus = "";
		    		String incomplet = "";
		    		if(!unknowns.isEmpty()) {
		    			inconnus = " (Identifiants : "  + StringUtils.join(unknowns, ",") + ")";
		    		}
		    		if(!missingData.isEmpty()) {
		    			incomplet = " (Identifiants : "  + StringUtils.join(missingData, ",") + ")";
		    		}
		            log.info("import inscriptions : Succès : " + i + " - Déjà inscrits : " + j + " Inconnus : " +  k + inconnus + " Données manquantes : " +  l + incomplet);
		            logService.log(ACTION.IMPORT_INSCRIPTION, RETCODE.SUCCESS, 
		            				"import inscriptions " + se.getNomSessionEpreuve() + " : Succès : " + i + " - Déjà inscrits : " + j + " - Inconnus : " +  k + inconnus + " Données manquantes : " +  l + incomplet, null,
		            				null, emargementContext, null);
		            bilanCsv.add(i);
		            bilanCsv.add(j);
		            bilanCsv.add(k);
		            bilanCsv.add(l);
		    	}
    		}
    	}
    	return bilanCsv;
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
        if(tc.getPerson() != null) {
	        if(!tc.getPerson().getNumIdentifiant().isEmpty()){
	        	 strings.add(tc.getPerson().getNumIdentifiant());
	        }else if(!tc.getPerson().getEppn().isEmpty()){
	        	 strings.add(tc.getPerson().getEppn());
	        }
        }else if(tc.getGuest() != null) {
        	strings.add(tc.getGuest().getEmail().concat(";").concat(tc.getGuest().getNom()).concat(";").concat(tc.getGuest().getPrenom()));
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
				Long count = null;
				Long countGroupe = null;
				Person person = tc.getPerson();
				Guest guest = tc.getGuest();
				if(person != null) {
					count = tagCheckRepository.countTagCheckByPerson(person);
					List<Person> persons = new ArrayList<Person>();
					persons.add(person);
					countGroupe = groupeRepository.countByPersonsIn(persons);
					if(count==0 && countGroupe==0) {
		    			personRepository.delete(person);
		    		}
				}else if(guest != null) {
					count = tagCheckRepository.countTagCheckByGuest(guest);
					List<Guest> guests = new ArrayList<Guest>();
					guests.add(guest);
					countGroupe = groupeRepository.countByGuestsIn(guests);
					if(count==0 && countGroupe==0){
		    			guestRepository.delete(guest);
		    		}
				}
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
						tc.setNomPrenom(userLdaps.get(0).getUsername().concat(userLdaps.get(0).getPrenom()));
					}else {
						tc.setNomPrenom("");
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
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), toolUtil.getDureeEpreuve(tc.getSessionEpreuve()));
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
				List<TagCheck> listTc = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullOrderByPersonEppn(seId);
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
							emailService.sendMessageWithAttachment(appliConfigService.getNoReplyAdress(), email, subject, bodyMsg, filePath, "convocation.pdf", ccArray, null);
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
					String from = appliConfigService.getNoReplyAdress();
					emailService.sendSimpleMessage(from, ccArray[0], "Erreurs d'envoi de convocations", bodyMsg, ccArray);
				}
			}
		}
	}
	
	public boolean tagAction(String eppn, EsupNfcTagLog esupNfcTagLog, String action) throws ParseException {
		
		boolean isOk = false;
		boolean isSessionLibre = false;
		log.info("tagaction pour l'eppn : " + eppn);
		String locationNom = esupNfcTagLog.getLocation();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		String[] splitLocationNom = locationNom.split(" // "); 
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findByNomSessionEpreuve(splitLocationNom[0], null).getContent().get(0);
		Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
		if("isTagable".equals(action)) {
			log.info("isTagable pour l'eppn : " + eppn);
			boolean isUnknown = false;
			Long tc =  null;
			if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
				tc = tagCheckRepository.checkIsTagable(splitLocationNom[1], eppn,  date, splitLocationNom[0]);
			}else {
				tc = tagCheckRepository.checkIsTagableWithDateFin(splitLocationNom[1], eppn, date, dateFin, splitLocationNom[0]);
			}
			SessionLocation sessionLocationBadged =  null;
			TagChecker tagChecker = null;
			Context ctx = sessionEpreuve.getContext();
			if(sessionEpreuve.getIsSessionLibre()) {
				isSessionLibre = true;
				sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), splitLocationNom[1]);
				tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(esupNfcTagLog.getEppnInit(), null).getContent().get(0);
			}
			
			if (tc ==1 || isSessionLibre) {
				if(isSessionLibre) {
					try {
						Long totalPresent = tagCheckRepository.countTagCheckBySessionLocationExpected (sessionLocationBadged);
						if(totalPresent >= sessionLocationBadged.getCapacite()) {
							TagCheck tagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationBadged.getId(), eppn);
							if(tagCheck != null) {
								saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, splitLocationNom[0]);
								isOk = true;
							}else {
								isOk = false;
							}
						}else {
							Groupe gpe = sessionEpreuve.getBlackListGroupe();
							boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);
							String msgError = "";
							if(!isBlackListed) {
								saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, splitLocationNom[0]);
								if(BooleanUtils.isTrue(sessionEpreuve.getIsSaveInExcluded())) {
									List <Long> idsGpe = new ArrayList<Long>();
									idsGpe.add(gpe.getId());
									groupeService.addMember(eppn,idsGpe);
								}
							}else {
								msgError = eppn;
							}
							isOk = true;
							dataEmitterService.sendData(new TagCheck(), new Float(0), new Long(0), 1, new SessionLocation(), msgError);
						}
					} catch (Exception e) {
						log.error("Session libre, problème de carte pour l'eppn : "  + eppn, e);
					}
				}else{
					isOk = true;
				}
			}else {
				try {
					String comment = "";
					String eppnInit = esupNfcTagLog.getEppnInit();
					tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppnInit, null).getContent().get(0);
					sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), splitLocationNom[1]);
					//on regarde si la personne est dans une autre salle de la session
					Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date, splitLocationNom[0]);
					Long countSe = sessionEpreuveRepository.countSessionEpreuveIdExpected(eppn, date);
					SessionLocation sl = null;
					Long seId = null;
					if(sessionLocationId != null) {
						if(sessionEpreuve.getTypeBadgeage().equals(TypeBadgeage.SALLE)) {
							sl = sessionLocationRepository.findById(sessionLocationId).get();
							comment = "Inconnu dans cette salle, Salle attendue : " +  sl.getLocation().getNom();
						}else {
							isOk = true;
						}
					}
					else if (sessionLocationId == null && countSe>0) {//On regarde si il est est dans une autre session aujourd'hui
						LocalTime now = LocalTime.now();
						seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, date, now); 
						String lieu = " Autre session dans la journée";
						if(seId != null) {
							SessionEpreuve otherSe = sessionEpreuveRepository.findById(seId).get();
							lieu = " --> Session : "+ otherSe.getNomSessionEpreuve();
							List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(otherSe.getId(), eppn, null).getContent();
							if(!list.isEmpty()) {
								if(list.get(0).getSessionLocationExpected() !=null) {
									String location = list.get(0).getSessionLocationExpected() .getLocation().getNom();
									lieu = lieu.concat(" , lieu : ").concat(location);
								}
							}
							comment = "Inconnu dans cette session " + lieu;
						}else {
							comment = "Inconnu dans cette session, Attendu dans une autre session aujourd'hui";
							isUnknown = true;
						}
					}else { //Il est vraiment inconnu!!!
						comment = "Inconnu";
						isUnknown = true;
					}
					log.info("On enregistre l'inconnu dans la session : " +  eppn);
					if(sl != null || seId != null || isUnknown) {
						saveUnknownTagCheck(comment, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker, isSessionLibre, null);
						dataEmitterService.sendData(new TagCheck(), new Float(0), new Long(0), 1, new SessionLocation(), "");
					}
				} catch (Exception e) {
					log.error("Problème de carte pour l'eppn : "  + eppn, e);
				}
			}
		}else if("validateTag".equals(action)) {
			log.info("validateTag pour l'eppn : " + eppn);
			Long realSlId = null;
			log.info("Eppn : " + eppn + " Date " + date);
			Long sessionLocationId =  null;
			if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
				sessionLocationId = tagCheckRepository.getSessionLocationId(splitLocationNom[1], eppn, date, splitLocationNom[0]);
			}else {
				sessionLocationId = tagCheckRepository.getSessionLocationIdWithDateFin(splitLocationNom[1], eppn, date, dateFin, splitLocationNom[0]);
			}
			log.info("SessionLocationId : " + sessionLocationId);
			TagCheck presentTagCheck = null;
			if(sessionLocationId!=null) {
				realSlId = sessionLocationId;
				presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, eppn);
			}else {
				Long slId = tagCheckRepository.getSessionLocationId(splitLocationNom[1], date, splitLocationNom[0]);
				realSlId = slId;
				if(slId!=null) {
					SessionLocation sl = sessionLocationRepository.findById(slId).get();
					presentTagCheck = tagCheckRepository.findTagCheckBySessionEpreuveIdAndEppn(sl.getSessionEpreuve().getId(), esupNfcTagLog.getEppn());
				}
			}
			
			if(presentTagCheck!=null) {
		    	presentTagCheck.setTagDate(new Date());
		    	presentTagCheck.setTypeEmargement(TypeEmargement.CARD);
		    	SessionLocation sl = sessionLocationRepository.findById(realSlId).get();
				TagChecker tagChecker = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(sl, esupNfcTagLog.getEppnInit());
		    	presentTagCheck.setTagChecker(tagChecker);
		    	presentTagCheck.setSessionLocationBadged(sl);
		    	presentTagCheck.setNbBadgeage(this.getNbBadgeage(presentTagCheck, true));
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
		    	dataEmitterService.sendData(presentTagCheck, percent, totalPresent, 0, sl, "");
			}
		}
		return isOk;
	}
	
	public void saveUnknownTagCheck(String comment, Context ctx, String eppn, SessionEpreuve sessionEpreuve, SessionLocation sessionLocationBadged, 
				TagChecker tagChecker, boolean isSessionLibre, String locationNom) {
		TagCheck unknownTc = null;
		List<TagCheck> badgedTcs = new ArrayList<TagCheck>();
		if(isSessionLibre) {
			Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, sessionEpreuve.getDateExamen(), locationNom);
			if(sessionLocationId != null) {
				badgedTcs = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn);
			}
		}else {
			badgedTcs = tagCheckRepository.findTagCheckBySessionLocationBadgedIdAndPersonEppnEquals(sessionLocationBadged.getId(), eppn);
		}
		if(!badgedTcs.isEmpty()) {
			unknownTc = badgedTcs.get(0);
			unknownTc.setTagDate(new Date());
		}else {
			unknownTc = new TagCheck();
			unknownTc.setComment(comment);
			unknownTc.setSessionEpreuve(sessionEpreuve);
			unknownTc.setContext(ctx);
			Person p = personRepository.findByEppnAndContext(eppn, ctx);
			if(p == null) {
				p = new Person();
				p.setContext(ctx);
				p.setEppn(eppn);
				personRepository.save(p);
			}
			List<UserLdap> users = userLdapRepository.findByEppnEquals(eppn);
			if(!users.isEmpty()) {
				p.setType((users.get(0).getNumEtudiant()!=null)? "student" : "staff");
				p.setNumIdentifiant(users.get(0).getNumEtudiant());
			}
			if(sessionEpreuve.getIsSessionLibre()) {
				unknownTc.setSessionLocationExpected(sessionLocationBadged);
			}
			unknownTc.setPerson(p);
			unknownTc.setTagChecker(tagChecker);
			unknownTc.setSessionLocationBadged(sessionLocationBadged);
			unknownTc.setTagDate(new Date());
			unknownTc.setTypeEmargement(TypeEmargement.CARD);
		}
		tagCheckRepository.save(unknownTc);	
	}
	
	public void save(TagCheck tagCheck, String emargementContext) {
        TagCheck tc = tagCheckRepository.findById(tagCheck.getId()).get();
        tc.setIsTiersTemps(tagCheck.getIsTiersTemps());
        tc.setComment(tagCheck.getComment());
        tagCheckRepository.save(tc);
        String identifiant = (tc.getPerson() != null ) ?  tc.getPerson().getEppn() : tc.getGuest().getEmail();
        log.info("maj inscrit ok : " + identifiant);
        logService.log(ACTION.UPDATE_INSCRIPTION, RETCODE.SUCCESS, "maj inscrit : " + identifiant, null,
				null, emargementContext, null);
	}
	
	public void exportTagChecks(String type, Long id, String tempsAmenage, HttpServletResponse response, String emargementContext, String anneeUniv) {
		List<TagCheck> list = null;
        Long count = new Long(0);
        Long presentTotal = new Long(0);
        String nomFichier = "export";
        String fin = "";
		if(anneeUniv != null) {
			list = tagCheckRepository.findTagCheckBySessionEpreuveAnneeUniv(anneeUniv);
			nomFichier = "Export_inscrits_annee_universitaire_" + anneeUniv;
		}else {
			SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
			Date dateFin = se.getDateFin();
	    	fin = (dateFin != null)? "_" + String.format("%1$td-%1$tm-%1$tY", dateFin) : "" ;
			nomFichier = se.getNomSessionEpreuve().concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen())).concat(fin);
	   		list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(id, null).getContent();
	   		nomFichier = nomFichier.replace(" ", "_");
			count = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(id);
			presentTotal = tagCheckRepository.countTagCheckBySessionEpreuveIdAndTagDateIsNotNullAndSessionLocationExpectedLocationIdIsNotNull(id);
		}
		

		this.setNomPrenomTagChecks(list);
		if ("PDF".equals(type)) {
			int nnColumns = 0;
			if(anneeUniv != null) {
				nnColumns = 13;
			}else {
				nnColumns = 11 ;
			}
			
		   	PdfPTable table = new PdfPTable(nnColumns);
	    	
	    	table.setWidthPercentage(100);
	    	table.setHorizontalAlignment(Element.ALIGN_CENTER);

	        //On créer l'objet cellule.
	    	String libelleSe = null;
	    	if(anneeUniv != null) {
	    		libelleSe = "Année universitaire " + anneeUniv;
	    	}else {
		    	TagCheck tch = list.get(0);
		    	libelleSe = tch.getSessionEpreuve().getNomSessionEpreuve().concat(" -- ").
		    			concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionEpreuve().getDateExamen()))).concat(fin)
		    			.concat(" --  nb de présents :  ").concat(presentTotal.toString()).concat("/").concat(count.toString());
	    	}
	        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
	        cell.setBackgroundColor(BaseColor.GREEN);
	        cell.setColspan(nnColumns);
	        table.addCell(cell);
	        PdfPCell header0 = null;
	        PdfPCell header00 = null;
	        //contenu du tableau.
	        if(anneeUniv != null) {
	        	header0 = new PdfPCell(new Phrase("Session")); header0.setBackgroundColor(BaseColor.GRAY);
	        	header00 = new PdfPCell(new Phrase("Date")); header00.setBackgroundColor(BaseColor.GRAY);
	        }
	        PdfPCell header1 = new PdfPCell(new Phrase("N° Identifiant")); header1.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header2 = new PdfPCell(new Phrase("Eppn")); header2.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header3 = new PdfPCell(new Phrase("Nom")); header3.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header4 = new PdfPCell(new Phrase("Prénom")); header4.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header5 = new PdfPCell(new Phrase("Présent")); header5.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header6 = new PdfPCell(new Phrase("Emargement")); header6.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header7 = new PdfPCell(new Phrase("Type")); header7.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header8 = new PdfPCell(new Phrase("Lieu attendu")); header8.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header9 = new PdfPCell(new Phrase("Lieu badgé")); header9.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header10 = new PdfPCell(new Phrase("Exempt")); header10.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header11 = new PdfPCell(new Phrase("Temps aménagé")); header11.setBackgroundColor(BaseColor.GRAY);
	        
	        if(anneeUniv != null) {
		        table.addCell(header0);
		        table.addCell(header00);
	        }
	        table.addCell(header1);
	        table.addCell(header2);
	        table.addCell(header3);
	        table.addCell(header4);
	        table.addCell(header5);
	        table.addCell(header6);
	        table.addCell(header7);
	        table.addCell(header8);
	        table.addCell(header9);
	        table.addCell(header10);
	        table.addCell(header11);
	        
	        if(!list.isEmpty()) {
	        	for(TagCheck tc : list) {
	        		String presence = "Absent";
	        		String date  = "--";
	        		String attendu  = "--";
	        		String badged  = "--";
	        		String tiersTemps  = "--";
	        		String dateSessionEpreuve = "";
	        		String dateFin = "";
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String numIdentifiant = "";
	        		String typeEmargement = "";
	        		String isExempt = (BooleanUtils.isTrue(tc.getIsExempt()))? "Exempt" : "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getEppn();
	        			numIdentifiant = tc.getPerson().getNumIdentifiant();
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        		}
	        		if(tc.getTypeEmargement()!=null) {
	        			typeEmargement = messageSource.getMessage("typeEmargement.".concat( tc.getTypeEmargement().name().toLowerCase()), null, null);
	        		}
	        		BaseColor b = new BaseColor(232, 97, 97, 50);
	        		PdfPCell dateCell = null;
	        		if(tc.getTagDate() != null) {
	        			presence = "Présent";
	        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
	        			b = new BaseColor(19, 232, 148, 50);
	        		}
	        		if(tc.getSessionLocationExpected()!=null) {
	        			attendu = tc.getSessionLocationExpected().getLocation().getNom();
	        		}	        		
	        		if(tc.getSessionLocationBadged()!=null) {
	        			badged = tc.getSessionLocationBadged().getLocation().getNom();
	        		}
	        		if(tc.getIsTiersTemps()) {
	        			tiersTemps = "Oui";
	        		}
	        		if(anneeUniv != null) {
	        			if(tc.getSessionEpreuve() != null) {
	    	                dateCell = new PdfPCell(new Paragraph(tc.getSessionEpreuve().getNomSessionEpreuve()));
	    	                dateCell.setBackgroundColor(b);
	    	                table.addCell(dateCell);
	        				dateSessionEpreuve = String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateExamen());
	        				dateFin = (tc.getSessionEpreuve().getDateFin() != null)? " / " + String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateFin()) : "";
	        				dateCell = new PdfPCell(new Paragraph(dateSessionEpreuve + dateFin));
	    	        		dateCell.setBackgroundColor(b);
	    	                table.addCell(dateCell);
	        			}
	        		}
	        		dateCell = new PdfPCell(new Paragraph(numIdentifiant));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(identifiant));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);	                
	        		dateCell = new PdfPCell(new Paragraph(nom));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(prenom));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(presence));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(date));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(new Paragraph(typeEmargement)));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(attendu));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);	                
	        		dateCell = new PdfPCell(new Paragraph(badged));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(isExempt));
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
	          response.setHeader("Content-Disposition","attachment; filename=".concat(nomFichier));
	          PdfWriter.getInstance(document,  response.getOutputStream());
	         
	          document.open();

	          document.add(table);
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
	       
	        document.close();
			
			
		}else if("CSV".equals(type)){
			try {
				
				String filename =  nomFichier.concat(".csv");

				response.setContentType("text/csv");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				        "attachment; filename=\"" + filename + "\"");

				//create a csv writer
				CSVWriter writer = new CSVWriter(response.getWriter()); 
				String [] headers = {"Session", "Date", "Numéro Etu", "Eppn", "Nom", "Prénom", "Présent", "Emargement", "Type", "Lieu attendu", "Lieu badgé", "Exempt", "Tiers-temps"};
				writer.writeNext(headers);
				for(TagCheck tc : list) {
					List <String> line = new ArrayList<String>();
					String dateSession = String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateExamen());
					String dateFin = (tc.getSessionEpreuve().getDateFin() != null)? " / " + String.format("%1$td-%1$tm-%1$tY", tc.getSessionEpreuve().getDateFin()) : "";
	        		String presence = "Absent";
	        		String date  = "--";
	        		String badged  = "--";
	        		String attendu  = "--";
	        		String tiersTemps  = "--";
	        		String typeEmargement = "--";
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String numIdentifiant = "";
	        		String isExempt = (BooleanUtils.isTrue(tc.getIsExempt()))? "Exempt" : "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getEppn();
	        			numIdentifiant = tc.getPerson().getNumIdentifiant();
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        		}
	        		if(tc.getTagDate() != null) {
	        			presence = "Présent";
	        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
	        		}
	        		if(tc.getSessionLocationExpected()!=null) {
	        			attendu = tc.getSessionLocationExpected().getLocation().getNom();
	        		}		        		
	        		if(tc.getSessionLocationBadged()!=null) {
	        			badged = tc.getSessionLocationBadged().getLocation().getNom();
	        		}
	        		if(tc.getIsTiersTemps()) {
	        			tiersTemps = "Oui";
	        		}
	        		if(tc.getTypeEmargement()!=null) {
	        			typeEmargement = messageSource.getMessage("typeEmargement.".concat( tc.getTypeEmargement().name().toLowerCase()), null, null);
	        		}
					line.add(tc.getSessionEpreuve().getNomSessionEpreuve());
					line.add(dateSession + dateFin);
					line.add(numIdentifiant);
					line.add(identifiant);
					line.add(nom);
					line.add(prenom);
					line.add(presence);
					line.add(date);
					line.add(typeEmargement);
					line.add(attendu);
					line.add(badged);
					line.add(isExempt);
					line.add(tiersTemps);
					writer.writeNext(line.toArray(new String[1]));
				}
		        // closing writer connection 
		        writer.close(); 
				log.info("Extraction csv :  " + list.size() + "résultats" );
				logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction csv :" +  list.size() + " résultats" , null,
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
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findByNomSessionEpreuve(splitLocationNom[0], null).getContent().get(0);
		Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
		if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
			key = tagCheckRepository.getContextId(splitLocationNom[1], eppn, date, splitLocationNom[0]);
		}else {
			key = tagCheckRepository.getContextIdWithDateFin(splitLocationNom[1], eppn, date, dateFin, splitLocationNom[0]);
		}
		
		if(key == null) {
			Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date, splitLocationNom[0]);
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
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsTrue(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(id, eppn, repartitionId, pageable);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(id, repartitionId, pageable);
    			}
    		}
    	}else if("notTiers".equals(tempsAmenage)) {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId ==null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsFalse(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(id, eppn, repartitionId, pageable);
    			}
    		}
    		else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(id, repartitionId, pageable);
    			}
    		}
    	}else {
    		if(eppn.isEmpty() && repartitionId == null) {
	    		tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveId(id, pageable);
    		}else if(!eppn.isEmpty()) {
    			if(repartitionId == null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(id, eppn, pageable);
    			}else {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEquals(id, eppn, repartitionId, pageable);
    			}
    		}else if(eppn.isEmpty()) {
    			if(repartitionId != null) {
    				tagCheckPage = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEquals(id, repartitionId, pageable);
    			}
    		}
    	}
		return tagCheckPage;
	}
	
	public List<String> findDistinctCodeEtapeSessionEpreuve(Long id) {
		List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(id);
		List<String> codesEtape = new ArrayList<>();
		if(!tcs.isEmpty()) {
			codesEtape  = tcs.stream() .filter(l ->l.getCodeEtape() != null && !l.getCodeEtape().equals("")).map(l -> l.getCodeEtape()).distinct().collect(Collectors.toList());
		}
		return codesEtape;
	}
	
	public boolean checkImportIntoSessionLocations(Long slId, int sizeList) {
		boolean isOk = false;
		SessionLocation sl = sessionLocationRepository.findById(slId).get();
		int capacite = sl.getCapacite();
		Long countUsedLocation = tagCheckRepository.countBySessionLocationExpectedId(slId);
		int freePlaces = capacite - Integer.valueOf(String.valueOf(countUsedLocation));
		if(sizeList <= freePlaces) {
			isOk = true;
		}
		return isOk;
	}
	
	@Transactional
	public void archiverTagChecks(String anneeUniv, String emargementContext) {
		List<TagCheck> list  = tagCheckRepository.findTagCheckBySessionEpreuveAnneeUniv(anneeUniv);
		Context ctx = contextRepository.findByContextKey(emargementContext);
		String anonymousEppn = ANONYMOUS.concat("_").concat(emargementContext).concat("@").concat(nomDomaine);
		if (!list.isEmpty()) {
			List<Person> persons = personRepository.findByEppn(anonymousEppn);
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String loginArchivage = ldapService.getEppn(auth.getName());
			Person  p = null;
			if(!persons.isEmpty()) {
				p = persons.get(0);
			}else {
				p = new Person();
				p.setEppn(anonymousEppn);
				p.setContext(ctx);
				personRepository.save(p);
			}
			for(TagCheck tc : list) {
				tc.setPerson(p);
				tc.getSessionEpreuve().setDateArchivage(new Date());
				tc.getSessionEpreuve().setLoginArchivage(loginArchivage);
				tc.getSessionEpreuve().setIsSessionEpreuveClosed(true);
				tagCheckRepository.save(tc);
			}
		}
		log.info("Archivage effectuée pour l'année universitaire " + anneeUniv);
		logService.log(ACTION.ARCHIVE_SESSIONS, RETCODE.SUCCESS, "Année universitaire: " + anneeUniv + " Nombre " + list.size(), null, null, emargementContext, null);
		//Nettoyage
		int clean = personRepository.cleanPersons(ctx.getId());
		if(clean >0) {
			log.info("Nettoyage après archivage : " + clean);
			logService.log(ACTION.CLEAN_PERSONS, RETCODE.SUCCESS, " Nombre " + clean, null, null, emargementContext, null);

		}
	}
	
	public int getNbBadgeage(TagCheck tc, boolean isPresent) {
		int previousNb = (tc.getNbBadgeage() == null)? 0 : tc.getNbBadgeage();
		int nbBadgeage = 0;
		if(isPresent) {
			nbBadgeage = previousNb + 1;
		}else {
			if(previousNb > 0) {
				nbBadgeage = previousNb - 1;
			}
		}
		return nbBadgeage;
	}
}
