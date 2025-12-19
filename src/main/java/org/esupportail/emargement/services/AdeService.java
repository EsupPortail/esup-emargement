package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeInstructorBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.TypeSession;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.exceptions.AdeApiRequestException;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

@Service
public class AdeService {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final static String ADE_STORED_PROJET = "adeStoredProjet";

	public final static String ADE_STORED_COMPOSANTE = "adeStoredComposante";
	
	@Autowired
	private AdeApiService adeApiService;

	@Autowired
	private PrefsRepository prefsRepository;
	
	@Autowired
	private LocationRepository locationRepository;
	
	@Autowired	
	private SessionLocationRepository sessionLocationRepository;

	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	private TypeSessionRepository typeSessionRepository;
	
	@Autowired
	private ContextRepository contextRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private LdapUserRepository ldapUserRepository;
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	private UserAppRepository userAppRepository;
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
    @Autowired
    private GroupeRepository groupeRepository;

    @Resource 
    private PreferencesService preferencesService;
    
    @Resource 
    private SessionEpreuveService sessionEpreuveService;

    @Resource    
    private DataEmitterService dataEmitterService;

    @Resource
    private AppliConfigService appliConfigService;
    
	@Resource
	private LogService logService;
	
    @Resource 
    private LdapService ldapService;
    
    @Resource 
    private GroupeService groupeService;

	public List<AdeClassroomBean> getListClassrooms(String sessionId, String idItem, List<Long> selectedIds, Context ctx)  throws  ParseException {
		return adeApiService.getListClassrooms(sessionId, idItem, selectedIds, ctx);
	}
	
	public List<AdeClassroomBean> getListClassrooms2(String sessionId, String idItem, List<Long> selectedIds)  throws  ParseException {
		return adeApiService.getListClassrooms2(sessionId, idItem, selectedIds);
	}

	public Map<String,String> getItemsFromInstructors(String sessionId, String choice) {
		return adeApiService.getItemsFromInstructors(sessionId, choice);
	}
	
	public Map<String, String> getClassroomsList(String sessionId) {
		return adeApiService.getClassroomsList(sessionId);
	}

	public boolean isResourceFolder(String sessionId, String resourceId, Context ctx) throws Exception {
		return adeApiService.isResourceFolder(sessionId, resourceId, ctx);
	}

	public Map<Long, String> getResourceLeavesIdNameMap(String sessionId, String resourceId, Context ctx) throws Exception {
		return adeApiService.getResourceLeavesIdNameMap(sessionId, resourceId, ctx);
	}

    public Map<String, String> getMapComposantesFormations(String sessionId, String category) {
		return adeApiService.getMapComposantesFormations(sessionId, category);
    }

