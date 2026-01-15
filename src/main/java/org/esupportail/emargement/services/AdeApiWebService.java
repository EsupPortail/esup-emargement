package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeInstructorBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.exceptions.AdeApiRequestException;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.utils.ToolUtil;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class AdeApiWebService implements AdeApiService {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	final static String ADE_STORED_SESSION = "adeStoredSession";
	
	private final static String ADE_STORED_PROJET = "adeStoredProjet";
	
//	private final static String pathCopyFile = "/opt/ade";

	public final static String ADE_STORED_COMPOSANTE = "adeStoredComposante";
	
	@Value("${emargement.ade.api.url}")
	private String urlAde;
	
	@Value("${emargement.ade.api.login}")
	private String loginAde;
	
	@Value("${emargement.ade.api.password}")
	private String passwordAde;
	
	@Value("${emargement.ade.api.url.encrypted}")
	private String encryptedUrl;
	
	@Value("${emargement.ade.api.auth_type}")
	private String adeApiAuthType;

	@Autowired
	private PrefsRepository prefsRepository;
	
	@Autowired
	private LocationRepository locationRepository;
	
	@Autowired
	private CampusRepository campusRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;

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
    
    public String getFatherIdResource(String sessionId, String idItem, String category, String tree){
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

	public List<AdeClassroomBean> getListClassrooms(String sessionId, String idItem, List<Long> selectedIds, Context ctx)  throws  ParseException {
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
									boolean isAlreadyimport = (locationRepository.countByAdeClassRoomIdAndContext(id, ctx) >0)? true : false;
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
		return toolUtil.sortByValue(items);
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

	    return toolUtil.sortByValue(mapClassrooms);
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
	        String adeSuperGroupe =  appliConfigService.getAdeSuperGroupe(ctx).trim();
	        if ("members".equals(target)) {
	        	log.info("Début de récupération des membres ... pour ressource : " + idResource + " -- Contexte : " + ctx.getKey());
	            NodeList memberNodes = (NodeList) xpath.evaluate("//resource/allMembers/member", doc, XPathConstants.NODESET);
	            for (int i = 0; i < memberNodes.getLength(); i++) {
	                Element memberElement = (Element) memberNodes.item(i);
	                String category = memberElement.getAttribute("category");
	                String memberId = memberElement.getAttribute("id");
	                if (appliConfigService.getCategoriesAde(ctx).equals(category) || !adeSuperGroupe.isEmpty() && adeSuperGroupe.equals(category)) {
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
	                if(!adeSuperGroupe.isEmpty()){
	                	NodeList resourceNodes1 = (NodeList) xpath.evaluate(
	    	                    "//resource[@category='" + adeSuperGroupe + "']/@" + adeAttribute, doc, XPathConstants.NODESET);
    	                for (int i = 0; i < resourceNodes1.getLength(); i++) {
    	                    String code = resourceNodes1.item(i).getNodeValue();
    	                    if ("email".equals(adeAttribute)) {
    	                        code = code.split(";")[0];
    	                    }
    	                    listMembers.add(code);
    	                }
	                }
	            }
	        }
	    } catch (Exception e) {
	        log.error("Erreur lors de la récupération des membres de l'évènement, url : " + urlMembers, e);
	        e.printStackTrace();
	    }
	    return new ArrayList<>(listMembers);
 	}
	
	public boolean isResourceFolder(String sessionId, String resourceId) throws Exception {
		String detail = "4"; // Niveau min pour avoir attribut isGroup
		String url = String.format("%s?sessionId=%s&function=getResources&tree=false&id=%s&detail=%s",
			urlAde, sessionId, resourceId, detail
		);
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(url);
			doc.getDocumentElement().normalize();
	
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();

			NodeList resourceNodes = (NodeList) xpath.evaluate(
				"/resources/resource/@isGroup", doc, XPathConstants.NODESET
			);
			if (0 == resourceNodes.getLength()) {
				throw new Exception("Ressource ADE #"+resourceId+" non trouvée");
			} else if (1 < resourceNodes.getLength()) {
				throw new Exception("Plusieurs ressources ADE #"+resourceId+" trouvées. Improbable");
			}

			String isGroupStr = resourceNodes.item(0).getNodeValue();
			if (null == isGroupStr) {
				throw new Exception("Attribut isGroup non renseigné pour la ressources ADE #"+resourceId);
			}

			switch (isGroupStr) {
				case "false":
					return false;
				case "true":
					return true;
				default:
					throw new Exception("Valeur isGroup ["+isGroupStr+"] inattendue pour la ressources ADE #"+resourceId);
			}

		} catch (Exception e) {
			log.error("Erreur au moment de déterminer si une ressource est de type dossier ou pas, url : " + url, e);
			e.printStackTrace();
			throw e;
		}
	}

	public Map<Long, String> getResourceLeavesIdNameMap(String sessionId, String resourceId) throws Exception {
		String detail = "4"; // Niveau min pour avoir attribut isGroup
		String url = String.format("%s?sessionId=%s&function=getResources&tree=false&leaves=true&fatherIds=%s&detail=%s",
			urlAde, sessionId, resourceId, detail
		);

		Map<Long, String> resourceLeaves = new HashMap<Long, String>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(url);
			doc.getDocumentElement().normalize();
	
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();

			NodeList resourceNodes = (NodeList) xpath.evaluate(
				"/resources/resource[@isGroup='false']", doc, XPathConstants.NODESET
			);

			for (int i = 0; i < resourceNodes.getLength(); i++) {
				Long id = Long.valueOf(resourceNodes.item(i).getAttributes().getNamedItem("id").getTextContent());
				String name = resourceNodes.item(i).getAttributes().getNamedItem("name").getTextContent();
				resourceLeaves.put(id, name);
			}

			return resourceLeaves;
		} catch (Exception e) {
			log.error("Erreur au moment de récupérer les (id, name) des feuilles de la resource "+resourceId+", url : " + url, e);
			e.printStackTrace();
			throw e;
		}
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

        return toolUtil.sortByValue(map);
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
			String urlEvents = urlAde + "?sessionId=" + sessionId + "&function=getEvents&startDate="+ toolUtil.formatDate(strDateMin) + 
					"&endDate=" + toolUtil.formatDate(strDateMax) + "&resources=" + resourceId + "&detail=" +detail;
			setEvents(urlEvents, adeBeans, existingSe, sessionId, resourceId, update, ctx);
		}
		return adeBeans;
	}

	public boolean eventExistsInAde(SessionEpreuve se, String sessionId) throws Exception {
		String urlEvent = urlAde + "?sessionId=" + sessionId + "&function=getEvents" + "&eventId=" + se.getAdeEventId()
				+ "&detail=8";
		Document doc = getDocument(urlEvent);
		doc.getDocumentElement().normalize();
		NodeList list = doc.getElementsByTagName("event");
		if (list.getLength() == 0) {
			return false;
		}
		// ✅ Vérification du couple (eventId + activityId)
		for (int i = 0; i < list.getLength(); i++) {
			Element event = (Element) list.item(i);
			String eventId = event.getAttribute("id");
			String activityId = event.getAttribute("activityId");
			if (eventId == null || activityId == null) {
				continue;
			}
			if (eventId.equals(String.valueOf(se.getAdeEventId()))
					&& activityId.equals(String.valueOf(se.getAdeActiviteId()))) {
				return true;
			}
		}
		return false;
	}

	private boolean isOldEnoughToDelete(SessionEpreuve se) {
	    return se.getDateCreation().toInstant()
	            .isBefore(Instant.now().minus(24, ChronoUnit.HOURS));
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
						String activityId = element.getAttribute("activityId");
						Long activityIdValue = Long.valueOf(activityId);
						boolean isAlreadyimport = false;
						Date dateExamen = formatter.parse(element.getAttribute("date"));
						Date heureDebut = formatter1.parse(element.getAttribute("startHour"));
						Date heureFin = formatter1.parse(element.getAttribute("endHour"));
						if(sessionEpreuveRepository.countByAdeActiviteIdAndDateExamenAndHeureEpreuveAndFinEpreuveAndContext(activityIdValue, dateExamen, heureDebut, heureFin, ctx)>0){
							isAlreadyimport = true;
						}else if(sessionEpreuveRepository.countByAdeEventIdAndContext(eventId, ctx) >0) {
							isAlreadyimport = true;
						}
						if(existingSe == null && !isAlreadyimport && !update|| "true".equals(existingSe)|| update){
							SessionEpreuve se = null;
							if(isAlreadyimport) {
								List<SessionEpreuve> ses = null;
								if(!sessionEpreuveRepository.findByAdeActiviteIdAndDateExamenAndHeureEpreuveAndFinEpreuveAndContext(activityIdValue, dateExamen, heureDebut, heureFin, ctx).isEmpty()) {
									ses = sessionEpreuveRepository.findByAdeActiviteIdAndDateExamenAndHeureEpreuveAndFinEpreuveAndContext(activityIdValue, dateExamen, heureDebut, heureFin, ctx);
									se = ses.get(0);
								}else if (!sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx).isEmpty()) {
									ses = sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx);
									se = ses.get(0);
								}
							}else {
								se = new SessionEpreuve();
							}
							adeResourceBean.setActivityId(activityIdValue);
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
								se.setDateExamen(dateExamen);
								se.setHeureEpreuve(heureDebut);
								se.setFinEpreuve(heureFin);
								se.setAdeEventId( Long.valueOf(element.getAttribute("id")));
								se.setAdeActiviteId(activityIdValue);
								//pour l'instant
								if(!campusRepository.findByContext(ctx).isEmpty()) {
									se.setCampus(campusRepository.findByContext(ctx).get(0));
								}
								if(isAlreadyimport) {
									if(!sessionEpreuveRepository.findByAdeActiviteIdAndDateExamenAndHeureEpreuveAndFinEpreuveAndContext(activityIdValue, dateExamen, heureDebut, heureFin, ctx).isEmpty()) {
										se.setDateCreation(sessionEpreuveRepository.findByAdeActiviteIdAndDateExamenAndHeureEpreuveAndFinEpreuveAndContext(activityIdValue, dateExamen, heureDebut, heureFin, ctx).get(0).getDateCreation());
									}else if (!sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx).isEmpty()) {
										se.setDateCreation(sessionEpreuveRepository.findByAdeEventIdAndContext(eventId, ctx).get(0).getDateCreation());
									}
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
																if(appliConfigService.isAdeVetDisplayed(ctx)) {
																	String adeVet = getVersioneEtape(sessionId, element3.getAttribute("id"));
																	adeResourceBean.setVet(adeVet);
																	se.setAdeVET(adeVet);
																}
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
								adeResourceBean.setSessionEpreuve(se);
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
		return getDocument(url, null, null);
	}
	
	public Document getDocument(String url, String login, String password) throws IOException, ParserConfigurationException, SAXException {
		URL urlConnect = new URL(url);
		HttpURLConnection con = (HttpURLConnection)urlConnect.openConnection();

		if (null != login) {
		    String encoded = Base64.getEncoder().encodeToString((login+":"+password).getBytes(StandardCharsets.UTF_8));
		    con.setRequestProperty("Authorization", "Basic "+encoded);
		}

		InputStream inputStream = con.getInputStream();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream);
		inputStream.close();
		return doc;
	}
