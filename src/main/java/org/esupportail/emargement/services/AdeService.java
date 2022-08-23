package org.esupportail.emargement.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Service
public class AdeService {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ADE_STORED_SESSION = "adeStoredSession";
	
	private final static String pathCopyFile = "/opt/ade";
	
	@Value("${emargement.ade.api.url}")
	private String urlAde;
	
	@Value("${emargement.ade.api.login}")
	private String loginAde;
	
	@Value("${emargement.ade.api.password}")
	private String passwordAde;
	
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
	LogService logService;
    
    public String getFatherIdResource(String sessionId, String idItem, String category, String tree) throws IOException {
		String fatherId = "0";
		String detail = "2";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String url = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree="+ tree + "&detail=" + detail + 
				"&category=" + category + idParam;
		log.debug("Fatherid  " + category + " : " + url);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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

	public List<String>  getMembersOfEvent(String sessionId, String idResource, String target) {
		String detail = "13";
		String urlMembers = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&id=" + idResource + "&detail=" + detail;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		List<String> listMembers = new ArrayList<String>();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
	
	public Map<String, String> getMapComposantes(String sessionId) throws IOException, ParserConfigurationException, SAXException {
		String detail = "12";
		String urlAllResources = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&leaves=false&category=trainee&path=1&detail=" +detail;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		HashMap<String, String> mapComposantes = new HashMap<String, String>();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
								String name = element2.getAttribute("name");
								String code = element2.getAttribute("code");
								mapComposantes.put(code, name);
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération de la map des composantes, url : " + urlAllResources);
		}
		return sortByValue(mapComposantes);
	}

	public TypeSession getTypeSession(String str) {
	   List<TypeSession>  types = typeSessionRepository.findAllByOrderByLibelle();
        for (TypeSession type : types) {
        	if (StringUtils.containsIgnoreCase(StringUtils.substringBefore(str, " "), type.getKey())){
        		return type;
       	 	}
            if (StringUtils.containsIgnoreCase(StringUtils.substringBefore(str, " "), type.getLibelle())){
                return type;
            }
        }
        return new TypeSession();
    }
	
	public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe) throws IOException, ParserConfigurationException, SAXException, ParseException {
		String detail = "8";
		String urlEvents = urlAde + "?sessionId=" + sessionId + "&function=getEvents&resources=" + resourceId + "&detail=" +detail;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		List<AdeResourceBean> adeBeans = new ArrayList<AdeResourceBean>();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		Date dateMin = (strDateMin !=null && !strDateMin.isEmpty())? new SimpleDateFormat("yyyy-MM-dd").parse(strDateMin) : new Date();
		Date dateMax = (strDateMax !=null && !strDateMax.isEmpty())? new SimpleDateFormat("yyyy-MM-dd").parse(strDateMax) : null;
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Document doc = getDocument(urlEvents);
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
						if((idEvents != null && !idEvents.isEmpty() && idEvents.contains(Long.valueOf(element.getAttribute("id")))) || idEvents == null ){
							AdeResourceBean adeResourceBean = new AdeResourceBean();
							int compareMax = (dateMax == null)? 0 : toolUtil.compareDate(formatter.parse(element.getAttribute("date")), dateMax, "yyyy-MM-dd");
							int compare = toolUtil.compareDate(formatter.parse(element.getAttribute("date")), dateMin, "yyyy-MM-dd");
							if(compare >=0 && compareMax < 1){
								Long eventId = Long.valueOf(element.getAttribute("id"));
								boolean isAlreadyimport = (sessionEpreuveRepository.countByAdeEventId(eventId) >0)? true : false;
								if(existingSe == null && !isAlreadyimport || "true".equals(existingSe)){
								SessionEpreuve se = new SessionEpreuve();
								se.setNomSessionEpreuve( element.getAttribute("name"));
								se.setDateExamen(formatter.parse(element.getAttribute("date")));
								se.setHeureEpreuve(formatter1.parse(element.getAttribute("startHour")));
								se.setFinEpreuve(formatter1.parse(element.getAttribute("endHour")));
								se.setAdeEventId( Long.valueOf(element.getAttribute("id")));
								//pour l'instant
								se.setCampus(campusRepository.findAll().get(0));
								adeResourceBean.setSessionEpreuve(se);
								if(isAlreadyimport) {
									SessionEpreuve seOld = sessionEpreuveRepository.findByAdeEventId(eventId).get(0);
									se.setTypeSession(seOld.getTypeSession());
									se.setDateCreation(seOld.getDateCreation());
								}else {
									se.setTypeSession(getTypeSession(element.getAttribute("name")));
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
												if("category6".equals(category)){
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
								}
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Erreur lors de la récupération des évènements, url : " + urlEvents);
		}
		
		return adeBeans;
	}
	
	public Document getDocument(String url) throws IOException, ParserConfigurationException, SAXException {
		URL urlConnect = new URL(url);
		URLConnection urlConnection = urlConnect.openConnection();
		InputStream inputStream = urlConnection.getInputStream();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
		List<Prefs> prefsAdeSession = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_SESSION);
		String sessionId = "";
		if(!prefsAdeSession.isEmpty() && !forceNewId){
			sessionId = prefsAdeSession.get(0).getValue();
		}else {
			String urlConnexion = urlAde + "?function=connect&login=" + loginAde  + "&password=" + passwordAde;
			Document doc = getDocument(urlConnexion);
			log.debug("Root Element connect :" + doc.getDocumentElement().getNodeName());
			sessionId = doc.getDocumentElement().getAttribute("id");
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
	
	@Async
	public void saveEvents(List<AdeResourceBean> beans, String sessionId, String emargementContext, List<String> typesSession,  List<String> campuses, String eppn) throws IOException, ParserConfigurationException, SAXException, ParseException {
		Context ctx = contextRepository.findByContextKey(emargementContext);
		int i = 0;
		String total = String.valueOf(beans.size());
		for(AdeResourceBean ade : beans) {
			SessionEpreuve se = ade.getSessionEpreuve();
			se.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentanneUniv()));
			Calendar c = Calendar.getInstance();
		    c.setTime(se.getHeureEpreuve());
		    c.add(Calendar.MINUTE, -15);
		    Date heureConvocation = c.getTime();
		    se.setHeureConvocation(heureConvocation);
			se.setContext(ctx);
			se.setStatut(Statut.STANDBY);
			se.setTypeBadgeage(TypeBadgeage.SALLE);
			for(String str : typesSession) {
				if(str.contains(ade.getEventId().toString())) {
					String [] splitTypesSession = str.split("@@");
					TypeSession typeSession = typeSessionRepository.findById(Long.valueOf(splitTypesSession[1])).get();
					if(!typeSession.equals(se.getTypeSession())){
						se.setTypeSession(typeSession);
					}
				}
			}
			for(String str : campuses) {
				if(str.contains(ade.getEventId().toString())) {
					String [] splitCampuses = str.split("@@");
					Campus campus = campusRepository.findById(Long.valueOf(splitCampuses[1])).get();
					if(!campus.equals(se.getCampus())){
						se.setCampus(campus);
					}
				}
			}
			se.setDateCreation(new Date());
			sessionEpreuveRepository.save(se);
			//Etudiants
			List<Map<Long, String>> listAdeTrainees = ade.getTrainees();
			if(listAdeTrainees!= null && !listAdeTrainees.isEmpty()) {
				List<String> allMembers = new ArrayList<String>();
				for(Map<Long, String> map : listAdeTrainees) {
					for (Entry<Long, String> entry : map.entrySet()) {
						allMembers = getMembersOfEvent(sessionId, entry.getKey().toString(), "members") ;
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
							person.setEppn(ldapUsers.get(0).getEppn());
							person.setNumIdentifiant(code);
							person.setType("student");
							personRepository.save(person);
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
								if(!locationRepository.findByAdeClassRoomId(adeClassRoomId).isEmpty()){
									location = locationRepository.findByAdeClassRoomId(adeClassRoomId).get(0);
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
			List<Map<Long, String>> listAdeInstructors = ade.getInstructors();
			if(listAdeInstructors!= null &&!listAdeInstructors.isEmpty()) {
				String firstId = listAdeInstructors.get(0).keySet().toArray()[0].toString();
				String fatherIdInstructor = getFatherIdResource(sessionId, firstId, "instructor", "true");
				for(Map<Long, String> map : listAdeInstructors) {
					for (Entry<Long, String> entry : map.entrySet()) {
						List<AdeInstructorBean> adeInstructorBeans = getListInstructors(sessionId, fatherIdInstructor, entry.getKey().toString());
						for(AdeInstructorBean bean : adeInstructorBeans) {
							UserApp userApp = null;
							Long adeInstructorId = bean.getIdInstructor();
							if(!userAppRepository.findByAdeInstructorIdAndContext(adeInstructorId, ctx).isEmpty()) {
								userApp = userAppRepository.findByAdeInstructorIdAndContext(adeInstructorId, ctx).get(0);
							}else {
								List<LdapUser> ldapUsers = ldapUserRepository.findByEmailContainingIgnoreCase(bean.getEmail());
								if(!ldapUsers.isEmpty()) {
									userApp = new UserApp();
									userApp.setContext(ctx);
									userApp.setAdeInstructorId(adeInstructorId);
									userApp.setDateCreation(new Date());
									userApp.setContextPriority(0);
									userApp.setEppn(ldapUsers.get(0).getEppn());
									userApp.setUserRole(Role.MANAGER);
									userAppRepository.save(userApp);
								}
							}
							//On associe à un surveillant
							if(!sls.isEmpty()) {
								for(SessionLocation sl : sls) {
									TagChecker tc =  new TagChecker();
									tc.setContext(ctx);
									tc.setUserApp(userApp);
									tc.setSessionLocation(sl);
									tagCheckerRepository.save(tc);
								}
							}
						}
					}
				}
			}
			i++;
			dataEmitterService.sendDataImport(String.valueOf(i).concat("/").concat(total));
		}
		logService.log(ACTION.ADE_IMPORT, RETCODE.SUCCESS, "Import évènements : " + i, eppn, null, emargementContext, eppn);
	}
}