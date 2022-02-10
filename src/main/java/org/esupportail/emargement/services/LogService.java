package org.esupportail.emargement.services;

import java.util.Date;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LogsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

@Service
public class LogService {

	//private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	LogsRepository logsRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	ContextService contexteService;
	
	public static enum RETCODE {
		FAILED, SUCCESS
	}
	
	public static enum TYPE {
		SYSTEM, USER
	}
	
	public static enum ACTION {
		AJOUT_SURVEILLANT, UPDATE_SURVEILLANT, DELETE_SURVEILLANT, AJOUT_LOCATION, UPDATE_LOCATION, DELETE_LOCATION,
		AJOUT_CAMPUS, UPDATE_CAMPUS, DELETE_CAMPUS, AJOUT_CONFIG, UPDATE_CONFIG, DELETE_CONFIG, AJOUT_SESSION_EPREUVE, 
		UPDATE_SESSION_EPREUVE, DELETE_SESSION_EPREUVE, COPY_SESSION_EPREUVE, EXECUTE_REPARTITION, RESET_REPARTITION, AJOUT_SESSION_LOCATION,
		UPDATE_SESSION_LOCATION, DELETE_SESSION_LOCATION, AJOUT_AGENT, UPDATE_AGENT, DELETE_AGENT, IMPORT_INSCRIPTION,
		UPDATE_INSCRIPTION, EXPORT_CSV, EXPORT_PDF, SEND_CONVOCATIONS, WSREST_LOCATIONS, WSREST_ISTAGABLE, WSREST_VALIDATETAG, CLOSE_SESSION,
		AFFINER_REPARTITION, AJOUT_CONTEXTE, UPDATE_CONTEXTE, DELETE_CONTEXTE, AJOUT_HELP, UPDATE_HELP, DELETE_HELP, PURGE_LOG,
		AJOUT_EVENT, UPDATE_EVENT, DELETE_EVENT, SWITCH_USER, AJOUT_GROUPE, UPDATE_GROUPE, DELETE_GROUPE, IMPORT_GROUPE, ARCHIVE_SESSIONS,
		CLEAN_PERSONS, AJOUT_TYPESESSION, UPDATE_TYPESESSION, DELETE_TYPESESSION, SEND_LINK, REPORT_LINK_TOO_LATE, REPORT_LINK_TOO_SOON, SEND_QRCODE,
		SEND_PDF_EXPORT
		
	}
	
	@Resource
	LdapService ldapService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void log( ACTION action, RETCODE success, String comment, String eppnCible, String ip, String emargementContext, String wsEppn) {

		TYPE type = TYPE.SYSTEM;
		String eppn = "system";
	
		String remoteAddress = "";
		if(ip !=null){
			remoteAddress = ip;
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(wsEppn != null) {
			eppn = wsEppn;
		}else{
			if(auth != null) {
				eppn = auth.getName();
				type = TYPE.USER;
				
				// Switch User Par un Supervisor ?
			/*	String supervisorUsername = supervisorService.findCurrentSupervisorUsername();
				if(supervisorUsername != null) {
					eppn = supervisorUsername + "@" + auth.getName();
					type = TYPE.SUPERVISOR;
				}*/
				
				// IP ?
				if(ip == null){
				Object detail = auth.getDetails();
					if(detail instanceof WebAuthenticationDetails) {
						WebAuthenticationDetails webAuth = (WebAuthenticationDetails) detail;
						remoteAddress = webAuth.getRemoteAddress();
					}
				}
			} 
		}
		Context context = null;
		if(emargementContext==null) {
			context = contexteService.getcurrentContext();
		}else {
			context = contextRepository.findByContextKey(emargementContext);
		}

		Date logDate = new Date();

		Log log = new Log();
		log.setLogDate(logDate);
		log.setEppn(eppn);
		log.setType(type.name());
		log.setAction(action.name());
		log.setRetCode(success.name());
		log.setComment(comment);
		log.setCibleLogin(eppnCible);
		log.setRemoteAddress(remoteAddress);
		log.setContext(context);
		logsRepository.save(log);
	}
	
}

