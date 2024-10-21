package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AssiduiteBean;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagCheckBean;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.security.ContextHelper;
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
	ContextRepository contextRepository;
	
	@Autowired
    LdapUserRepository ldapUserRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
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
    
    public List<Integer> importTagCheckCsv(Reader reader,  List<List<String>> finalList, Long sessionEpreuveId, 
    		String emargementContext, Map<String,String> mapEtapes, Boolean checkLdap, Long sessionLocationId) throws Exception {
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
			    			if(line.contains(",")) {
			    				splitLine = line.split(",");
			    			}else if(line.contains(";")) {
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
												person.setType("student");
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
		
		personService.deleteUnusedPersons(contextRepository.findByContextKey(ContextHelper.getCurrentContext()));
    }
    
	public int setNomPrenomTagChecks(List<TagCheck> tagChecks, boolean setTagChecker, boolean setProxy){
		
		int count = 0;
		
		if(!tagChecks.isEmpty()) {
			// récupération des informations du LDAP pour tous les inscrits
			List<String> eppnList = tagChecks.stream().filter(tagCheck -> tagCheck.getPerson()!=null)
					.map(tagCheck -> tagCheck.getPerson().getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(eppnList, "eduPersonPrincipalName");

			Map<String, LdapUser> mapTagCheckerLdapUsers = new HashMap<>();
			if(setTagChecker) {
				// récupération des informations du LDAP pour tous les surveillants
				List<String> tagCheckerList = tagChecks.stream().filter(tagCheck -> tagCheck.getTagChecker() != null)
						.map(tagCheck -> tagCheck.getTagChecker().getUserApp().getEppn())
						.collect(Collectors.toList());
				mapTagCheckerLdapUsers = ldapService.getLdapUsersFromNumList(tagCheckerList, "eduPersonPrincipalName");
			}

			Map<String, LdapUser> mapTcProxyLdapUsers = new HashMap<>();
			if(setProxy) {
				// récupération des informations du LDAP pour toutes les procurations
				List<String> tcProxyList = tagChecks.stream().filter(tagCheck -> tagCheck.getProxyPerson() != null)
						.map(tagCheck -> tagCheck.getProxyPerson().getEppn())
						.collect(Collectors.toList());
				mapTcProxyLdapUsers = ldapService.getLdapUsersFromNumList(tcProxyList, "eduPersonPrincipalName");
			}

			for(TagCheck tc : tagChecks) {
				if(tc.getPerson()!=null) {
					LdapUser ldapUser = mapLdapUsers.get(tc.getPerson().getEppn());
					if(ldapUser!=null) {
						tc.getPerson().setCivilite(ldapUser.getCivilite());
						tc.getPerson().setNom(ldapUser.getName());
						tc.getPerson().setPrenom(ldapUser.getPrenom());
						tc.setNomPrenom(ldapUser.getName().concat(ldapUser.getPrenom()));
					}else {
						tc.setNomPrenom("");
					}
				}else if(tc.getGuest()!=null) {
					tc.setNomPrenom(tc.getGuest().getNom().concat(tc.getGuest().getPrenom()));
				}

				if(setTagChecker && tc.getTagChecker()!=null) {
					tc.getTagChecker().getUserApp().setNom("");
					tc.getTagChecker().getUserApp().setPrenom("");
					if(!mapTagCheckerLdapUsers.isEmpty()) {
						LdapUser ldapUser = mapTagCheckerLdapUsers.get(tc.getTagChecker().getUserApp().getEppn());
						if (ldapUser!=null) {
							tc.getTagChecker().getUserApp().setNom(ldapUser.getName());
							tc.getTagChecker().getUserApp().setPrenom(ldapUser.getPrenom());
						}
					}
				}

				if(setProxy && tc.getProxyPerson()!=null) {
					tc.getProxyPerson().setNom("");
					tc.getProxyPerson().setPrenom("");
					if(!mapTcProxyLdapUsers.isEmpty()) {
						LdapUser ldapUser = mapTcProxyLdapUsers.get(tc.getProxyPerson().getEppn());
						if (ldapUser!=null) {
							tc.getProxyPerson().setNom(ldapUser.getName());
							tc.getProxyPerson().setPrenom(ldapUser.getPrenom());
						}
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
		finalString = htmltemplate.replace(wrapChar.concat("civilite").concat(wrapChar), civilite);
		finalString = finalString.replace(wrapChar.concat("prenom").concat(wrapChar), prenom);
		finalString = finalString.replace(wrapChar.concat("nom").concat(wrapChar), nom);
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
						emailService.sendMessageWithAttachment(email, subject, bodyMsg, filePath, "convocation.pdf", ccArray, null);
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
				Context ctx = sessionEpreuve.getContext();
				if(sessionEpreuve.getIsSessionLibre()) {
					isSessionLibre = true;
					sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), nomSalle);
					tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(esupNfcTagLog.getEppnInit(), null).getContent().get(0);
				}
				
				if (tc ==1 || isSessionLibre) {
					Long totalPresent = tagCheckRepository.countTagCheckBySessionLocationExpected (sessionLocationBadged);
					String msgError = "";
					TagCheck newTagCheck = new TagCheck();
					Long count = 0L;
					if(isSessionLibre) {
						try {
							if(totalPresent >= sessionLocationBadged.getCapacite()) {
								TagCheck tagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationBadged.getId(), eppn);
								if(tagCheck != null) {
									saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, TypeEmargement.CARD);
									isOk = true;
								}else {
									isOk = false;
								}
							}else {
								if(!isBlackListed) {
									newTagCheck = saveUnknownTagCheck(null, ctx, eppn, sessionEpreuve, sessionLocationBadged, tagChecker,isSessionLibre, TypeEmargement.CARD);
									newTagCheck.setIsBlacklisted(false);
									count = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationBadged.getId());
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
				}else {
					try {
						String comment = "";
						String eppnInit = esupNfcTagLog.getEppnInit();
						tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppnInit, null).getContent().get(0);
						sessionLocationBadged = sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndLocationNom(sessionEpreuve.getId(), nomSalle);
						//on regarde si la personne est dans une autre salle de la session
						Long sessionLocationId = tagCheckRepository.getSessionLocationIdExpected(eppn, date, id);
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
							//comparaison avec les heures de début et de fin
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
				    	presentTagCheck.setTagDate(new Date());
				    	presentTagCheck.setTypeEmargement(TypeEmargement.CARD);
						TagChecker tagChecker = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(sl, esupNfcTagLog.getEppnInit());
				    	presentTagCheck.setTagChecker(tagChecker);
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
			if(!typeEmargement.name().startsWith(TypeEmargement.QRCODE.name()) || typeEmargement.name().startsWith(TypeEmargement.QRCODE.name()) && personRepository.findByContextAndEppn(ctx, eppn)!=null ) {
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
			Person p = null;
			if(typeEmargement.name().startsWith(TypeEmargement.QRCODE.name())){
				p = personRepository.findByContextAndEppn(ctx, eppn);
			}else {
				p = personRepository.findByEppnAndContext(eppn, ctx);
			}
			if(p == null) {
				p = new Person();
				p.setContext(ctx);
				p.setEppn(eppn);
				personRepository.save(p);
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
		unknownTc.setTagChecker(tagChecker);
		unknownTc.setTagDate(new Date());
		unknownTc.setTypeEmargement(typeEmargement);
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
			
		}    
		else if ("PDF".equals(type)) {
	        Document document = new Document();
	        document.setMargins(10, 10, 10, 10);
	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        PdfWriter writer = null;
	        try 
	        {
				if (signature) {
					writer = PdfWriter.getInstance(document, byteArrayOutputStream);
				} else {
					response.setContentType("application/pdf");
					response.setHeader("Content-Disposition", "attachment; filename=".concat(nomFichier));
					writer = PdfWriter.getInstance(document, response.getOutputStream());
				}

				String dateFin = (se.getDateFin() != null) ? "\n" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin()))
						: "";
				String date = String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).concat("\n"  + dateFin);
				String heures = String.format("%1$tH:%1$tM", se.getHeureEpreuve()) + " - "
						+ String.format("%1$tH:%1$tM", se.getFinEpreuve());
				Long totalExpected = tagCheckRepository
						.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(se.getId());
				Long totalPresent = tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNull(se.getId());
				String total = String.valueOf(totalPresent) + " / " + String.valueOf(totalExpected);
				String title = se.getNomSessionEpreuve();
				List<TagChecker> tagCheckers = tagCheckerRepository
						.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
				tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
				String surveillants = tagCheckers.stream()
						.map(t -> (t.getUserApp().getPrenom() + "-" + t.getUserApp().getNom())).distinct()
						.collect(Collectors.joining(","));

				Image image = Image.getInstance(PresenceService.class.getResource("/static/images/logo.jpg"));
				image.scaleAbsolute(150f, 50f);// image width,height

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
				PdfPCell summaryL = new PdfPCell(tagCheckerTable);
				summaryL.setColspan(4);
				summaryL.setPadding(1.0f);

				PdfPTable remarques = new PdfPTable(1);
				remarques.setWidthPercentage(100);
				remarques.addCell(pdfGenaratorUtil.getRemarquesCell("Remarques: " + se.getComment()));
				PdfPCell summaryR = new PdfPCell(remarques);
				summaryR.setColspan(3);

				PdfPTable describer = new PdfPTable(1);
				describer.setWidthPercentage(100);
				describer.addCell(pdfGenaratorUtil.getDescCell(" "));
				describer.addCell(pdfGenaratorUtil.getDescCell("Esup-emargement - " + Year.now().getValue()));

				document.open();
				headerTable.setTotalWidth(300f);
				headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(), document.top(),
						writer.getDirectContent());

				PdfPTable mainTable = new PdfPTable(7);
				mainTable.setWidthPercentage(100);
				mainTable.setWidths(new float[] { 0.7f, 1.5f, 1.5f, 2, 0.8f, 1.5f, 1.4f });
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

				if (!list.isEmpty()) {
					int i = 0;
					int j = 1;
					for (TagCheck tc : list) {
						String dateEmargement = "";
						String nom = "";
						String prenom = "";
						String identifiant = "";
						String typeemargement = "";
						String typeIndividu = "";
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
						} else if (tc.getGuest() != null) {
							nom = tc.getGuest().getNom();
							prenom = tc.getGuest().getPrenom();
							identifiant = tc.getGuest().getEmail();
							typeIndividu = "Ex";
						}
						if (BooleanUtils.isTrue(tc.getIsExempt())) {
							dateEmargement = "Exempt";
						} else if (tc.getTagDate() != null) {
							dateEmargement = String.format("%1$tH:%1$tM", tc.getTagDate());
							if (tc.getIsUnknown()) {
								dateEmargement = "Inconnu";
							}
						}
						if (tc.getIsTiersTemps()) {
							dateEmargement += " \nTemps aménagé";
						}
						if (tc.getTypeEmargement() != null) {
							typeemargement = messageSource.getMessage(
									"typeEmargement.".concat(tc.getTypeEmargement().name().toLowerCase()), null, null) + "\n";;
						}
						typeemargement += (tc.getProxyPerson()!=null)? "Proc : " + tc.getProxyPerson().getPrenom() + ' ' + tc.getProxyPerson().getNom(): "";;
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
							headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(),
									document.top(), writer.getDirectContent());
							document.add(name);
							mainTable = new PdfPTable(7);
							mainTable.setWidthPercentage(100);
							mainTable.setWidths(new float[] { 0.7f, 1.5f, 1.5f, 2, 0.8f, 1.5f, 1.4f });
							mainTable.setSpacingBefore(20.0f);
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("#"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Nom"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Prénom"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Identifiant"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Type"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Emargement"));
							mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Mode"));
							i = 0;
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
	        
	        document.close();
	        if(signature) {
	        	pdfBytes = byteArrayOutputStream.toByteArray();
	        }
	       
		}else if("CSV".equals(type)){
			try {
				
				String filename =  nomFichier.concat(".csv");

				response.setContentType("text/csv");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				        "attachment; filename=\"" + filename + "\"");
				response.setCharacterEncoding("UTF-8");

				//create a csv writer
				CSVWriter writer = new CSVWriter(response.getWriter());
				String [] headers = {"Session", "Date", "Numéro Etu", "Eppn", "Nom", "Prénom", "Présent", "Emargement", "Type", "Lieu attendu", "Lieu badgé", "Exempt", "Tiers-temps"};
				writer.writeNext(headers);
				for(TagCheck tc : list) {
					List <String> line = new ArrayList<>();
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
		
		return pdfBytes;
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
				tc.getSessionEpreuve().setStatut(Statut.CLOSED);
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
			if(tc.getTypeEmargement().name().startsWith(TypeEmargement.QRCODE.name())) {
				if(tc.getTagDate() == null) {
					nbBadgeage = previousNb + 1;
				}else {
					Date newDate = new Date();
					Long seconds = ChronoUnit.SECONDS.between(tc.getTagDate().toInstant(), newDate.toInstant());
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
				bean.setEstExempte(BooleanUtils.toString(tc.getIsExempt(), "Oui", "Non"));
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
}
