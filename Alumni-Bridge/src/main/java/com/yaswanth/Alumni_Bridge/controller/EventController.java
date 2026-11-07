package com.yaswanth.Alumni_Bridge.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.yaswanth.Alumni_Bridge.entity.Event;
import com.yaswanth.Alumni_Bridge.entity.EventRegistration;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.EventService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EventController {

	@Autowired
	private EventService eventService;

	// VIEW EVENTS (Batch-preloads registration status to eliminate N+1 queries)
	@GetMapping("/events")
	public String viewEvents(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		List<Event> events = eventService.getAllEvents();
		model.addAttribute("events", events);

		Set<Long> registeredEventIds = eventService.getUserRegistrations(user).stream()
				.filter(r -> r.getEvent() != null)
				.map(r -> r.getEvent().getId())
				.collect(Collectors.toSet());
		model.addAttribute("registeredEventIds", registeredEventIds);

		return "events";
	}

	// CREATE EVENT PAGE
	@GetMapping("/create-event")
	public String createEventPage(HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null || (!"ALUMNI".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/events";
		}

		return "create-event";
	}

	// SAVE EVENT with role verification
	@PostMapping("/save-event")
	public String saveEvent(@ModelAttribute Event event, HttpSession session, Model model) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null || (!"ALUMNI".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/events";
		}

		if (event.getTitle() == null || event.getTitle().trim().isEmpty() || event.getDate() == null) {
			model.addAttribute("error", "Event title and date are required.");
			return "create-event";
		}

		event.setCreatedBy(user);
		eventService.saveEvent(event);

		return "redirect:/events";
	}

	// REGISTER FOR EVENT (Support both GET and POST)
	@RequestMapping(value = "/register-event/{id}", method = {RequestMethod.GET, RequestMethod.POST})
	public String register(@PathVariable Long id, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Event event = eventService.getEventById(id);
		if (event != null) {
			eventService.register(user, event);
		}

		return "redirect:/events";
	}

	// EVENT ATTENDEES VIEW FOR ORGANIZERS
	@GetMapping("/events/{id}/attendees")
	public String viewEventAttendees(@PathVariable Long id, Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Event event = eventService.getEventById(id);
		if (event == null) {
			return "redirect:/events";
		}

		if (event.getCreatedBy() == null || (!event.getCreatedBy().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/events";
		}

		List<EventRegistration> attendees = eventService.getEventAttendees(event);
		model.addAttribute("event", event);
		model.addAttribute("attendees", attendees);

		return "event-attendees";
	}

	// MY REGISTERED EVENTS
	@GetMapping("/my-events")
	public String myEvents(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		List<EventRegistration> regs = eventService.getUserRegistrations(user);
		model.addAttribute("registrations", regs);

		return "my-events";
	}
}
