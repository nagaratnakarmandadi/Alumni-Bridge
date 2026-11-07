package com.yaswanth.Alumni_Bridge.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AlumniController {

	@Autowired
	private UserService userService;

	@GetMapping("/alumni")
	public String viewAlumni(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role, 
			@RequestParam(required = false) String company,
			@RequestParam(required = false) String skill,
			@RequestParam(required = false) String gradYear,
			@RequestParam(required = false) String error,
			Model model, HttpSession session) {

		User currentUser = (User) session.getAttribute("loggedUser");
		if (currentUser == null) {
			return "redirect:/login";
		}

		List<User> users = userService.searchUsers(keyword, role, company, skill, gradYear, currentUser.getId());

		model.addAttribute("users", users);
		model.addAttribute("keyword", keyword != null ? keyword : "");
		model.addAttribute("role", role != null ? role : "");
		model.addAttribute("company", company != null ? company : "");
		model.addAttribute("skill", skill != null ? skill : "");
		model.addAttribute("gradYear", gradYear != null ? gradYear : "");
		if ("not_found".equals(error)) {
			model.addAttribute("errorMessage", "The requested user profile was not found.");
		}

		return "alumni-list";
	}

	@GetMapping("/view-user/{id}")
	public String viewUser(@PathVariable Long id, Model model, HttpSession session) {
		User currentUser = (User) session.getAttribute("loggedUser");
		if (currentUser == null) {
			return "redirect:/login";
		}

		User user = userService.getUserById(id);
		if (user == null) {
			return "redirect:/alumni?error=not_found";
		}

		model.addAttribute("user", user);
		model.addAttribute("currentUser", currentUser);

		return "view-user";
	}
}
