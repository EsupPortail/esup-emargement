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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
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
import org.json.JSONObject;
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

	public List<AdeClassroomBean> getListClassrooms(String sessionId, String fatherId, String idItem, List<Long> selectedIds)  throws  ParseException {
		String detail = "9";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlClassroom = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=false&detail=" +detail + "&category=classroom&fatherIds=" + fatherId + idParam;
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
	
    public List<String> getMembersOfEvent(String sessionId, String idResource, String target) {
        String detail = "13";
        String urlMembers = String.format("%s?sessionId=%s&function=getResources&tree=false&id=%s&detail=%s", urlAde, sessionId, idResource, detail);
        List<String> listMembers = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(urlMembers);
            doc.getDocumentElement().normalize();

            NodeList resourceList = doc.getElementsByTagName("resource");
            String adeAttribute = appliConfigService.getAdeMemberAttribute();
            for (int i = 0; i < resourceList.getLength(); i++) {
                Element resourceElement = (Element) resourceList.item(i);
                if ("members".equals(target)) {
                    NodeList memberList = resourceElement.getElementsByTagName("member");
                    for (int j = 0; j < memberList.getLength(); j++) {
                        Element memberElement = (Element) memberList.item(j);
                        listMembers.add(memberElement.getAttribute("id"));
                    }
                } else if (adeAttribute.equals(target)) {
                    String code = resourceElement.getAttribute(adeAttribute);
                    if("email".equals(adeAttribute)) {
						// Si plusieurs mails, on prend le 1er
						String[] splitCode = code.split(";");
						code = splitCode[0];
                    }
                    listMembers.add(code);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Erreur lors de la récupération des membres de l'évènement, url : " + urlMembers, e);
        }
        return listMembers;
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

	public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, boolean update) throws IOException, ParseException {
		String detail = "8";
		List<AdeResourceBean> adeBeans = new ArrayList<>();
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
						if(resourceId != null) {
							adeResourceBean.setDegreeId(Long.valueOf(resourceId));
						}
						Long eventId = Long.valueOf(element.getAttribute("id"));
						boolean isAlreadyimport = (sessionEpreuveRepository.countByAdeEventId(eventId) >0)? true : false;
						if(existingSe == null && !isAlreadyimport || "true".equals(existingSe)|| update){
							SessionEpreuve se = null;
							String activityId = element.getAttribute("activityId");
							adeResourceBean.setActivityId(Long.valueOf(activityId));
							if(update) {
								List<SessionEpreuve> ses = sessionEpreuveRepository.findByAdeEventId(eventId);
								se = (ses != null) ? ses.get(0) :  null;
								Map<String, AdeResourceBean>  activities2 = getActivityFromResource(sessionId, null, activityId);
								AdeResourceBean beanActivity2 = activities2.get(activityId);
								if(beanActivity2!= null) {
									adeResourceBean.setTypeEvent(beanActivity2.getTypeEvent());
								}
							}else {
								se = new SessionEpreuve();
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
												Long id = !element3.getAttribute("id").isEmpty()? Long.valueOf(element3.getAttribute("id")) : null;
												String name = element3.getAttribute("name");
												if(appliConfigService.getCategoriesAde().get(1).equals(category)){
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
			String idProject, boolean update, String typeSync, List<Long> groupes, Long dureeMax, String codePref, String typePref) throws ParseException {
		Context ctx = contextRepository.findByContextKey(emargementContext);
		int i = 0;
		String total = String.valueOf(beans.size());
		int maj = 0;
		StopWatch time = new StopWatch( );
		time.start( );
		for(AdeResourceBean ade : beans) {
			if(sessionEpreuveRepository.countByAdeEventId(ade.eventId)==0 && !update || update){
				SessionEpreuve se = ade.getSessionEpreuve();
				boolean isUpdateOk = false;
				Date today = DateUtils.truncate(new Date(),  Calendar.DATE);
				if(update && se.getDateExamen().compareTo(today)>=0) {
					if(se.getDateImport() != null && ade.getLastUpdate().compareTo(se.getDateImport())>=0) {
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
				if(!update || update && isUpdateOk){
					processSessionEpreuve(se, campus, ctx);
					processTypeSession(ade, se, ctx);
					sessionEpreuveRepository.save(se);
					processStudents(se, ade, sessionId, groupes, ctx);
					List<SessionLocation> sls = new ArrayList<>();
					processLocations(se, ade, sessionId, ctx, sls);
					//repartition
					sessionEpreuveService.executeRepartition(se.getId(), "alpha");
					processInstructors(ade, sessionId, ctx, sls, codePref, typePref);
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
	
	private void processStudents(SessionEpreuve se, AdeResourceBean ade, String sessionId, List<Long> groupes, Context ctx) {
		if(appliConfigService.isMembersAdeImport()) {
			List<Map<Long, String>> listAdeTrainees = ade.getTrainees();
			if(listAdeTrainees!= null && !listAdeTrainees.isEmpty()) {
				List<String> allMembers = new ArrayList<>();
				for(Map<Long, String> map : listAdeTrainees) {
					for (Entry<Long, String> entry : map.entrySet()) {
						List<String> membersOfEvent = getMembersOfEvent(sessionId, entry.getKey().toString(), "members");
						if(!membersOfEvent.isEmpty()) {
							allMembers.addAll(membersOfEvent);
						}
					}
				}
				List <String> allCodes = new ArrayList<>();
				String adeAttribute = appliConfigService.getAdeMemberAttribute();
				if(!allMembers.isEmpty()) {
					for(String id : allMembers) {
						allCodes.add(getMembersOfEvent(sessionId, id, adeAttribute).get(0));
					}
				}
				String filter = "code".equals(adeAttribute)? "supannEtuId" : "mail";
				Map<String, LdapUser> users =  ldapService.getLdapUsersFromNumList(allCodes, filter);
				if(!allCodes.isEmpty()) {
					for (String code : allCodes) {
						List<Person> persons = personRepository.findByNumIdentifiant(code);
						Person person = null;
						if(!persons.isEmpty()) {
							person = persons.get(0);
						}else {
							person = new Person();
							person.setContext(ctx);
							if(!users.isEmpty()) {
								LdapUser ldapUser = users.get(code);
								person.setEppn(ldapUser.getEppn());
								person.setNumIdentifiant(ldapUser.getNumEtudiant());
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
						if(!groupes.isEmpty()) {
							groupeService.addPerson(person, groupes);
						}
					}
				}
			}
		}
	}
	
	private void processLocations(SessionEpreuve se, AdeResourceBean ade, String sessionId, Context ctx, List<SessionLocation> sls) throws ParseException {
		List<Map<Long, String>> listAdeClassRooms = ade.getClassrooms();
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
	}
	
	private void processInstructors(AdeResourceBean ade, String sessionId, Context ctx, List<SessionLocation> sls, String codePref, String typePref) {
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
								userApp.setUserRole(Role.MANAGER);
							}
							userAppRepository.save(userApp);
							if(typePref!=null && codePref!=null) {
								preferencesService.updatePrefs(typePref, codePref, eppn, ctx.getKey());
							}
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
			String codeComposante, List<String> idList) throws IOException, ParserConfigurationException, SAXException, ParseException{
        List<AdeResourceBean> adeResourceBeans = new ArrayList<>();

		if("myEvents".equals(codeComposante)) {
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			List<LdapUser> ldapUsers = ldapService.getUsers(auth.getName());
			if(!ldapUsers.isEmpty()) {
				String supannEmpId = ldapUsers.get(0).getNumPersonnel();
				String fatherId = getIdComposante(sessionId, supannEmpId, "instructor", true);
				adeResourceBeans = getEventsFromXml(sessionId, fatherId, strDateMin, strDateMax, idEvents, existingSe, false);
			}	
		}else if(idList !=null && !idList.isEmpty()){
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
	
	public static String formatDate(String date) {
		String [] splitDate = date.split("-");
		return splitDate[1].concat("/").concat(splitDate[2]).concat("/").concat(splitDate[0]);
	}
	
	public void updateSessionEpreuve(List<SessionEpreuve> seList, String emargementContext, String typeSync) throws IOException, ParserConfigurationException, SAXException, ParseException {
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
				saveEvents(beans, sessionId, emargementContext, null, idProject, true, typeSync, null, null, null, null);
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
	
	public int importEvents(List<Long> idEvents, String emargementContext, String strDateMin, String strDateMax,
			String newGroupe, List<Long> existingGroupe, String existingSe, String codeComposante,
			Campus campus, List<String> idList, List<AdeResourceBean> beans, String idProject, Long dureeMax, String codePref, String typePref)
			throws IOException, ParserConfigurationException, SAXException, ParseException {
		int nbImports = 0;
		if (idEvents != null) {
			String sessionId = getSessionId(false, emargementContext);
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if(idProject == null) {
				idProject = getCurrentProject(null, auth.getName(), emargementContext);
			}
			if (getConnectionProject(idProject, sessionId) == null) {
				sessionId = getSessionId(true, emargementContext);
				getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
			if(beans == null) {
				beans = getAdeBeans(sessionId, strDateMin, strDateMax, idEvents, existingSe,
						codeComposante, idList);
			}

			if (!beans.isEmpty()) {
				List<Long> groupes = new ArrayList<>();
				if (appliConfigService.isAdeCampusGroupeAutoEnabled()) {
					if (existingGroupe != null) {
						groupes.addAll(existingGroupe);
					}
					if (newGroupe!=null && !newGroupe.isEmpty()) {
						Groupe groupe = groupeService.createNewGroupe(newGroupe, emargementContext,
								sessionEpreuveService.getCurrentanneUniv(), auth.getName());
						groupes.add(groupe.getId());
					}
				}
				nbImports = saveEvents(beans, sessionId, emargementContext, campus, idProject, false, null, groupes, dureeMax, codePref, typePref);
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
	        preferencesService.updatePrefs(ADE_STORED_PROJET, projet, eppn, emargementContext);
	        return projet;
	    }
	    return getValuesPref(eppn, ADE_STORED_PROJET).isEmpty() ? null : getValuesPref(eppn, ADE_STORED_PROJET).get(0);
	}
}