package com.yaswanth.Alumni_Bridge.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yaswanth.Alumni_Bridge.entity.Event;
import com.yaswanth.Alumni_Bridge.entity.EventRegistration;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.EventRegistrationRepository;
import com.yaswanth.Alumni_Bridge.repository.EventRepository;

@Service
public class EventService {

	@Autowired
	private EventRepository eventRepo;

	@Autowired
	private EventRegistrationRepository regRepo;

	public List<Event> getAllEvents() {
		return eventRepo.findAll();
	}

	public Event getEventById(Long id) {
		if (id == null) return null;
		return eventRepo.findById(id).orElse(null);
	}

	public void saveEvent(Event event) {
		eventRepo.save(event);
	}

	public void register(User user, Event event) {
		if (user == null || event == null) {
			return;
		}
		if (!regRepo.existsByUserAndEvent(user, event)) {
			EventRegistration reg = new EventRegistration();
			reg.setUser(user);
			reg.setEvent(event);
			reg.setRegistrationDate(LocalDate.now().toString());
			regRepo.save(reg);
		}
	}

	public boolean isRegistered(User user, Event event) {
		if (user == null || event == null) {
			return false;
		}
		return regRepo.existsByUserAndEvent(user, event);
	}

	public List<EventRegistration> getUserRegistrations(User user) {
		if (user == null) return Collections.emptyList();
		return regRepo.findByUser(user);
	}

	public List<EventRegistration> getEventAttendees(Event event) {
		if (event == null) return Collections.emptyList();
		return regRepo.findByEvent(event);
	}

	@Transactional
	public void deleteEvent(Long id) {
		Event event = getEventById(id);
		if (event != null) {
			regRepo.deleteByEvent(event);
			eventRepo.delete(event);
		}
	}
}
