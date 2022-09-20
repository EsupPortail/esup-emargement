package org.esupportail.emargement.web.wsrest;

import java.text.ParseException;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PresenceService;
import org.esupportail.emargement.services.SessionLocationService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.web.supervisor.SearchLongPollController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Transactional
@RequestMapping("/wsrest/nfc")
@Controller 
public class WsRestEsupNfcController {
	
	@Resource
	SearchLongPollController searchLongPollController;
	
	@Resource
	SessionLocationService sessionLocationService;

	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
    @Resource
    ContextService contextService;
    
    @Resource
    PresenceService presenceService;
    
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Example :
	 * curl -v -H "Content-Type: application/json" http://localhost:8080/wsrest/nfc/locations?eppn=joe@univ-ville.fr
	 */
	@RequestMapping(value="/locations",  method=RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public List<String> getLocations(@RequestParam String eppn) {
	    
		List<String> sessionLocations = sessionLocationService.findWsRestLocations(eppn, contextService.getDefaultContext());
		
	    return sessionLocations;
	}
	
	/**
	 * Example :
	 * curl -v -X POST -H "Content-Type: application/json" -d '{"eppn":"joe@univ-ville.fr","location":"SHS - Amphi120", "eppnInit":"jack@univ-ville.fr"}' http://localhost:8080/wsrest/nfc/isTagable 
	 * @throws ParseException 
	 */
	@RequestMapping(value="/isTagable",  method=RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> isTagable(@RequestBody EsupNfcTagLog esupNfcTagLog) throws ParseException {
		HttpHeaders responseHeaders = new HttpHeaders();
		String eppn = esupNfcTagLog.getEppn();
		
		boolean isTagable =  tagCheckService.tagAction(eppn, esupNfcTagLog,"isTagable") ;

		if(isTagable){
			return new ResponseEntity<String>("OK", responseHeaders, HttpStatus.OK);
		}else {
			log.warn("Erreur 'isTagable' des Ws Rest pour l'eppn : " + eppn );
			logService.log(ACTION.WSREST_ISTAGABLE, RETCODE.FAILED, "Eppn : " + eppn, eppn, null, contextService.getDefaultContext(), esupNfcTagLog.getEppnInit());
			return new ResponseEntity<String>("Personne non trouvée", responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	
	/**
	 * Example :
	 * curl -v -X POST -H "Content-Type: application/json" -d '{"eppn":"joe@univ-ville.fr","location":"SHS - Amphi120", "eppnInit":"jack@univ-ville.fr"}' http://localhost:8080/wsrest/nfc/validateTag
	 * @throws ParseException 
	 */
	@RequestMapping(value="/validateTag",  method=RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> validateTag(@RequestBody EsupNfcTagLog esupNfcTagLog) throws ParseException {
		HttpHeaders responseHeaders = new HttpHeaders();
		String eppn = esupNfcTagLog.getEppn();
		boolean isOk =  tagCheckService.tagAction(eppn, esupNfcTagLog, "validateTag") ;
		try {
			String keyContext = tagCheckService.getContextWs(esupNfcTagLog);
			searchLongPollController.handleCard(esupNfcTagLog, keyContext);
		} catch (Exception e) {
			log.error("Problème de search LongPoll", e);
		}

		if(isOk){
			return new ResponseEntity<String>("OK", responseHeaders, HttpStatus.OK);
		} else {
			log.warn("Erreur 'validateTag' des Ws Rest pour l'eppn : " + eppn );
			logService.log(ACTION.WSREST_VALIDATETAG, RETCODE.FAILED, "Eppn : " + eppn, eppn, null, contextService.getDefaultContext(), esupNfcTagLog.getEppnInit());
			return new ResponseEntity<String>("Erreur de validation de présence", responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value="/display",  method=RequestMethod.POST)
	@ResponseBody
	public String display(@RequestBody EsupNfcTagLog taglog, Model uiModel) {
		String photo64 = presenceService.getBase64Photo(taglog);
		String image ="";

		try {
			if(photo64 != null) {
				String close = "<script>$('#displayModal').on('show.bs.modal', function(){ var myModal = $(this);" +
				        "clearTimeout(myModal.data('hideInterval'));" +
				        "myModal.data('hideInterval', setTimeout(function(){ " +
				         "myModal.modal('hide');"+
				        "}, 1750));});</script>";
					
					image= "<h1>" + taglog.getFirstname() + " " + taglog.getLastname() + "</h1><p><img width='225' height='282' class='img-fluid img-thumbnail' alt='...' "
				    		+ "src = 'data:image/jpeg;base64, " + photo64 + "' /></p>" + close;
			}
		} catch (Exception e) {
			log.info("Pas d'affichage d'image");
		}
		return image;
	}
	


}
