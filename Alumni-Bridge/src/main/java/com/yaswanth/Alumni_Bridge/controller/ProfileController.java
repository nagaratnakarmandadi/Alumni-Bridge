package com.yaswanth.Alumni_Bridge.controller;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yaswanth.Alumni_Bridge.entity.Profile;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.ProfileService;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProfileController {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private UserService userService;

	// VIEW PROFILE
	@GetMapping("/profile")
	public String viewProfile(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		// Refresh user from DB to avoid stale session data
		User dbUser = userService.getUserById(user.getId());
		if (dbUser != null) {
			user = dbUser;
			session.setAttribute("loggedUser", user);
		}

		Profile profile = profileService.getProfile(user);

		model.addAttribute("profile", profile);
		model.addAttribute("user", user);

		return "profile";
	}

	// EDIT PROFILE PAGE
	@GetMapping("/edit-profile")
	public String editProfile(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Profile profile = profileService.getProfile(user);
		if (profile == null) {
			profile = new Profile();
		}

		model.addAttribute("profile", profile);
		return "edit-profile";
	}

	// SAVE PROFILE + SECURE IMAGE UPLOAD
	@PostMapping("/save-profile")
	public String saveProfile(@ModelAttribute Profile profile,
			@RequestParam(value = "image", required = false) MultipartFile file, 
			HttpSession session, Model model, RedirectAttributes redirectAttributes) throws Exception {

		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		// SECURE FILE UPLOAD
		if (file != null && !file.isEmpty()) {
			String originalFilename = file.getOriginalFilename();
			String ext = "";
			if (originalFilename != null && originalFilename.contains(".")) {
				ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
			}

			// Validate file extension whitelist
			List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
			if (!allowedExtensions.contains(ext)) {
				model.addAttribute("profile", profile);
				model.addAttribute("error", "Only image files (JPG, PNG, WebP) are allowed.");
				return "edit-profile";
			}

			// Validate maximum file size (10MB)
			if (file.getSize() > 10 * 1024 * 1024) {
				model.addAttribute("profile", profile);
				model.addAttribute("error", "Image size must not exceed 10MB.");
				return "edit-profile";
			}

			String uploadDir = System.getProperty("user.dir") + "/uploads/";
			File uploadPath = new File(uploadDir);
			if (!uploadPath.exists()) {
				uploadPath.mkdirs();
			}

			// Generate randomized UUID filename (prevents collisions & path traversal)
			String fileName = UUID.randomUUID().toString() + "." + ext;
			file.transferTo(new File(uploadPath, fileName));

			user.setProfileImage(fileName);
			userService.updateUser(user);
			session.setAttribute("loggedUser", user);
		}

		// PROFILE UPDATE
		Profile existingProfile = profileService.getProfile(user);

		if (existingProfile != null) {
			existingProfile.setSkills(profile.getSkills());
			existingProfile.setCompany(profile.getCompany());
			existingProfile.setExperience(profile.getExperience());
			existingProfile.setBio(profile.getBio());
			existingProfile.setDegree(profile.getDegree());
			existingProfile.setBranch(profile.getBranch());
			existingProfile.setGraduationYear(profile.getGraduationYear());
			existingProfile.setPhone(profile.getPhone());
			existingProfile.setLinkedinUrl(profile.getLinkedinUrl());
			existingProfile.setGithubUrl(profile.getGithubUrl());

			profileService.saveProfile(existingProfile);
		} else {
			profile.setUser(user);
			profileService.saveProfile(profile);
		}

		redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
		return "redirect:/profile";
	}
}
