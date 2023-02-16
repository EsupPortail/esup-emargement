package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.time.DateUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class TaskService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired	
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	LogsRepository logsRepository;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	@Resource	
	AdeService adeService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${log.all.retention}")
	private int retentionLogsAll;
	
	@Value("${emargement.ade.sync.range}")
	private String rangeDays;
	
	public void purgecontext(Context ctx, int daysNb, String ctxKey) {
		Date currentDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDate);
		cal.add(Calendar.DAY_OF_MONTH, -daysNb);
		Date datePurge = cal.getTime();
		List<Log> logs2purge = logsRepository.findLogByContextAndLogDateLessThan(ctx, datePurge);
		if(!logs2purge.isEmpty()) {
			for(Log log: logs2purge) {
				logsRepository.delete(log);
			}
			log.info(logs2purge.size() + " logs en base vieux de " + daysNb  + " jours purgés");
			logService.log(ACTION.PURGE_LOG, RETCODE.SUCCESS, logs2purge.size() + " logs en base vieux de " + daysNb  + " jours purgés", "system", null, 
					ctxKey, null);
		}
	}
	
	
	@Scheduled(cron= "${log.cron.purge}")
	public void purge() {
		List<Context> contextList = contextRepository.findAll();
		if(!contextList.isEmpty()) {
			for(Context ctx : contextList) {
				int daysNb = appliConfigService.getRetentionLogs(ctx);
				purgecontext(ctx, daysNb, ctx.getKey()) ;
			}
		}
		//Pour le contexte "all"
		purgecontext(null, retentionLogsAll, "all") ;
	}
	
	@Scheduled(cron= "${emargement.ade.sync.cron}")
	public void updateAdeSessionEpreuve() throws IOException, ParserConfigurationException, SAXException, ParseException {
		List<Context> contextList = contextRepository.findAll();
		if(!contextList.isEmpty()) {
			for(Context ctx : contextList) {
				Date today = DateUtils.truncate(new Date(),  Calendar.DATE);
				List<SessionEpreuve> ses = null;
				if(rangeDays != null && !rangeDays.isEmpty()) {
					Date dt = new Date();
					Calendar c = Calendar.getInstance(); 
					c.setTime(dt); 
					c.add(Calendar.DATE, Integer.valueOf(rangeDays));
					Date endDate = DateUtils.truncate(c.getTime(),  Calendar.DATE);
					ses = sessionEpreuveRepository.findByContextAndDateExamenGreaterThanEqualAndDateExamenLessThanEqual(ctx, today, endDate);
				}
				else {
					ses = sessionEpreuveRepository.findByContextAndDateExamenGreaterThanEqual(ctx, today);
				}
				log.info("Début syncrhonisation ADE");
			    adeService.updateSessionEpreuve(ses, ctx.getKey(), "cron");
			    log.info("Fin syncrhonisation ADE");
			}
		}
	}
}
