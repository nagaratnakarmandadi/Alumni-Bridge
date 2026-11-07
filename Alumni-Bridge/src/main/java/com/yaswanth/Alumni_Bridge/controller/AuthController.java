package com.yaswanth.Alumni_Bridge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

	@Autowired
	private UserService service;

	// REGISTER PAGE
	@GetMapping("/register")
	public String showRegister(@RequestParam(required = false) String redirect, Model model) {
		model.addAttribute("user", new User());
		if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
			model.addAttribute("redirect", redirect);
		}
		return "register";
	}

	// REGISTER SAVE with privilege escalation protection & duplicate email handling
	@PostMapping("/register")
	public String register(@ModelAttribute User user, 
						   @RequestParam(required = false) String redirect, 
						   Model model) {
		try {
			// Restrict registration roles strictly to STUDENT or ALUMNI
			if (!"ALUMNI".equals(user.getRole())) {
				user.setRole("STUDENT");
			}

			if (user.getName() == null || user.getName().trim().isEmpty()) {
				model.addAttribute("error", "Full Name is required.");
				return "register";
			}

			if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
				model.addAttribute("error", "Email is required.");
				return "register";
			}

			if (user.getPassword() == null || user.getPassword().length() < 4) {
				model.addAttribute("error", "Password must be at least 4 characters long.");
				return "register";
			}

			service.registerUser(user);

			String target = "/login?registered=true";
			if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
				target += "&redirect=" + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8);
			}
			return "redirect:" + target;
		} catch (IllegalArgumentException e) {
			model.addAttribute("error", e.getMessage());
			if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
				model.addAttribute("redirect", redirect);
			}
			return "register";
		}
	}

	// LOGIN PAGE
	@GetMapping("/login")
	public String showLogin(@RequestParam(required = false) String registered,
							@RequestParam(required = false) String error,
							@RequestParam(required = false) String reason,
							@RequestParam(required = false) String redirect,
							Model model) {
		if ("true".equals(registered)) {
			model.addAttribute("success", "Account created successfully! Please sign in.");
		}
		if ("blocked".equals(error)) {
			model.addAttribute("error", "Your account is deactivated. Please contact an administrator.");
		}
		if ("auth_required".equals(reason)) {
			model.addAttribute("info", "Please sign in or register to access the Alumni Directory, career jobs, and community events.");
		}
		if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
			model.addAttribute("redirect", redirect);
		}
		return "login";
	}

	// LOGIN PROCESS with blocked check and safe target redirection
	@PostMapping("/login")
	public String loginUser(@RequestParam String email, 
							@RequestParam String password,
							@RequestParam(required = false) String redirect,
							HttpSession session,
							Model model) {

		User existingUser = service.findByEmail(email);
		if (existingUser != null && Boolean.TRUE.equals(existingUser.getBlocked())) {
			model.addAttribute("error", "Your account has been deactivated. Please contact an administrator.");
			return "login";
		}

		User user = service.loginUser(email, password);

		if (user != null) {
			session.setAttribute("loggedUser", user);

			if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
				return "redirect:" + redirect;
			}

			if ("ADMIN".equals(user.getRole())) {
				return "redirect:/admin/dashboard";
			} else {
				return "redirect:/dashboard";
			}
		}

		model.addAttribute("error", "Invalid credentials");
		if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
			model.addAttribute("redirect", redirect);
		}
		return "login";
	}

	// LOGOUT (Support GET and POST)
	@RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
	public String logout(HttpSession session) {
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/login";
	}

	@GetMapping("/")
	public String home() {
		return "index";
	}
}
