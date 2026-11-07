package com.yaswanth.Alumni_Bridge.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yaswanth.Alumni_Bridge.entity.Job;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.ApplicationRepository;
import com.yaswanth.Alumni_Bridge.repository.JobRepository;

@Service
public class JobService {

	@Autowired
	private JobRepository repo;

	@Autowired
	private ApplicationRepository appRepo;

	public Job saveJob(Job job) {
		return repo.save(job);
	}

	public List<Job> getAllJobs() {
		return repo.findAll();
	}

	public List<Job> getJobsByPostedBy(User user) {
		return repo.findByPostedBy(user);
	}

	public Job getJobById(Long id) {
		if (id == null) return null;
		return repo.findById(id).orElse(null);
	}

	@Transactional
	public void deleteJob(Long id) {
		Job job = repo.findById(id).orElse(null);
		if (job != null) {
			appRepo.deleteByJob(job);
			repo.delete(job);
		}
	}
}
