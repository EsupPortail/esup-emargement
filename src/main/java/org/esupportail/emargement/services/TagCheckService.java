package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AssiduiteBean;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagCheckBean;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StatutSessionRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.opencsv.CSVWriter;

@Service
public class TagCheckService {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
    LdapUserRepository ldapUserRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	StatutSessionRepository statutSessionRepository;
	
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
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
    @Resource
	DataEmitterService dataEmitterService;
    
    @Resource   
    TagCheckerService tagCheckerService;
    
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;

	@Resource
	PersonService personService;  
	
	@Autowired
	ParamUtil paramUtil;
	
	@Autowired
    private MessageSource messageSource;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	EsupSignatureRepository esupSignatureRepository;

	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	private static final String ANONYMOUS = "anonymous";

	public static final String COL_EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS = "EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS"; // ou raison d'absence + eventuellement info temps amenagé
	public static final String COL_EMARGEMENT_MODE = "EMARGEMENT_MODE";
	public static final String COL_EMARGEMENT_MODE_OU_ABSENCE = "EMARGEMENT_MODE_OU_ABSENCE";
	public static final String COL_LIGNE_NUM = "LIGNE_NUM";
	public static final String COL_PARTICIPANT_GROUPES = "PARTICIPANT_GROUPES";
	public static final String COL_PARTICIPANT_IDENTIFIANT = "PARTICIPANT_IDENTIFIANT"; // n° étudiant ou email
	public static final String COL_PARTICIPANT_NOM = "PARTICIPANT_NOM";
	public static final String COL_PARTICIPANT_PRENOM = "PARTICIPANT_PRENOM";
	public static final String COL_PARTICIPANT_TYPE = "PARTICIPANT_TYPE"; // E, P, Ext
	public static final String COL_SESSION_DATE_HEURE_DEBUT = "SESSION_DATE_HEURE_DEBUT";
	public static final String COL_SESSION_DUREE = "SESSION_DUREE";
	public static final String COL_SESSION_NOM = "SESSION_NOM";
	public static final String COL_SESSION_SURVEILLANTS = "SESSION_SURVEILLANTS";
	public static final String COL_SESSION_RATIO_PRESENTS = "SESSION_RATIO_PRESENTS";
	public static final String COL_SESSION_DATES_DEBUT_FIN = "SESSION_DATES_DEBUT_FIN";
	public static final String COL_SESSION_HEURES_DEBUT_FIN = "SESSION_HEURES_DEBUT_FIN";

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    public void resetSessionLocationExpected(Long sessionEpreuveId){
    	
    	List<TagCheck> tagCkecks = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
    	List<TagChecker> tagCkeckers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(sessionEpreuveId);    
    	
    	if(!tagCkecks.isEmpty()) {
    		for (TagCheck tc : tagCkecks) {
    			tc.setTagDate(null);
    			tc.setTagDate2(null);
				tc.setSessionLocationExpected(null);
				tc.setSessionLocationBadged(null);
				tc.setDateEnvoiConvocation(null);
				tc.setTypeEmargement(null);
				tc.setTypeEmargement2(null);
				tc.setTagChecker(null);
				tc.setTagChecker2(null);
				tc.setNumAnonymat(null);
				tc.setNbBadgeage(null);
				tc.setAbsence(null);
				tagCheckRepository.save(tc);
    		}
    	}
    	if(!tagCkeckers.isEmpty()) {
    		for (TagChecker tc : tagCkeckers) {
    			tc.setTagDate(null);
    			tc.setTagDate2(null);
    			tc.setTagValidator(null);
    			tc.setTagValidator2(null);
    			tc.setTypeEmargement(null);
    			tc.setTypeEmargement2(null);
    		}
    	}
    }
    
    public Long countNbTagCheckRepartitionNull(Long sessionEpreuveId, boolean isTiersTemps) {
    	if(isTiersTemps) {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(sessionEpreuveId);
    	}
    	return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(sessionEpreuveId);
    }
    
    public Long countNbTagCheckRepartitionNotNull(Long sessionEpreuveId,  boolean isTiersTemps) {
    	if(isTiersTemps) {
    		return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrue(sessionEpreuveId);
    	}
    	return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalse(sessionEpreuveId);
    }
    