	public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, boolean update, Context ctx) throws IOException, ParseException {
		return adeApiService.getEventsFromXml(sessionId, resourceId, strDateMin, strDateMax, idEvents, existingSe, update, ctx);
	}
	
	public void checkEvents(Context context, Date startOfDay) {
		
		List<SessionEpreuve> ses = sessionEpreuveRepository
		        .findByContextAndDateCreationLessThan(context, startOfDay);

		// Récupération des TagCheck associés
		List<TagCheck> tcs = tagCheckRepository
		        .findTagCheckBySessionEpreuveIn(ses);

		// Sessions à exclure car elles ont un TagCheck avec absence ou date
		Set<Long> sessionsAvecAbsOuDate = tcs.stream()
		        .filter(tc -> tc.getAbsence() != null || tc.getTagDate() != null)
		        .map(tc -> tc.getSessionEpreuve().getId())
		        .collect(Collectors.toSet());

		List<SessionEpreuve> finalList = new ArrayList<>();

		for (SessionEpreuve se : ses) {
		    if (!sessionsAvecAbsOuDate.contains(se.getId())) {
		        // Ajouter à la liste finale
		        finalList.add(se);
		    }else {
		    	  // Ajouter le commentaire
		        se.setComment("Session orpheline");
		        sessionEpreuveRepository.save(se);
		    }
		}
		if (finalList.isEmpty()) {
			log.info("Aucune session à vérifier pour le contexte : " + context.getKey());
			return;
		}

		log.info("[" + context.getKey() + "] Vérification de " + finalList.size() + " sessions");

		Map<Long, List<SessionEpreuve>> sessionsByProject = finalList.stream()
				.filter(se -> se.getAdeProjectId() != null && se.getAdeEventId() != null)
				.collect(Collectors.groupingBy(SessionEpreuve::getAdeProjectId));

		int deletedCount = 0;
		int errorCount = 0;

		for (Map.Entry<Long, List<SessionEpreuve>> entry : sessionsByProject.entrySet()) {
			Long projectId = entry.getKey();
			List<SessionEpreuve> projectSessions = entry.getValue();
			String sessionId;
			try {
				sessionId = getSessionId(false, context.getKey(), String.valueOf(projectId));
			} catch (Exception e) {
				log.error("[" + context.getKey() + "] Impossible d'obtenir la session ADE pour le projet " + projectId,
						e);
				errorCount += projectSessions.size();
				continue;
			}
			for (SessionEpreuve se : projectSessions) {
				try {
					if (!eventExistsInAde(se, sessionId)) {
						// Sécurité : ne pas supprimer une session trop récente
						if (isOldEnoughToDelete(se)) {
							sessionEpreuveService.delete(se);
							deletedCount++;
							log.info("[" + context.getKey() + "] Suppression session orpheline : "
									+ se.getNomSessionEpreuve() + " [eventId=" + se.getAdeEventId() + "]");
						} else {
							log.warn("[" + context.getKey() + "] Session absente dans ADE mais trop récente : "
									+ se.getNomSessionEpreuve() + " [eventId=" + se.getAdeEventId() + "]");
						}
					}
				} catch (Exception e) {
					log.error("[" + context.getKey() + "] Erreur lors de la vérification de la session "
							+ se.getNomSessionEpreuve() + " [eventId=" + se.getAdeEventId() + "]", e);
					errorCount++;
				}
			}
		}
		log.info("[" + context.getKey() + "] Vérification terminée - supprimées=" + deletedCount + ", erreurs="
				+ errorCount);
	}
	
	private boolean eventExistsInAde(SessionEpreuve se, String sessionId) throws Exception {
		return adeApiService.eventExistsInAde(se, sessionId);
	}

	private boolean isOldEnoughToDelete(SessionEpreuve se) {
	    return se.getDateCreation().toInstant()
	            .isBefore(Instant.now().minus(24, ChronoUnit.HOURS));
	}

	public String getSessionIdByProjectId(
 		String projectId,
		String emargementContext
	) throws AdeApiRequestException {
		return getSessionIdByProjectId(projectId, emargementContext, false);
	}

	// On part du principe que l'on mémorise un identifiant de session API ADE
 	// par projet ADE
	// REM: Eventuellement (ex: API TAPIR de type REST) ce pourra être le même
	//      identifiant de session (ou bearer dans le cas TAPIR) pour tous les
	//      projets
	// REM: emargementContext ne sert vraiment qu'a enregistrer l'id de la 
	//      personne connectée au moment de la récupération de la session. Pas
	//      forcément utile et pas forcément pertinent dans l'interface de
	//      cette méthode. Voir updatePrefs() appelé par la méthode 
	//      getSessionId()
	public String getSessionIdByProjectId(
		String projectId,
		String emargementContext,
		boolean disconnectSessionBeforeNewSessionId
	) throws AdeApiRequestException {
		String sessionId = null;
		try {
			// S'il n'y a pas déjà eu de connexion à l'API dans le cadre d'une
			// interrogation API pour ce projet alors on créé une nouvelle
			//connexion. Sinon, on se contente de récupérer l'id de session
			sessionId = getSessionId(false, emargementContext, projectId);

			// Si la connexion vient d'être créée alors le projet n'est pas
			// encore sélectionné (cas de l'API Web d'ADE)
			// On fait donc un appel API ADE pour sélectioner le projet
			// REM: Dans beaucoup d'autres cas, cet appel sera inutile
			getConnectionProject(projectId, sessionId);

			// Pour vérifier que la session est bien toujours active, on fait
			// un test API en récupérant la liste des projets
			// REM: Il serait préférable de ne pas ajouter ce test la plupart
			// du temps inutile et ne forcer la connexion que si c'est l'appel
			// à l'API (qui suit cette demande d'id de session) qui échoue.
			// Mais c'est sans doute (un peu) plus délicat à mettre en oeuvre.
			if(getProjectLists(sessionId).isEmpty()) {
				// Y a-t-il vraiment des cas, pour lesquels il convient de se
				// déconnecter
				// (voir implémentation de updateSessionEpreuve v1.1.5+)
				if (disconnectSessionBeforeNewSessionId) {
					disconnectSession(emargementContext);
				}

				// Si la requête a échoué c'est qu'on a récupéré en mémoire un
				// sessionId qui n'est plus valable. On force alors une
				// nouvelle connexion à l'API suivi d'un appel API pour
				// sélectionner le projet (selon le principe de fonction de
				// l'API Web ADE)
				sessionId = getSessionId(true, emargementContext, projectId);
				getConnectionProject(projectId, sessionId);
				log.info("Récupération du projet Ade " + projectId);
			}
		} catch (IOException | ParserConfigurationException | SAXException e) {
			log.error(""+e);
			throw new AdeApiRequestException("ERREUR: Impossible de récupérer un id de session API pour le projet ["
				+ projectId + "]");
		}

		return sessionId;
	}

	public Map<String, String> getProjectLists(String sessionId) throws AdeApiRequestException {
		return adeApiService.getProjectLists(sessionId);
	}
	
	public String getConnectionProject(String numProject, String sessionId) throws IOException, ParserConfigurationException, SAXException{
		return adeApiService.getConnectionProject(numProject, sessionId);
	}
	
	protected String getSessionId(boolean forceNewId, String emargementContext, String idProject) throws IOException, ParserConfigurationException, SAXException{
		return adeApiService.getSessionId(forceNewId, emargementContext, idProject);
	}

	@Async
	@Transactional
	public int saveEvents(List<AdeResourceBean> beans, String sessionId, String emargementContext, Campus campus, 
			String idProject, boolean update, String typeSync, List<Long> groupes, Long dureeMax) throws ParseException {
		Context ctx = contextRepository.findByContextKey(emargementContext);
		int i = 0;
		String total = String.valueOf(beans.size());
		int maj = 0;
		StopWatch time = new StopWatch( );
		time.start( );
		for(AdeResourceBean ade : beans) {
			boolean isSessionExisted = sessionEpreuveRepository.countByAdeEventIdAndContext(ade.eventId, ctx)==0 ? false : true;
			if(!isSessionExisted && !update || update){
				SessionEpreuve se = ade.getSessionEpreuve();
				boolean isUpdateOk = false;
				Date today = DateUtils.truncate(new Date(),  Calendar.DATE);
				if(isSessionExisted && update && se.getDateExamen().compareTo(today)>=0) {
					if(ade.getLastImport() != null && ade.getLastUpdate().compareTo(ade.getLastImport())>=0) {
						clearSessionRelatedData(se);
						isUpdateOk = true;
					}else{
						log.info("Aucune maj de l'évènement car il est déjà à jour , id stocké : " + ade.getEventId());
					}
				}else if(update && se.getDateExamen().compareTo(today)<0) {
					log.info("Aucune maj de l'évènement car l'èvènement est passé , id stocké : " + ade.getEventId());
				}else {
					se.setAdeProjectId(Long.parseLong(idProject));
					se.setDateCreation(new Date());
				}
				boolean isMembersChanged = false;
				if(update) {
					isMembersChanged = adeApiService.haveAnyMemberGroupsBeenUpdated(ade,sessionId, ctx);
				}
				if(update && isMembersChanged && !isUpdateOk) {
					List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(se.getAdeEventId());
					tagCheckRepository.deleteAll(tcs);
				}
				if(!isSessionExisted || !update || update && isUpdateOk){
					processSessionEpreuve(se, campus, ctx);
					processTypeSession(ade, se, ctx);
					sessionEpreuveRepository.save(se);
					int nbStudents = processStudents(se, ade, sessionId, groupes, ctx);
					List<SessionLocation> sls = new ArrayList<>();
					processLocations(se, ade, sessionId, ctx, sls, nbStudents);
					//repartition
					sessionEpreuveService.executeRepartition(se.getId(), "alpha");
					processInstructors(ade, sessionId, ctx, sls);
					if (appliConfigService.isAdeImportAfficherGroupes(ctx)) {
						se.setIsGroupeDisplayed(true);
					}
					i++;
					dataEmitterService.sendDataImport(String.valueOf(i).concat("/").concat(total));
					if(update && isUpdateOk){
						maj++;
					}
					if(dureeMax != null && time.getTime() > dureeMax*1000) {
						log.info("Temps d'import ADE Campus dépassé : " + time.getTime() + "secondes");
						break;
					}
				}
			}
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = (auth!=null)?  auth.getName() : "system";
		if(update) {
			log.info("Bilan syncrhonisation ADE : " + maj + " importé(s)");
			logService.log(ACTION.ADE_SYNC, RETCODE.SUCCESS, typeSync + " - Nb maj sessions : " + maj, eppn, null, emargementContext, eppn);
		}else {
			if(i>0) {
				logService.log(ACTION.ADE_IMPORT, RETCODE.SUCCESS, "Import évènements : " + i, eppn, null, emargementContext, eppn);
			}
		}
		return i;
	}

	private void clearSessionRelatedData(SessionEpreuve se) {
		Long sessionEpreuveId = se.getId();
		List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
		tagCheckRepository.deleteAll(tcs);
		List<TagChecker> tcers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(sessionEpreuveId);
		tagCheckerRepository.deleteAll(tcers);
		List<SessionLocation> sls =  sessionLocationRepository.findSessionLocationBySessionEpreuveId(sessionEpreuveId);
		sessionLocationRepository.deleteAll(sls);
		se.setDateImport(new Date());
	}
	
	private void processSessionEpreuve(SessionEpreuve se, Campus campus, Context ctx) {
		se.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentAnneeUnivFromDate(se.getDateExamen())));
		Calendar c = Calendar.getInstance();
	    c.setTime(se.getHeureEpreuve());
	    c.add(Calendar.MINUTE, -15);
	    Date heureConvocation = c.getTime();
	    se.setHeureConvocation(heureConvocation);
		se.setContext(ctx);
		se.setStatutSession(sessionEpreuveService.getStatutSession(se));
		if(se.getTypeBadgeage()==null) {
			se.setTypeBadgeage(TypeBadgeage.SESSION);
		}
		if(campus != null && !campus.equals(se.getCampus())){
			se.setCampus(campus);
		}
		se.setDateImport(new Date());
		sessionEpreuveRepository.save(se);
	}
	
	private void processTypeSession(AdeResourceBean ade, SessionEpreuve se, Context ctx) {
		TypeSession typeSession = null;
		String typeEvent = ade.getTypeEvent();
		if(typeEvent == null) {
			if(!typeSessionRepository.findByKey("n/a").isEmpty()) {
				typeSession = typeSessionRepository.findByKey("n/a").get(0);
			}else {
				typeSession = new TypeSession();
				typeSession.setKey("n/a");
				typeSession.setLibelle("n/a");
				typeSession.setAddByAdmin(true);
				typeSession.setComment("Ajouté d'Ade Campus");
				typeSession.setContext(ctx);
				typeSession.setDateModification(new Date());
				typeSession = typeSessionRepository.save(typeSession);
			}
		}else {
			List<TypeSession> typeSessions = typeSessionRepository.findByKeyAndContext(typeEvent, ctx);
			if(!typeSessions.isEmpty()) {
				typeSession = typeSessions.get(0);
			}else {
				typeSession = new TypeSession();
				typeSession.setKey(ade.getTypeEvent());
				typeSession.setLibelle(ade.getTypeEvent());
				typeSession.setAddByAdmin(true);
				typeSession.setComment("Ajouté d'Ade Campus");
				typeSession.setContext(ctx);
				typeSession.setDateModification(new Date());
				typeSession = typeSessionRepository.save(typeSession);
			}
		}
		se.setTypeSession(typeSession);
	}
	
	private int processStudents(SessionEpreuve se, AdeResourceBean ade, String sessionId, List<Long> groupes, Context ctx) {
		int nbStudents = 0;

        if ("aucune".equals(appliConfigService.getAdeImportSourceParticipants(ctx))) {
            // Pas d'import des participants
            return nbStudents;
        }

        // Reprise de la liste des participants "trainee" telle que déclarée dans ADE
		List<Map<Long, String>> listAdeTrainees = ade.getTrainees();
        if (listAdeTrainees != null && !listAdeTrainees.isEmpty()) {
            // Chaque élément de listAdeTrainees représente un groupe d'étudiants
            // Reste a récupérer la liste des étudiants qui composent le groupe
            // Plusieurs options possibles (Cf. configuration du contexte):
            // - Dans ADE les étudiants sont déclarés en tant que membre du groupe (source = ade)
            // - Les étudiants ne sont pas déclarés dans ADE mais il y a un groupe du même nom dans esup-emargement
            //   qui contient la liste des étudiants (source = esup-emargement)
            switch (appliConfigService.getAdeImportSourceParticipants(ctx)) {
                case "ade":
					List<Map<Long, String>> listAdeSuperGroupes = ade.getSuperGroupe();
					Set<String> membersSet = new HashSet<>();
					List<String> allMembers1 = new ArrayList<>();
					List<String> allMembers2 = new ArrayList<>();
					List<String> allCodes = new ArrayList<>();
					String adeAttribute = appliConfigService.getAdeMemberAttribute(ctx);
					boolean isAdeCampusLimitQueriesEnabled = appliConfigService.isAdeCampusLimitQueriesEnabled(ctx);
					    if (isAdeCampusLimitQueriesEnabled) {
					    	String ids = listAdeTrainees.stream()
					    		    .flatMap(map -> map.keySet().stream())
					    		    .map(Object::toString)
					    		    .collect(Collectors.joining("|"));
					    	allMembers1 = adeApiService.getMembersOfEvent(sessionId, ids, "members", ctx);
					    	if(listAdeSuperGroupes != null && !listAdeSuperGroupes.isEmpty()) {
						    	String ids2 = listAdeSuperGroupes.stream()
						    		    .flatMap(map -> map.keySet().stream())
						    		    .map(Object::toString)
						    		    .collect(Collectors.joining("|"));
						        allMembers2 = adeApiService.getMembersOfEvent(sessionId, ids2, "members", ctx);
					    	}
					    } else {
					    	allMembers1 = listAdeTrainees.stream()
					    		    .flatMap(map -> map.keySet().stream())
					    		    .map(key -> adeApiService.getMembersOfEvent(sessionId, key.toString(), "members", ctx))
					    		    .filter(list -> !list.isEmpty())
					    		    .flatMap(List::stream)
					    		    .collect(Collectors.toList());
					    	if(listAdeSuperGroupes != null && !listAdeSuperGroupes.isEmpty()) {
								allMembers2 = listAdeSuperGroupes.stream()
						    		    .flatMap(map -> map.keySet().stream())
						    		    .map(key -> adeApiService.getMembersOfEvent(sessionId, key.toString(), "members", ctx))
						    		    .filter(list -> !list.isEmpty())
						    		    .flatMap(List::stream)
						    		    .collect(Collectors.toList());
					    	}
					    }
					    membersSet.addAll(allMembers1);
					    if(!allMembers2.isEmpty()) {
					    	membersSet.addAll(allMembers2);
					    }
					    List<String> allMembers = new ArrayList<>(membersSet);
					    if (!allMembers.isEmpty()) {
					    	int chunkSize = 100; // nombre d'id passés dans l'url
					        Map<Integer, List<String>> chunkedMap = new HashMap<>();
					        int chunkIndex = 0;
					        for (int i = 0; i < allMembers.size(); i += chunkSize) {
					            chunkedMap.put(chunkIndex++, 
					                new ArrayList<>(allMembers.subList(i, Math.min(i + chunkSize, allMembers.size()))));
					        }
					        if (isAdeCampusLimitQueriesEnabled) {
					        	for (Map.Entry<Integer, List<String>> entry : chunkedMap.entrySet()) {
					        		allCodes.addAll(adeApiService.getMembersOfEvent(sessionId, String.join("|",  entry.getValue()), adeAttribute, ctx));
					        	}
					        } else {
					            allCodes = allMembers.stream()
						                .map(id -> adeApiService.getMembersOfEvent(sessionId, id, adeAttribute, ctx))
						                .filter(list -> !list.isEmpty())
						                .map(list -> list.get(0))
						                .collect(Collectors.toList());
					        }
					    }
		
						String filter = "code".equals(adeAttribute)? "supannEtuId" : "mail";
						Map<String, LdapUser> users =  ldapService.getLdapUsersFromNumList(allCodes, filter);
						if(!allCodes.isEmpty()) {
							for (String code : allCodes) {
								String numIdentifiant = code;
                                if("mail".equals(filter)) {
                                    LdapUser ldapUser = users.get(code);
                                    if (ldapUser != null) {
                                        numIdentifiant = ldapUser.getNumEtudiant();
                                    } else {
                                        log.warn("Utilisateur LDAP non trouvé pour le code : " + code);
                                        continue; 
                                    }
                                }
								List<Person> persons = personRepository.findByNumIdentifiantAndContext(numIdentifiant, ctx);
								Person person = null;
								boolean isUnknown = false;
								if(!persons.isEmpty()) {
									person = persons.get(0);
								}else {
									person = new Person();
									person.setContext(ctx);
									if(!users.isEmpty()) {
										LdapUser ldapUser = users.get(code);
										if(ldapUser !=null) {
											person.setEppn(ldapUser.getEppn());
											person.setNumIdentifiant(ldapUser.getNumEtudiant());
											person.setType("student");
											personRepository.save(person);
										}else {
											isUnknown= true;
											log.info("code inconnu : " + code +  "  --> ne sera pas enregistré");
										}
									}else {
										log.info("Le numéro de cet étudiant à importer d'Ade Campus n'a pas été trouvé dans le ldap : " + code);
									}
								}
								if(!isUnknown) {
									TagCheck tc = new TagCheck();
									//A voir 
									//tc.setCodeEtape(codeEtape);
									Date endDate =  se.getDateFin() != null? se.getDateFin() : se.getDateExamen();
									List<Absence> absences = absenceRepository.findOverlappingAbsences(person,
		                                    se.getDateExamen(), endDate, se.getHeureEpreuve(), se.getFinEpreuve(), ctx);
									if(!absences.isEmpty()) {
										nbStudents++;
					    				tc.setAbsence(absences.get(0));
					    			}
									tc.setContext(ctx);
									tc.setPerson(person);
									tc.setSessionEpreuve(se);
									tagCheckRepository.save(tc);
									nbStudents++;
									if(groupes!=null && !groupes.isEmpty()) {
										groupeService.addPerson(person, groupes);
									}
								}
							}
						}
                    break;
                case "esup-emargement":
					// Import des étudiants depuis les groupes esup-emargement portant les
					// même nom que les ressources (hors dossier) ADE
					// Cas d'usage: 1 ressource "trainee" ADE (feuille) = 1 formation
					Set<Person> persons = new HashSet<Person>();
					// listAdeTrainees = liste avec pour chaque ressource une Map contenant une/des entrée(s)
					// (cle "id de la ressource", valeur "name de la ressource") 
					for (Map<Long, String> map : listAdeTrainees) {
						for (Entry<Long, String> entry : map.entrySet()) {
							Long traineeResourceId = entry.getKey();
							Map<Long,String> resourceLeaves;
							try {
								if (isResourceFolder(sessionId, ""+traineeResourceId, ctx)) {
									log.debug("La ressource "+traineeResourceId+" est un dossier... Il faut le fouiller");
									// Si la ressource est un dossier
									// alors récupérer toutes les feuilles sous la ressource (toutes profondeurs confondues)
									resourceLeaves = getResourceLeavesIdNameMap(sessionId, ""+traineeResourceId, ctx);
								} else {
									log.debug("La ressource "+traineeResourceId+" n'est pas un dossier. On va pouvoir chercher un groupe du même nom dans esup-emargement.");
									resourceLeaves = new HashMap<Long,String>();
									resourceLeaves.put(traineeResourceId, entry.getValue());
								}

								for (Entry<Long, String> leafTraineeResource : resourceLeaves.entrySet()) {
									// Recherche par nom du groupe dans esup-emargement
									String expectedGroupName = leafTraineeResource.getValue();
									log.debug("Recherche d'un groupe esup-emargement ayant pour nom ["+expectedGroupName+"]");
									List<Groupe> groupesDuNomGroupeADE = groupeRepository
											.findByNomLikeIgnoreCaseAndContext(expectedGroupName, ctx);
									if (1 == groupesDuNomGroupeADE.size()) {
										Groupe groupe = groupesDuNomGroupeADE.get(0);
										log.info("Un groupe esup-emargement (unique) portant le même nom que la ressource ADE ["
												+ groupe.getNom() + "] a été trouvé.");

										// On récupère toutes les personnes du groupes et on les ajoute
										// à la liste de ceux qui doivent être ajoutés à la session
										// (sans mettre 2 fois le même participant qui serait dans plusieurs groupes)
										Set<Person> groupMembers = groupe.getPersons();
										log.debug("Le groupe ["+expectedGroupName+"] contient "+groupMembers.size()+" membres");
										for (Person groupMember: groupMembers) {
											if (!persons.contains(groupMember)) {
												persons.add(groupMember);
											}
										}
									} else if (0 == groupesDuNomGroupeADE.size()) {
										log.info("Aucun groupe portant le nom [" + expectedGroupName
												+ "] n'a été trouvé dans esup-emargement.");
									} else {
										log.info("Mmmm... Il semblerait qu'il y ait, dans esup-emargement, plusieurs groupes ("
												+ groupesDuNomGroupeADE.size() + ") portant le nom [" + expectedGroupName
												+ "]");
									}
								}
							} catch (Exception e) {
								// Pb avec les appels à ADE ?
								e.printStackTrace();
							}
						}
					}

					log.debug("Il y a donc "+persons.size()+" participants à ajouter à la session");
					for (Person person : persons) {
						TagCheck tc = new TagCheck();
						// A voir
						// tc.setCodeEtape(codeEtape);
						tc.setContext(ctx);
						tc.setPerson(person);
						tc.setSessionEpreuve(se);
						// TODO En profiter pour noter dès maintenant les étudiants avec dispense ??
						tagCheckRepository.save(tc);
						nbStudents++;
					}

					break;
                default:
                    log.error("Mode ["+appliConfigService.getAdeImportSourceParticipants(ctx)+"] non supporté");
                    // throw new Exception("Mode ["+appliConfigService.getAdeImportSourceParticipants()+"] non supporté");
                    // FIXME Gestion des erreurs
            } // switch source participants
        } // listAdeTrainees not empty test

		return nbStudents;
	}
	
	@Transactional
	private void processLocations(SessionEpreuve se, AdeResourceBean ade, String sessionId, Context ctx,
			List<SessionLocation> sls, int nbStudents) throws ParseException {

		List<Map<Long, String>> listAdeClassRooms = ade.getClassrooms();
		boolean isCapaciteSalleEnabled = appliConfigService.isAdeCampusUpdateCapaciteSalleEnabled(ctx);

		if (listAdeClassRooms == null || listAdeClassRooms.isEmpty()) {
			log.warn("Aucune salle ADE trouvée pour la session " + sessionId);
			return;
		}

		// --- Étape 1 : charger toutes les classrooms ADE une fois ---
		Map<Long, List<AdeClassroomBean>> classroomsById = new HashMap<>();
		for (Map<Long, String> map : listAdeClassRooms) {
			for (Entry<Long, String> entry : map.entrySet()) {
				List<AdeClassroomBean> beans = getListClassrooms(sessionId, entry.getKey().toString(), null, ctx);
				if (beans != null && !beans.isEmpty()) {
					classroomsById.put(entry.getKey(), beans);
				}
			}
		}

		if (classroomsById.isEmpty()) {
			log.warn("Aucune classroom ADE valide trouvée pour la session " + sessionId);
			return;
		}

		// --- Étape 2 : calcul du total et de la salle la plus petite ---
		int totalSize = 0;
		int minSize = Integer.MAX_VALUE;
		Long minIdClassroom = null;

		for (Map.Entry<Long, List<AdeClassroomBean>> entry : classroomsById.entrySet()) {
			Long id = entry.getKey();
			List<AdeClassroomBean> beans = entry.getValue();
			int roomMinSize = beans.stream().mapToInt(AdeClassroomBean::getSize).min().orElse(Integer.MAX_VALUE);
			totalSize += beans.stream().mapToInt(AdeClassroomBean::getSize).sum();
			if (roomMinSize < minSize || (roomMinSize == minSize && (minIdClassroom == null || id < minIdClassroom))) {
				minSize = roomMinSize;
				minIdClassroom = id;
			}
		}
		log.info("Session " + sessionId + " - total capacité ADE = " + totalSize + ", min salle ID = " + minIdClassroom);
		
	    List<AdeClassroomBean> selectedBeans = classroomsById.get(minIdClassroom);

		// --- Étape 3 : ajustement si besoin ---
		if (isCapaciteSalleEnabled && nbStudents > totalSize && minIdClassroom != null) {
			int adjustment = nbStudents - totalSize;
			log.info("Ajustement de capacité : +" + adjustment + " sur salle " + minIdClassroom);
			List<Location> locs = locationRepository.findByAdeClassRoomIdAndContext(minIdClassroom, ctx);

	        Location loc;
	        if (!locs.isEmpty()) {
	            loc = locs.get(0);
	            if (locs.size() > 1) {
	                log.warn("Doublons détectés pour la salle " + minIdClassroom + ". Ignorés, première Location utilisée.");
	            }
	            loc.setCapacite(loc.getCapacite() + adjustment);
	            locationRepository.save(loc);
	        }  else {
	            AdeClassroomBean bean = selectedBeans.get(0);
	            loc = new Location();
	            loc.setAdeClassRoomId(bean.getIdClassRoom());
	            loc.setAdresse(bean.getChemin());
	            loc.setCampus(ade.getSessionEpreuve().getCampus());
	            loc.setCapacite(bean.getSize() + adjustment);
	            loc.setContext(ctx);
	            loc.setNom(bean.getNom());
	            locationRepository.save(loc);
			}
			// Mettre à jour le totalSize après ajustement
			totalSize += adjustment;
		}

		// --- Étape 4 : création des SessionLocation ---
	    for (Map.Entry<Long, List<AdeClassroomBean>> entry : classroomsById.entrySet()) {
	        Long adeClassRoomId = entry.getKey();
	        List<AdeClassroomBean> beans = entry.getValue();
	        List<Location> locs = locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx);
	        Location location;
	        if (locs.isEmpty()) {
	            AdeClassroomBean bean = beans.get(0);
	            location = new Location();
	            location.setAdeClassRoomId(adeClassRoomId);
	            location.setAdresse(bean.getChemin());
	            location.setCampus(ade.getSessionEpreuve().getCampus());
	            location.setCapacite(bean.getSize());
	            location.setContext(ctx);
	            location.setNom(bean.getNom());
	            locationRepository.save(location);
	        } else {
	            location = locs.get(0);
	            if (locs.size() > 1) {
	                log.warn("Doublons détectés pour adeClassRoomId=" + adeClassRoomId +
	                        ". Ignorés, première Location utilisée.");
	            }
	        }
	        // SessionLocation
	        SessionLocation sl = new SessionLocation();
	        sl.setCapacite(location.getCapacite());
	        sl.setContext(ctx);
	        sl.setLocation(location);
	        sl.setSessionEpreuve(se);
	        sl.setPriorite(1);
	        sls.add(sessionLocationRepository.save(sl));
	    }
	    log.info("processLocations terminé pour session " + sessionId);
	}
	
	private void processInstructors(AdeResourceBean ade, String sessionId, Context ctx, List<SessionLocation> sls) {
		List<Map<Long, String>> listAdeInstructors = ade.getInstructors();
		if(listAdeInstructors!= null &&!listAdeInstructors.isEmpty()) {
			String firstId = listAdeInstructors.get(0).keySet().toArray()[0].toString();
			String fatherIdInstructor = adeApiService.getFatherIdResource(sessionId, firstId, "instructor", "true");
			for(Map<Long, String> map : listAdeInstructors) {
				for (Entry<Long, String> entry : map.entrySet()) {
					List<AdeInstructorBean> adeInstructorBeans = adeApiService.getListInstructors(sessionId, fatherIdInstructor, entry.getKey().toString());
					for(AdeInstructorBean bean : adeInstructorBeans) {
						UserApp userApp = null;
						List<LdapUser> ldapUsers = ldapUserRepository.findByEmailContainingIgnoreCase(bean.getEmail());
						if(!ldapUsers.isEmpty()) {
							String eppn = ldapUsers.get(0).getEppn();
							userApp = userAppRepository.findByEppnAndContext(eppn, ctx);
							if(userApp == null) {
								userApp = new UserApp();
								userApp.setContext(ctx);
								userApp.setDateCreation(new Date());
								String splitInst[] = bean.getPath().split("\\.");
								if(splitInst.length > 1) {
									userApp.setSpeciality(splitInst[1]);
								}
								userApp.setContextPriority(0);
								userApp.setEppn(ldapUsers.get(0).getEppn());
								Role role = appliConfigService.isAdeCampusInstructorManager(ctx)? Role.MANAGER : Role.SUPERVISOR;
								userApp.setUserRole(role);
							}
							userAppRepository.save(userApp);
						}
						//On associe à un surveillant
						if(!sls.isEmpty()) {
							for(SessionLocation sl : sls) {
								if(userApp != null) {
									TagChecker tc =  new TagChecker();
									tc.setContext(ctx);
									tc.setUserApp(userApp);
									tc.setSessionLocation(sl);
									tagCheckerRepository.save(tc);
								}else {
									log.warn("Import surveillant impossible car la personne correspondant à cet email :" + bean.getEmail() + 
											", n'est pas dans le ldap "  );
								}
							}
						}
					}
				}
			}
		}
	}
	
	public List<AdeResourceBean> getAdeBeans(String sessionId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, 
			String codeComposante, List<String> idList, Context ctx, boolean update) throws IOException, ParserConfigurationException, SAXException, ParseException{
        List<AdeResourceBean> adeResourceBeans = new ArrayList<>();

		if("myEvents".equals(codeComposante)) {
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			List<LdapUser> ldapUsers = ldapService.getUsers(auth.getName());
			if(!ldapUsers.isEmpty()) {
				String supannEmpId = ldapUsers.get(0).getNumPersonnel();
				String fatherId = adeApiService.getIdComposante(sessionId, supannEmpId, "instructor", true);
				adeResourceBeans = adeApiService.getEventsFromXml(sessionId, fatherId, strDateMin, strDateMax, idEvents, existingSe, update, ctx);
			}	
		}else if(idList !=null && !idList.isEmpty()){
			for(String id : idList) {
		        List<AdeResourceBean> beansTrainee = adeApiService.getEventsFromXml(sessionId, id, strDateMin, strDateMax, idEvents, existingSe, update, ctx);
		        adeResourceBeans.addAll(beansTrainee);
			}
		}
        return adeResourceBeans; 
    }
	
	public void disconnectSession(String emargementContext) {
		adeApiService.disconnectSession(emargementContext);
	}

	public void updateSessionEpreuve(List<SessionEpreuve> seList, String emargementContext, String typeSync, Context ctx) throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		
		Map<Long, List<SessionEpreuve>> mapSE = seList.stream().filter(t -> t.getAdeProjectId() != null)
		        .collect(Collectors.groupingBy(t -> t.getAdeProjectId()));
		
		for (Long key : mapSE.keySet()) {
	        String idProject = String.valueOf(key);
			String sessionId = getSessionIdByProjectId(idProject, emargementContext, false);
	        List<Long> idEvents  = mapSE.get(key).stream().map(o -> o.getAdeEventId()).collect(Collectors.toList());
			List<AdeResourceBean> beans = getEventsFromXml(sessionId , null, null, null, idEvents, "", true, ctx);
			if(!beans.isEmpty()) {
				saveEvents(beans, sessionId, emargementContext, null, idProject, true, typeSync, null, null);
			}
	    }
	}
	
    public String getJsonfile(String fatherId, String emargementContext, String category, String idProject) {
		return adeApiService.getJsonfile(fatherId, emargementContext, category, idProject);
	}

	public int importEvents(List<Long> idEvents, String emargementContext, String strDateMin, String strDateMax,
			String newGroupe, List<Long> existingGroupe, String existingSe, String codeComposante,
			Campus campus, List<String> idList, List<AdeResourceBean> beans, String idProject, Long dureeMax, boolean update)
			throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		int nbImports = 0;
		if (idEvents != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if(idProject == null) {
				idProject = getCurrentProject(null, auth.getName(), emargementContext);
			}
			String sessionId = getSessionIdByProjectId(idProject, emargementContext);
			Context ctx = contextRepository.findByKey(emargementContext);
			if(beans == null) {
				beans = getAdeBeans(sessionId, strDateMin, strDateMax, idEvents, existingSe,
						codeComposante, idList, ctx, update);
			}
			if (!beans.isEmpty()) {
				List<Long> groupes = new ArrayList<>();
				if (appliConfigService.isAdeCampusGroupeAutoEnabled(ctx)) {
					if (existingGroupe != null) {
						groupes.addAll(existingGroupe);
					}
					if (newGroupe!=null && !newGroupe.isEmpty() && auth != null) {
						Groupe groupe = groupeService.createNewGroupe(newGroupe, emargementContext,
								sessionEpreuveService.getCurrentanneUniv(), auth.getName());
						groupes.add(groupe.getId());
					}
				}
				nbImports = saveEvents(beans, sessionId, emargementContext, campus, idProject, update, null, groupes, dureeMax);
			} else {
				log.info("Aucun évènement à importer");
			}
		}
		return nbImports;
	}
	
	public List<String> getValuesPref(String eppn, String pref) {
	    List<String> values = new ArrayList<>(); 
	    List<Prefs> prefsAdeStored = prefsRepository.findByUserAppEppnAndNom(eppn, pref);
	    if (!prefsAdeStored.isEmpty()) {
	        List<String> temp = prefsAdeStored.stream()
	                .map(Prefs::getValue)
	                .collect(Collectors.toList());
	        if (!temp.isEmpty() && temp.get(0) != null) { // Check if temp has elements and is non-null
	            String[] splitAll = temp.get(0).split(";;");
	            values = Arrays.asList(splitAll);
	            Collections.sort(values);
	        } else if (!temp.isEmpty()) {
	            prefsRepository.delete(prefsAdeStored.get(0));
	        }
	    }
	    return values;
	}
	
	public List<String> getPrefByContext(String nom) {
		List<String> values = new ArrayList<>();
		List<Prefs> prefs = prefsRepository.findByNom(nom);
		if(!prefs.isEmpty()) {
			String liste =  prefs.get(0).getValue().trim();
			String [] splitList = liste.split(",");
			values = Arrays.asList(splitList);
		}
		return values;
	}
	
	public String getCurrentProject(String projet, String eppn, String emargementContext) {
	    if (!appliConfigService.getProjetAde().isEmpty()) {
	        return appliConfigService.getProjetAde();
	    }
	    if (projet != null && !projet.isEmpty()) {
	        preferencesService.updatePrefs(ADE_STORED_PROJET, projet, eppn, emargementContext, "dummy");
	        return projet;
	    }
	    return getValuesPref(eppn, ADE_STORED_PROJET).isEmpty() ? "0" : getValuesPref(eppn, ADE_STORED_PROJET).get(0);
	}
}
