package org.esupportail.emargement.services;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.beans.DashboardData;
import org.esupportail.emargement.beans.DashboardSession;
import org.esupportail.emargement.beans.DashboardWeekDay;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource	
	AppliConfigService appliConfigService;
	
    @Resource
    TagCheckerService tagCheckerService;
    
    @Autowired
    TagCheckerRepository tagCheckerRepository;
    
    @Autowired
    TagCheckRepository tagCheckRepository;
    
    @Autowired
    PrefsRepository prefsRepository;
    
    @Autowired
    SessionEpreuveRepository sessionEpreuveRepository;

	public DashboardData getToday(String contextKey, boolean mesSessions, String emargementContext) {
		
		DashboardData data = new DashboardData();
		LocalDate today = LocalDate.now();

		Date dateDebut = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<SessionEpreuve> ses = getSessions(dateDebut, null, contextKey, mesSessions, true);

		List<DashboardSession> sessions = getTodaySessions(ses, emargementContext);

		data.setTodaySessions(sessions);
		data.setHasSupervisorSessions(
				sessions.stream()
		                .anyMatch(s -> s.getSupervisorUrl() != null));
		data.setSessions(sessions.size());

		int participants = 0;
		int presents = 0;
		int absents = 0;

		int sessionsEnCours = 0;
		
		for (DashboardSession session : sessions) {
			participants += session.getParticipants();
			presents += session.getPresents();
			absents += session.getAbsents();

			if (session.isEnCours()) {
		        sessionsEnCours++;
		    }
		}
		
		data.setEncours(sessionsEnCours);
		data.setParticipants(participants);
		data.setPresents(presents);
		data.setAbsents(absents);

		return data;
	}
	
	public DashboardData getWeek(String contextKey, boolean mesSessions) {

		DashboardData data = new DashboardData();
		LocalDate today = LocalDate.now();
		LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		data.setWeekStart(weekStart);
		data.setWeek(getWeekDays(today, contextKey, mesSessions));

		return data;
	}

	public DashboardData getMonth(String contextKey, boolean mesSessions) {
		DashboardData data = new DashboardData();
		LocalDate today = LocalDate.now();

		data.setMonthStart(today.withDayOfMonth(1));
		data.setMonth(getMonthDays(today, contextKey, mesSessions));

		return data;
	}
	
	public DashboardData getYear(String contextKey, boolean mesSessions) {
		DashboardData data = new DashboardData();
		LocalDate today = LocalDate.now();

		data.setYearStart(today.withDayOfYear(1));
		data.setAnneeUniversitaire(getAnneeUniversitaire(today));
		data.setYear(getYearDays(today, contextKey, mesSessions));

		return data;
	}

	private List<DashboardSession> getTodaySessions(List<SessionEpreuve> ses, String emargementContext) {
		String eppn = WebUtils.getEppn();
		List<Long> sessionIds = ses.stream().map(SessionEpreuve::getId).collect(Collectors.toList());
		
		List<TagChecker> tcs = tagCheckerRepository.findBySessionEpreuveIn(sessionIds);
		tagCheckerService.setNomPrenom4TagCheckers(tcs);
		Map<Long, List<TagChecker>> tagCheckersParSession = new HashMap<>();
		
		List<TagCheck> tagChecks = tagCheckRepository.findBySessionEpreuveIn(sessionIds);
		Map<Long, List<TagCheck>> tagChecksParSession = new HashMap<>();

		for (TagCheck tc : tagChecks) {
		    Long sessionId = tc.getSessionEpreuve().getId();
		    tagChecksParSession
		            .computeIfAbsent(sessionId, k -> new ArrayList<>())
		            .add(tc);
		}
		for (TagChecker tc : tcs) {
			Long sessionId = tc.getSessionLocation().getSessionEpreuve().getId();
			tagCheckersParSession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(tc);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yy");
		List<DashboardSession> result = new ArrayList<>();

		for (SessionEpreuve se : ses) {
			List<TagChecker> sessionTagCheckers = tagCheckersParSession.getOrDefault(se.getId(),
					Collections.emptyList());
			
			TagChecker currentTagChecker = sessionTagCheckers.stream()
			        .filter(tc -> tc.getUserApp() != null)
			        .filter(tc -> eppn.equals(tc.getUserApp().getEppn()))
			        .findFirst()
			        .orElseGet(() -> sessionTagCheckers.stream()
			                .filter(tc -> tc.getUserApp() != null)
			                .findFirst()
			                .orElse(null));
			String supervisorUrl = null;
			boolean isLinkDisplayed = false;
			if (currentTagChecker != null
			        && currentTagChecker.getSessionLocation() != null
			        && currentTagChecker.getSessionLocation().getLocation() != null) {
				String eppnTagChecker = currentTagChecker.getUserApp().getEppn();
				isLinkDisplayed = isLinkDisplayed(eppnTagChecker, se.getContext().getKey(), emargementContext);
			    Long locationId =
			            currentTagChecker.getSessionLocation()
			                    .getId();

			    supervisorUrl = String.format(
			            "/%s/supervisor/presence"
			                    + "?keyStatut=OPENED"
			                    + "&sessionEpreuve=%d"
			                    + "&location=%d"
			                    + "&sessionUser=%s",
			            se.getContext().getKey(),
			            se.getId(),
			            locationId,
			            eppnTagChecker);
			}
			
			List<String> locations = new ArrayList<>();
			List<String> surveillants = new ArrayList<>();

			for (TagChecker tc : sessionTagCheckers) {
				if (tc.getSessionLocation() != null && tc.getSessionLocation().getLocation() != null) {
					locations.add(tc.getSessionLocation().getLocation().getNom());
				}
				if (tc.getUserApp() != null) {
					surveillants.add(tc.getUserApp().getNom() + " " + tc.getUserApp().getPrenom());
				}
			}

			String lieux = StringUtils.join(locations, ", ");
			String nomSurveillants = StringUtils.join(surveillants, ", ");
			String horaire = "";

			if(se.getDateFin() == null || se.getDateExamen().compareTo(se.getDateFin()) == 0) {
				if (se.getHeureEpreuve() != null) {
					horaire = sdf.format(se.getHeureEpreuve());
					if (se.getFinEpreuve() != null) {
						horaire += "–" + sdf.format(se.getFinEpreuve());
					}
				}
			}else {
				horaire = "Fin : " + sdf1.format(se.getDateFin());
			}
			
			List<TagCheck> sessionTagChecks = tagChecksParSession.getOrDefault(se.getId(), Collections.emptyList());

			int inscrits = getNbInscrits(sessionTagChecks);
			int presents = getNbPresents(sessionTagChecks);
			int absents  = getNbAbsents(sessionTagChecks);
			int inconnus = getNbInconnus(sessionTagChecks);
			DashboardSession ds = new DashboardSession(se.getId(), horaire, se.getNomSessionEpreuve(),
					se.getTypeSession().getKey(), se.getContext().getKey(), lieux, nomSurveillants,
					presents, inscrits, absents, inconnus);
			ds.setEnCours(estEnCours(se));
			ds.setTerminee(estTerminee(se));
			ds.setSupervisorUrl(supervisorUrl);
			ds.setLinkDisplayed(isLinkDisplayed);
			result.add(ds);
		}
		return result;
	}
	
	private List<DashboardWeekDay> getWeekDays(LocalDate date, String contextKey, boolean mesSessions) {
		
		LocalDate dateDebut = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate dateFin = dateDebut.plusDays(7);
		Date debut = Date.from(dateDebut.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date fin = Date.from(dateFin.atStartOfDay(ZoneId.systemDefault()).toInstant());
		sessionEpreuveRepository.findSessionsBetween(debut, fin, contextKey);
		List<SessionEpreuve> sessions = getSessions(debut, fin, contextKey, mesSessions, false);
		
		List<Long> sessionIds = sessions.stream()
		        .map(SessionEpreuve::getId)
		        .collect(Collectors.toList());
	
		List<TagCheck> tagChecks = tagCheckRepository.findBySessionEpreuveIn(sessionIds);
			
		Map<Long, List<TagCheck>> tagChecksParSession = new HashMap<>();
		for (TagCheck tc : tagChecks) {
			Long sessionId = tc.getSessionEpreuve().getId();
			tagChecksParSession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(tc);
		}

		Map<String, DashboardWeekDay> resultMap = new LinkedHashMap<>();

		for (SessionEpreuve se : sessions) {
			if (se.getDateExamen() == null
			        || (se.getDateFin() != null
			            && !se.getDateFin().equals(se.getDateExamen()))) {
			    continue;
			}
			LocalDate jour = se.getDateExamen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			String sessionContextKey = se.getContext().getKey();
			String key = jour + "|" + sessionContextKey;
			DashboardWeekDay row = resultMap.computeIfAbsent(key, k -> new DashboardWeekDay(jour, sessionContextKey));
			row.setSessions(row.getSessions() + 1);
			List<TagCheck> sessionTagChecks = tagChecksParSession.getOrDefault(se.getId(), Collections.emptyList());
			addPresenceStatistics(row, sessionTagChecks);
	    }
		for (DashboardWeekDay row : resultMap.values()) {
		    calculateNonEmarges(row);
		}
		List<DashboardWeekDay> result = new ArrayList<>(resultMap.values());
		result.sort(Comparator.comparing(DashboardWeekDay::getDate).thenComparing(DashboardWeekDay::getContextKey));

	    return result;
	}
	
	private List<DashboardWeekDay> getMonthDays(LocalDate date, String contextKey, boolean mesSessions) {
		LocalDate dateDebut = date.withDayOfMonth(1);
		LocalDate dateFin = dateDebut.plusMonths(1);
		Date debut = Date.from(dateDebut.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date fin = Date.from(dateFin.atStartOfDay(ZoneId.systemDefault()).toInstant());

		List<SessionEpreuve> sessions = getSessions(debut, fin, contextKey, mesSessions, false);
		
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}

		List<Long> sessionIds = sessions.stream().map(SessionEpreuve::getId).collect(Collectors.toList());
		List<TagCheck> tagChecks = tagCheckRepository.findBySessionEpreuveIn(sessionIds);
		Map<Long, List<TagCheck>> tagChecksParSession = new HashMap<>();

		for (TagCheck tc : tagChecks) {
			Long sessionId = tc.getSessionEpreuve().getId();
			tagChecksParSession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(tc);
		}

		Map<String, DashboardWeekDay> resultMap = new LinkedHashMap<>();

		for (SessionEpreuve se : sessions) {
			if (se.getDateExamen() == null
			        || (se.getDateFin() != null
			            && !se.getDateFin().equals(se.getDateExamen()))) {
			    continue;
			}
			LocalDate jour = se.getDateExamen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			String sessionContextKey = se.getContext().getKey();
			String key = jour + "|" + sessionContextKey;
			DashboardWeekDay row = resultMap.computeIfAbsent(key, k -> new DashboardWeekDay(jour, sessionContextKey));

			row.setSessions(row.getSessions() + 1);
			List<TagCheck> sessionTagChecks = tagChecksParSession.getOrDefault(se.getId(), Collections.emptyList());
			addPresenceStatistics(row, sessionTagChecks);
		}
		
		for (DashboardWeekDay row : resultMap.values()) {
			calculateNonEmarges(row);
		}

		List<DashboardWeekDay> result = new ArrayList<>(resultMap.values());
		result.sort(Comparator.comparing(DashboardWeekDay::getDate).thenComparing(DashboardWeekDay::getContextKey));

		return result;
	}
	
	private List<DashboardWeekDay> getYearDays(LocalDate date, String contextKey, boolean mesSessions) {
		
		int anneeUniversitaire = date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
		LocalDate dateDebut = LocalDate.of(anneeUniversitaire, 9, 1);
		LocalDate dateFin = dateDebut.plusYears(1);
		Date debut = Date.from(dateDebut.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date fin = Date.from(dateFin.atStartOfDay(ZoneId.systemDefault()).toInstant());

		List<SessionEpreuve> sessions = getSessions(debut, fin, contextKey, mesSessions, false);

		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}

		List<Long> sessionIds = sessions.stream().map(SessionEpreuve::getId).collect(Collectors.toList());
		List<TagCheck> tagChecks = tagCheckRepository.findBySessionEpreuveIn(sessionIds);

		Map<Long, List<TagCheck>> tagChecksParSession = new HashMap<>();

		for (TagCheck tc : tagChecks) {
			Long sessionId = tc.getSessionEpreuve().getId();
			tagChecksParSession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(tc);
		}

		Map<String, DashboardWeekDay> resultMap = new LinkedHashMap<>();

		for (SessionEpreuve se : sessions) {
			if (se.getDateExamen() == null
			        || (se.getDateFin() != null
			            && !se.getDateFin().equals(se.getDateExamen()))) {
			    continue;
			}

			LocalDate jour = se.getDateExamen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate mois = jour.withDayOfMonth(1);

			String sessionContextKey = se.getContext().getKey();
			String key = mois + "|" + sessionContextKey;

			DashboardWeekDay row = resultMap.computeIfAbsent(key, k -> new DashboardWeekDay(mois, sessionContextKey));
			row.setSessions(row.getSessions() + 1);
			List<TagCheck> sessionTagChecks = tagChecksParSession.getOrDefault(se.getId(), Collections.emptyList());
			addPresenceStatistics(row, sessionTagChecks);
		}

		for (DashboardWeekDay row : resultMap.values()) {
			calculateNonEmarges(row);
		}

		List<DashboardWeekDay> result = new ArrayList<>(resultMap.values());
		result.sort(Comparator.comparing(DashboardWeekDay::getDate).reversed()
				.thenComparing(DashboardWeekDay::getContextKey));
		return result;
	}

	private void addPresenceStatistics(DashboardWeekDay row, List<TagCheck> tagChecks) {

	    row.setParticipants(row.getParticipants() + tagChecks.size());

	    for (TagCheck tc : tagChecks) {

	        if (Boolean.TRUE.equals(tc.getIsUnknown())) {
	            row.setUnknown(row.getUnknown() + 1);
	        } else if (tc.getSessionLocationBadged() != null) {
	            row.setPresents(row.getPresents() + 1);
	        } else if (tc.getAbsence() != null) {
	            row.setAbsents(row.getAbsents() + 1);
	        }
	    }
	}
	
	private void calculateNonEmarges(DashboardWeekDay row) {
		int nonEmarges = row.getParticipants() - row.getPresents() - row.getAbsents() - row.getUnknown();
		row.setNonEmarges(Math.max(0, nonEmarges));
	}

    private boolean estEnCours(SessionEpreuve se) {
        if (se.getHeureEpreuve() == null || se.getFinEpreuve() == null) {
            return false;
        }

        Calendar now = Calendar.getInstance();

        int minutesNow =
                now.get(Calendar.HOUR_OF_DAY) * 60
                + now.get(Calendar.MINUTE);

        Calendar debut = Calendar.getInstance();
        debut.setTime(se.getHeureEpreuve());

        int minutesDebut =
                debut.get(Calendar.HOUR_OF_DAY) * 60
                + debut.get(Calendar.MINUTE);

        Calendar fin = Calendar.getInstance();
        fin.setTime(se.getFinEpreuve());

        int minutesFin =
                fin.get(Calendar.HOUR_OF_DAY) * 60
                + fin.get(Calendar.MINUTE);

        return minutesNow >= minutesDebut
                && minutesNow < minutesFin;
    }
    
    private int getNbInscrits(List<TagCheck> tagChecks) {
        return tagChecks.size();
    }

	private int getNbPresents(List<TagCheck> tagChecks) {
		int result = 0;
		for (TagCheck tc : tagChecks) {
			if (tc.getSessionLocationBadged() != null) {
				result++;
			}
		}
		return result;
	}

	private int getNbAbsents(List<TagCheck> tagChecks) {
		int result = 0;
		for (TagCheck tc : tagChecks) {
			if (tc.getAbsence() != null) {
				result++;
			}
		}
		return result;
	}

	private int getNbInconnus(List<TagCheck> tagChecks) {
		int result = 0;
		for (TagCheck tc : tagChecks) {
			if (Boolean.TRUE.equals(tc.getIsUnknown())) {
				result++;
			}
		}
		return result;
	}
    
    private int getNbNonEmarges(List<TagCheck> tagChecks) {

        int inscrits = getNbInscrits(tagChecks);
        int presents = getNbPresents(tagChecks);
        int absents = getNbAbsents(tagChecks);
        int inconnus = getNbInconnus(tagChecks);

        return inscrits - presents - absents - inconnus;
    }
    
    private String getAnneeUniversitaire(LocalDate date) {

        int annee = date.getMonthValue() >= 9
                ? date.getYear()
                : date.getYear() - 1;

        return annee + "–" + (annee + 1);
    }
    
    public String getContextKey(String contextKey) {
    	if(contextKey == null) {
    		contextKey = "";
    	}
    	if(!WebUtils.isSuperAdmin()) {
    		contextKey = ContextHelper.getCurrentContext();
    	}
    	return contextKey;
    }
    
    public static String getRole() {
        if (WebUtils.isSuperAdmin())   return "isSuperAdmin";
        if (WebUtils.isAdmin())        return "isAdmin";
        if (WebUtils.isSuperManager()) return "isSuperManager";
        if (WebUtils.isManager())      return "isManager";
        if (WebUtils.isSupervisor())   return "isSupervisor";
        if (WebUtils.isUser())         return "isUser";
        return "isAnonymous";
    }
    
    private List<SessionEpreuve> getSessions(Date dateDebut, Date dateFin, String contextKey, boolean mesSessions, boolean isToday) {
    	String eppn = WebUtils.getEppn();
        if (mesSessions) {
            return isToday
                    ? sessionEpreuveRepository.findAllSessionsForSupervisor(dateDebut, contextKey, eppn)
                    : sessionEpreuveRepository.findSessionsBetweenForSupervisor(dateDebut, dateFin, contextKey, eppn);
        }
        return isToday
                ? sessionEpreuveRepository.findAllSessions(dateDebut, dateFin, contextKey)
                : sessionEpreuveRepository.findSessionsBetween(dateDebut, dateFin, contextKey);
    }
    
    public List<Prefs> getPrefs(String prefKey){
    	return prefsRepository.findByUserAppEppnAndNom(WebUtils.getEppn(), prefKey);
    }
    
    public boolean isLinkDisplayed(
            String eppnTagChecker,
            String sessionContextKey,
            String currentContextKey) {

        if (!currentContextKey.equals(sessionContextKey)) {
            return false;
        }

        String eppn = WebUtils.getEppn();

        if (appliConfigService.isTagCheckersListDisplayed()) {
            return true;
        }

        return eppn.equals(eppnTagChecker);
    }

    private boolean estTerminee(SessionEpreuve se) {
        if (se.getFinEpreuve() == null) {
            return false;
        }
        LocalTime fin = new java.sql.Time(se.getFinEpreuve().getTime()).toLocalTime();
        return LocalTime.now().isAfter(fin);
    }
}