    public Page<TagCheck> getListTagChecksBySessionLocationId(Long id, Pageable pageable,  Long presentId, boolean withUnknown){
    	
    	Page<TagCheck> allTagChecks =  null;
    	if(presentId != null) {
    		allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndIdEquals(id, presentId, pageable);
    	}
    	else {
    		if(withUnknown) {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(id, id, pageable);
    		}else {
    			allTagChecks =  tagCheckRepository.findTagCheckBySessionLocationExpectedId(id, pageable);
    		}
    	}
		List<String> tcList = allTagChecks.stream().filter(tagCheck -> tagCheck.getPerson()!=null).map(tagCheck -> tagCheck.getPerson().getEppn())
				.collect(Collectors.toList());
		Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");

		if(!allTagChecks.getContent().isEmpty()) {
			List<String> tagCheckerList = allTagChecks.stream().filter(tagCheck -> tagCheck.getTagChecker()!=null).map(tagCheck -> tagCheck.getTagChecker().getUserApp().getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers2 = ldapService.getLdapUsersFromNumList(tagCheckerList, "eduPersonPrincipalName");
			
			for(TagCheck tc : allTagChecks.getContent()) {
				if(tc.getSessionLocationBadged() != null && tc.getSessionLocationExpected() == null) {
					tc.setIsUnknown(true);
				}else {
					tc.setIsUnknown(false);
				}
				if(tc.getPerson() != null) {
					if(!mapLdapUsers.isEmpty()) {
						LdapUser ldapUser = mapLdapUsers.get(tc.getPerson().getEppn());
						if(ldapUser!=null) {
							tc.getPerson().setNom(ldapUser.getName());
							tc.getPerson().setPrenom(ldapUser.getPrenom());
							tc.setNomPrenom(ldapUser.getName().concat(ldapUser.getPrenom()));
						}
					}else {
						tc.setNomPrenom("");
					}
				}
				if(tc.getTagChecker() != null) {
					String eppn = tc.getTagChecker().getUserApp().getEppn();
					if(!mapLdapUsers2.isEmpty()) {
						LdapUser ldapUser2 = mapLdapUsers2.get(eppn);
						if(ldapUser2!=null) {
							tc.getTagChecker().getUserApp().setNom(mapLdapUsers2.get(eppn).getName());
							tc.getTagChecker().getUserApp().setPrenom(mapLdapUsers2.get(eppn).getPrenom());
						}
					}
					if(mapLdapUsers2.isEmpty() && tc.getTagChecker().getUserApp().getEppn().startsWith(paramUtil.getGenericUser())) {
						tc.getTagChecker().getUserApp().setNom(tc.getTagChecker().getUserApp().getContext().getKey());
						tc.getTagChecker().getUserApp().setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
					}
				}
			}

		}
    	
    	return allTagChecks;
    }
    
    public Map<String, List<Absence>> getMapAbsences(Date dateDebut, Date dateFin){
		Map<String, List<Absence>> mapEtp = absenceRepository.findAbsencesWithinDateRange(dateDebut, dateFin!=null? dateFin : dateDebut).stream()
				.collect(Collectors.groupingBy(abs -> abs.getPerson().getEppn()));
		
		return mapEtp;
    }
    
    public List<Integer> importTagCheckCsv(Reader reader,  List<List<String>> finalList, Long sessionEpreuveId, 
    		String emargementContext, Map<String,String> mapEtapes, Boolean checkLdap, Long sessionLocationId, TagCheck tcToImport) throws Exception {
    	List<List<String>> rows  = new ArrayList<>();
    	List<Integer> bilanCsv = new ArrayList<>();
    	if(reader!=null) {
    		rows  = importExportService.readAll(reader);
    	}else if(finalList != null) {
    		rows = finalList;
    	}
    	if(rows != null) {
	    	List<TagCheck> tcToSave = new ArrayList<>();
	    	List<String> unknowns = new ArrayList<>();
	    	List<String> missingData = new ArrayList<>();
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
			    			List<LdapUser> ldapUsers = null;
			    			boolean isFromDomain = false;
			    			String line = row.get(0).trim();
			    			line = line.replace("\"","");
			    			Long tcTest = 0L;
			    			//Pour Guest
			    			String [] splitLine = null;
			    			if(line.contains(";")) {
			    				splitLine = line.split(";");
			    			}
			    			if(!line.startsWith("#")) {
				    			try {
				    				if(line.chars().allMatch(Character::isDigit)){
				    					ldapUsers = ldapUserRepository.findByNumEtudiantEquals(line);
										if(!ldapUsers.isEmpty()) {
											eppn = ldapUsers.get(0).getEppn();
											List<Person> existingPersons = personRepository.findByEppn(eppn);
											if(!existingPersons.isEmpty()) {
												person = existingPersons.get(0);
											}else {
												person = new Person();
												person.setNumIdentifiant(line);
												person.setContext(contexteService.getcurrentContext());
												person.setEppn(eppn);
												person.setType("student");
												personRepository.save(person);
											}
										}
										if(mapEtapes != null) {
											tc.setCodeEtape(mapEtapes.get(line));
										}
				    				}else {
				    					ldapUsers = ldapUserRepository.findByEppnEquals(line);
				    					if(ldapUsers.isEmpty()) {
				    						ldapUsers = ldapUserRepository.findByEmailContainingIgnoreCase(line);
				    					}
				    					if(!ldapUsers.isEmpty() || !checkLdap) {
				    						eppn =(!ldapUsers.isEmpty())? ldapUsers.get(0).getEppn() : line;
				    						List<Person> existingPersons = personRepository.findByEppn(eppn);
				    						if(!existingPersons.isEmpty()) {
												person = existingPersons.get(0);
											}else {
												person = new Person();
												if(checkLdap) {
						    						if(ldapUsers.get(0).getNumEtudiant()!=null && !ldapUsers.get(0).getNumEtudiant().isEmpty()) {
						    							if(mapEtapes!= null && mapEtapes.containsKey("addUser")) {
						    								tc.setCodeEtape(mapEtapes.get("addUser"));
						    							}
														person.setNumIdentifiant(ldapUsers.get(0).getNumEtudiant());
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
											List<Guest> existingGuests = new ArrayList<>();
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
					    			if(tcToImport != null) {
					    				tc.setComment(tcToImport.getComment());
					    				tc.setIsTiersTemps(tcToImport.getIsTiersTemps());
					    			}
					    			Date endDate =  se.getDateFin() != null? se.getDateFin() : se.getDateExamen();
					    			List<Absence> absences = absenceRepository.findOverlappingAbsences(person,
		                                      se.getDateExamen(), endDate, se.getHeureEpreuve(), se.getFinEpreuve(), se.getContext());
					    			if(!absences.isEmpty()) {
					    				tc.setAbsence(absences.get(0));
					    			}
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
		    							TagChecker tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0);
		    							tc.setTypeEmargement(TypeEmargement.MANUAL);
		    							tc.setTagDate(new Date());
		    							tc.setTagChecker(tagChecker);
	    								tc.setSessionLocationBadged(slLibre);
	    								tc.setSessionLocationExpected(slLibre);
					    			}
					    			if(!ldapUsers.isEmpty() || !checkLdap && eppn!=null) {
					    				tcTest = tagCheckRepository.countTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionEpreuveId, eppn);
					    			}else if(!isFromDomain && splitLine!=null) {
					    				tcTest = tagCheckRepository.countTagCheckBySessionEpreuveIdAndGuestEmailEquals(sessionEpreuveId, splitLine[0]);
					    			}

								} catch (Exception e) {
									tcTest = new Long(-1);
									log.error("Erreur sur le login ou numéro identifiant "  + line + " lors de la recherche LDAP", e);
								}
					    		if(!ldapUsers.isEmpty() && tcTest == 0 || ldapUsers.isEmpty() && tcTest == 0 && guest!=null || !checkLdap && tcTest == 0 && eppn!=null ){
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
    	List<List<String>> rows  = new ArrayList<>();
    	for(String user: usersGroupLdap) {
    		List<String> subList = new ArrayList<>();
    		subList.add(user);
    		rows.add(subList);
    	}
    	return rows;
    }
    
    
    public  List<List<String>> setAddList(TagCheck tc){
    	
        List<String> strings = new ArrayList<>();
        if(tc.getPerson() != null) {
	        if(!tc.getPerson().getNumIdentifiant().isEmpty()){
	        	 strings.add(tc.getPerson().getNumIdentifiant());
	        }else if(!tc.getPerson().getEppn().isEmpty()){
	        	 strings.add(tc.getPerson().getEppn());
	        }
        }else if(tc.getGuest() != null) {
        	strings.add(tc.getGuest().getEmail().concat(";").concat(tc.getGuest().getNom()).concat(";").concat(tc.getGuest().getPrenom()));
        }
        List<List<String>> finalList = new ArrayList<>();
        finalList.add(strings);
        return finalList;
    }
    
    public void deleteAllTagChecksBySessionEpreuveId(Long sessionEpreuveId) {
		List<TagCheck> tagChecks =  tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
		List<EsupSignature> list = esupSignatureRepository.findByTagCheckIn(tagChecks);
		if(!list.isEmpty()) {
			esupSignatureRepository.deleteAll(list);
		}
		tagCheckRepository.deleteAll(tagChecks);
		SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
		personService.deleteUnusedPersons(contextRepository.findByContextKey(se.getContext().getKey()));
    }
    
    public int setNomPrenomTagChecks(List<TagCheck> tagChecks, boolean setTagChecker, boolean setProxy) {
        int count = 0;

        if (!tagChecks.isEmpty()) {
            List<String> eppnList = tagChecks.stream()
                .filter(tagCheck -> tagCheck.getPerson() != null)
                .map(tagCheck -> tagCheck.getPerson().getEppn())
                .collect(Collectors.toList());

            List<String> tagCheckerList = setTagChecker ? tagChecks.stream()
                .filter(tagCheck -> tagCheck.getTagChecker() != null)
                .map(tagCheck -> tagCheck.getTagChecker().getUserApp().getEppn())
                .collect(Collectors.toList()) : Collections.emptyList();

            List<String> tcProxyList = setProxy ? tagChecks.stream()
                .filter(tagCheck -> tagCheck.getProxyPerson() != null)
                .map(tagCheck -> tagCheck.getProxyPerson().getEppn())
                .collect(Collectors.toList()) : Collections.emptyList();

            Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(eppnList, "eduPersonPrincipalName");
            Map<String, LdapUser> mapTagCheckerLdapUsers = setTagChecker 
                ? ldapService.getLdapUsersFromNumList(tagCheckerList, "eduPersonPrincipalName") 
                : Collections.emptyMap();
            Map<String, LdapUser> mapTcProxyLdapUsers = setProxy 
                ? ldapService.getLdapUsersFromNumList(tcProxyList, "eduPersonPrincipalName") 
                : Collections.emptyMap();

            for (TagCheck tc : tagChecks) {
                if (tc.getPerson() != null) {
                    LdapUser ldapUser = mapLdapUsers.get(tc.getPerson().getEppn());
                    if (ldapUser != null) {
                        tc.getPerson().setCivilite(ldapUser.getCivilite());
                        tc.getPerson().setNom(ldapUser.getName());
                        tc.getPerson().setPrenom(ldapUser.getPrenom());
                        tc.setNomPrenom(ldapUser.getName().concat(ldapUser.getPrenom()));
                    } else {
                        tc.setNomPrenom("");
                    }
                } else if (tc.getGuest() != null) {
                    tc.setNomPrenom(tc.getGuest().getNom().concat(tc.getGuest().getPrenom()));
                }

                if (setTagChecker && tc.getTagChecker() != null) {
                    LdapUser ldapUser = mapTagCheckerLdapUsers.get(tc.getTagChecker().getUserApp().getEppn());
                    if (ldapUser != null) {
                        tc.getTagChecker().getUserApp().setNom(ldapUser.getName());
                        tc.getTagChecker().getUserApp().setPrenom(ldapUser.getPrenom());
                    } else {
                        tc.getTagChecker().getUserApp().setNom("");
                        tc.getTagChecker().getUserApp().setPrenom("");
                    }
                }

                if (setProxy && tc.getProxyPerson() != null) {
                    LdapUser ldapUser = mapTcProxyLdapUsers.get(tc.getProxyPerson().getEppn());
                    if (ldapUser != null) {
                        tc.getProxyPerson().setNom(ldapUser.getName());
                        tc.getProxyPerson().setPrenom(ldapUser.getPrenom());
                    } else {
                        tc.getProxyPerson().setNom("");
                        tc.getProxyPerson().setPrenom("");
                    }
                }
            }
        }
        return count;
    }
	
	public String snTagChecks(List<Long> tagCheckIds){
		
		List<String> snTagChecks = new ArrayList<>();
		
		if(!tagCheckIds.isEmpty()) {
			List<TagCheck> tcs = new ArrayList<>();
			for(Long id : tagCheckIds) {
				tcs.add(tagCheckRepository.findById(id).get());
			}
			List<String> tcList = tcs.stream().filter(tc->tc.getPerson()!=null).map(tagCheck -> tagCheck.getPerson().getEppn())
									.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
			for(TagCheck tc : tcs) {
				String sn = "";
				LdapUser ldapUser = mapLdapUsers.get(tc.getPerson().getEppn());
				if(ldapUser!=null) {
					sn = ldapUser.getPrenom().concat(" ").concat(ldapUser.getName());
				}else if(tc.getGuest()!=null){
					sn = tc.getGuest().getPrenom().concat(" ").concat(tc.getGuest().getNom());
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
		String civilite = (tc.getPerson() != null && tc.getPerson().getCivilite() != null)? tc.getPerson().getCivilite() : "";
		String nom = (tc.getPerson() != null)? tc.getPerson().getNom() : tc.getGuest().getNom();
		String prenom = (tc.getPerson() != null)? tc.getPerson().getPrenom() : tc.getGuest().getPrenom();
		SessionEpreuve se = tc.getSessionEpreuve();
		SessionLocation sl = tc.getSessionLocationExpected();
		finalString = htmltemplate.replace(wrapChar.concat("civilite").concat(wrapChar), civilite);
		finalString = finalString.replace(wrapChar.concat("prenom").concat(wrapChar), prenom);
		finalString = finalString.replace(wrapChar.concat("nom").concat(wrapChar), nom);
		finalString = finalString.replace(wrapChar.concat("nomSession").concat(wrapChar), se.getNomSessionEpreuve());
		finalString = finalString.replace(wrapChar.concat("dateExamen").concat(wrapChar), String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()));
		finalString = finalString.replace(wrapChar.concat("heureConvocation").concat(wrapChar), String.format("%1$tH:%1$tM", se.getHeureConvocation()));
		finalString = finalString.replace(wrapChar.concat("debutEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", se.getHeureEpreuve()));
		finalString = finalString.replace(wrapChar.concat("finEpreuve").concat(wrapChar), String.format("%1$tH:%1$tM", se.getFinEpreuve()));
		finalString = finalString.replace(wrapChar.concat("dureeEpreuve").concat(wrapChar), toolUtil.getDureeEpreuve(se.getHeureEpreuve(),
				se.getFinEpreuve(), null));
		finalString = finalString.replace(wrapChar.concat("site").concat(wrapChar), se.getCampus().getSite());
		finalString = finalString.replace(wrapChar.concat("salle").concat(wrapChar), sl.getLocation().getNom());
		finalString = finalString.replace(wrapChar.concat("adresse").concat(wrapChar), sl.getLocation().getAdresse());
		
		return finalString;
	}
	
	public void sendEmailConvocation (String subject, String bodyMsg, boolean isSendToManager, List<Long> listeIds, String htmltemplatePdf, 
			String emargementContext, boolean isAll, Long seId, boolean includePdf) throws Exception {
		if(!listeIds.isEmpty() || isAll) {
			int i=0; int j=0; 
			ArrayList<String> errors = new ArrayList<>();
			String[] ccArray = {};
			if(isSendToManager){
				List<String> managers = appliConfigService.getListeGestionnaires();
				ccArray = managers.stream().toArray(String[]::new);
			}
			//Get ListId from all
			List<TagCheck> tcs = new ArrayList<>();
			if(isAll) {
				tcs = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullOrderByPersonEppn(seId);
			}else {
				for(Long id : listeIds) {
					tcs.add(tagCheckRepository.findById(id).get());
				}
			}
			List<String> tcList = tcs.stream().filter(tc->tc.getPerson()!=null).map(tagCheck -> tagCheck.getPerson().getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(tcList, "eduPersonPrincipalName");
			for(TagCheck tc : tcs) {
				String email =  "";
				try {
					LdapUser ldapUser = mapLdapUsers.get(tc.getPerson().getEppn());
					if(ldapUser != null){
						tc.getPerson().setNom(ldapUser.getName());
						tc.getPerson().setPrenom(ldapUser.getPrenom());
						tc.getPerson().setCivilite(ldapUser.getCivilite());
						email = ldapUser.getEmail();
					}else if(tc.getGuest() != null){
						email = tc.getGuest().getEmail();
					}
					String filePath = pdfGenaratorUtil.createPdf(replaceFields(htmltemplatePdf,tc));
					if(appliConfigService.isSendEmails()){
						boolean addAttachment = htmltemplatePdf.isEmpty() || !includePdf? false : true;
						emailService.sendMessageWithAttachment(email, subject, bodyMsg, filePath, "convocation.pdf", ccArray, null, addAttachment);
					}
					tc.setDateEnvoiConvocation(new Date());
					tagCheckRepository.save(tc);
					i++;
				} catch (Exception e) {
					errors.add(email);
					e.printStackTrace();
					j++;
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
		boolean isSessionLibre = false;
		boolean isTagCheckerTagged = false;
		log.info("tagaction pour l'eppn : " + eppn);
		String locationNom = esupNfcTagLog.getLocation();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		// location --> heureDebut//nomSession//nomSalle//idSession
		String[] splitLocationNom = locationNom.split(" // "); 
		String nomSalle = splitLocationNom[2];
		String idSession = splitLocationNom[3];
		Long id = Long.valueOf(idSession);
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
		Context ctx = sessionEpreuve.getContext();
		if(appliConfigService.isTagCheckerDisplayed() && !tagCheckerRepository.findByContextAndUserAppEppn(ctx, eppn).isEmpty()) {
			isTagCheckerTagged = true;
		}
		if(sessionEpreuve != null) {
			Groupe gpe = sessionEpreuve.getBlackListGroupe();
			boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);		
			Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
			if("isTagable".equals(action)) {
				log.info("isTagable pour l'eppn : " + eppn);
				boolean isUnknown = false;
				Long tc =  null;
				if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
					tc = tagCheckRepository.checkIsTagable(nomSalle, eppn,  date, id);
				}else {
					tc = tagCheckRepository.checkIsTagableWithDateFin(nomSalle, eppn, date, dateFin, id);
				}
				SessionLocation sessionLocationBadged =  null;
				TagChecker tagChecker = null;
				sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), nomSalle);
				Long sessionLocationBadgedId = sessionLocationBadged.getId();
				if(sessionEpreuve.getIsSessionLibre()) {
					isSessionLibre = true;
					tagChecker = tagCheckerRepository.findByContextAndUserAppEppn(ctx,esupNfcTagLog.getEppnInit()).get(0);
				}
				
				if (tc ==1 || isSessionLibre || isTagCheckerTagged) {
					Long totalPresent = tagCheckRepository.countTagCheckBySessionLocationExpected (sessionLocationBadged);
					String msgError = "";
					TagCheck newTagCheck = new TagCheck();
					Long count = 0L;
					if(isSessionLibre) {
						try {
							if(totalPresent >= sessionLocationBadged.getCapacite()) {
								TagCheck tagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationBadged.getId(), eppn);
								if(tagCheck != null && !isTagCheckerTagged) {
									saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, TypeEmargement.CARD);
									isOk = true;
								}else {
									isOk = false;
								}
							}else {
								if(!isBlackListed) {
									newTagCheck = saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, TypeEmargement.CARD);
									newTagCheck.setIsBlacklisted(false);
									count = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationBadgedId);
								}else {
									msgError = eppn;
								}
								isOk = true;
								dataEmitterService.sendData(newTagCheck, 0f, count, new SessionLocation(), msgError);
							}
						} catch (Exception e) {
							log.error("Session libre, problème de carte pour l'eppn : "  + eppn, e);
						}
					}else{
						isOk = true;
					}
					if(isTagCheckerTagged) {
						String presence ="true,tagchecker," + eppn + "," + sessionLocationBadgedId;
						tagCheckerService.updatePresentsTagCkeckers(presence, TypeEmargement.CARD);
						isOk = true;
					}
				}else {
					try {
						String comment = "";
						String eppnInit = esupNfcTagLog.getEppnInit();
						tagChecker = tagCheckerRepository.findByContextAndUserAppEppn(ctx, eppnInit).get(0);
						sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), nomSalle);
						//on regarde si la personne est dans une autre salle de la session
						Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date, id);
						Long countSe = sessionEpreuveRepository.countSessionEpreuveIdExpected(eppn, date);
						SessionLocation sl = null;
						List<Long> seId = null;
						if(sessionLocationId != null) {
							if(sessionEpreuve.getTypeBadgeage().equals(TypeBadgeage.SALLE)) {
								sl = sessionLocationRepository.findById(sessionLocationId).get();
								comment = "Inconnu dans cette salle, Salle attendue : " +  sl.getLocation().getNom();
							}else {
								isOk = true;
							}
						}
						else if (sessionLocationId == null && countSe>0) {//On regarde si il est est dans une autre session aujourd'hui
							//comparaison avec les heures de début et de fin
							LocalTime now = LocalTime.now();
							seId = sessionEpreuveRepository.getSessionEpreuveIdExpected(eppn, date, now);
							if(seId != null) {
								String lieu = "";
								for(Long sessionId : seId) {
									List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdAndPersonEppnEquals(sessionId, eppn, null).getContent();
									if(!list.isEmpty()) {
										TagCheck tagCheck = list.get(0);
										if(tagCheck.getSessionLocationExpected() !=null) {
											lieu = lieu.concat(" --> Session(s) : "+ tagCheck.getSessionEpreuve().getNomSessionEpreuve());
											String location = list.get(0).getSessionLocationExpected() .getLocation().getNom();
											lieu = lieu.concat(" , lieu : ").concat(location);
										}
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
							TagCheck newTc = saveUnknownTagCheck(comment, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker, isSessionLibre, TypeEmargement.CARD);
							dataEmitterService.sendData(newTc, 0f, 0L, new SessionLocation(), "");
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
					sessionLocationId = tagCheckRepository.getSessionLocationId(nomSalle, eppn, date, id);
				}else {
					sessionLocationId = tagCheckRepository.getSessionLocationIdWithDateFin(nomSalle, eppn, date, dateFin, id);
				}
				log.info("SessionLocationId : " + sessionLocationId);
				TagCheck presentTagCheck = null;
				if(sessionLocationId!=null) {
					realSlId = sessionLocationId;
					presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, eppn);
				}else {
					Long slId = tagCheckRepository.getSessionLocationId(nomSalle, date, id);
					realSlId = slId;
					if(slId!=null) {
						SessionLocation sl = sessionLocationRepository.findById(slId).get();
						presentTagCheck = tagCheckRepository.findTagCheckBySessionEpreuveIdAndEppn(sl.getSessionEpreuve().getId(), esupNfcTagLog.getEppn());
					}
				}
				String msgError = "";
				SessionLocation sl = sessionLocationRepository.findById(realSlId).get();
				if(presentTagCheck!=null) {
					boolean isBlacklisted = true;
					if(!isBlackListed) {
						isBlacklisted = false;
						TagChecker tagChecker = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(sl, esupNfcTagLog.getEppnInit());
						if(sessionEpreuve.getIsSecondTag()!= null && sessionEpreuve.getIsSecondTag()) {
							presentTagCheck.setTagDate2(new Date());
							presentTagCheck.setTypeEmargement2(TypeEmargement.CARD);
							presentTagCheck.setTagChecker2(tagChecker);
						}else {
							presentTagCheck.setTagDate(new Date());
							presentTagCheck.setTypeEmargement(TypeEmargement.CARD);
							presentTagCheck.setTagChecker(tagChecker);
						}
				    	presentTagCheck.setSessionLocationBadged(sl);
				    	presentTagCheck.setNbBadgeage(this.getNbBadgeage(presentTagCheck, true));
				    	Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sl);
				    	sl.setNbPresentsSessionLocation(countPresent);
				    	List<TagChecker> tcList = new ArrayList<>();
			        	tcList.add(presentTagCheck.getTagChecker());
				    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
				    	List<TagCheck> tagCheckList = new ArrayList<>();
				    	tagCheckList.add(presentTagCheck);
				    	setNomPrenomTagChecks(tagCheckList, false, false);
				    	tagCheckRepository.save(presentTagCheck);
				    	isOk = true;					
					}else {
						msgError = eppn;
					}
	
			    	//On prend l'id du sl expected
			    	Long slExpected = presentTagCheck.getSessionLocationExpected().getId();
			    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(slExpected);
			    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(slExpected);
			    	float percent = 0;
			    	if(totalExpected!=0) {
			    		percent = 100*(Long.valueOf(totalPresent).floatValue()/ Long.valueOf(totalExpected).floatValue() );
			    	}
			    	presentTagCheck.setIsBlacklisted(isBlacklisted);
			    	dataEmitterService.sendData(presentTagCheck, percent, totalPresent, sl, msgError);
				}else {
					if(isTagCheckerTagged) {
						String presence ="true,tagchecker," + eppn + "," + sl.getId();
						tagCheckerService.updatePresentsTagCkeckers(presence, TypeEmargement.CARD);
						isOk = true;
					}
				}
			}
		}
		return isOk;
	}
	
	public TagCheck saveUnknownTagCheck(String comment, Context ctx, String eppn, SessionEpreuve sessionEpreuve, SessionLocation sessionLocationBadged, 
				TagChecker tagChecker, boolean isSessionLibre, TypeEmargement typeEmargement) {
		TagCheck unknownTc = null;
		List<TagCheck> badgedTcs = new ArrayList<>();
		//rm!!   Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, sessionEpreuve.getDateExamen(),sessionEpreuve.getId());
		if(isSessionLibre) {
			if(typeEmargement.name().startsWith(TypeEmargement.QRCODE.name())) {
				List<TagCheck> tcs = tagCheckRepository.findByContextAndPersonEppnAndSessionEpreuve(ctx, eppn, sessionEpreuve);
				if(!tcs.isEmpty()) {
					Long sessionLocationId = tcs.get(0).getSessionLocationExpected().getId();
					badgedTcs = tagCheckRepository.findByContextAndSessionLocationExpectedIdAndPersonEppnEquals(ctx, sessionLocationId, eppn);
				}
			}else {
				List<TagCheck> tcs = tagCheckRepository.findTagCheckByPersonEppnAndSessionEpreuve(eppn, sessionEpreuve);
				if(!tcs.isEmpty()) {
					Long sessionLocationId = tcs.get(0).getSessionLocationExpected().getId();
					badgedTcs = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn);
				}
			}
		}else {
			if(!typeEmargement.name().startsWith(TypeEmargement.QRCODE.name()) || typeEmargement.name().startsWith(TypeEmargement.QRCODE.name()) && !personRepository.findByEppnAndContext(eppn, ctx).isEmpty()) {
				badgedTcs = tagCheckRepository.findByContextAndSessionLocationBadgedIdAndPersonEppnEquals(ctx, sessionLocationBadged.getId(), eppn);
			}
		}
		if(!badgedTcs.isEmpty()) {
			unknownTc = badgedTcs.get(0);
		}else {
			unknownTc = new TagCheck();
			unknownTc.setComment(comment);
			unknownTc.setSessionEpreuve(sessionEpreuve);
			unknownTc.setContext(ctx);
			List<Person> persons = personRepository.findByEppnAndContext(eppn, ctx);
			Person p = null;
			if(persons.isEmpty()) {
				p = new Person();
				p.setContext(ctx);
				p.setEppn(eppn);
				personRepository.save(p);
			}else {
				p = persons.get(0);
			}
			List<LdapUser> users = ldapUserRepository.findByEppnEquals(eppn);
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
			unknownTc.setIsUnknown(true);
		}
		if(sessionEpreuve.getIsSecondTag()!= null && sessionEpreuve.getIsSecondTag()) {
			unknownTc.setTagDate2(new Date());
			unknownTc.setTypeEmargement2(TypeEmargement.CARD);
			unknownTc.setTagChecker2(tagChecker);
		}else {
			unknownTc.setTagDate(new Date());
			unknownTc.setTypeEmargement(TypeEmargement.CARD);
			unknownTc.setTagChecker(tagChecker);
		}
		unknownTc.setNbBadgeage(getNbBadgeage(unknownTc, true));
		TagCheck tc =tagCheckRepository.save(unknownTc);
		return tc;
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
	
	public byte[] exportTagChecks(String type, Long id, HttpServletResponse response, String emargementContext, String anneeUniv, boolean signature) {
		List<TagCheck> list = null;
		byte[] pdfBytes = null;
        String nomFichier = "export";
        String fin = "";
        SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		if(anneeUniv != null) {
			list = tagCheckRepository.findTagCheckBySessionEpreuveAnneeUniv(anneeUniv);
			nomFichier = "Export_inscrits_annee_universitaire_" + anneeUniv;
		}else {
			Date dateFin = se.getDateFin();
	    	fin = (dateFin != null)? "_" + String.format("%1$td-%1$tm-%1$tY", dateFin) : "" ;
			nomFichier = se.getNomSessionEpreuve().concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen())).concat(fin);
	   		list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(id, null).getContent();
	   		nomFichier = nomFichier.replace(" ", "_");
		}

		this.setNomPrenomTagChecks(list, false, false);
		if ("QRC".equals(type)) {
			String filename = nomFichier.concat(".pdf");
			PdfPTable table = new PdfPTable(3);
			table.setWidthPercentage(100);
	    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
	        //On créer l'objet cellule.
	    	String libelleSe = null;
	    	if(anneeUniv != null) {
	    		libelleSe = "Année universitaire " + anneeUniv;
	    	}else {
		    	TagCheck tch = list.get(0);
		    	libelleSe = tch.getSessionEpreuve().getNomSessionEpreuve().concat(" -- ").
		    			concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionEpreuve().getDateExamen()))).concat(fin);
	    	}
	        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
	        cell.setBackgroundColor(BaseColor.GREEN);
	        cell.setColspan(3);
	        table.addCell(cell);

	        PdfPCell header1 = new PdfPCell(new Phrase("Personne")); header1.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header2 = new PdfPCell(new Phrase("Type")); header2.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header3 = new PdfPCell(new Phrase("QR Code")); header3.setBackgroundColor(BaseColor.GRAY);
	        table.addCell(header1);
	        table.addCell(header2);
	        table.addCell(header3);
	        list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(id, null).getContent();
	        if(!list.isEmpty()) {
	        	for(TagCheck tc : list) {
	        		PdfPCell dateCell = null;
	        		String nom = (tc.getPerson()!=null) ? tc.getPerson().getNom() : tc.getGuest().getNom();
	        		String prenom = (tc.getPerson()!=null) ? tc.getPerson().getPrenom() : tc.getGuest().getPrenom();
	        		dateCell = new PdfPCell(new Paragraph(nom.concat(" ").concat(prenom)));
	                table.addCell(dateCell);
	                String typeInd = (tc.getPerson()!=null) ? tc.getPerson().getType() : "ext";
	                typeInd = messageSource.getMessage("person.type.".concat(typeInd.toLowerCase()), null, null);
	                dateCell = new PdfPCell(new Paragraph(typeInd));
	                table.addCell(dateCell);
	                String identifiant = (tc.getPerson()!=null) ? tc.getPerson().getEppn() : tc.getGuest().getEmail();
	                try {
						String qrCodeString = "true," + identifiant + "," + tc.getSessionLocationExpected().getId() + "," + identifiant + ",qrcode@@@notime@@@" + tc.getContext().getId();
						String enocdedQrCode = toolUtil.encodeToBase64(qrCodeString);
						InputStream is = toolUtil.generateQRCodeImage("qrcode".concat(enocdedQrCode), 5, 5);
						byte[] bytes = IOUtils.toByteArray(is);
						Image image1 = Image.getInstance(bytes);
						dateCell = new PdfPCell(image1, true);
						dateCell.setFixedHeight(60f);
						table.addCell(dateCell);
					} catch (Exception e) {
						log.error("Impossible de générer un QR code pour l'identifiant " + identifiant);
					}                
	        	}		
	        }
	        Document document = new Document();
	        document.setMargins(10, 10, 10, 10);
	        try 
	        {
	          response.setContentType("application/pdf");
	          response.setHeader("Content-Disposition","attachment; filename=".concat(filename));
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
			
		} else if ("PDF".equals(type)) {
			String filename = nomFichier.concat(".pdf");
	        Document document = new Document();
	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        PdfWriter writer = null;
	        try 
	        {
				if (signature) {
					writer = PdfWriter.getInstance(document, byteArrayOutputStream);
				} else {
					response.setContentType("application/pdf");
					response.setHeader("Content-Disposition", "attachment; filename=".concat(filename));
					writer = PdfWriter.getInstance(document, response.getOutputStream());
				}

				getTagCheckListAsPDF(list, document, writer, se, emargementContext);
			} catch (DocumentException de) {
				de.printStackTrace();
				logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
						null, emargementContext, null);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
						null, emargementContext, null);
			}
	 
