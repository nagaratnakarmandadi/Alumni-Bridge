package com.yaswanth.Alumni_Bridge.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.*;

@Service
public class UserService {

	@Autowired
	private UserRepository repo;

	@Autowired
	private JobRepository jobRepo;

	@Autowired
	private ApplicationRepository appRepo;

	@Autowired
	private EventRepository eventRepo;

	@Autowired
	private EventRegistrationRepository regRepo;

	@Autowired
	private MessageRepository msgRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// REGISTER with password encryption and duplicate check
	public User registerUser(User user) {
		if (user.getId() == null && repo.existsByEmail(user.getEmail())) {
			throw new IllegalArgumentException("An account with email " + user.getEmail() + " already exists.");
		}
		
		// If password is raw (not already BCrypt hashed)
		if (user.getPassword() != null && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}

		if (user.getBlocked() == null) {
			user.setBlocked(false);
		}

		return repo.save(user);
	}

	// Direct update without re-hashing password if already hashed
	public User updateUser(User user) {
		return repo.save(user);
	}

	// LOGIN with BCrypt verification and blocked account guard
	public User loginUser(String email, String password) {
		User user = repo.findByEmail(email);

		if (user != null) {
			if (Boolean.TRUE.equals(user.getBlocked())) {
				return null; // Blocked user cannot authenticate
			}

			boolean matches = false;
			try {
				matches = passwordEncoder.matches(password, user.getPassword());
			} catch (Exception e) {
				matches = false;
			}

			// Fallback migration for legacy plaintext passwords
			if (!matches && user.getPassword() != null && user.getPassword().equals(password)) {
				user.setPassword(passwordEncoder.encode(password));
				repo.save(user);
				matches = true;
			}

			if (matches) {
				return user;
			}
		}

		return null;
	}

	public User findByEmail(String email) {
		return repo.findByEmail(email);
	}

	public List<User> getAllUsers() {
		return repo.findAll();
	}

	public long countAlumni() {
		return repo.countByRole("ALUMNI");
	}

	// Backwards-compatible 3-arg overload
	public List<User> searchUsers(String keyword, String role, Long currentUserId) {
		return searchUsers(keyword, role, null, null, null, null, currentUserId);
	}

	// Backwards-compatible 6-arg overload
	public List<User> searchUsers(String keyword, String role, String company, String skill, String gradYear, Long currentUserId) {
		return searchUsers(keyword, role, company, skill, gradYear, null, currentUserId);
	}

	// Multi-field search querying name, company, skills, branch, degree, graduation year, and mentorship status
	public List<User> searchUsers(String keyword, String role, String company, String skill, String gradYear, Boolean mentorship, Long currentUserId) {
		String cleanKeyword = (keyword != null) ? keyword.trim() : "";
		String cleanRole = (role != null) ? role.trim() : "";
		String cleanCompany = (company != null) ? company.trim() : "";
		String cleanSkill = (skill != null) ? skill.trim() : "";
		String cleanGradYear = (gradYear != null) ? gradYear.trim() : "";

		return repo.searchDirectory(cleanKeyword, cleanRole, cleanCompany, cleanSkill, cleanGradYear, mentorship, currentUserId);
	}

	public User getUserById(Long id) {
		if (id == null) return null;
		return repo.findById(id).orElse(null);
	}

	// Safe cascading deletion
	@Transactional
	public void deleteUser(Long id) {
		User user = repo.findById(id).orElse(null);
		if (user != null) {
			// Remove messages where user is sender or receiver
			msgRepo.deleteBySenderOrReceiver(user, user);

			// Remove event registrations by user
			regRepo.deleteByUser(user);

			// Remove job applications by user
			appRepo.deleteByUser(user);

			// Remove jobs posted by user
			jobRepo.findByPostedBy(user).forEach(job -> {
				appRepo.deleteByJob(job);
				jobRepo.delete(job);
			});

			// Remove events created by user
			eventRepo.findByCreatedBy(user).forEach(event -> {
				regRepo.deleteByEvent(event);
				eventRepo.delete(event);
			});

			repo.delete(user);
		}
	}

	public void toggleBlockUser(Long id) {
		User user = repo.findById(id).orElse(null);

		if (user != null) {
			Boolean blocked = user.getBlocked();
			if (blocked == null) {
				blocked = false;
			}
			user.setBlocked(!blocked);
			repo.save(user);
		}
	}
}
