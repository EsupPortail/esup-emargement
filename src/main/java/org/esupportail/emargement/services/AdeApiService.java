package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeInstructorBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.exceptions.AdeApiRequestException;
import org.xml.sax.SAXException;

public interface AdeApiService {
    public String getFatherIdResource(String sessionId, String idItem, String category, String tree);
    public List<AdeClassroomBean> getListClassrooms(String sessionId, String idItem, List<Long> selectedIds, Context ctx) throws ParseException;
    public List<AdeClassroomBean> getListClassrooms2(String sessionId, String idItem, List<Long> selectedIds)  throws  ParseException;
    public List<AdeInstructorBean> getListInstructors(String sessionId, String fatherId, String idItem);
    public Map<String,String> getItemsFromInstructors(String sessionId, String choice);
    public Map<String, String> getClassroomsList(String sessionId);
    public Map<String, AdeResourceBean> getActivityFromResource(String sessionId, String resourceId, String activityId) throws IOException;
    public boolean haveAnyMemberGroupsBeenUpdated(AdeResourceBean ade, String sessionId, Context ctx);
    public List<String> getMembersOfEvent(String sessionId, String idResource, String target, Context ctx);
    public boolean isResourceFolder(String sessionId, String resourceId) throws Exception;
    public Map<Long, String> getResourceLeavesIdNameMap(String sessionId, String resourceId) throws Exception;
    public Map<String, String> getMapComposantesFormations(String sessionId, String category);
    public List<AdeResourceBean> getEventsFromXml(String sessionId, String resourceId, String strDateMin, String strDateMax, List<Long> idEvents, String existingSe, boolean update, Context ctx) throws IOException, ParseException;
    public boolean eventExistsInAde(SessionEpreuve se, String sessionId) throws Exception;
    public String getIdComposante(String sessionId, String code, String category, boolean isEvent) throws IOException, ParserConfigurationException, SAXException;
	public Map<String, String> getProjectLists(String sessionId) throws AdeApiRequestException;
	public String getConnectionProject(String numProject, String sessionId) throws IOException, ParserConfigurationException, SAXException;
	public String getSessionId(boolean forceNewId, String emargementContext, String idProject) throws IOException, ParserConfigurationException, SAXException;
	public void disconnectSession(String emargementContext);
    public String getJsonfile(String fatherId, String emargementContext, String category, String idProject);
    public String getVersioneEtape(String sessionId, String idItem) throws IOException, ParserConfigurationException, SAXException;
}
