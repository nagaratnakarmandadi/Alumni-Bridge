package com.yaswanth.Alumni_Bridge.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yaswanth.Alumni_Bridge.entity.Application;
import com.yaswanth.Alumni_Bridge.entity.Job;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.ApplicationRepository;

@Service
public class ApplicationService {

	@Autowired
	private ApplicationRepository repo;

	public Application apply(Application app) {
		if (app.getAppliedDate() == null) {
			app.setAppliedDate(LocalDate.now().toString());
		}
		if (app.getStatus() == null) {
			app.setStatus("APPLIED");
		}
		return repo.save(app);
	}

	public boolean alreadyApplied(User user, Job job) {
		if (user == null || job == null) {
			return false;
		}
		return repo.existsByUserAndJob(user, job);
	}

	public List<Application> getUserApplications(User user) {
		if (user == null) return Collections.emptyList();
		return repo.findByUser(user);
	}

	public List<Application> getApplicationsByUser(User user) {
	    return getUserApplications(user);
	}

	public List<Application> getApplicationsByJob(Job job) {
		if (job == null) return Collections.emptyList();
		return repo.findByJob(job);
	}

	public List<Application> getAllApplications() {
		return repo.findAll();
	}

	public Application getApplicationById(Long id) {
		if (id == null) return null;
		return repo.findById(id).orElse(null);
	}

	public void updateStatus(Long applicationId, String status) {
		Application app = getApplicationById(applicationId);
		if (app != null) {
			app.setStatus(status);
			repo.save(app);
		}
	}
}