	        document.close();
	        if(signature) {
	        	pdfBytes = byteArrayOutputStream.toByteArray();
	        }
	       
		} else if("CSV".equals(type)){
			try {
			    String filename = nomFichier.concat(".csv");

			    response.setContentType("text/csv");
			    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
			    response.setCharacterEncoding("UTF-8");

			    // Create a CSV writer
			    CSVWriter writer = new CSVWriter(response.getWriter());
			    
			    // Define headers
			    String[] headers = { "Session", "Date", "Numéro Etu", "Eppn", "Nom", "Prénom", "Présent", "Emargement", 
			                         "Type", "Lieu attendu", "Lieu badgé", "Absence", "Tiers-temps" };
			    writer.writeNext(headers);

			    for (TagCheck tc : list) {
			        String dateSession = formatDate(tc.getSessionEpreuve().getDateExamen());
			        String dateFin = (tc.getSessionEpreuve().getDateFin() != null) ? " / " + formatDate(tc.getSessionEpreuve().getDateFin()) : "";
			        String presence = (tc.getTagDate() != null) ? "Présent" : "Absent";
			        String date = (tc.getTagDate() != null) ? formatTime(tc.getTagDate()) : "--";
			        String badged = (tc.getSessionLocationBadged() != null) ? tc.getSessionLocationBadged().getLocation().getNom() : "--";
			        String attendu = (tc.getSessionLocationExpected() != null) ? tc.getSessionLocationExpected().getLocation().getNom() : "--";
			        String tiersTemps = (tc.getIsTiersTemps()) ? "Oui" : "--";
			        String typeEmargement = (tc.getTypeEmargement() != null) ? messageSource.getMessage("typeEmargement.".concat(tc.getTypeEmargement().name().toLowerCase()), null, null) : "--";
			        String nom = (tc.getPerson() != null) ? tc.getPerson().getNom() : (tc.getGuest() != null) ? tc.getGuest().getNom() : "";
			        String prenom = (tc.getPerson() != null) ? tc.getPerson().getPrenom() : (tc.getGuest() != null) ? tc.getGuest().getPrenom() : "";
			        String identifiant = (tc.getPerson() != null) ? tc.getPerson().getEppn() : (tc.getGuest() != null) ? tc.getGuest().getEmail() : "";
			        String numIdentifiant = (tc.getPerson() != null) ? tc.getPerson().getNumIdentifiant() : "";
	    			String absence = (tc.getAbsence() != null) ? tc.getAbsence().getMotifAbsence().getTypeAbsence().name() + '-' + tc.getAbsence().getMotifAbsence().getStatutAbsence().name() : "";

			        String[] line = {
			            tc.getSessionEpreuve().getNomSessionEpreuve(),
			            dateSession + dateFin,
			            numIdentifiant,
			            identifiant,
			            nom,
			            prenom,
			            presence,
			            date,
			            typeEmargement,
			            attendu,
			            badged,
			            absence,
			            tiersTemps
			        };
			        
			        writer.writeNext(line);
			    }
			    
			    // Close the writer after all rows are written
			    writer.close();
			    
			    log.info("Extraction CSV: " + list.size() + " résultats");
			    logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction CSV: " + list.size() + " résultats", null, null, emargementContext, null);

			} catch (Exception e) {
			    log.error("Erreur lors de l'extraction CSV", e);  // Log the exception for better traceability
			    logService.log(ACTION.EXPORT_CSV, RETCODE.FAILED, "Erreur lors de l'extraction CSV", null, null, emargementContext, null);
			    e.printStackTrace();
			}
		}
		return pdfBytes;
	}

	public void getTagCheckListAsPDF(
		List<TagCheck> list,
		Document document,
		PdfWriter writer,
		SessionEpreuve se,
		String emargementContext
	) {
		getTagCheckListAsPDF(list, document, writer, se, emargementContext, null);
	}

	public void getTagCheckListAsPDF(
		List<TagCheck> list,
		Document document,
		PdfWriter writer,
		SessionEpreuve se,
		String emargementContext,
		Long sessionLocationId
	) {
		getTagCheckListAsPDF(list, document, writer, se, null, emargementContext, sessionLocationId);
	}

	public void getTagCheckListAsPDF(
		List<TagCheck> list,
		Document document,
		PdfWriter writer,
		Person person,
		String emargementContext
	) {
		getTagCheckListAsPDF(list, document, writer, null, person, emargementContext, null);
	}

	protected void getTagCheckListAsPDF(
		List<TagCheck> list,
		Document document,
		PdfWriter writer,
		SessionEpreuve se,
		Person person,
		String emargementContext,
		Long sessionLocationId
	) {
		if (
			((null != se) && (null != person))
			||
			((null == se) && (null == person))
		) {
			// Entre feuille d'émargement d'une session ou feuille d'émargement d'un participant il faut choisir"
			return;
		}

		// Commenter/ajouter les lignes correspondant aux colonnes que vous souhaitez/ne souhaitez pas afficher
		// Vous pouvez également en modifier l'ordre ou le libellé (second paramètre du put)
		// ATTENTION Cela s'applique à tous les contextes
		LinkedHashMap<String, String> displayedCols = new LinkedHashMap<String, String>();

		// Commenter/Ajouter les lignes correspondant aux données que vous souhaiter/ne souhaitez pas
		// avoir dans le cartouche (en haut à droite de la page)
		// Vous pouvez également en modifier l'ordre ou le libellé (second paramètre du put)
		// ATTENTION Cela s'applique à tous les contextes
		LinkedHashMap<String, String> headerRequestedFields = new LinkedHashMap<String, String>();

		if (null != se) {
			// Configuration adapté à une liste d'émargement pour une session donnée (avec plusieurs participants)
			displayedCols.put(COL_LIGNE_NUM, "#");
			displayedCols.put(COL_PARTICIPANT_NOM, "Nom");
			displayedCols.put(COL_PARTICIPANT_PRENOM, "Prénom");
			displayedCols.put(COL_PARTICIPANT_IDENTIFIANT, "Identifiant"); // n° étudiant (Etudiant) ou adresse mail (Personnel)
			displayedCols.put(COL_PARTICIPANT_GROUPES, "Groupes");
			displayedCols.put(COL_PARTICIPANT_TYPE, "Type"); // E, P ou Ex
			displayedCols.put(COL_EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS, "Emargement");
			displayedCols.put(COL_EMARGEMENT_MODE, "Mode");

			headerRequestedFields.put(COL_SESSION_RATIO_PRESENTS, "Présents");
			headerRequestedFields.put(COL_SESSION_DATES_DEBUT_FIN, "Date");
			headerRequestedFields.put(COL_SESSION_HEURES_DEBUT_FIN, "Heure");
		} else {
			// Configuration adapté à une liste d'émargement pour un participant donné (au cours de diférentes sessions)
			displayedCols.put(TagCheckService.COL_SESSION_DATE_HEURE_DEBUT, "Date Heure");
			displayedCols.put(TagCheckService.COL_SESSION_DUREE, "Durée");
			displayedCols.put(TagCheckService.COL_SESSION_NOM, "Cours");
			displayedCols.put(TagCheckService.COL_SESSION_SURVEILLANTS, "Enseignant(s)"); // n° étudiant (Etudiant) ou adresse mail (Personnel)
			displayedCols.put(TagCheckService.COL_EMARGEMENT_MODE_OU_ABSENCE, "Émargement");	

			headerRequestedFields.put(TagCheckService.COL_PARTICIPANT_NOM, "Nom");
			headerRequestedFields.put(TagCheckService.COL_PARTICIPANT_PRENOM, "Prénom");
			headerRequestedFields.put(TagCheckService.COL_PARTICIPANT_IDENTIFIANT, "Identifiant");	
		}

		getTagCheckListAsPDF(list, document, writer, se, person, displayedCols, headerRequestedFields, emargementContext, null);
	}

	// Il y avait de tous petits deltas entre l'implémentation dans TagCheckService et celle dans PresenceService
	// Dans la version PresenceService, le lieu est ajouté au titre (en s'appuyant sur la variable "sessionLocationId").
	// Dans la version TagCheckService, la date de fin est précédée de "\n"
	// Pour ne pas introduire plus de modifications que nécessaire, on considèrera que:
	// - Si on a renseigné sessionLocationId, on est dans le mode de fonctionnement de PresenceService
	// - sinon dans le mode de fonctionnement de TagCheckService
	// A noter, à partir de sessionLocationId, on peut récupérer un objet SessionLocation dont on peut visiblement récupérer
	// SessionEpreuve (dans dans ce cas le paramètre pourrait être homis)
	protected void getTagCheckListAsPDF(
		List<TagCheck> list,
		Document document,
		PdfWriter writer,
		SessionEpreuve se,
		Person person,
		LinkedHashMap<String, String> displayedCols,
		LinkedHashMap<String, String> headerRequestedFields,
		String emargementContext,
		Long sessionLocationId
	) {
		if (
			((null != se) && (null != person))
			||
			((null == se) && (null == person))
		) {
			// Entre feuille d'émargement d'une session ou feuille d'émargement d'un participant il faut choisir"
			return;
		}

		// -----------------------------------------------------------
		// Configuration "locale" (applicable à tous les contextes)
		// -----------------------------------------------------------
		// A noter: Bien que l'objectif visé par le paramètre "personnaliserLogoParFormation"
		// soit bien de personnaliser le logo en fonction de la formation, en pratique
		// il est personnalisé en fonction des groupes (seule information plus ou moins disponible)
		// On fait donc l'hypothèse 1 groupe = 1 formation.
		// L'affichage du logo dédié à la formation pourra se faire uniquement s'il l'un des fichiers
 		// suivants existe (par ordre de priorité):
		// - /static/images/logos_formations/par_contexte/<identifiant du contexte>/<nom du groupe>.png
		// - /static/images/logos_formations/<nom du groupe>.png
		// sinon, c'est le logo standard qui est affiché (/static/images/logo.jpg)
		// A noter: le logo doit avoir des dimensions largeur:hauteur dans le ratio 3:1
		// Rem: La personnalisation du logo n'est vraiment pertinente que si l'ensemble des participants
		// appartient à la même formation (i.e. groupe)..
		boolean personnaliserLogoParFormation = true; 

		// ...s'il s'avère qu'il y a plusieurs formations (i.e. groupes) représentés, on peut
		// choisir d'afficher le logo de la première formation venue ou de se rabattre sur le logo
		// par défaut
		// A noter: L'affichage éventuel de plusieurs logos n'est pas géré.
		boolean prendreLogoPremiereFormationSiPlusieursFormations = false;

		// Si (au moins) 1 des participants n'est pas associé à un (ou plusieurs) groupe(s)
		// on peut faire le choix de l'ignorer (et l'assimiler aux groupes représentés)
		// ou au contraire considérer qu'il appartient à un groupe distinct
		boolean participantSansGroupeEquivautAutreGroupe = true;

		// Dans le cas où l'on a activé l'affichage du groupe des participants à la session
		// (i.e. getIsGroupeDisplayed = true)
		// Si tous les partipants appartiennent au même groupe, on peut choisir de remplacer
		// la colonne (qui prend de la place dans le tableau) par une ligne unique en tête
		// de tableau.
		// Cela est notamment pensé dans le cas où groupe = formation. La feuille d'émargement
		// peut ainsi rappeler la formation concernée.
		boolean remplacerColGroupeParTitreSiPossible = true;

		// Afin de laisser de la place pour les autres informations, on peut vouloir
		// masquer la colonne Type (de participant) lorsque la session n'implique
		// que des étudiants
		boolean masquerColTypeSiUniquementTypeE = false;
	
		// Pour un étudiant l'identifiant est le n° étudiant. Il prend bien moins de place
		// qu'une adresse mail (identifiant utilisé pour les autres types de participant).
		// Afin de laisser de la place pour les autres informations, lorsqu'il n'y a que
		// des étudiants parmi les participants, il peut être intéressant de réduire la
		// largeur de la colonne identifiant au strict minimum (8 chiffres?)
		boolean optimiserLargeurColIdentifiantSiUniquementTypeE = false;

		// Option d'affichage avec une police de caractères plus petite
		// et des marges (padding) plus petites également
		boolean isCompactMode = false;

		// Pour conserver les tailles de polices de caractères du tableau principal
		// ainsi que les largeurs de colonnes anciennement préconfigurées
		// (dans l'hypothèse où il n'y a pas de changement dans le choix des colonnes affichées)
		// positionner à true
		// Sera ignoré si optimiserLargeurColIdentifiantSiUniquementTypeE = true
		// ou si isCompactMode = true
		boolean legacyDisplayConfig = true;

		if (optimiserLargeurColIdentifiantSiUniquementTypeE) {
			legacyDisplayConfig = false;
		}

		int nbLigneMaxParPage        = 18;

		int mainTableHeaderFontSize  = 10;
		float mainTableHeaderPadding = 5f;
		int mainTableFontSize        = 10;
		float mainTablePadding       = 5f;

		if (isCompactMode) {
			mainTableHeaderFontSize = 8;
			mainTableHeaderPadding  = 3f;
			mainTableFontSize       = 8;
			mainTablePadding        = 3f;

			legacyDisplayConfig = false;
		}

		if (null == se) {
			legacyDisplayConfig = false;
		}

		try {
			document.setMargins(10, 10, 10, 10);

			List<String> groupesRepresentes = new ArrayList<String>();
			boolean foundParticipantWithNoGroup = false;
			boolean listWithOnlyTypeE = true;
			boolean isGroupesRepresentesValueRequired = personnaliserLogoParFormation || remplacerColGroupeParTitreSiPossible;
			boolean isListWithOnlyTypeEValueRequired = optimiserLargeurColIdentifiantSiUniquementTypeE || masquerColTypeSiUniquementTypeE;
			// (si besoin) On va dans un premier temps déterminer quels sont les groupes représentés sur cette feuille
			if (isGroupesRepresentesValueRequired || isListWithOnlyTypeEValueRequired) {
				if (null != person) {
					// S'il s'agit de l'émargement d'un seul participant (sur plusieurs sessions)
					for (Groupe groupe: person.getGroupes()) {
						groupesRepresentes.add(groupe.getNom());
					}
				} else {
					// S'il s'agit de l'émargement de plusieurs participants à une session unique
					if (!list.isEmpty()) {
						for(TagCheck tc : list) {
							String typeIndividu = "";
							if (tc.getPerson() != null ) {
								if (isListWithOnlyTypeEValueRequired) {
									typeIndividu = messageSource.getMessage("person.type.".concat(tc.getPerson().getType()), null, null).substring(0,1);
								}

								if (isGroupesRepresentesValueRequired) {
									if ((tc.getPerson().getGroupes() != null) && (tc.getPerson().getGroupes().size() > 0)) {
										List<String> groupes = tc.getPerson().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
										groupesRepresentes = Stream.concat(groupesRepresentes.stream(), groupes.stream()).distinct().collect(Collectors.toList());
									} else {
										foundParticipantWithNoGroup = true;
									}
								}
							} else if(tc.getGuest() != null ) {
								if (isListWithOnlyTypeEValueRequired) {
									typeIndividu = "Ex";
								}

								if (isGroupesRepresentesValueRequired) {
									if ((tc.getGuest().getGroupes() != null)  && (tc.getPerson().getGroupes().size() > 0)) {
										List<String> groupes = tc.getGuest().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
										groupesRepresentes = Stream.concat(groupesRepresentes.stream(), groupes.stream()).distinct().collect(Collectors.toList());
									} else {
										foundParticipantWithNoGroup = true;
									}
								}
							}

							if (
								isListWithOnlyTypeEValueRequired
								&&
								(!"E".equals(typeIndividu))
							) {
								listWithOnlyTypeE = false;
							}
						}
					}
				}
			}

			Paragraph paragrapheNomGroupeUnique = null;

			if (null != se) {
				// S'il s'agit de l'émargement de plusieurs participants à une session unique
				if (
					(!BooleanUtils.isTrue(se.getIsGroupeDisplayed()))
					||
					(
						remplacerColGroupeParTitreSiPossible
						&&
						(1 == groupesRepresentes.size())
						&&
						(
							(!participantSansGroupeEquivautAutreGroupe)
							||
							(!foundParticipantWithNoGroup)
						)
					)
				) {
					// Dans ce cas là, on n'affiche pas la colonne groupes
					if (displayedCols.containsKey(COL_PARTICIPANT_GROUPES)) {
						displayedCols.remove(COL_PARTICIPANT_GROUPES);
					}

					if (BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
						// Si on remplace la colonne par un texte en tête de tableau
						Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD);
						paragrapheNomGroupeUnique = new Paragraph(groupesRepresentes.get(0), font);	
					}
				}
			}

			if (masquerColTypeSiUniquementTypeE && listWithOnlyTypeE) {
				if (displayedCols.containsKey(COL_PARTICIPANT_TYPE)) {
					displayedCols.remove(COL_PARTICIPANT_TYPE);
				}
			}

			if (7 != displayedCols.size()) {
				legacyDisplayConfig = false;
			}

			// Détermination du logo à afficher (en haut à gauche)
			//------------------------------------------------------------------------
			Image image = null;
			String imageDirPath = "/static/images/";
			String defaultImagePath = imageDirPath+"logo.jpg";

			URL logoResource = null;
			if (
				personnaliserLogoParFormation
				&&
				(groupesRepresentes.size() > 0)
				&&
				(
					(1 == groupesRepresentes.size() + ((participantSansGroupeEquivautAutreGroupe && foundParticipantWithNoGroup)?1:0))
					||
					(
						(1 < groupesRepresentes.size() + ((participantSansGroupeEquivautAutreGroupe && foundParticipantWithNoGroup)?1:0))
						&&
						prendreLogoPremiereFormationSiPlusieursFormations
					)
				)
			) {
				String imagePath = imageDirPath+"logos_formations/par_contexte/"+emargementContext+"/"+groupesRepresentes.get(0)+".png";
				File file = new File(imagePath);
				if (file.getCanonicalPath().startsWith(imageDirPath)) {
					logoResource = PresenceService.class.getResource(imagePath);
				} else {
					// emargementContext ou nom de formation contient vraisemblablement des /../
					// tentative de hack ?
					log.warn(imagePath+" n'est pas un chemin valide");
				}

				if (null == logoResource) {
					// Logo "personnalisé" non trouvé dans l'espace contextuel
					// on se rabat sur le logo dans l'espace par défaut
					imagePath = imageDirPath+"logos_formations/"+groupesRepresentes.get(0)+".png";
					file = new File(imagePath);
					if (file.getCanonicalPath().startsWith(imageDirPath)) {
						logoResource = PresenceService.class.getResource(imagePath);
					} else {
						// emargementContext ou nom de formation contient vraisemblablement des /../
						// tentative de hack ?
						log.warn(imagePath+" n'est pas un chemin valide");
					}
				}

				if (null == logoResource) {
					// Logo "personnalisé" non trouvé dans l'espace contextuel et ni dans l'espace racine
					// on se rabat sur le logo par défaut
					logoResource = PresenceService.class.getResource(defaultImagePath);
				}
			} else {
				logoResource = PresenceService.class.getResource(defaultImagePath);
			}

			image = Image.getInstance(logoResource);
			image.scaleAbsolute(150f, 50f);// image width,height

			//-----------------------------------------------------
			// Cartouche en haut à droite
			//-----------------------------------------------------
			LinkedHashMap<String, String> headerFields = new LinkedHashMap<String, String>();
			for (String cartoucheField: headerRequestedFields.keySet()) {
				String value = "???"+cartoucheField+"???";
				switch (cartoucheField) {
					case COL_PARTICIPANT_IDENTIFIANT:
						// Uniquement applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						// Non applicable à une liste de participants à une session unique
						if (null != person) {
							value = person.getNumIdentifiant();
							if (null == value) {
								value = person.getEppn();
							}
						}
						break;
					case COL_PARTICIPANT_NOM:
						// Uniquement applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						// Non applicable à une liste de participants à une session unique
						if (null != person) {
							value = person.getNom().toUpperCase();
						}
						break;
					case COL_PARTICIPANT_PRENOM:
						// Uniquement applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						// Non applicable à une liste de participants à une session unique
						if (null != person) {
							value = person.getPrenom();
						}
						break;
					case COL_SESSION_DATES_DEBUT_FIN:
						// Uniquement applicable à une liste de participants à une session unique
						// Non applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						if (null != se) {
							String dateFin = (se.getDateFin() != null)
								? (null == sessionLocationId?"\n":"") + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin()))
								: "";
							value = String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).concat("\n"  + dateFin);
						}
						break;
					case COL_SESSION_HEURES_DEBUT_FIN:
						// Uniquement applicable à une liste de participants à une session unique
						// Non applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						if (null != se) {
							value = String.format("%1$tH:%1$tM", se.getHeureEpreuve()) + " - "
									+ String.format("%1$tH:%1$tM", se.getFinEpreuve());
						}
						break;
					case COL_SESSION_RATIO_PRESENTS:
						// Uniquement applicable à une liste de participants à une session unique
						// Non applicable à une liste d'émargements pour un participant unique (à plusieurs sessions)
						if (null != se) {
							Long totalExpected = tagCheckRepository
								.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(se.getId());
							Long totalPresent = tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNull(se.getId());
							value = String.valueOf(totalPresent) + " / " + String.valueOf(totalExpected);
						}
						break;
					default:
						value = "???"+cartoucheField+"???";
				}
				headerFields.put(headerRequestedFields.get(cartoucheField), value);
			}
			PdfPTable headerTable = pdfGenaratorUtil.getHorizontalCartoucheTable(headerFields);

			//------------------------------------------------------
			// Titre de la page
			//------------------------------------------------------
			String title = null;
			
			if (null != se) {
				SessionLocation sl = null;
				if (null != sessionLocationId) {
					sl = sessionLocationRepository.findById(sessionLocationId).get();
				}

				// titre = nom de la session et éventuellement localisation
				title = se.getNomSessionEpreuve() + (null == sl?"": " // " + sl.getLocation().getNom());
			} else {
				// titre = nom de la formation
				// A défaut d'être en mesure de récupérer l'information "formation" pour un participant
				// on affiche le(s) groupe(s) auxquels il est associé et ça fera le job
				// pour les cas où le participant n'appartient qu'à un seul groupe esup-emargement
				// et que le nom du groupe est le nom de la formation
				title = StringUtils.join(groupesRepresentes,", ");
			}
			Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD);
			Paragraph titleParagraph = new Paragraph(title, font);
			titleParagraph.setSpacingBefore(2f);

			//----------------------------------------------------------------------------
			// Pied de tableau (zone gauche: surveillants, zone droite: remarques)
			//----------------------------------------------------------------------------
			PdfPCell tableFooterCell = null;
			if (null != se) {
				// S'il s'agit de l'émargement de plusieurs participants à une session unique
				List<TagChecker> tagCheckers = tagCheckerRepository
					.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
				tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
				String surveillants = tagCheckers.stream()
						.map(t -> (t.getUserApp().getPrenom() + "-" + t.getUserApp().getNom())).distinct()
						.collect(Collectors.joining(","));
		
				PdfPTable tagCheckerTable = new PdfPTable(1);
				tagCheckerTable.setWidthPercentage(100);
				String surveillantTerme = appliConfigService.getSurveillantTerm();
				tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell(surveillantTerme + "s :"));
				tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell(surveillants));
				PdfPCell summaryL = new PdfPCell(tagCheckerTable);
				summaryL.setPadding(1.0f);

				PdfPTable remarques = new PdfPTable(1);
				remarques.setWidthPercentage(100);
				remarques.addCell(pdfGenaratorUtil.getRemarquesCell("Remarques: " + se.getComment()));
				PdfPCell summaryR = new PdfPCell(remarques);
				
				PdfPTable tableFooterTable = new PdfPTable(2);
				tableFooterTable.setWidthPercentage(100);
				tableFooterTable.addCell(summaryL);
				tableFooterTable.addCell(summaryR);

				tableFooterCell = new PdfPCell(tableFooterTable);
				tableFooterCell.setColspan(displayedCols.size());
			}

			document.open();

			headerTable.setTotalWidth(300f);
			headerTable.writeSelectedRows(
				0,
				-1,
				document.right() - headerTable.getTotalWidth(),
				document.top(),
				writer.getDirectContent()
			);

			PdfPTable mainTable = new PdfPTable(displayedCols.size());
			mainTable.setWidthPercentage(100);

			// Détermination des largeurs de colonne du tableau principal
			//----------------------------------------------------------------
			float[] mainTableWidths;

			if (legacyDisplayConfig) {
				// Garder comme c'était avant
				mainTableHeaderFontSize = 11; // default
				mainTableFontSize       = 0; // default
				mainTableWidths         = new float[] { 0.7f, 1.5f, 1.5f, 2, 0.8f, 1.5f, 1.4f };
			} else {
				// Largeurs optimales par colonne, en % de la largeur du tableau
				HashMap<String, Float> colsOptimizedSize = new HashMap<String, Float>();
				if (isCompactMode) {
					// Avec fontSize 8 et padding 3 (mode compact)
					colsOptimizedSize.put(COL_LIGNE_NUM, 3.9f);        // 3 chiffres
					colsOptimizedSize.put(COL_PARTICIPANT_TYPE, 4.7f); // "E", "P", "Ex". Facteur limitant = entête de colonne "Type"
					colsOptimizedSize.put(COL_EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS, 9.5f); // Facteur limitant = entête de colonne "Emargement" ou raison d'absence
					colsOptimizedSize.put(COL_EMARGEMENT_MODE, 8f);  // Ex: "Manuel", "QrCode participant"
					colsOptimizedSize.put(COL_EMARGEMENT_MODE_OU_ABSENCE, 8.5f);  // Ex: "Manuel", "QrCode participant", "ABSENCE-INJUSTIFIE"
					colsOptimizedSize.put(COL_SESSION_DATE_HEURE_DEBUT, 11.9f);
					colsOptimizedSize.put(COL_SESSION_DUREE, 4.9f); // Facteur limitant = entête de colonne "Durée"
				} else {
					// Avec fontSize 10 et padding 5 (mode "historique")
					colsOptimizedSize.put(COL_LIGNE_NUM, 4.7f);        // 3 chiffres
					colsOptimizedSize.put(COL_PARTICIPANT_TYPE, 5.7f); // "E", "P", "Ex". Facteur limitant = entête de colonne "Type"
					colsOptimizedSize.put(COL_EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS, 12f);  // Facteur limitant = entête de colonne "Emargement" ou raison d'absence
					colsOptimizedSize.put(COL_EMARGEMENT_MODE, 9.8f);  // Ex: "Manuel", "QrCode participant", ..., "ABSENCE-INJUSTIFIE"
				}

				if (optimiserLargeurColIdentifiantSiUniquementTypeE && listWithOnlyTypeE) {
					// Optimisé pour 8 chiffres
					if (isCompactMode) {
						colsOptimizedSize.put(COL_PARTICIPANT_IDENTIFIANT, 7.5f);
					} else {
						colsOptimizedSize.put(COL_PARTICIPANT_IDENTIFIANT, 9.3f);
					}
				}

				HashMap<String, Float> colsWidthWeight = new HashMap<String, Float>();
				colsWidthWeight.put(COL_PARTICIPANT_NOM, 1f);
				colsWidthWeight.put(COL_PARTICIPANT_PRENOM, 1f);
				// Il faut une largeur plus grande pour un identifiant mail incluant nom, prénom et nom de domaine
				// plutôt que nom et prénom pris individuellement. On lui attribue donc un poids supérieur
				// Ca vaut ce que ça vaut (ça ne tient pas compte du contenu effectif des colonnes)
				// mais en attendant mieux ça ira
				colsWidthWeight.put(COL_PARTICIPANT_IDENTIFIANT, 1.5f);

				// On détermine l'espace occupé par les colonnes dont on connait la largeur optimale
				// (i.e plus petite possible tout en permettant d'afficher l'ensemble du contenu)
				// et on regarde combien il reste de colonnes qui doivent être les plus larges possibles
				mainTableWidths = new float[displayedCols.size()];
				float totalLargeursFixes = 0f;
				float poidsTotalColsLargeurAjustable = 0f;
				for (String colName : displayedCols.keySet()) {
					if (colsOptimizedSize.containsKey(colName)) {
						totalLargeursFixes += (float)colsOptimizedSize.get(colName);
					} else if (colsWidthWeight.containsKey(colName)) {
						poidsTotalColsLargeurAjustable += (float)colsWidthWeight.get(colName);
					} else {
						poidsTotalColsLargeurAjustable += 1f;
					}
				}

				// Pour les colonnes qui doivent être les plus larges possible, on va répartir
				// l'espace disponible restant avec la pondération prédéfinie
				// TODO Attention si la somme des colonnes fixes dépasse 100 !!
				int colIdx = 0;
				for (String colName : displayedCols.keySet()) {
					if (colsOptimizedSize.containsKey(colName)) {
						mainTableWidths[colIdx] = (float)colsOptimizedSize.get(colName);
					} else if (colsWidthWeight.containsKey(colName)) {
						mainTableWidths[colIdx] = (float)colsWidthWeight.get(colName)*(100f-totalLargeursFixes)/poidsTotalColsLargeurAjustable;
					} else {
						mainTableWidths[colIdx] = (float)(100f-totalLargeursFixes)/poidsTotalColsLargeurAjustable;
					}
					colIdx++;
				}
			}

			mainTable.setWidths(mainTableWidths);
			mainTable.setSpacingBefore(20.0f);

			for (String colName : displayedCols.keySet()) {
				mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell(displayedCols.get(colName), mainTableHeaderFontSize, mainTableHeaderPadding));
			}

			int pageCount = 0;

			pdfGenaratorUtil.addPageFooter(writer, document, pageCount+1, list.size(), nbLigneMaxParPage);

			document.add(image);
			document.add(titleParagraph);
			if (null != paragrapheNomGroupeUnique) {
				// REM: Dans le cas groupe = formation, on s'attendrait sans doute plutôt
				// à avoir formation AVANT nom du cours (+ lieu)
				document.add(paragrapheNomGroupeUnique);
			}
			
			if (!list.isEmpty()) {
				// Calculer la durée de chaque session (i.e. delta dateheure fin/dateheure debut)
				setNbHoursSession(list);
				int lineInPageCount = 0;
				int lineCount       = 1;
				for (TagCheck tc : list) {
					String heureEmargementOuAbsenceEtTiersTemps = "";
					String nom = "";
					String prenom = "";
					String identifiant = "";
					String typeEmargement = "";
					String typeEmargementOuAbsence = "";
					String typeIndividu = "";
					String groupes = "";
					// nom, prénom, identifiant, typeIndividu, groupes
					//----------------------
					if (tc.getPerson() != null) {
						nom = tc.getPerson().getNom();
						prenom = tc.getPerson().getPrenom();
						identifiant = tc.getPerson().getNumIdentifiant();
						if (identifiant == null) {
							identifiant = tc.getPerson().getEppn();
						}
						typeIndividu = messageSource
								.getMessage("person.type.".concat(tc.getPerson().getType()), null, null)
								.substring(0, 1);
						if (displayedCols.containsKey(COL_PARTICIPANT_GROUPES) && (tc.getPerson().getGroupes() != null)) {
							List<String> groupeList = tc.getPerson().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
							groupes = StringUtils.join(groupeList,", ");
						}
					} else if (tc.getGuest() != null) {
						nom = tc.getGuest().getNom();
						prenom = tc.getGuest().getPrenom();
						identifiant = tc.getGuest().getEmail();
						typeIndividu = "Ex";

						if (displayedCols.containsKey(COL_PARTICIPANT_GROUPES) && (tc.getGuest().getGroupes() != null)) {
							List<String> groupeList = tc.getGuest().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
							groupes = StringUtils.join(groupeList,", ");
						}
					}

					// heureEmargementOuAbsenceEtTiersTemps
					// typeEmargementOuAbsence
					//----------------------
					if (tc.getAbsence()!=null) {
						String absence = tc.getAbsence().getMotifAbsence().getTypeAbsence().name() + '-' + tc.getAbsence().getMotifAbsence().getStatutAbsence().name();
						heureEmargementOuAbsenceEtTiersTemps = absence;
						typeEmargementOuAbsence = absence;
					} else if (tc.getTagDate() != null) {
						heureEmargementOuAbsenceEtTiersTemps = String.format("%1$tH:%1$tM", tc.getTagDate());
						if (tc.getIsUnknown()) {
							heureEmargementOuAbsenceEtTiersTemps = "Inconnu";
						}
					}
					if (tc.getIsTiersTemps()) {
						heureEmargementOuAbsenceEtTiersTemps += " \nTemps aménagé";
					}

					// typeEmargement
					// typeEmargementOuAbsence
					//----------------------
					if (tc.getTypeEmargement() != null) {
						typeEmargement = messageSource.getMessage(
							"typeEmargement.".concat(tc.getTypeEmargement().name().toLowerCase()),
							null,
							null
						) + "\n";
						typeEmargementOuAbsence = typeEmargement;
					}

					typeEmargement += (tc.getProxyPerson()!=null)? "Proc : " + tc.getProxyPerson().getPrenom() + ' ' + tc.getProxyPerson().getNom(): "";

					for (String colName : displayedCols.keySet()) {
						String cellContent = "???"+colName+"???";
						int align = Element.ALIGN_CENTER;
						SessionEpreuve lineSessionEpreuve;
						if (null != person) {
							lineSessionEpreuve = tc.getSessionEpreuve();
						} else {
							lineSessionEpreuve = se;
						}		
						switch (colName) {
							case COL_EMARGEMENT_HEURE_OU_ABSENCE_ET_TIERS_TEMPS:
								cellContent = heureEmargementOuAbsenceEtTiersTemps;
								break;
							case COL_EMARGEMENT_MODE:
								cellContent = typeEmargement;
								break;
							case COL_EMARGEMENT_MODE_OU_ABSENCE:
								cellContent = typeEmargementOuAbsence;
								break;
							case COL_LIGNE_NUM:
								cellContent = String.valueOf(lineCount);
								break;
							case COL_PARTICIPANT_GROUPES:
								cellContent = groupes;
								break;
							case COL_PARTICIPANT_IDENTIFIANT:
								cellContent = identifiant;
								break;
							case COL_PARTICIPANT_NOM:
								cellContent = nom;
								if (!legacyDisplayConfig) {
									align = Element.ALIGN_LEFT;
								}
								break;
							case COL_PARTICIPANT_PRENOM:
								cellContent = prenom;
								if (!legacyDisplayConfig) {
									align = Element.ALIGN_LEFT;
								}
								break;
							case COL_PARTICIPANT_TYPE:
								cellContent = typeIndividu;
								break;
							case COL_SESSION_NOM:
								// Peu d'intérêt dans le cas d'une liste de participants à une session unique
								// mais adapté au cas liste d'émargements d'un participant à plusieurs sessions
								cellContent = lineSessionEpreuve.getNomSessionEpreuve();
								align = Element.ALIGN_LEFT;
								break;
							case COL_SESSION_DATE_HEURE_DEBUT:
								// Peu d'intérêt dans le cas d'une liste de participants à une session unique
								// mais adapté au cas liste d'émargements d'un participant à plusieurs sessions
								String dateDebutSession  = String.format("%1$td-%1$tm-%1$tY", lineSessionEpreuve.getDateExamen());
								String heureDebutSession = String.format("%1$tH:%1$tM", lineSessionEpreuve.getHeureEpreuve());
								cellContent = dateDebutSession+" "+heureDebutSession;
								break;
							case COL_SESSION_DUREE:
								// Peu d'intérêt dans le cas d'une liste de participants à une session unique
								// mais adapté au cas liste d'émargements d'un participant à plusieurs sessions
								cellContent = lineSessionEpreuve.getNbHours();
								break;
							case COL_SESSION_SURVEILLANTS:
								// Peu d'intérêt dans le cas d'une liste de participants à une session unique
								// mais adapté au cas liste d'émargements d'un participant à plusieurs sessions
								List<TagChecker> tagCheckers = tagCheckerRepository
									.findTagCheckerBySessionLocationSessionEpreuveId(lineSessionEpreuve.getId());
								tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
								cellContent = tagCheckers.stream()
									.map(t -> (t.getUserApp().getPrenom() + " " + t.getUserApp().getNom().toUpperCase())).distinct()
									.collect(Collectors.joining(","));
								align = Element.ALIGN_LEFT;
								break;
							default:
								cellContent = "???"+colName+"???";
								break;
						}

						mainTable.addCell(
							pdfGenaratorUtil.getMainRowCell(
								cellContent,
								mainTableFontSize,
								mainTablePadding,
								align,
								// S'il n'y a pas de pied de tableau et que c'est la dernière ligne
								// du tableau (dans l'absolu ou dernière de la page)
								// alors il faut tracer le bord inférieur
								((null == tableFooterCell) && ((lineCount == list.size() || (lineInPageCount-1 == nbLigneMaxParPage))))?1:0
							)
						);
					}

					lineInPageCount++;
					if ((lineInPageCount == nbLigneMaxParPage) && (lineCount < list.size())) {
						if (null != tableFooterCell) {
							mainTable.addCell(tableFooterCell);
						}
						document.add(mainTable);

						// Préparation d'une nouvelle page
						document.newPage();
						pageCount++;

						pdfGenaratorUtil.addPageFooter(writer, document, pageCount+1, list.size(), nbLigneMaxParPage);

						document.add(image);
						headerTable.setTotalWidth(300f);
						headerTable.writeSelectedRows(0,
							-1,
							document.right() - headerTable.getTotalWidth(),
							document.top(),
							writer.getDirectContent()
						);
						document.add(titleParagraph);
						mainTable = new PdfPTable(displayedCols.size());
						mainTable.setWidthPercentage(100);
						mainTable.setWidths(mainTableWidths);
						mainTable.setSpacingBefore(20.0f);
						for (String colName : displayedCols.keySet()) {
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell(displayedCols.get(colName), mainTableHeaderFontSize, mainTableHeaderPadding));
						}			
						lineInPageCount = 0;
					}
					lineCount++;
				}
			}

			if (null != tableFooterCell) {
				mainTable.addCell(tableFooterCell);
			}
			document.add(mainTable);
			logService.log(ACTION.EXPORT_PDF, RETCODE.SUCCESS, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);

		} catch (DocumentException de) {
			de.printStackTrace();
			logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
		}
	}

	private String formatDate(Date date) {
	    return String.format("%1$td-%1$tm-%1$tY", date);
	}
	
	private String formatTime(Date date) {
	    return String.format("%1$tH:%1$tM:%1$tS", date);
	}
	
	public String getContextWs(EsupNfcTagLog esupNfcTagLog) throws ParseException {
		String key = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		String locationNom = esupNfcTagLog.getLocation();
		String[] splitLocationNom = locationNom.split(" // ");
		String nomSalle = splitLocationNom[2];
		String idSession = splitLocationNom[3];
		Long id = Long.valueOf(idSession);
		String eppn = esupNfcTagLog.getEppn();
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
		Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
		if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
			key = tagCheckRepository.getContextId(nomSalle, eppn, date, id);
		}else {
			key = tagCheckRepository.getContextIdWithDateFin(nomSalle, eppn, date, dateFin, id);
		}
		
		if(key == null) {
			Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date, id);
			SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
			
			key = tagCheckRepository.getContextIdBySeId(sl.getSessionEpreuve().getId(), eppn, date);
		}
		
		return key;
	}
	
	public List<Location> searchRepartition(Long id){
		
		List<Location> searchRepartition = new ArrayList<>();
		
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
		Long count = 0L;
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
			String loginArchivage = auth.getName();
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
				tc.getSessionEpreuve().setStatutSession(statutSessionRepository.findByKey("CLOSED"));
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
		String typeEmargement = null;
		Date tagDate = null;
		if(isPresent) {
			SessionEpreuve se = tc.getSessionEpreuve();
			if(se.getIsSecondTag()!=null && se.getIsSecondTag()) {
				typeEmargement= tc.getTypeEmargement2().name();
				tagDate = tc.getTagDate2();
	    	}else {
	    		typeEmargement= tc.getTypeEmargement().name();
	    		tagDate = tc.getTagDate();
	    	}
			if(typeEmargement.startsWith(TypeEmargement.QRCODE.name())) {
				if(tagDate == null) {
					nbBadgeage = previousNb + 1;
				}else {
					Date newDate = new Date();
					Long seconds = ChronoUnit.SECONDS.between(tagDate.toInstant(), newDate.toInstant());
					if(seconds>60) {
						nbBadgeage = previousNb + 1;
					}
				}
			}else {
				nbBadgeage = previousNb + 1;
			}
		}else {
			if(previousNb > 0) {
				nbBadgeage = previousNb - 1;
			}
		}
		return nbBadgeage;
	}
	
	public List<TagCheckBean> getBeansFromTagChecks(List<TagCheck> tagChecks){
		
		List<TagCheckBean> beans = new ArrayList<>();
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm");  
		
		if(!tagChecks.isEmpty()) {
			setNomPrenomTagChecks(tagChecks, true, true);
			for(TagCheck tc : tagChecks) {
				TagCheckBean bean = new TagCheckBean();
				bean.setCodeEtape(StringUtils.defaultString(tc.getCodeEtape(), ""));
				bean.setComentaire(StringUtils.defaultString(tc.getComment(), ""));
				String strDateEmargment = (tc.getTagDate() != null)? dateFormat.format(tc.getTagDate()) : "";
				String strDateConvocation = (tc.getDateEnvoiConvocation() != null)? dateFormat.format(tc.getDateEnvoiConvocation()) : "";
				bean.setDateEmargement(strDateEmargment);
				bean.setDateEnvoiConvocation(strDateConvocation);
    			String absence = tc.getAbsence()!=null? tc.getAbsence().getMotifAbsence().getTypeAbsence().name() + '-' 
    					+ tc.getAbsence().getMotifAbsence().getStatutAbsence().name() : "";
				bean.setAbsence(absence);
				bean.setEstInconnu(BooleanUtils.toString(tc.getIsUnknown(), "Oui", "Non"));
				bean.setLieuAttendu((tc.getSessionLocationExpected()!=null)? 
						tc.getSessionLocationExpected().getLocation().getNom() :"");
				bean.setLieuBadge((tc.getSessionLocationBadged()!=null)? tc.getSessionLocationBadged().getLocation().getNom() : "");
				bean.setNom((tc.getPerson()!=null)? tc.getPerson().getNom() : tc.getGuest().getNom());
				bean.setPrenom((tc.getPerson()!=null)? tc.getPerson().getPrenom() : tc.getGuest().getPrenom());
				String procuration = "";
				if(tc.getProxyPerson()!=null){
					String nom = tc.getProxyPerson().getNom();
					if(nom.isEmpty()) {
						procuration = tc.getProxyPerson().getEppn();
					}else {
						procuration = tc.getProxyPerson().getPrenom().concat(" ").concat(nom);
					}
				}	
				bean.setProcuration(procuration);
				String surveillant = "";
				if(tc.getTagChecker()!=null){
					String nom = tc.getTagChecker().getUserApp().getNom();
					if(nom.isEmpty()) {
						surveillant = tc.getTagChecker().getUserApp().getEppn();
					}else {
						surveillant = tc.getTagChecker().getUserApp().getPrenom().concat(" ").concat(nom);
					}
				}
				bean.setSurveillant(surveillant);
				bean.setTypeEmargement(getTypeEmargement(tc));
				bean.setTypeIndividu(getTypeIndividu(tc));
				beans.add(bean);
			}
		}
		
		return beans;
	}
	
	public String getTypeIndividu(TagCheck tc) {
		if(tc.getPerson()!=null) {
			return  messageSource.getMessage("person.type.".concat(tc.getPerson().getType()), null, null);
		}
		return "Externe";
	}
	public String getTypeEmargement(TagCheck tc) {
		String typeEmargement = "";
		if(tc.getTypeEmargement()!=null) {
			typeEmargement = messageSource.getMessage("typeEmargement.".concat( tc.getTypeEmargement().name().
					toLowerCase()), null, null);
		}
		return typeEmargement;
	}
	
	public List<AssiduiteBean> setListAssiduiteBean(List<TagCheck> pTagChecks, String anneeUniv) {
		
		List<AssiduiteBean> list = new ArrayList<>();
		Map<String, String> mapTypes = new HashMap<>();
		
		//Annees
		Set<String> annees = null;
		if(anneeUniv == null || anneeUniv.isEmpty()) {
			annees = pTagChecks.stream().map(p -> p.getSessionEpreuve().getAnneeUniv()).collect(Collectors.toSet());
		}else {
			annees = new HashSet<>();
			annees.add(anneeUniv);
		}
		
		//Type sessions
		Set<String> tcSessions = pTagChecks.stream().map(p -> p.getSessionEpreuve().getTypeSession().getKey()).collect(Collectors.toSet());
		
		for(String annee : annees) {
			long countPresent = pTagChecks
					  .stream()
					  .filter(c -> c.getSessionLocationBadged() != null && annee.equals(c.getSessionEpreuve().getAnneeUniv()))
					  .count();
			
			for(String type : tcSessions) {
				long countPresentNotnull = pTagChecks
						  .stream()
						  .filter(c -> c.getSessionLocationBadged() != null && annee.equals(c.getSessionEpreuve().getAnneeUniv())
								  && type.equals(c.getSessionEpreuve().getTypeSession().getKey()))
						  .count();
				long countPresentNull = pTagChecks
						  .stream()
						  .filter(c -> c.getSessionLocationBadged() == null && annee.equals(c.getSessionEpreuve().getAnneeUniv())
								  && type.equals(c.getSessionEpreuve().getTypeSession().getKey()))
						  .count();				
				
				float percent2 = Float.valueOf(countPresentNotnull) / Float.valueOf(countPresentNotnull + countPresentNull) * 100;
				mapTypes.put(type, String.format("%.2f", percent2) + " %");
			}
			
			AssiduiteBean assiduiteBean = new AssiduiteBean();
			assiduiteBean.setDetailPerence(mapTypes);
			assiduiteBean.setAnneeUniv(annee);
			long totalSession = pTagChecks.stream().count();
			assiduiteBean.setTotalSession(totalSession);
			assiduiteBean.setNbPresent(countPresent);
			float percent = Float.valueOf(countPresent) / Float.valueOf(totalSession) * 100;
			assiduiteBean.setPercentPresent(String.format("%.2f", percent));
			list.add(assiduiteBean);
		}

		return list;
	}
	
	public byte[]  getAttestationPresence(Long id, HttpServletResponse response, String emargementContext, boolean signature) {
		byte[] pdfBytes = null;
		Document document = new Document();
	    document.setMargins(30, 30, 30, 30);
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    TagCheck tc = tagCheckRepository.findById(id).get();
	    List<TagCheck> tcs = new ArrayList<>();
	    tcs.add(tc);
	    setNomPrenomTagChecks(tcs, false, false);
	    SessionEpreuve se = tc.getSessionEpreuve();
	    String nomFichier = "attestation_ " + tc.getNomPrenom() + "_" 
	    		+ se.getNomSessionEpreuve().replace("", "_") + ".pdf";
	    try 
	    {
			if (signature) {
				PdfWriter.getInstance(document, byteArrayOutputStream);
			} else {
				response.setContentType("application/pdf");
				response.setHeader("Content-Disposition", "attachment; filename=".concat(nomFichier));
				PdfWriter.getInstance(document, response.getOutputStream());
			}
				String title = "ATTESTATION DE PRESENCE";

				Image image = Image.getInstance(PresenceService.class.getResource("/static/images/logo.jpg"));
				image.scaleAbsolute(150f, 50f);// image width,height

				Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 20, Font.NORMAL);
				Font mainFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, Font.NORMAL);
				
				Paragraph name = new Paragraph(title, font);
			//	name.setAlignment(Element.ALIGN_CENTER);
				name.setSpacingBefore(30f);
				
				LineSeparator sep = new LineSeparator();
				BaseColor linecolor = new BaseColor(118,78,22, 50);
				sep.setLineColor(linecolor);
				sep.setOffset(-4);
				
				String text = replaceFields(appliConfigService.getAttestationTexte(), tc) ;
				
				Paragraph mainText = new Paragraph(text, mainFont);
				mainText.setAlignment(Element.ALIGN_CENTER);
				mainText.setSpacingBefore(100f);
				mainText.setSpacingAfter(250f);
				
				PdfPTable describer = new PdfPTable(1);
				describer.setWidthPercentage(100);
				describer.addCell(pdfGenaratorUtil.getDescCell(" "));
				describer.addCell(pdfGenaratorUtil.getDescCell("Esup-emargement - " + Year.now().getValue()));

				document.open();

				document.add(image);
				document.add(name);
				document.add(sep);
				document.add(mainText);
				document.add(describer);
				logService.log(ACTION.EXPORT_PDF, RETCODE.SUCCESS, "Extraction pdf :", null, null, emargementContext,
						null);

			} catch (DocumentException de) {
				de.printStackTrace();
				logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :", null, null, emargementContext,
						null);
			} catch (IOException de) {
				de.printStackTrace();
				logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :", null, null, emargementContext,
						null);
			}
	    	document.close();
			if (signature) {
				pdfBytes = byteArrayOutputStream.toByteArray();
			}

			return pdfBytes;
	}
	
	public void setNbHoursSession(List<TagCheck> tcs) {
		for(TagCheck tc : tcs) {
			SessionEpreuve se = tc.getSessionEpreuve();
			long diffInMillis = se.getFinEpreuve().getTime() - se.getHeureEpreuve().getTime();
			se.setNbHours(formatTime(diffInMillis));
		}
	}
	
	public String formatTime(long diffInMillis) {
	    long hoursBetween = TimeUnit.MILLISECONDS.toHours(diffInMillis);
	    long minutesBetween = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
		return String.format("%02d:%02d", hoursBetween, minutesBetween);
	}

    public Map<String, String> getPersonWithTotalDuration(List<TagCheck> tcs) {
        Map<String, Long> result = new HashMap<>();
        for (TagCheck tc : tcs) {
            Person person = tc.getPerson();
            String eppn = person.getEppn();
            SessionEpreuve sessionEpreuve = tc.getSessionEpreuve();
            if (sessionEpreuve != null && !"CANCELLED".equals(sessionEpreuve.getStatutSession().getKey())) {
                Date heureDebut = sessionEpreuve.getHeureEpreuve();
                Date heureFin = sessionEpreuve.getFinEpreuve();
                if (heureDebut != null && heureFin != null) {
                    long durationMillis = heureFin.getTime() - heureDebut.getTime();
                    long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
                    result.put(eppn, result.getOrDefault(eppn, 0L) + durationMinutes);
                }
            }
        }
        Map<String, String> stringMap = result.entrySet() .stream() .collect(Collectors.toMap( 
        		Map.Entry::getKey,  entry -> toolUtil.getDureeEpreuve(null, null, (entry.getValue()))));

        return stringMap;
    }
    
    private LocalDate convertToLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate();
    }

    public Map<String, Long> getPersonWithTotalDays(List<TagCheck> tcs) {
        Map<String, Long> result = new HashMap<>();
        for (TagCheck tc : tcs) {
            Person person = tc.getPerson();
            String eppn = person.getEppn();
            SessionEpreuve sessionEpreuve = tc.getSessionEpreuve();
            long totalDaysForTagCheck = 0;
            if (sessionEpreuve != null && !"CANCELLED".equals(sessionEpreuve.getStatutSession().getKey())) {
                Date startDate = sessionEpreuve.getDateExamen();
                Date endDate = sessionEpreuve.getDateFin();
                if(endDate == null) {
                	endDate = startDate;
                }
                if (startDate != null && endDate != null) {
                    LocalDate startLocalDate = convertToLocalDate(startDate);
                    LocalDate endLocalDate = convertToLocalDate(endDate);
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, endLocalDate);
                    if (daysBetween == 0) {
                        daysBetween = 1;
                    }
                    totalDaysForTagCheck += daysBetween;
                    result.put(eppn, result.getOrDefault(eppn, 0L) + totalDaysForTagCheck);
                }
            }
        }
        return result;
    }

    public Map<String, Long> getPersonWithTotalSessionCount(List<TagCheck> tcs) {
        Map<String, Long> result = new HashMap<>();
        for (TagCheck tc : tcs) {
            Person person = tc.getPerson();
            String eppn = person.getEppn();
            SessionEpreuve sessionEpreuve = tc.getSessionEpreuve();
            if (sessionEpreuve != null && !"CANCELLED".equals(sessionEpreuve.getStatutSession().getKey())) {
            	long sessionCountForTagCheck = 1;
                result.put(eppn, result.getOrDefault(eppn, 0L) + sessionCountForTagCheck);
            }
        }
        return result;
    }
    
    public void deleteAbsence (Absence absence) {
    	List<TagCheck> tagChecks = tagCheckRepository.findByAbsence(absence);
    	if(!tagChecks.isEmpty()) {
    		for(TagCheck tc : tagChecks) {
    			tc.setAbsence(null);
    			tagCheckRepository.save(tc);
    		}
    	}
    }
}
