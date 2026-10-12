package org.esupportail.emargement.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.beans.ParticipantStart;
import org.esupportail.emargement.domain.CalendarDTO;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import flexjson.JSONSerializer;

@Service
public class CalendarService {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Resource
	PreferencesService preferencesService;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Value("${app.url}")
	private String appUrl;
	
	private static final String DEFAULT_COLOR = "#3788d8";
	private static final Map<String, String> STATUS_COLORS = Map.of(
	        "OPENED", "#198754",   // vert Bootstrap
	        "CLOSED", "#dc3545",   // rouge Bootstrap
	        "STANDBY", "#fd7e14"   // orange Bootstrap
	);
	public String getEvents(String start, String end, boolean isAll, String view,
	                        String calendarPref, String emargementContext, String from) {

	    String eppn = SecurityContextHolder.getContext().getAuthentication().getName();

	    if (!isAll && calendarPref != null) {
	        preferencesService.updatePrefs(calendarPref, view == null ? "" : view, eppn, emargementContext, "dummy");
	    }

	    Date startDate = Date.from(OffsetDateTime.parse(start).toInstant());
	    Date endDate = Date.from(OffsetDateTime.parse(end).toInstant());

	    List<SessionEpreuve> sessions = findSessions(startDate, endDate, isAll, view, eppn);

	    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	    DateFormat hourFormat = new SimpleDateFormat("HH:mm");
	    boolean supervisor = "supervisor".equals(from);

	    // évite un findByEppnAndContext par session (N+1) : un appel par contexte
	    Map<Long, Boolean> accessByContext = new HashMap<>();

	    List<CalendarDTO> events = new ArrayList<>(sessions.size());
	    for (SessionEpreuve se : sessions) {
	        boolean isFromContext = accessByContext.computeIfAbsent(se.getContext().getId(),
	                id -> userAppRepository.findByEppnAndContext(eppn, se.getContext()) != null);
	        
	        events.add(toCalendarDTO(se, isAll, isFromContext, supervisor, dateFormat, hourFormat));
	    }

	    return new JSONSerializer().deepSerialize(events);
	}
	
	public String getParticipantEvents(String start, String end) {
		String eppn = SecurityContextHolder.getContext().getAuthentication().getName();

		Date startDate = Date.from(OffsetDateTime.parse(start).toInstant());
		Date endDate = Date.from(OffsetDateTime.parse(end).toInstant());

		List<TagCheck> tagChecks = tagCheckRepository.findForParticipantCalendar(eppn, startDate, endDate);

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat hourFormat = new SimpleDateFormat("HH:mm");

		List<CalendarDTO> events = new ArrayList<>();

		for (TagCheck tagCheck : tagChecks) {
			SessionEpreuve se = tagCheck.getSessionEpreuve();
			if (se == null) {
				continue;
			}
			List<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
			String professeur = "";
			String salle = "";

			if (!tagCheckers.isEmpty()) {
				salle = tagCheckers.stream().map(TagChecker::getSessionLocation).filter(Objects::nonNull)
						.map(sl -> sl.getLocation().getNom()).distinct().collect(Collectors.joining(", "));

				List<UserApp> userApps = tagCheckers.stream().map(TagChecker::getUserApp).filter(Objects::nonNull)
						.distinct().collect(Collectors.toList());

				userAppService.setNomPrenom(userApps, false);
				professeur = userApps.stream().map(u -> u.getNom() + " " + u.getPrenom()).sorted()
						.collect(Collectors.joining(", "));
			}
			events.add(toParticipantCalendarDTO(se, tagCheck, dateFormat, hourFormat, professeur, salle));
		}

		return new JSONSerializer().deepSerialize(events);
	}
	
	private String getParticipantStatus(TagCheck tagCheck) {

	    if (Boolean.TRUE.equals(tagCheck.getAbsence())) {
	        return "absent";
	    }

	    if (tagCheck.getTagDate() != null || tagCheck.getTagDate2() != null) {
	        return "emarge";
	    }

	    return "a-emarger";
	}
	
	private CalendarDTO toParticipantCalendarDTO(SessionEpreuve se, TagCheck tagCheck, DateFormat dateFormat,
			DateFormat hourFormat, String professeur, String salle) {

		String statut = se.getStatutSession().getKey();
		String day = dateFormat.format(se.getDateExamen());
		Date endDay = se.getDateFin() != null ? se.getDateFin() : se.getDateExamen();
		LocalDate today = LocalDate.now();
		LocalDate debut = toLocalDate(se.getDateExamen());
		LocalDate fin = se.getDateFin() != null ? toLocalDate(se.getDateFin()) : debut;
		boolean enCours = !today.isBefore(debut) && !today.isAfter(fin);
		boolean isUserQrCodeEnabled = appliConfigService.isUserQrCodeEnabled();
		boolean isSessionQrCodeEnabled = appliConfigService.isSessionQrCodeEnabled();

		boolean dispo = enCours
		        && isUserQrCodeEnabled && isSessionQrCodeEnabled
		        && tagCheck.getSessionLocationExpected() != null
		        && tagCheck.getSessionLocationBadged() == null
		        && tagCheck.getTagDate() == null
		        && !Boolean.TRUE.equals(se.getIsSessionLibre());

		CalendarDTO calendar = new CalendarDTO();
		calendar.setId(se.getId());
		calendar.setQrCodeAvailable(dispo);
		if (dispo) {
			calendar.setQrEppn(tagCheck.getPerson().getEppn());
			calendar.setQrSession(tagCheck.getSessionLocationExpected().getId());
		}
		String participantStatus = getParticipantStatus(tagCheck);

		calendar.setTitle(se.getNomSessionEpreuve());
		calendar.setStart(day + "T" + hourFormat.format(se.getHeureEpreuve()));

		if (se.getFinEpreuve() != null) {
			calendar.setEnd(dateFormat.format(endDay) + "T" + hourFormat.format(se.getFinEpreuve()));
		}

		calendar.setAllDay(se.getDateFin() != null && !day.equals(dateFormat.format(se.getDateFin())));
		calendar.setColor(STATUS_COLORS.getOrDefault(statut, DEFAULT_COLOR));
		calendar.setClassNames(new String[] { "participant-" + participantStatus });
		calendar.setProfesseur(professeur);
		calendar.setSalle(salle);
		calendar.setSite(se.getCampus().getSite());
		calendar.setTypeSession(se.getTypeSession().getKey());
		return calendar;
	}

