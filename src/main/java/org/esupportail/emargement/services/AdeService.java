package org.esupportail.emargement.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeInstructorBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
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
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
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
import org.esupportail.emargement.utils.ToolUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
	
	private final static String ADE_STORED_SESSION = "adeStoredSession";
	
	private final static String ADE_STORED_PROJET = "adeStoredProjet";
	
	private final static String pathCopyFile = "/opt/ade";
	
	@Value("${emargement.ade.api.url}")
	private String urlAde;
	
	@Value("${emargement.ade.api.login}")
	private String loginAde;
	
	@Value("${emargement.ade.api.password}")
	private String passwordAde;
	
	@Value("${emargement.ade.api.url.encrypted}")
	private String encryptedUrl;

	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired	
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	LdapUserRepository ldapUserRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	PersonRepository personRepository;
	
    @Resource 
    PreferencesService preferencesService;
    
    @Resource 
    SessionEpreuveService sessionEpreuveService;
    
    @Resource    
    DataEmitterService dataEmitterService;

    @Resource
    AppliConfigService appliConfigService;
    
	@Resource
	LogService logService;
	
    @Resource 
    LdapService ldapService;
    
    public String getFatherIdResource(String sessionId, String idItem, String category, String tree) throws IOException {
		String fatherId = "0";
		String detail = "2";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String url = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree="+ tree + "&detail=" + detail + 
				"&category=" + category + idParam;
		log.debug("Fatherid  " + category + " : " + url);
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
			log.error("Erreur lors de la récupération du FatherId de : " + category);
		}
    	return fatherId;
    }

	public List<AdeClassroomBean> getListClassrooms(String sessionId, String fatherId, String idItem, List<Long> selectedIds)  throws IOException, ParserConfigurationException, SAXException, ParseException {
		String detail = "9";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlClassroom = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=classroom&fatherIds=" + fatherId + idParam;
		List<AdeClassroomBean> adeBeans = new ArrayList<AdeClassroomBean>();
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
			log.error("Erreur lors de la récupération de la liste des salles, url : " + urlClassroom);
		}
		return adeBeans;
	}
	
	public List<AdeInstructorBean> getListInstructors(String sessionId, String fatherId, String idItem) throws IOException, ParserConfigurationException, SAXException {
		String detail = "10";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlInstructors = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=instructor&fatherIds=" + fatherId + idParam;
		List<AdeInstructorBean> adeBeans = new ArrayList<AdeInstructorBean>();
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
								adeInstructorBean.setIdInstructor(Long.valueOf(element2.getAttribute("id")));
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
			log.error("Erreur lors de la récupération de la liste des enseignants, url : " + urlInstructors);
		}
		return adeBeans;
	}
	
	public Map<String, String> getClassroomsList(String sessionId) throws IOException, ParserConfigurationException, SAXException {
		HashMap<String, String> mapClassrooms = new HashMap<String, String>();
		String detail = "8";
		String urlClassroom = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&detail=" +detail + "&category=classroom";
		try {
			Document doc = getDocument(urlClassroom);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("category");

			if(list.getLength() == 0) {
				log.info("Aucune salle trouvée!");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("branch");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2; 
								mapClassrooms.put(element2.getAttribute("id"), element2.getAttribute("name"));
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la map des salles, url : " + urlClassroom);
		}
		return sortByValue(mapClassrooms);
	}
	
    public Map<String, AdeResourceBean> getActivityFromResource(String sessionId, String resourceId, String activityId) throws IOException {
    	String url = null;
    	if(activityId != null) {
    		url = urlAde + "?sessionId=" + sessionId + "&function=getActivities&id="+ activityId + "&detail=9";
    	}else {
    		url = urlAde + "?sessionId=" + sessionId + "&function=getActivities&resources="+ resourceId + "&detail=9";
    	}
    	Map<String, AdeResourceBean> mapActivities = new HashMap<String, AdeResourceBean>();
		try {
			Document doc;
			doc = getDocument(url);
			NodeList list = doc.getElementsByTagName("activities");
			for (int temp = 0; temp < list.getLength(); temp++) {
				Node node = list.item(temp);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node; 
					NodeList branches = element.getElementsByTagName("activity");
					for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
						Node node2 = branches.item(temp2);
						if (node2.getParentNode().equals(node)) {
							Element element2 = (Element) node2;
							AdeResourceBean  adeResourceBean = new AdeResourceBean();
							adeResourceBean.setTypeEvent(element2.getAttribute("type"));
							mapActivities.put(element2.getAttribute("id"), adeResourceBean);
						}
					}
				}
			}			
		} catch (ParserConfigurationException | SAXException e) {
			log.error("erreur lors de la récupération du type d'évènement Ade Campus", e);
		}

		return mapActivities;
    }

	public List<String>  getMembersOfEvent(String sessionId, String idResource, String target) {
		String detail = "13";
		String urlMembers = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&id=" + idResource + "&detail=" + detail;
		List<String> listMembers = new ArrayList<String>();
		try {
			Document doc = getDocument(urlMembers);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("resources");

			if(list.getLength() == 0) {
				log.info("Aucun étudiant récupéré.");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("resource");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2; 
								if("members".equals(target)) {
									NodeList resource3 = element2.getElementsByTagName("allMembers");
									for (int temp3 = 0; temp3 < resource3.getLength(); temp3++) {
										Node node3 = resource3.item(temp3);
										Element element3 = (Element) node3; 
										NodeList resource4 = element3.getElementsByTagName("member");
										for (int temp4 = 0; temp4 < resource4.getLength(); temp4++) {
											Node node4 = resource4.item(temp4);
											Element element4 = (Element) node4; 
											listMembers.add(element4.getAttribute("id"));
										}
									}
								}else if("code".equals(target)) {
									listMembers.add(element2.getAttribute("code"));
								}
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération des membres de l'évènement, url : " + urlMembers);
		}
		return listMembers;
	}
	
	public String getCodeComposante(String sessionId, String id) throws IOException, ParserConfigurationException, SAXException {
		String detail = "12";
		String urlAllResources = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&leaves=false"
				+ "&category=trainee&detail=" +detail +"&id=" + id;
		String code = "";
		try {
			Document doc = getDocument(urlAllResources);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("category");
			if(list.getLength() == 0) {
				log.info("Aucune composante !");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("branch");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2; 
									code = element2.getAttribute("code");
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération du code composante, url : " + urlAllResources);
		}
		return code;
	}
	
	public Map<String, String> getMapComposantesFormations(String sessionId, String category) throws IOException, ParserConfigurationException, SAXException {
		String detail = "12";
		String urlAllResources = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&leaves=false&category="
				+ category + "&detail=" + detail;
		HashMap<String, String> map = new HashMap<String, String>();
		try {
			Document doc = getDocument(urlAllResources);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("category");
			if(list.getLength() == 0) {
				log.info("Aucun résultat!");
			}else {
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node; 
						NodeList branches = element.getElementsByTagName("branch");
						for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
							Node node2 = branches.item(temp2);
							if (node2.getParentNode().equals(node)) {
								Element element2 = (Element) node2; 
								String name = element2.getAttribute("name");
								String code = element2.getAttribute("id");
								map.put(code, name);
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la map, url : " + urlAllResources);
		}
		return sortByValue(map);
	}

	public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, boolean update) throws IOException, ParserConfigurationException, SAXException, ParseException {
		String detail = "8";
		List<AdeResourceBean> adeBeans = new ArrayList<AdeResourceBean>();
		if(idEvents != null) {
			for(Long id : idEvents) {
				String urlEvent = urlAde + "?sessionId=" + sessionId + "&function=getEvents&eventId=" + id + "&detail=" +detail;
				setEvents(urlEvent, adeBeans, existingSe, sessionId, resourceId, update);
			}
		}else {
			String urlEvents = urlAde + "?sessionId=" + sessionId + "&function=getEvents&startDate="+ formatDate(strDateMin) + 
					"&endDate=" + formatDate(strDateMax) + "&resources=" + resourceId + "&detail=" +detail;
			setEvents(urlEvents, adeBeans, existingSe, sessionId, resourceId, update);
		}

		return adeBeans;
	}
	
	public void setEvents(String url, List<AdeResourceBean> adeBeans, String existingSe, String sessionId, String resourceId, boolean update) throws ParseException, IOException {
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
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node;
						AdeResourceBean adeResourceBean = new AdeResourceBean();
						Long eventId = Long.valueOf(element.getAttribute("id"));
						boolean isAlreadyimport = (sessionEpreuveRepository.countByAdeEventId(eventId) >0)? true : false;
						if(existingSe == null && !isAlreadyimport || "true".equals(existingSe)|| update){
							SessionEpreuve se = null;
							if(update) {
								List<SessionEpreuve> ses = sessionEpreuveRepository.findByAdeEventId(eventId);
								se = (ses != null) ? ses.get(0) :  null;
								Map<String, AdeResourceBean>  activities2 = getActivityFromResource(sessionId, null, element.getAttribute("activityId"));
								AdeResourceBean beanActivity2 = activities2.get(element.getAttribute("activityId"));
								if(beanActivity2!= null) {
									adeResourceBean.setTypeEvent(beanActivity2.getTypeEvent());
								}
							}else {
								se = new SessionEpreuve();
								AdeResourceBean beanActivity = activities.get(element.getAttribute("activityId"));
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
								if(!campusRepository.findAll().isEmpty()) {
									se.setCampus(campusRepository.findAll().get(0));
								}
								adeResourceBean.setSessionEpreuve(se);
								if(isAlreadyimport) {
									SessionEpreuve seOld = sessionEpreuveRepository.findByAdeEventId(eventId).get(0);
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
												if(appliConfigService.getCategoriesAde().get(1).equals(category)){
													List<Map<Long,String>> category6 = (adeResourceBean.getCategory6() == null)? 
															new ArrayList<>() : adeResourceBean.getCategory6();
															HashMap<Long, String> mapCategory6= new HashMap<>();
															mapCategory6.put(Long.valueOf(element3.getAttribute("id")), element3.getAttribute("name"));
															category6.add(mapCategory6);
															adeResourceBean.setCategory6(category6);
												}else if("trainee".equals(category)){
													List<Map<Long,String>> trainees = (adeResourceBean.getTrainees() == null)? 
															new ArrayList<>() : adeResourceBean.getTrainees();
															HashMap<Long, String> maptrainees= new HashMap<>();
															maptrainees.put(Long.valueOf(element3.getAttribute("id")), element3.getAttribute("name"));
															trainees.add(maptrainees);
															adeResourceBean.setTrainees(trainees);	
												}else if("instructor".equals(category)){
													List<Map<Long,String>> instructors = (adeResourceBean.getInstructors() == null)? 
															new ArrayList<>() : adeResourceBean.getInstructors();
															HashMap<Long, String> mapInstructors= new HashMap<>();
															mapInstructors.put(Long.valueOf(element3.getAttribute("id")), element3.getAttribute("name"));
															instructors.add(mapInstructors);
															adeResourceBean.setInstructors(instructors);
												}else if("classroom".equals(category)){
													List<Map<Long,String>>  classrooms = (adeResourceBean.getClassrooms() == null)? 
															new ArrayList<>() : adeResourceBean.getClassrooms();
															HashMap<Long, String> mapClassrooms= new HashMap<>();
															mapClassrooms.put(Long.valueOf(element3.getAttribute("id")), element3.getAttribute("name"));
															classrooms.add(mapClassrooms);
															adeResourceBean.setClassrooms(classrooms);	  
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
			log.error("Erreur lors de la récupération des évènements, url : " + url);
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
	
	public Map<String, String> getProjectLists(String sessionId) throws IOException, ParserConfigurationException, SAXException {
		String detail = "4";
		String url = urlAde + "?sessionId=" + sessionId + "&function=getProjects&detail=" + detail;
		HashMap<String, String> mapProjects = new HashMap<String, String>();
		Document doc = getDocument(url);
		NodeList list = doc.getElementsByTagName("projects");
		
		for (int temp = 0; temp < list.getLength(); temp++) {
			Node node = list.item(temp);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node; 
				NodeList branches = element.getElementsByTagName("project");
				for (int temp2 = 0; temp2 < branches.getLength(); temp2++) {
					Node node2 = branches.item(temp2);
					if (node2.getParentNode().equals(node)) {
						Element element2 = (Element) node2; 
						mapProjects.put(element2.getAttribute("id"), element2.getAttribute("name"));
					}
				}
			}
		}
		return sortByValue(mapProjects);
	}
	
	public NamedNodeMap getConnectionProject(String numProject, String sessionId) throws IOException, ParserConfigurationException, SAXException{
		String urlProject = urlAde + "?sessionId=" + sessionId + "&function=setProject&projectId=" + numProject;
		Document doc = getDocument(urlProject);
		return doc.getAttributes();
	}
	
	public String getSessionId(boolean forceNewId, String emargementContext) throws IOException, ParserConfigurationException, SAXException{
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String sessionId = "";
		if(auth != null) {
			List<Prefs> prefsAdeSession = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_SESSION);
			if(!prefsAdeSession.isEmpty() && !prefsAdeSession.get(0).getValue().isEmpty() && !forceNewId){
				sessionId = prefsAdeSession.get(0).getValue();
			}
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
		}
		if(auth != null) {
			preferencesService.updatePrefs(ADE_STORED_SESSION, sessionId, auth.getName(), emargementContext) ;	
		}
		log.info("Ade sessionId : " + sessionId);
		return sessionId;
	}
	private Map<String, String> sortByValue(Map<String, String> unsortMap) {
		List<Map.Entry<String, String>> list =
				new LinkedList<Map.Entry<String, String>>(unsortMap.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
			public int compare(Map.Entry<String, String> o1,
					Map.Entry<String, String> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});
		Map<String, String> sortedMap = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	//@Async
	@Transactional
	public void saveEvents(List<AdeResourceBean> beans, String sessionId, String emargementContext, List<String> campuses, 
			String eppn, boolean update, String typeSync) throws IOException, ParserConfigurationException, SAXException, ParseException {
		Context ctx = contextRepository.findByContextKey(emargementContext);
		int i = 0;
		String total = String.valueOf(beans.size());
		int maj = 0;
		String idProject =  null;
		if(eppn !=null && !"system".equals(eppn)) {
		    idProject = prefsRepository.findByUserAppEppnAndNom(eppn, ADE_STORED_PROJET).get(0).getValue();
	        if(getConnectionProject(idProject, sessionId)==null) {
				sessionId = getSessionId(true, emargementContext);
				getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
		}
		
		for(AdeResourceBean ade : beans) {
			SessionEpreuve se = ade.getSessionEpreuve();
			boolean isUpdateOk = false;
			Date today = DateUtils.truncate(new Date(),  Calendar.DATE);
			if(update && se.getDateExamen().compareTo(today)>=0) {
				if(se.getDateImport() != null && ade.getLastUpdate().compareTo(se.getDateImport())>=0) {
					Long sessionEpreuveId = se.getId();
					isUpdateOk = true;
					List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
					tagCheckRepository.deleteAll(tcs);
					List<TagChecker> tcers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(sessionEpreuveId);
					tagCheckerRepository.deleteAll(tcers);
					List<SessionLocation> sls =  sessionLocationRepository.findSessionLocationBySessionEpreuveId(sessionEpreuveId);
					sessionLocationRepository.deleteAll(sls);
					se.setDateImport(new Date());
				}else{
					log.info("Aucune maj de l'évènement car il est déjà à jour , id stocké : " + ade.getEventId());
				}
			}else if(update && se.getDateExamen().compareTo(today)<0) {
				log.info("Aucune maj de l'évènement car l'èvènement est passé , id stocké : " + ade.getEventId());
			}else {
				se.setAdeProjectId(Long.parseLong(idProject));
				se.setDateCreation(new Date());
			}
			if(!update || update && isUpdateOk){
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
				if(campuses != null) {
					for(String str : campuses) {
						if(str.contains(ade.getEventId().toString())) {
							String [] splitCampuses = str.split("@@");
							Campus campus = campusRepository.findById(Long.valueOf(splitCampuses[1])).get();
							if(!campus.equals(se.getCampus())){
								se.setCampus(campus);
							}
						}
					}
				}
				
				se.setDateImport(new Date());
				se = sessionEpreuveRepository.save(se);
				String typeEvent= ade.getTypeEvent();
				TypeSession typeSession = null;
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
				sessionEpreuveRepository.save(se);
				//Etudiants
				List<Map<Long, String>> listAdeTrainees = ade.getTrainees();
				if(listAdeTrainees!= null && !listAdeTrainees.isEmpty()) {
					List<String> allMembers = new ArrayList<String>();
					for(Map<Long, String> map : listAdeTrainees) {
						for (Entry<Long, String> entry : map.entrySet()) {
							allMembers.addAll(getMembersOfEvent(sessionId, entry.getKey().toString(), "members"));
						}
					}
					List <String> allCodes = new ArrayList<String>();
					if(!allMembers.isEmpty()) {
						for(String id : allMembers) {
							allCodes.add(getMembersOfEvent(sessionId, id, "code").get(0));
						}
					}
					if(!allCodes.isEmpty()) {
						for (String code : allCodes) {
							List<Person> persons = personRepository.findByNumIdentifiant(code);
							Person person = null;
							if(!persons.isEmpty()) {
								person = persons.get(0);
							}else {
								person = new Person();
								person.setContext(ctx);
								List<LdapUser> ldapUsers = ldapUserRepository.findByNumEtudiantEquals(code);
								if(!ldapUsers.isEmpty()) {
									person.setEppn(ldapUsers.get(0).getEppn());
									person.setNumIdentifiant(code);
									person.setType("student");
									personRepository.save(person);
								}else {
									log.info("Le numéro de cet étudiant à importer d'Ade Campus n'a pas été trouvé dans le ldap : " + code);
								}
							}
							TagCheck tc = new TagCheck();
							//A voir 
							//tc.setCodeEtape(codeEtape);
							tc.setContext(ctx);
							tc.setPerson(person);
							tc.setSessionEpreuve(se);
							tagCheckRepository.save(tc);
						}
					}
				}
				List<Map<Long, String>> listAdeClassRooms = ade.getClassrooms();
				List<SessionLocation> sls = new ArrayList<SessionLocation>();
				if(listAdeClassRooms!= null && !listAdeClassRooms.isEmpty()) {
					String firstId = listAdeClassRooms.get(0).keySet().toArray()[0].toString();
					String fatherIdClassroom = getFatherIdResource(sessionId, firstId, "classroom", "true");
					for(Map<Long, String> map : listAdeClassRooms) {
						for (Entry<Long, String> entry : map.entrySet()) {
							List<AdeClassroomBean> adeClassroomBeans = getListClassrooms(sessionId, fatherIdClassroom, entry.getKey().toString(), null);
							if(!adeClassroomBeans.isEmpty()) {
								for(AdeClassroomBean bean : adeClassroomBeans) {
									Location location = null;
									Long adeClassRoomId = bean.getIdClassRoom();
									if(!locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).isEmpty()){
										location = locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).get(0);
									}else {
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
				//repartition
				sessionEpreuveService.executeRepartition(se.getId(), true);
				List<Map<Long, String>> listAdeInstructors = ade.getInstructors();
				if(listAdeInstructors!= null &&!listAdeInstructors.isEmpty()) {
					String firstId = listAdeInstructors.get(0).keySet().toArray()[0].toString();
					String fatherIdInstructor = getFatherIdResource(sessionId, firstId, "instructor", "true");
					for(Map<Long, String> map : listAdeInstructors) {
						for (Entry<Long, String> entry : map.entrySet()) {
							List<AdeInstructorBean> adeInstructorBeans = getListInstructors(sessionId, fatherIdInstructor, entry.getKey().toString());
							for(AdeInstructorBean bean : adeInstructorBeans) {
								Long adeInstructorId = bean.getIdInstructor();
								UserApp userApp = null;
								List<LdapUser> ldapUsers = ldapUserRepository.findByEmailContainingIgnoreCase(bean.getEmail());
								if(!ldapUsers.isEmpty()) {
									userApp = userAppRepository.findByEppnAndContext(ldapUsers.get(0).getEppn(), ctx);
									if(userApp != null) {
										userApp.setAdeInstructorId(adeInstructorId);
									}else {
										userApp = new UserApp();
										userApp.setContext(ctx);
										userApp.setAdeInstructorId(adeInstructorId);
										userApp.setDateCreation(new Date());
										userApp.setContextPriority(0);
										userApp.setEppn(ldapUsers.get(0).getEppn());
										userApp.setUserRole(Role.MANAGER);
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
											log.info("Import surveillant impossible car la personne correspondant à cet email :" + bean.getEmail() + 
													", n'est pas dans le ldap "  );
										}
									}
								}
							}
						}
					}
				}
				i++;
				dataEmitterService.sendDataImport(String.valueOf(i).concat("/").concat(total));
				if(update && isUpdateOk){
					maj++;
				}
			}
		}
		if(update) {
			log.info("Bilan syncrhonisation ADE : " + maj + " importé(s)");
			logService.log(ACTION.ADE_SYNC, RETCODE.SUCCESS, typeSync + " - Nb maj sessions : " + maj, eppn, null, emargementContext, eppn);
		}else {
			logService.log(ACTION.ADE_IMPORT, RETCODE.SUCCESS, "Import évènements : " + i, eppn, null, emargementContext, eppn);
		}
	}
	public List<AdeResourceBean> getAdeBeans(String sessionId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, 
			String codeComposante, List<String> idList) throws IOException, ParserConfigurationException, SAXException, ParseException{
        List<AdeResourceBean> adeResourceBeans = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<LdapUser> ldapUsers = ldapService.getUsers(auth.getName());
		if("myEvents".equals(codeComposante)) {
			if(!ldapUsers.isEmpty()) {
				String supannEmpId = ldapUsers.get(0).getNumPersonnel();
				String fatherId = getIdComposante(sessionId, supannEmpId, "instructor", true);
				adeResourceBeans = getEventsFromXml(sessionId, fatherId, strDateMin, strDateMax, idEvents, existingSe, false);
			}	
		}else if(idList !=null && !idList.isEmpty()) {
			for(String id : idList) {
		        List<AdeResourceBean> beansTrainee = getEventsFromXml(sessionId, id, strDateMin, strDateMax, idEvents, existingSe, false);
		        adeResourceBeans.addAll(beansTrainee);
			}
		}
        return adeResourceBeans; 
    }
	
	public void disconnectSession() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<Prefs> prefsAdeSession = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_SESSION);
		if(!prefsAdeSession.isEmpty() && !prefsAdeSession.get(0).getValue().isEmpty()){
			try {
				String url = urlAde + "?sessionId=" + prefsAdeSession.get(0).getValue() + "&function=disconnect";			
				URL urlConnect = new URL(url);
				HttpURLConnection con = (HttpURLConnection)urlConnect.openConnection();
				con.connect();
				prefsRepository.delete(prefsAdeSession.get(0));
			} catch (Exception e) {
				log.error("impossible de se déconnecter de la session pour l'utilsateur : " + auth.getName(),e);
			}
		}else {
			log.info("Impossible de se déconnecter car aucune session enregistrée pour l'utilsateur : " + auth.getName());
		}
	}
	
	public String formatDate(String date) {
		String [] splitDate = date.split("-");
		return splitDate[1].concat("/").concat(splitDate[2]).concat("/").concat(splitDate[0]);
	}
	
	public void updateSessionEpreuve(List<SessionEpreuve> seList, String emargementContext, String typeSync) throws IOException, ParserConfigurationException, SAXException, ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String sessionId = getSessionId(false, emargementContext);
		Map<Long, List<SessionEpreuve>> mapSE = seList.stream().filter(t -> t.getAdeProjectId() != null)
		        .collect(Collectors.groupingBy(t -> t.getAdeProjectId()));
		
		for (Long key : mapSE.keySet()) {
	        String idProject = String.valueOf(key);
	        if(getConnectionProject(idProject, sessionId)==null) {
				sessionId = getSessionId(true, emargementContext);
				getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
	        List<Long> idEvents  = mapSE.get(key).stream().map(o -> o.getAdeEventId()).collect(Collectors.toList());
	        
			List<AdeResourceBean> beans = getEventsFromXml(sessionId , null, null, null, idEvents, "", true);
			if(!beans.isEmpty()) {
				String eppn = (auth!=null)?  auth.getName() : "system";
				saveEvents(beans, sessionId, emargementContext, null, eppn, true, typeSync);
			}
	    }
	}
	
    public String getJsonfile(String fatherId, String emargementContext, String category) {
        String prettyJson = null;
        String rootComposante = "";
        ObjectMapper xmlMapper = new XmlMapper();
        try {
            String sessionId = getSessionId(false, emargementContext);
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
            StreamResult streamResult = new StreamResult(byteArrayOutputStream);

            // Create a Transformer and perform the transformation
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, streamResult);

			ByteArrayInputStream in = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			String inContent = new String(IOUtils.toByteArray(in));
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
}