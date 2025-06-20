package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.Task;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TaskRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.web.manager.AdeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class TaskService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	TaskRepository taskRepository;
	
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
	
	@Resource
	ContextService contextService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${log.all.retention}")
	private int retentionLogsAll;
	
	@Value("${emargement.ade.sync.range}")
	private String rangeDays;
	
	@Value("${emargement.ade.import.duree}")
	private String dureeMaxImport;
	
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
	public void updateAdeSessionEpreuve() throws IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
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
			    adeService.updateSessionEpreuve(ses, ctx.getKey(), "cron", ctx);
			    log.info("Fin syncrhonisation ADE");
			}
		}
	}
	
	public void processTask(Task task, String emargementContext, Long dureeMax, int numImport) throws IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		String idProject = task.getAdeProject();
		String sessionId = adeService.getSessionId(false, emargementContext, idProject);
		adeService.getConnectionProject(idProject, sessionId);
		if(adeService.getProjectLists(sessionId).isEmpty()) {
			sessionId = adeService.getSessionId(true, emargementContext, idProject);
			adeService.getConnectionProject(idProject, sessionId);
			log.info("Récupération du projet Ade " + idProject);
		}
		task.setStatus(org.esupportail.emargement.domain.Task.Status.INPROGRESS);
		taskRepository.save(task);
		List<String> idList = Arrays.asList(task.getParam().split(","));
	    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	    List<String> planifications = adeService.getPrefByContext(AdeController.ADE_PLANIFICATION + idProject);
	    if(!planifications.isEmpty()) {
		    int nbJours = Integer.valueOf(planifications.get(0));
		    Date datesDebutFin [] = getStartEndDates(nbJours);
		    String dateDebut = dateFormat.format(datesDebutFin[0]);
		    String dateFin = dateFormat.format(datesDebutFin[1]);
			task.setDateExecution(new Date());
			Context ctx = contextRepository.findByKey(emargementContext);
			List<AdeResourceBean> adebeans = adeService.getAdeBeans(sessionId,
					dateDebut, dateFin, null, null, null, idList, ctx, true);
			List<Long> idEvents = adebeans.stream().map(tc -> tc.getEventId()).collect(Collectors.toList());
			log.info("Id évènements : " + idEvents);
			log.info("Contexte :" + task.getContext().getKey());
			log.info("import tâche :" + task.getLibelle());
			log.info("import # :" + numImport);
			int nbImports = adeService.importEvents(idEvents, emargementContext, dateDebut, dateFin, "", null, "false", 
					null, task.getCampus(), idList, adebeans, idProject, dureeMax, true);
			int total = nbImports + task.getNbModifs();
			task.setNbModifs(total);
			task.setStatus(org.esupportail.emargement.domain.Task.Status.ENDED);
			task.setDateFinExecution(new Date());
			taskRepository.save(task);
	    }else {
	    	log.info("Aucune valeur de planification pour le projet : " + idProject);
	    }
	}
	
	//@Scheduled(cron = "0 38 14 * * ?")
	@Scheduled(cron= "${emargement.ade.import.cron}")
	public void importAdeSession(){
		List<Context> contextList = contextRepository.findAll();
		Long dureeMax =  (dureeMaxImport == null || dureeMaxImport.isEmpty())? null : Long.valueOf(dureeMaxImport);
		if(!contextList.isEmpty()) {
			 ExecutorService executorService = Executors.newFixedThreadPool(10);
			    List<CompletableFuture<Void>> futures = new ArrayList<>();
			for(Context ctx : contextList) {
		        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
		            try {
						List<Task> tasks =  taskRepository.findByContextAndIsActifTrue(ctx);
						String emargementContext = ctx.getKey();
						if(!tasks.isEmpty()) {
							int i = 1;
							StopWatch time = new StopWatch( );
							time.start( );
							log.info("Début import ADE Campus : " + time.getTime());
							for(Task task : tasks) {
								log.info("import # :" + i);
								try {
									processTask(task, emargementContext, dureeMax, i);
									i++;
									logService.log(ACTION.TASK_CREATE, RETCODE.SUCCESS, task.getLibelle(), null,
											null, emargementContext, null);
								} catch (Exception e) {
									log.error("Erreur lors de l'import depuis ADE campus", e);
									task.setStatus(org.esupportail.emargement.domain.Task.Status.FAILED);
									taskRepository.save(task);
									logService.log(ACTION.TASK_CREATE, RETCODE.FAILED, task.getLibelle(), null,
											null, emargementContext, null);
								}
							}
							log.info("Temps total de l'import : " + time.getTime() + "secondes");
						}else {
							log.info("Aucun import à effectuer, la liste des tâches est vide pour le contexte : " +  emargementContext);
						}
		                Thread.sleep(10);
		            } catch (InterruptedException e) {
		            	log.error("Erreur lors de l'import ADE", e);
		            }
		        }, executorService);
		        futures.add(future);
			}
			 CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			    executorService.shutdown();
		}
	}
	
    public Date[] getStartEndDates(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, n - 1);
        Date endDate = calendar.getTime();
        return new Date[]{startDate, endDate};
    }
}