	private List<SessionEpreuve> findSessions(Date start, Date end, boolean isAll, String view, String eppn) {
	    if (isAll) {
	        return view == null
	                ? sessionEpreuveRepository.getAllSessionEpreuveForCalendar(start, end)
	                : sessionEpreuveRepository.getAllSessionEpreuveForCalendarByContext(start, end, Long.valueOf(view));
	    }
	    if (view == null) {
	        return sessionEpreuveRepository
	                .findAllByDateExamenLessThanEqualAndDateFinGreaterThanEqualOrDateExamenGreaterThanEqualAndDateExamenLessThanEqualOrDateFinGreaterThanEqualAndDateFinLessThanEqual(
	                        start, end, start, end, start, end);
	    }
	    return tagCheckerRepository
	            .findByUserAppEppnAndSessionLocationSessionEpreuveDateExamenLessThanEqualAndSessionLocationSessionEpreuveDateFinGreaterThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateExamenGreaterThanEqualAndSessionLocationSessionEpreuveDateExamenLessThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateFinGreaterThanEqualAndSessionLocationSessionEpreuveDateFinLessThanEqual(
	                    eppn, start, end, eppn, start, end, eppn, start, end)
	            .stream()
	            .map(tc -> tc.getSessionLocation().getSessionEpreuve())
	            .distinct()
	            .collect(Collectors.toList());
	}

	private CalendarDTO toCalendarDTO(SessionEpreuve se, boolean isAll, boolean isFromContext, boolean supervisor,
			DateFormat dateFormat, DateFormat hourFormat) {

		String statut = se.getStatutSession().getKey();
		String day = dateFormat.format(se.getDateExamen());

		Date endDay = se.getDateFin() != null ? se.getDateFin() : se.getDateExamen();
		CalendarDTO c = new CalendarDTO();
		c.setId(se.getId());
		c.setTitle(isAll ? se.getContext().getKey().toUpperCase() + " : " + se.getNomSessionEpreuve()
				: se.getNomSessionEpreuve());
		c.setStart(day + "T" + hourFormat.format(se.getHeureEpreuve()));
		if (se.getFinEpreuve() != null) {
			c.setEnd(dateFormat.format(endDay) + "T" + hourFormat.format(se.getFinEpreuve()));
		}
		c.setAllDay(se.getDateFin() != null && !day.equals(dateFormat.format(se.getDateFin())));
		c.setColor(STATUS_COLORS.getOrDefault(statut, DEFAULT_COLOR));
		c.setUrl(buildUrl(se, statut, isFromContext, supervisor));
		return c;
	}

	private String buildUrl(SessionEpreuve se, String statut, boolean isFromContext, boolean supervisor) {
	    if (!isFromContext) {
	        return "#";
	    }
	    String ctx = se.getContext().getKey();
	    if (supervisor) {
	        // requête faite uniquement quand elle est utile
	        List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(se);
	        String slId = sls.isEmpty() ? "" : String.valueOf(sls.get(0).getId());
	        return String.format("%s/%s/supervisor/presence?sessionEpreuve=%s&location=%s&keyStatut=%s&from=supervisor",
	                appUrl, ctx, se.getId(), slId, statut);
	    }
	    return String.format("%s/%s/manager/sessionEpreuve/%s", appUrl, ctx, se.getId());
	}
	
	public List<LocalDate> getDatesInPeriod(Date startDate, Date endDate) {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate start = toLocalDate(startDate);
		LocalDate end = toLocalDate(endDate);
		while (!start.equals(end)) {
			dates.add(start);
			start = start.plusDays(1);
		}
		dates.add(end);
		return dates;
	}

	public static LocalDate toLocalDate(Date date) {
		Date lDate = new Date(date.getTime());
		return lDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public ParticipantStart defaultParticipantStart(String eppn, LocalDate today) {
	    Date from = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    Optional<LocalDate> next = tagCheckRepository.findNextEventDate(eppn, from)
	            .map(d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

	    if (next.isEmpty()) return new ParticipantStart("history", today);
	    LocalDate d = next.get();

	    if (d.equals(today)) return new ParticipantStart("today", today);
	    if (!d.isAfter(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))))
	        return new ParticipantStart("week", d);
	    if (YearMonth.from(d).equals(YearMonth.from(today)))
	        return new ParticipantStart("month", d);
	    return new ParticipantStart("planning", d);   // autre mois : la date décide du mois affiché
	}
}
