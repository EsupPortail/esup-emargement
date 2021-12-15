package org.esupportail.emargement.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	LogsRepository logsRepository;
	
	@Resource	
	AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${log.all.retention}")
	private int retentionLogsAll;
	
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
}
