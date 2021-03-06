package org.esupportail.emargement.services;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.CalendarDTO;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import flexjson.JSONSerializer;

@Service
public class CalendarService {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Resource
	LdapService ldapService;
	
	@Value("${app.url}")
	private String appUrl;
	
	public String getEvents(String context, String start, String end, boolean isAll) throws ParseException{
		List <CalendarDTO> l= new ArrayList<CalendarDTO>();
		//2019-11-25T00:00:00+01:00
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	    Date startDate = formatter.parse(start);
	    Date endDate = formatter.parse(end);
	    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
	    DateFormat hourFormat = new SimpleDateFormat("HH:mm");
	    List<SessionEpreuve> listSe = new ArrayList<SessionEpreuve>();
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    String eppn = ldapService.getEppn(auth.getName());
	    listSe = sessionEpreuveRepository.findAllByDateExamenGreaterThanEqualAndDateExamenLessThanEqual(startDate, endDate);
	    if(isAll) {
	    	listSe = sessionEpreuveRepository.getAllSessionEpreuveForCalendar(startDate, endDate);
	    }
	    if(!listSe.isEmpty()) {
	    	for(SessionEpreuve se : listSe) {
    			UserApp userApp = userAppRepository.findByEppnAndContext(eppn, se.getContext());
    			boolean isFromContext = (userApp != null)? true : false;
	    		CalendarDTO c = new CalendarDTO();
	    		c.setId(se.getId());
	    		String title = (!isAll)? se.getNomSessionEpreuve() : se.getContext().getKey().toUpperCase().concat(" : ").concat(se.getNomSessionEpreuve());
	    		c.setTitle(title);
	    		String strStart = dateFormat.format(se.getDateExamen()).concat("T").concat(hourFormat.format(se.getHeureEpreuve()));  
	    		c.setStart(strStart);
	    		String strSEnd = dateFormat.format(se.getDateExamen()).concat("T").concat(hourFormat.format(se.getFinEpreuve())); 
	    		c.setEnd(strSEnd);
	    		String color = (se.isSessionEpreuveClosed)? "#e54c14" : "#0d9314";
	    		c.setColor(color);
	    		String url  = (!isFromContext)? "#" : appUrl.concat("/").concat(se.getContext().getKey()).concat("/manager/sessionEpreuve/").concat(se.getId().toString());
	    		c.setUrl(url);
	    		l.add(c);
	    	}
	    }
		JSONSerializer serializer = new JSONSerializer();
		String flexJsonString = "Aucune statistique à récupérer";
		flexJsonString = serializer.deepSerialize(l);
		return flexJsonString;

	}

}
