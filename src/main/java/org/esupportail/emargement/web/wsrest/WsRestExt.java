package org.esupportail.emargement.web.wsrest;

import javax.annotation.Resource;

import org.esupportail.emargement.services.EsupSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws")
public class WsRestExt {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	EsupSignatureService esupSignatureService;
	
	@GetMapping(value = "/return-test")
	@ResponseBody
	public ResponseEntity<Void> returnTest(@RequestParam String signRequestId,
			@RequestParam String status, @RequestParam String step) {
		//ws/return-test?signRequestId=3555&status=completed&step=end 
		log.info("Interrogation depuis esup-signature ..." + signRequestId + ", " + status + ", " + step);
		if("completed".equals(status) && "end".equals(step)) {
			esupSignatureService.saveFromEsupSignature(Long.valueOf(signRequestId), status);
			log.info("signRequestId : " + signRequestId + " - step : " + step + " - status : + " + status);
		}
		return ResponseEntity.ok().build();
	}
}
