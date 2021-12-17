package org.esupportail.emargement.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Event;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;

@Service
public class EventService {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	EventRepository eventRepository;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public List<CalendarComponent> geCalendarComponents(List<String> urlList) throws IOException, ParserException {
		List<CalendarComponent> allComponents = new ArrayList<CalendarComponent>();
		try {
			if(!urlList.isEmpty()) {
				for(String url : urlList) {
					InputStream fin = new URL(url).openStream();
					CalendarBuilder builder = new CalendarBuilder();
					Calendar calendar = builder.build(fin);
					if(!calendar.getComponents().isEmpty()) {
						allComponents.addAll(calendar.getComponents());
					}
				}
			}
		} catch (Exception e) {
			log.error("Impossible de récupérer les évènements ics", e);
		}
		return allComponents;
	}
	
	public List<String>  getLocationsFromICs(List<String> urlList) throws IOException, ParserException {
		List<CalendarComponent> components = geCalendarComponents(urlList);
		List<String> locations = components.stream().map(l -> l.getProperty("LOCATION").getValue().trim()).distinct().collect(Collectors.toList());
		locations.removeIf(item -> item == null || "".equals(item));
		return locations;
	}
	
	public List<Event>  getEventsListFromIcs(String context, List<String> urlList) throws IOException, ParserException, ParseException {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
		
		List<CalendarComponent> components = this.geCalendarComponents(urlList);
		List<Event> events = new ArrayList<Event>();
		
		for(CalendarComponent cal : components) {
			Property start = cal.getProperty("DTSTART");
			Property end = cal.getProperty("DTEND");
			Property summary = cal.getProperty("SUMMARY");
			Property location = cal.getProperty("LOCATION");
			Property description = cal.getProperty("DESCRIPTION");
			Property uid = cal.getProperty("UID");
			
			Event event = new Event();
			Context ctx = contextRepository.findByContextKey(context);
			event.setContext(ctx);
			Date starDate = formatter.parse(start.getValue().replaceAll("Z$", "+0000"));
			Date endDate = formatter.parse(end.getValue().replaceAll("Z$", "+0000"));
		    event.setStartDate(starDate);
		    event.setEndDate(endDate);
			event.setSummary(summary.getValue());
			event.setLocation(location.getValue());
			event.setDescription(description.getValue());
			event.setUid(uid.getValue());
			
			events.add(event);
			
		}
		events.sort(Comparator.comparing(Event::getStartDate));
		return events;
	}
	
	public Event searchEventByUid(String uid, String context, List<String> urlList) throws IOException, ParserException, ParseException {
		List<Event> events =  getEventsListFromIcs(context, urlList);
		Event event = events.stream()
				  .filter(e -> uid.equals(e.getUid()))
				  .findAny()
				  .orElse(null);
		return event;
	}
	
	public List<String> getAllUrlList() {
		List<Event> allEvents = eventRepository.findByIsEnabledTrue();
		List<String> urlList = allEvents.stream().map(Event::getUrl).collect(Collectors.toList());
		return urlList;
	}
	
	public List<Event> geteventsById(String context, Long id) throws IOException, ParserException, ParseException{
		
		List<Event> events = new ArrayList<Event>();
		
		Event event = eventRepository.findById(id).get();
		
		if(event!=null) {
			List<String> urls = new ArrayList<String>();
			urls.add(event.getUrl());
			events = getEventsListFromIcs(context,urls);
		}
		
		return events;
	}
	
	public void setNbEvent(List<Event> events) throws IOException, ParserException {
		if(!events.isEmpty()) {
			for(Event event : events) {
				List<String> urls = new ArrayList<String>();
				urls.add(event.getUrl());
				List<CalendarComponent> components = this.geCalendarComponents(urls);
				event.setNbEvent(components.size());
			}
		}
	}
}
