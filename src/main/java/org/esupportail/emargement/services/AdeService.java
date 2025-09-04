package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.TypeSession;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.CampusRepository;
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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class AdeService {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	final static String ADE_STORED_SESSION = "adeStoredSession";
	
	private final static String ADE_STORED_PROJET = "adeStoredProjet";
	
	private final static String pathCopyFile = "/opt/ade";

	public final static String ADE_STORED_COMPOSANTE = "adeStoredComposante";
	
	@Value("${emargement.ade.api.url}")
	private String urlAde;
	
	@Value("${emargement.ade.api.login}")
	private String loginAde;
	
	@Value("${emargement.ade.api.password}")
	private String passwordAde;
	
	@Value("${emargement.ade.api.url.encrypted}")
	private String encryptedUrl;

	@Autowired
	private PrefsRepository prefsRepository;
	
	@Autowired
	private LocationRepository locationRepository;
	
	@Autowired	
	private SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	private CampusRepository campusRepository;
	
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
    
    private String getFatherIdResource(String sessionId, String idItem, String category, String tree){
		String fatherId = "0";
		String detail = "2";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String url = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree="+ tree + "&detail=" + detail + 
				"&category=" + category + idParam;
		try {
			Document doc = getDocument(url);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("category");
			if(list.getLength() == 0) {
				log.info("Aucune donnée dans le XML");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						fatherId =  element.getElementsByTagName("branch").item(0).getAttributes().getNamedItem("id").getNodeValue();
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération du FatherId de : " + category, e);
		}
    	return fatherId;
    }

	public List<AdeClassroomBean> getListClassrooms(String sessionId, String idItem, List<Long> selectedIds)  throws  ParseException {
		String detail = "9";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlClassroom = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=classroom" + idParam;
		List<AdeClassroomBean> adeBeans = new ArrayList<>();
		try {
			Document doc = getDocument(urlClassroom);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("rooms");

			if(list.getLength() == 0) {
				log.info("Aucune salle touvée");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("room");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2;
								Long id = Long.valueOf(element2.getAttribute("id"));
								if((selectedIds != null && !selectedIds.isEmpty() && selectedIds.contains(id)) || 
										selectedIds == null ){
									AdeClassroomBean adeClassroomBean = new AdeClassroomBean();
									adeClassroomBean.setIdClassRoom(id);
									adeClassroomBean.setChemin(element2.getAttribute("path"));
									adeClassroomBean.setNom(element2.getAttribute("name"));
									adeClassroomBean.setSize(Integer.valueOf(element2.getAttribute("size")));
									adeClassroomBean.setType(element2.getAttribute("type"));
									boolean isAlreadyimport = (locationRepository.countByAdeClassRoomId(id) >0)? true : false;
									adeClassroomBean.setAlreadyimport(isAlreadyimport);
									if(!element2.getAttribute("lastUpdate").isEmpty()){
										Date lastUpdate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(element2.getAttribute("lastUpdate"));  
										adeClassroomBean.setLastUpdate(lastUpdate);
									}
									adeBeans.add(adeClassroomBean);
								}
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la liste des salles, url : " + urlClassroom, e);
		}
		return adeBeans;
	}
	
	public List<AdeClassroomBean> getListClassrooms2(String sessionId, String idItem, List<Long> selectedIds)  throws  ParseException {
		String detail = "9";
		String urlClassroom = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&detail=" +detail + "&category=classroom";
		List<AdeClassroomBean> adeBeans = new ArrayList<>();
		try {
		    Document doc = getDocument(urlClassroom);
		    XPath xpath = XPathFactory.newInstance().newXPath();
		    doc.getDocumentElement().normalize();

		    // Get all <leaf> nodes under the first <branch> of category=classroom
		    String xpathExpr = String.format("//branch[@id='%s']//leaf", idItem);
		    NodeList leafNodes = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);
		    if (leafNodes.getLength() == 0) {
		        log.info("Aucune salle trouvée");
		    } else {
		        for (int i = 0; i < leafNodes.getLength(); i++) {
		            Element element = (Element) leafNodes.item(i);
		            Long id = Long.valueOf(element.getAttribute("id"));

		            if ((selectedIds != null && !selectedIds.isEmpty() && selectedIds.contains(id)) || selectedIds == null) {
		                AdeClassroomBean adeClassroomBean = new AdeClassroomBean();
		                adeClassroomBean.setIdClassRoom(id);
		                adeClassroomBean.setChemin(element.getAttribute("path"));
		                adeClassroomBean.setNom(element.getAttribute("name"));
		                adeClassroomBean.setSize(Integer.valueOf(element.getAttribute("size")));
		                adeClassroomBean.setType(element.getAttribute("type"));

		                boolean isAlreadyimport = locationRepository.countByAdeClassRoomId(id) > 0;
		                adeClassroomBean.setAlreadyimport(isAlreadyimport);

		                if (!element.getAttribute("lastUpdate").isEmpty()) {
		                    Date lastUpdate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(element.getAttribute("lastUpdate"));
		                    adeClassroomBean.setLastUpdate(lastUpdate);
		                }

		                adeBeans.add(adeClassroomBean);
		            }
		        }
		    }
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException | ParseException e) {
		    log.error("Erreur lors de la récupération de la liste des salles, URL : {}", urlClassroom, e);
		}
		return adeBeans;
	}
	
	public List<AdeInstructorBean> getListInstructors(String sessionId, String fatherId, String idItem) {
		String detail = "10";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlInstructors = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=instructor&fatherIds=" + fatherId + idParam;
		List<AdeInstructorBean> adeBeans = new ArrayList<>();
		try {
			Document doc = getDocument(urlInstructors);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("instructors");
			if(list.getLength() == 0) {
				log.info("Aucun enseignant trouvé!");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("instructor");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2; 
								AdeInstructorBean adeInstructorBean = new AdeInstructorBean();
								adeInstructorBean.setEmail(element2.getAttribute("email"));
								adeInstructorBean.setNom(element2.getAttribute("name"));
								adeInstructorBean.setPath(element2.getAttribute("path"));
								adeBeans.add(adeInstructorBean);
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la liste des enseignants, url : " + urlInstructors, e);
		}
		return adeBeans;
	}
	
	public Map<String,String> getItemsFromInstructors(String sessionId, String choice) {
		String detail = "12";
		String urlInstructors = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=instructor";
		Map<String,String> items = new HashMap<>();
		try {
			Document doc = getDocument(urlInstructors);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("instructors");
			if(list.getLength() == 0) {
				log.info("Aucun enseignant trouvé!");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("instructor");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2;
								if(choice == null) {
									String name = element2.getAttribute("name");
									if(element2.getAttribute("fatherName").equals("") && element2.getAttribute("path").equals("")){
										items.put(name,name);
									}	
								}else {
									if(element2.getAttribute("path").contains(choice) && !element2.getAttribute("code").equals("")) {
										items.put(element2.getAttribute("code"), element2.getAttribute("path"));
									}
									
								}

							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la liste des enseignants, url : " + urlInstructors, e);
		}
		return sortByValue(items);
	}
	
	public Map<String, String> getClassroomsList(String sessionId) {
	    Map<String, String> mapClassrooms = new HashMap<>();
	    String urlClassroom = String.format("%s?sessionId=%s&function=getResources&tree=true&detail=8&category=classroom", 
	                                         urlAde, sessionId);
	    try {
	        Document doc = getDocument(urlClassroom);
	        XPath xpath = XPathFactory.newInstance().newXPath();
	        NodeList branches = (NodeList) xpath.evaluate("//category/branch", doc, XPathConstants.NODESET);

	        if (branches.getLength() == 0) {
	            log.info("No classrooms found!");
	            return mapClassrooms;
	        }

	        for (int i = 0; i < branches.getLength(); i++) {
	            Element branch = (Element) branches.item(i);
	            mapClassrooms.put(branch.getAttribute("id"), branch.getAttribute("name"));
	        }
	    } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
	        log.error("Error while retrieving the classrooms map, URL: {}", urlClassroom, e);
	    }

	    return sortByValue(mapClassrooms);
	}
	
	public Map<String, AdeResourceBean> getActivityFromResource(String sessionId, String resourceId, String activityId) throws IOException {
	    String url = String.format("%s?sessionId=%s&detail=9&function=getActivities%s",
	                                urlAde, sessionId, activityId != null ? "&id=" + activityId : "&resources=" + resourceId);
	    Map<String, AdeResourceBean> mapActivities = new HashMap<>();

	    try {
	        Document doc = getDocument(url);
	        NodeList activities = doc.getElementsByTagName("activity");

	        for (int i = 0; i < activities.getLength(); i++) {
	            Element activityElement = (Element) activities.item(i);
	            AdeResourceBean adeResourceBean = new AdeResourceBean();
	            adeResourceBean.setTypeEvent(activityElement.getAttribute("type"));
	            mapActivities.put(activityElement.getAttribute("id"), adeResourceBean);
	        }
	    } catch (ParserConfigurationException | SAXException e) {
	    	log.error("erreur lors de la récupération du type d'évènement Ade Campus", e);
	    }

	    return mapActivities;
	}
	
	public boolean haveAnyMemberGroupsBeenUpdated(AdeResourceBean ade, String sessionId, Context ctx) {
		if (ade.getLastImport() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.FRANCE);
			try {
				// Helper to build and test update from XML
				BiFunction<String, String, Boolean> checkResourceUpdated = (id, baseUrl) -> {
					try {
						String urlMembers = String.format(
								"%s?sessionId=%s&function=getResources&tree=false&id=%s&detail=13", baseUrl, sessionId,
								id);
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						factory.setNamespaceAware(true);
						DocumentBuilder builder = factory.newDocumentBuilder();
						Document doc = builder.parse(new java.net.URL(urlMembers).openStream());
						doc.getDocumentElement().normalize();

						XPath xpath = XPathFactory.newInstance().newXPath();
						String lastUpdateStr = xpath.evaluate("//resource/@lastUpdate", doc);
						if (lastUpdateStr == null || lastUpdateStr.isEmpty())
							return false;

						Date lastUpdateDate = sdf.parse(lastUpdateStr);
						boolean isAfter = lastUpdateDate.after(ade.getLastImport());
						return isAfter;
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
				};
				// Handle with campus limitation enabled
				List<Map<Long, String>> listAdeTrainees = ade.getTrainees();
				List<Map<Long, String>> listAdeSuperGroupes = ade.getSuperGroupe();
				boolean isAdeCampusLimitQueriesEnabled = appliConfigService.isAdeCampusLimitQueriesEnabled(ctx);
				if (isAdeCampusLimitQueriesEnabled) {
					if (listAdeTrainees != null) {
						String ids = listAdeTrainees.stream().flatMap(map -> map.keySet().stream())
								.map(Object::toString).collect(Collectors.joining("|"));
						if (checkResourceUpdated.apply(ids, urlAde))
							return true;
					}
					if (listAdeSuperGroupes != null) {
						String ids2 = listAdeSuperGroupes.stream().flatMap(map -> map.keySet().stream())
								.map(Object::toString).collect(Collectors.joining("|"));
						if (checkResourceUpdated.apply(ids2, urlAde))
							return true;
					}
				} else {
					if (listAdeTrainees != null) {
						for (Map<Long, String> map : listAdeTrainees) {
							for (Long id : map.keySet()) {
								if (checkResourceUpdated.apply(id.toString(), urlAde))
									return true;
							}
						}
					}
					if (listAdeSuperGroupes != null) {
						for (Map<Long, String> map : listAdeSuperGroupes) {
							for (Long id : map.keySet()) {
								if (checkResourceUpdated.apply(id.toString(), urlAde))
									return true;
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
 	public List<String> getMembersOfEvent(String sessionId, String idResource, String target, Context ctx) {
	    String detail = "13";
	    String urlMembers = String.format("%s?sessionId=%s&function=getResources&tree=false&id=%s&detail=%s", 
	                                      urlAde, sessionId, idResource, detail);
	    Set<String> listMembers = new HashSet<>(); 
	    try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        factory.setNamespaceAware(true);
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(urlMembers);
	        doc.getDocumentElement().normalize();
	
	        XPathFactory xPathFactory = XPathFactory.newInstance();
	        XPath xpath = xPathFactory.newXPath();
	
	        if ("members".equals(target)) {
	        	log.info("Début de récupération des membres ... pour ressource : " + idResource + " -- Contexte : " + ctx.getKey());
	            NodeList memberNodes = (NodeList) xpath.evaluate("//resource/allMembers/member", doc, XPathConstants.NODESET);
	            for (int i = 0; i < memberNodes.getLength(); i++) {
	                Element memberElement = (Element) memberNodes.item(i);
	                String category = memberElement.getAttribute("category");
	                String memberId = memberElement.getAttribute("id");
	                if (appliConfigService.getCategoriesAde(ctx).equals(category)) {
	                    listMembers.add(memberId);
	                } else {
	                    listMembers.addAll(getMembersOfEvent(sessionId, memberId, "members", ctx));
	                }
	            }
	            log.info("Fin de récupération des membres ... pour ressource : " + idResource + " -- Contexte : " + ctx.getKey());
	        } else {
	            String adeAttribute = appliConfigService.getAdeMemberAttribute(ctx);
	            if (adeAttribute.equals(target)) {
	                String categoryFilter = appliConfigService.getCategoriesAde(ctx);
	                NodeList resourceNodes = (NodeList) xpath.evaluate(
	                    "//resource[@category='" + categoryFilter + "']/@" + adeAttribute, doc, XPathConstants.NODESET);
	                for (int i = 0; i < resourceNodes.getLength(); i++) {
	                    String code = resourceNodes.item(i).getNodeValue();
	                    if ("email".equals(adeAttribute)) {
	                        code = code.split(";")[0];
	                    }
	                    listMembers.add(code);
	                }
	            }
	        }
	    } catch (Exception e) {
	        log.error("Erreur lors de la récupération des membres de l'évènement, url : " + urlMembers, e);
	        e.printStackTrace();
	    }
	    return new ArrayList<>(listMembers);
 	}
	
    public Map<String, String> getMapComposantesFormations(String sessionId, String category) {
        String url = String.format("%s?sessionId=%s&function=getResources&tree=true&leaves=false&category=%s&detail=12",
                                    urlAde, sessionId, category);
        Map<String, String> map = new HashMap<>();

        try {
            Document doc = getDocument(url);
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList branches = (NodeList) xpath.evaluate("//category/branch", doc, XPathConstants.NODESET);

            if (branches.getLength() == 0) {
                log.info("Aucun résultat!");
                return map;
            }

            for (int i = 0; i < branches.getLength(); i++) {
                Element branch = (Element) branches.item(i);
                map.put(branch.getAttribute("id"), branch.getAttribute("name"));
            }
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            log.error("Erreur lors de la récupération de la map, url : " + url, e);
        }

        return sortByValue(map);
    }

	public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, boolean update, Context ctx) throws IOException, ParseException {
		String detail = "8";
		List<AdeResourceBean> adeBeans = new ArrayList<>();
		if(idEvents != null) {
			for (Long id : idEvents) {
				String urlEvent = urlAde + "?sessionId=" + sessionId + "&function=getEvents&eventId=" + id
						+ "&detail=" + detail;
				setEvents(urlEvent, adeBeans, existingSe, sessionId, resourceId, update, ctx);
			}
		}else {
			String urlEvents = urlAde + "?sessionId=" + sessionId + "&function=getEvents&startDate="+ formatDate(strDateMin) + 
					"&endDate=" + formatDate(strDateMax) + "&resources=" + resourceId + "&detail=" +detail;
			setEvents(urlEvents, adeBeans, existingSe, sessionId, resourceId, update, ctx);
		}
		return adeBeans;
	}
	
	public void setEvents(String url, List<AdeResourceBean> adeBeans, String existingSe, String sessionId, String resourceId, boolean update, Context ctx) throws ParseException, IOException {
		Map<String, AdeResourceBean>  activities = getActivityFromResource(sessionId, resourceId, null);
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		try {
			Document doc = getDocument(url);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("event");
			if(list.getLength() == 0) {
				log.info("Aucun évènement récupéré.");
			}else {
				SimpleDateFormat formatter1 = new SimpleDateFormat("HH:mm");
				String adeSuperGroupe = appliConfigService.getAdeSuperGroupe(ctx);
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node;
						AdeResourceBean adeResourceBean = new AdeResourceBean();
						if(resourceId != null) {
							adeResourceBean.setDegreeId(Long.valueOf(resourceId));
						}
						Long eventId = Long.valueOf(element.getAttribute("id"));
						boolean isAlreadyimport = (sessionEpreuveRepository.countByAdeEventIdAndContext(eventId, ctx) >0)? true : false;
						if(existingSe == null && !isAlreadyimport && !update|| "true".equals(existingSe)|| update){
							SessionEpreuve se = null;
							if(isAlreadyimport) {
								List<SessionEpreuve> ses = sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx);
								se = ses.get(0);
							}else {
								se = new SessionEpreuve();
							}
							String activityId = element.getAttribute("activityId");
							adeResourceBean.setActivityId(Long.valueOf(activityId));
							if(update) {
								Map<String, AdeResourceBean>  activities2 = getActivityFromResource(sessionId, null, activityId);
								AdeResourceBean beanActivity2 = activities2.get(activityId);
								if(beanActivity2!= null) {
									adeResourceBean.setTypeEvent(beanActivity2.getTypeEvent());
								}
								adeResourceBean.setLastImport(se!=null? se.getDateImport() : null);
							}else {
								AdeResourceBean beanActivity = activities.get(activityId);
								if(beanActivity!= null) {
									adeResourceBean.setTypeEvent(beanActivity.getTypeEvent());
								}
							}
							if(se!=null) {
								se.setNomSessionEpreuve( element.getAttribute("name"));
								se.setDateExamen(formatter.parse(element.getAttribute("date")));
								se.setHeureEpreuve(formatter1.parse(element.getAttribute("startHour")));
								se.setFinEpreuve(formatter1.parse(element.getAttribute("endHour")));
								se.setAdeEventId( Long.valueOf(element.getAttribute("id")));
								//pour l'instant
								if(!campusRepository.findByContext(ctx).isEmpty()) {
									se.setCampus(campusRepository.findByContext(ctx).get(0));
								}
								adeResourceBean.setSessionEpreuve(se);
								if(isAlreadyimport) {
									SessionEpreuve seOld = sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx).get(0);
									se.setDateCreation(seOld.getDateCreation());
								}
								adeResourceBean.setAlreadyimport(isAlreadyimport);
								adeResourceBean.setEventId(eventId);
								Date lastUpdate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(element.getAttribute("lastUpdate"));  
								adeResourceBean.setLastUpdate(lastUpdate);
								NodeList branches = element.getElementsByTagName("resources");
								for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
									Node node2 = branches.item(temp2);
									if (node2.getParentNode().equals(node)) {
										NodeList resource = element.getElementsByTagName("resource");
										for (int temp3 = 0; temp3 < resource.getLength(); temp3++) {
											Node node3 = resource.item(temp3);
											if (node3.getParentNode().equals(node2)) {
												Element element3 = (Element) node3; 
												String category = element3.getAttribute("category");
												Long id = !element3.getAttribute("id").isEmpty()? Long.valueOf(element3.getAttribute("id")) : null;
												String name = element3.getAttribute("name");
												String formationAde = appliConfigService.getFormationAde(ctx);
												if(formationAde!=null && !formationAde.isEmpty() && formationAde.equals(category)){
													List<Map<Long,String>> category6 = (adeResourceBean.getCategory6() == null)? 
															new ArrayList<>() : adeResourceBean.getCategory6();
															HashMap<Long, String> mapCategory6= new HashMap<>();
															if(id != null && !name.isEmpty()) {
																mapCategory6.put(id, name);
																category6.add(mapCategory6);
																adeResourceBean.setCategory6(category6);
															}
												}else if("trainee".equals(category)){
													List<Map<Long,String>> trainees = (adeResourceBean.getTrainees() == null)? 
															new ArrayList<>() : adeResourceBean.getTrainees();
															HashMap<Long, String> maptrainees= new HashMap<>();
															if(id != null && !name.isEmpty()) {
																maptrainees.put(id, name);
																trainees.add(maptrainees);
																adeResourceBean.setTrainees(trainees);
															}
												}else if("instructor".equals(category)){
													List<Map<Long,String>> instructors = (adeResourceBean.getInstructors() == null)? 
															new ArrayList<>() : adeResourceBean.getInstructors();
															HashMap<Long, String> mapInstructors= new HashMap<>();
															if(id != null && !name.isEmpty()) {
																mapInstructors.put(id, name);
																instructors.add(mapInstructors);
																adeResourceBean.setInstructors(instructors);
															}
												}else if("classroom".equals(category)){
													List<Map<Long,String>>  classrooms = (adeResourceBean.getClassrooms() == null)? 
															new ArrayList<>() : adeResourceBean.getClassrooms();
															HashMap<Long, String> mapClassrooms= new HashMap<>();
															if(id != null && !name.isEmpty()) {
																mapClassrooms.put(id, name);
																classrooms.add(mapClassrooms);
																adeResourceBean.setClassrooms(classrooms);
															}
												}else if(adeSuperGroupe !=null && !adeSuperGroupe.isEmpty() && adeSuperGroupe.equals(category)){
													List<Map<Long,String>> superGroupes = (adeResourceBean.getSuperGroupe() == null)? 
															new ArrayList<>() : adeResourceBean.getSuperGroupe();
															HashMap<Long, String> mapSuperGroupes= new HashMap<>();
															if(id != null && !name.isEmpty()) {
																mapSuperGroupes.put(id, name);
																superGroupes.add(mapSuperGroupes);
																adeResourceBean.setSuperGroupe(superGroupes);
															}
												}
											}
										}
									}
								}
								adeBeans.add(adeResourceBean);
							}else {
								log.warn("Maj de l'évènement impossible, soit il n'existe plus, soit il a été recréé avec un Id différent, id stocké : " + eventId);
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération des évènements, url : " + url, e);
		}
	}
	
	public Document getDocument(String url) throws IOException, ParserConfigurationException, SAXException {
		URL urlConnect = new URL(url);
		HttpURLConnection con = (HttpURLConnection)urlConnect.openConnection();
		InputStream inputStream = con.getInputStream();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream);
		inputStream.close();
		return doc;
	}
	
	public void copyToFile(String urlResources, String type, String codeComposante, String detail) {
		try {
			URL urlPRessources = new URL(urlResources);
			URLConnection urlConnection = urlPRessources.openConnection();
			InputStream inputStream = urlConnection.getInputStream();
			URI u = URI.create(urlResources);
			try (InputStream is = u.toURL().openStream()) {
			    File file = new File(pathCopyFile.concat("/").concat(type).toLowerCase().concat("_").
			    		concat(codeComposante).toLowerCase().concat("_").concat(detail).concat(".xml"));
			    try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
		            int read;
		            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
		            while ((read = inputStream.read(bytes)) != -1) {
		                outputStream.write(bytes, 0, read);
		            }
		        }
			}
		}catch (Exception e) {
			log.error("Erreur de copie de fichier à partir de l'url " + urlResources, e);
		}		
	}
	
	public String getIdComposante(String sessionId, String code, String category, boolean isEvent) throws IOException, ParserConfigurationException, SAXException{
		String urlPComposante = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&code=" + code + "&category=" + category;
		Document doc = getDocument(urlPComposante);
		NodeList nList = doc.getElementsByTagName("category");
		String fatherId = ""; 
		String tag = (isEvent)? "leaf" : "branch";
		for (int temp = 0; temp < nList.getLength(); temp++)
		{
			Node node = nList.item(temp);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				NodeList nodeList = element.getElementsByTagName(tag);
				if(nodeList.item(0)!= null) {
					fatherId = nodeList.item(0).getAttributes().getNamedItem("id").getTextContent();
				}
			}
		}
		return fatherId;
	}
	
	public Map<String, String> getProjectLists(String sessionId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
	    String url = String.format("%s?sessionId=%s&function=getProjects&detail=4", urlAde, sessionId);
	    Map<String, String> mapProjects = new HashMap<>();

	    Document doc = getDocument(url);
	    XPath xpath = XPathFactory.newInstance().newXPath();
	    NodeList projects = (NodeList) xpath.evaluate("//projects/project", doc, XPathConstants.NODESET);

	    for (int i = 0; i < projects.getLength(); i++) {
	        Element project = (Element) projects.item(i);
	        mapProjects.put(project.getAttribute("id"), project.getAttribute("name"));
	    }

	    return sortByValue(mapProjects);
	}
	
	public String getConnectionProject(String numProject, String sessionId) throws IOException, ParserConfigurationException, SAXException{
		String urlProject = urlAde + "?sessionId=" + sessionId + "&function=setProject&projectId=" + numProject;
		Document doc = getDocument(urlProject);
		String projectId = doc.getDocumentElement().getAttribute("projectId");
		return projectId;
	}
	
	public String getSessionId(boolean forceNewId, String emargementContext, String idProject) throws IOException, ParserConfigurationException, SAXException{
		String sessionId = "";
		if(idProject != null) {
			String prefSessionAde = ADE_STORED_SESSION + idProject;
			List<Prefs> prefsAdeSession = prefsRepository.findByNomAllContexts(prefSessionAde);
			if(!prefsAdeSession.isEmpty() && !prefsAdeSession.get(0).getValue().isEmpty() && !forceNewId){
				sessionId = prefsAdeSession.get(0).getValue();
			}
			if(sessionId.isEmpty()) {
				String urlConnexion = "";
				if(!encryptedUrl.isEmpty()) {
					urlConnexion = urlAde + "?data=" + encryptedUrl;
				}else {
					urlConnexion = urlAde + "?function=connect&login=" + loginAde  + "&password=" + passwordAde;
				}
				Document doc = getDocument(urlConnexion);
				log.debug("Root Element connect :" + doc.getDocumentElement().getNodeName());
				sessionId = doc.getDocumentElement().getAttribute("id");
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if(auth != null && !sessionId.isEmpty()) {
					preferencesService.updatePrefs(prefSessionAde, sessionId, auth.getName(), emargementContext, prefSessionAde) ;
					log.info("Impossible de sauvegarder le sessionId");
				}
			}
			log.info("Ade sessionId : " + sessionId);
		}else {
			log.error("Impossible de récupérer un sessionId car idProject == null");
		}
		return sessionId;
	}
	
	private Map<String, String> sortByValue(Map<String, String> unsortedMap) {
	    return unsortedMap.entrySet()
	            .stream()
	            .sorted(Map.Entry.comparingByValue())
	            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
	                                      (oldValue, newValue) -> oldValue, LinkedHashMap::new));
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
					isMembersChanged = haveAnyMemberGroupsBeenUpdated(ade,sessionId, ctx);
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
		se.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentanneUniv()));
		Calendar c = Calendar.getInstance();
	    c.setTime(se.getHeureEpreuve());
	    c.add(Calendar.MINUTE, -15);
	    Date heureConvocation = c.getTime();
	    se.setHeureConvocation(heureConvocation);
		se.setContext(ctx);
		se.setStatut(Statut.STANDBY);
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
			List<TypeSession> typeSessions = typeSessionRepository.findByKey(typeEvent);
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
			    	allMembers1 = getMembersOfEvent(sessionId, ids, "members", ctx);
			    	if(listAdeSuperGroupes != null && !listAdeSuperGroupes.isEmpty()) {
				    	String ids2 = listAdeSuperGroupes.stream()
				    		    .flatMap(map -> map.keySet().stream())
				    		    .map(Object::toString)
				    		    .collect(Collectors.joining("|"));
				        allMembers2 = getMembersOfEvent(sessionId, ids2, "members", ctx);
			    	}
			    } else {
			    	allMembers1 = listAdeTrainees.stream()
			    		    .flatMap(map -> map.keySet().stream())
			    		    .map(key -> getMembersOfEvent(sessionId, key.toString(), "members", ctx))
			    		    .filter(list -> !list.isEmpty())
			    		    .flatMap(List::stream)
			    		    .collect(Collectors.toList());
			    	if(listAdeSuperGroupes != null && !listAdeSuperGroupes.isEmpty()) {
						allMembers2 = listAdeSuperGroupes.stream()
				    		    .flatMap(map -> map.keySet().stream())
				    		    .map(key -> getMembersOfEvent(sessionId, key.toString(), "members", ctx))
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
			        		allCodes.addAll(getMembersOfEvent(sessionId, String.join("|",  entry.getValue()), adeAttribute, ctx));
			        	}
			        } else {
			            allCodes = allMembers.stream()
				                .map(id -> getMembersOfEvent(sessionId, id, adeAttribute, ctx))
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
							numIdentifiant = users.get(code).getNumEtudiant();
						}
						List<Person> persons = personRepository.findByNumIdentifiant(numIdentifiant);
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
                                    se.getDateExamen(), endDate);
							if(!absences.isEmpty()) {
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
                    allMembers = new ArrayList<>();
                    for(Map<Long, String> map : listAdeTrainees) {
                        for (Entry<Long, String> entry : map.entrySet()) {
                            // Import des étudiants depuis le groupe esup-emargement (unique) portant le même nom que le groupe ADE
                            //
                            // Recherche par nom du groupe dans esup-emargement
                            String expectedGroupName = entry.getValue();
                            List<Groupe> groupesDuNomGroupeADE = groupeRepository.findByNomLikeIgnoreCase(expectedGroupName);
                            if (1 == groupesDuNomGroupeADE.size()) {
                                Groupe groupe = groupesDuNomGroupeADE.get(0);
                                log.debug("Un groupe esup-emargement (unique) portant le même nom que le groupe ADE ["+groupe.getNom()+"] a été trouvé.");

                                // On récupère toutes les personnes du groupes et on les associe à la session
                                Set<Person> persons = groupe.getPersons();
                                for (Person person: persons) {
                                    TagCheck tc = new TagCheck();
                                    //A voir
                                    //tc.setCodeEtape(codeEtape);
                                    tc.setContext(ctx);
                                    tc.setPerson(person);
                                    tc.setSessionEpreuve(se);
                                    tagCheckRepository.save(tc);
                                    nbStudents++;
			}
                            } else if (0 == groupesDuNomGroupeADE.size()) {
                                log.debug("Aucun groupe portant le nom ["+expectedGroupName+"] n'a été trouvé dans esup-emargement.");
                            } else {
                                log.warn("Mmmm... Il semblerait qu'il y ait, dans esup-emargement, plusieurs groupes ("+groupesDuNomGroupeADE.size()+") portant le nom ["+expectedGroupName+"]");
		}
                        }
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
	
	private void processLocations(SessionEpreuve se, AdeResourceBean ade, String sessionId, Context ctx, List<SessionLocation> sls, int nbStudents) throws ParseException {
		List<Map<Long, String>> listAdeClassRooms = ade.getClassrooms();
		boolean isCapaciteSalleEnabled = appliConfigService.isAdeCampusUpdateCapaciteSalleEnabled(ctx);
		if(listAdeClassRooms!= null && !listAdeClassRooms.isEmpty()) {
			int totalSize = 0;
			int minSize = Integer.MAX_VALUE;
			Long minIdClassroom = null;
			for (Map<Long, String> map : listAdeClassRooms) {
			    for (Entry<Long, String> entry : map.entrySet()) {
			        List<AdeClassroomBean> adeClassroomBeans = getListClassrooms(sessionId, entry.getKey().toString(), null);
			        if(!adeClassroomBeans.isEmpty()) {
				        for (AdeClassroomBean classroom : adeClassroomBeans) {
				            totalSize += classroom.getSize();
				            if (classroom.getSize() < minSize) {
				                minSize = classroom.getSize();
				                minIdClassroom = classroom.getIdClassRoom();
				            }
				        }
			        }
			    }
			}
			for(Map<Long, String> map : listAdeClassRooms) {
				for (Entry<Long, String> entry : map.entrySet()) {
					List<AdeClassroomBean> adeClassroomBeans = getListClassrooms(sessionId, entry.getKey().toString(), null);
					if(!adeClassroomBeans.isEmpty()) {
						for(AdeClassroomBean bean : adeClassroomBeans) {
							Location location = null;
							Long adeClassRoomId = bean.getIdClassRoom();
							if(!locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).isEmpty()){
								location = locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).get(0);
								if(isCapaciteSalleEnabled && nbStudents>totalSize && location.getAdeClassRoomId().equals(minIdClassroom)) {
									int adjustment = nbStudents - totalSize + location.getCapacite();
									location.setCapacite(adjustment);
									locationRepository.save(location);
									log.info("Location id Ade : " + location.getAdeClassRoomId() + " , ajustement de la capacité : "  + adjustment );
								}
							}else {
								if(isCapaciteSalleEnabled && nbStudents>totalSize && bean.getIdClassRoom().equals(minIdClassroom)) {
									int adjustment = nbStudents - totalSize + bean.getSize();
									bean.setSize(adjustment);
									log.info("Location id Ade : " + adeClassRoomId + " , ajustement de la capacité : "  + adjustment );
								}
								location = new Location();
								location.setAdeClassRoomId(adeClassRoomId);
								location.setAdresse(bean.getChemin());
								location.setCampus(ade.getSessionEpreuve().getCampus());
								location.setCapacite(bean.getSize());
								location.setContext(ctx);
								location.setNom(bean.getNom());
								locationRepository.save(location);
							}
							SessionLocation sl = new SessionLocation();
							sl.setCapacite(location.getCapacite());
							sl.setContext(ctx);
							sl.setLocation(location);
							sl.setSessionEpreuve(se);
							sl.setPriorite(1);
							SessionLocation savesl = sessionLocationRepository.save(sl);
							sls.add(savesl);
						}
					}
			    }
			}
		}
	}
	
	private void processInstructors(AdeResourceBean ade, String sessionId, Context ctx, List<SessionLocation> sls) {
		List<Map<Long, String>> listAdeInstructors = ade.getInstructors();
		if(listAdeInstructors!= null &&!listAdeInstructors.isEmpty()) {
			String firstId = listAdeInstructors.get(0).keySet().toArray()[0].toString();
			String fatherIdInstructor = getFatherIdResource(sessionId, firstId, "instructor", "true");
			for(Map<Long, String> map : listAdeInstructors) {
				for (Entry<Long, String> entry : map.entrySet()) {
					List<AdeInstructorBean> adeInstructorBeans = getListInstructors(sessionId, fatherIdInstructor, entry.getKey().toString());
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
				String fatherId = getIdComposante(sessionId, supannEmpId, "instructor", true);
				adeResourceBeans = getEventsFromXml(sessionId, fatherId, strDateMin, strDateMax, idEvents, existingSe, update, ctx);
			}	
		}else if(idList !=null && !idList.isEmpty()){
			for(String id : idList) {
		        List<AdeResourceBean> beansTrainee = getEventsFromXml(sessionId, id, strDateMin, strDateMax, idEvents, existingSe, update, ctx);
		        adeResourceBeans.addAll(beansTrainee);
			}
		}
        return adeResourceBeans; 
    }
	
	public void disconnectSession(String emargementContext) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String idProject = getCurrentProject(null, auth.getName(), emargementContext);
		List<Prefs> prefsAdeSession = prefsRepository.findByNomAllContexts(ADE_STORED_SESSION + idProject);
		if(!prefsAdeSession.isEmpty() && !prefsAdeSession.get(0).getValue().isEmpty()){
			try {
				String url = urlAde + "?sessionId=" + prefsAdeSession.get(0).getValue() + "&function=disconnect";			
				URL urlConnect = new URL(url);
				HttpURLConnection con = (HttpURLConnection)urlConnect.openConnection();
				con.connect();
				prefsRepository.delete(prefsAdeSession.get(0));
				log.info("Déconnexion de la session Ade par l'utilisateur : " + auth.getName());
			} catch (Exception e) {
				log.error("Impossible de se déconnecter de la session Ade pour l'utilisateur : " + auth.getName(),e);
			}
		}else {
			log.info("Impossible de se déconnecter car aucune session Ade enregistrée pour l'utilisateur : " + auth.getName());
		}
	}
	
	public static String formatDate(String date) {
		String [] splitDate = date.split("-");
		return splitDate[1].concat("/").concat(splitDate[2]).concat("/").concat(splitDate[0]);
	}
	
	public void updateSessionEpreuve(List<SessionEpreuve> seList, String emargementContext, String typeSync, Context ctx) throws IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		
		Map<Long, List<SessionEpreuve>> mapSE = seList.stream().filter(t -> t.getAdeProjectId() != null)
		        .collect(Collectors.groupingBy(t -> t.getAdeProjectId()));
		
		for (Long key : mapSE.keySet()) {
	        String idProject = String.valueOf(key);
	        String sessionId = getSessionId(false, emargementContext, idProject);
	        getConnectionProject(idProject, sessionId);
			if(getProjectLists(sessionId).isEmpty()) {
				disconnectSession(emargementContext);
			}
	        if(getProjectLists(sessionId).isEmpty()) {
				sessionId = getSessionId(true, emargementContext, idProject);
				getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
	        List<Long> idEvents  = mapSE.get(key).stream().map(o -> o.getAdeEventId()).collect(Collectors.toList());
			List<AdeResourceBean> beans = getEventsFromXml(sessionId , null, null, null, idEvents, "", true, ctx);
			if(!beans.isEmpty()) {
				saveEvents(beans, sessionId, emargementContext, null, idProject, true, typeSync, null, null);
			}
	    }
	}
	
    public String getJsonfile(String fatherId, String emargementContext, String category, String idProject) {
        String prettyJson = null;
        String rootComposante = "";
        ObjectMapper xmlMapper = new XmlMapper();
        try {
            String sessionId = getSessionId(false, emargementContext, idProject);
            String detail = "12";
            String urlAllResources = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&leaves=false&category=" + 
            			category + "&detail=" +detail + "&fatherIds=" + fatherId;
            Document doc = getDocument(urlAllResources);
            // Remove attributes from trainee elements
            NodeList traineeNodes = doc.getElementsByTagName(category);
            boolean isOk = false;
            for (int i = 0; i < traineeNodes.getLength(); i++) {
                Element traineeElement = (Element) traineeNodes.item(i);
                if(!isOk && fatherId.equals(traineeElement.getAttribute("fatherId"))) {
                	rootComposante = traineeElement.getAttribute("fatherName");
                	isOk = true;
                }
                removeAttributes(traineeElement);
                removeRightsTag(traineeElement);
            }

            // Save the modified XML to a file
            DOMSource source = new DOMSource(doc); 
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8);
            StreamResult streamResult = new StreamResult(writer);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, streamResult);
            writer.flush(); // Make sure everything is written

            String inContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
			String temp = removeXmlDeclaration2(inContent);
                
            JsonNode jsonNode = xmlMapper.readTree(temp); 

            // Create an ObjectMapper for JSON and serialize the JSONNode
            ObjectMapper jsonMapper = new ObjectMapper();
            String jsonOutput = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            String modifiedJsonContent = jsonOutput.replace("fatherId", "parent");
            String modifiedJsonContent2 = modifiedJsonContent.replace(category, "data");
            JSONObject jsonObject = new JSONObject(modifiedJsonContent2);

            // Remove the "parent" object 
            jsonObject.remove(category);
            ObjectMapper objectMapper = new ObjectMapper();
            String finalJson = objectMapper.writeValueAsString(jsonNode); 
            String toto= finalJson.replaceAll("fatherId", "parent");
            String tata = toto.replace("name", "text");
            String state = "\"state\" : {\"opened\": true}";
            String parent = "[{\"parent\":\"#\",\"id\":\"" + fatherId + "\",\"text\":\"" +  rootComposante + "\","  + state + "},";
            int start = 5 + category.length();
            prettyJson = parent + tata.substring(start,tata.length()-1);
        } catch (Exception e ) {
            e.printStackTrace();
        }
       return prettyJson;
	}
    
    private String removeXmlDeclaration2(String xmlString) {
        // Define a regular expression to match the XML declaration
        String regex = "<\\?xml[^>]*\\?>";
        
        // Replace the XML declaration with an empty string
        return xmlString.replaceAll(regex, "");
    }

	private void removeRightsTag(Element element) {
        NodeList rightsNodes = element.getElementsByTagName("rights");
        for (int i = 0; i < rightsNodes.getLength(); i++) {
            Node rightsNode = rightsNodes.item(i);
            element.removeChild(rightsNode);
        }
    }
	
	private void removeAttributes(Element element) {
	    String [] excludes = {"code", "address1", "address2", "availableQuantity","category", "city", "codeX", "codeY",  
				"codeZ",  "color", "consumer", "country", "creation", "durationInMinutes", 
				"email","fatherName",  "fax", "firstDay",  "firstSlot", "firstWeek",
				"info" , "isGroup",  "jobCategory", "lastDay", "lastSlot", "lastUpdate", "lastWeek", 
				"levelAccess", "manager", "nbEventsPlaced", "number",  "owner", "path", "size", 
				"state", "telephone", "timezone", "type", "url", "zipCode"};
		List<String> attributesToRemove = Arrays.asList(excludes);
		
		for(String item : attributesToRemove) {
			 element.removeAttribute(item);
		}
	
	    // Recur for child elements
	    NodeList childNodes = element.getChildNodes();
	    for (int i = 0; i < childNodes.getLength(); i++) {
	        Node childNode = childNodes.item(i);
	        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
	        	removeAttributes((Element) childNode);
	        }
	    }
	}
	
	public int importEvents(List<Long> idEvents, String emargementContext, String strDateMin, String strDateMax,
			String newGroupe, List<Long> existingGroupe, String existingSe, String codeComposante,
			Campus campus, List<String> idList, List<AdeResourceBean> beans, String idProject, Long dureeMax, boolean update)
			throws IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		int nbImports = 0;
		if (idEvents != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if(idProject == null) {
				idProject = getCurrentProject(null, auth.getName(), emargementContext);
			}
			String sessionId = getSessionId(false, emargementContext, idProject);
			getConnectionProject(idProject, sessionId);
			if (getProjectLists(sessionId).isEmpty()) {
				sessionId = getSessionId(true, emargementContext, idProject);
				getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
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
					if (newGroupe!=null && !newGroupe.isEmpty()) {
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