/*	
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
*/	
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
		Map<String, String> mapProjects = new HashMap<>();
		try {
			String url = String.format("%s?sessionId=%s&function=getProjects&detail=4", urlAde, sessionId);

			Document doc = getDocument(url);
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList projects = (NodeList) xpath.evaluate("//projects/project", doc, XPathConstants.NODESET);

			for (int i = 0; i < projects.getLength(); i++) {
				Element project = (Element) projects.item(i);
				mapProjects.put(project.getAttribute("id"), project.getAttribute("name"));
			}

		} catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
			log.error(""+e);
			throw new AdeApiRequestException("ERREUR: Impossible de récupérer la liste des projets ADE");
		}

		return toolUtil.sortByValue(mapProjects);
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
				String login = null;
				String password = null;
				if ("basic".equals(adeApiAuthType)) {
					urlConnexion = urlAde + "?function=connect";
					login = loginAde;
					password = passwordAde;
				} else if (!encryptedUrl.isEmpty()) {
					urlConnexion = urlAde + "?data=" + encryptedUrl;
				} else {
					urlConnexion = urlAde + "?function=connect&login=" + loginAde  + "&password=" + passwordAde;
				}
				Document doc = getDocument(urlConnexion, login, password);
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

    public String getJsonfile(String fatherId, String emargementContext, String category, String idProject) {
        String prettyJson = null;
        String rootComposante = "";
        ObjectMapper xmlMapper = new XmlMapper();
        try {
            String sessionId = getSessionIdByProjectId(idProject, emargementContext);
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

	public String getVersioneEtape(String sessionId, String idItem) throws IOException, ParserConfigurationException, SAXException {
		String detail = "11";
		String idParam = (idItem!=null)? "&id=" + idItem : "";
		String urlVet = urlAde + "?sessionId=" + sessionId + "&function=getResources&tree=true&detail=" + detail + "&category=trainee"+ idParam;
		String vet = "";
		try {
	    	  InputStream input = new URL(urlVet).openStream();
	          SAXBuilder sax = new SAXBuilder();
	          /// https://rules.sonarsource.com/java/RSPEC-2755 , prevent xxe
	          sax.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
	          sax.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
	          org.jdom2.Document doc = sax.build(input);
	          org.jdom2.Element rootNode = doc.getRootElement();
	          List<org.jdom2.Element> list = rootNode.getChildren("category");
	          for (org.jdom2.Element target : list) {
	        	  List<org.jdom2.Element> branch = target.getChildren("branch");
	        	  for (org.jdom2.Element target1 : branch) {
	        		  List<org.jdom2.Element> branch2 = target1.getChildren("branch");
	        		  for (org.jdom2.Element target2 : branch2) {
	        			  String code = target2.getAttributeValue("code");
							if(code!=null) {
								String splitCode [] = code.split("_");
								if(splitCode.length>1) {
									vet = splitCode[1];
								}
							}
	        		  }
	        	  }
	          }
		}catch (IOException | JDOMException e) {
	    	  log.error("Erreur lors de la récupération de la vet, url : " + urlVet, e);
	    }
		return vet;
	}
}
