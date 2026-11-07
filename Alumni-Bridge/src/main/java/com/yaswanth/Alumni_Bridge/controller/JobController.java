package com.yaswanth.Alumni_Bridge.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.yaswanth.Alumni_Bridge.entity.*;
import com.yaswanth.Alumni_Bridge.service.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class JobController {

	@Autowired
	private JobService jobService;

	@Autowired
	private ApplicationService appService;

	// VIEW JOBS (Batch-preloads applied status to eliminate N+1 queries)
	@GetMapping("/jobs")
	public String viewJobs(@RequestParam(required = false) String error, Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		List<Job> jobs = jobService.getAllJobs();
		model.addAttribute("jobs", jobs);

		// Preload applied job IDs to avoid N+1 query loop
		Set<Long> appliedJobIds = appService.getUserApplications(user).stream()
				.filter(a -> a.getJob() != null)
				.map(a -> a.getJob().getId())
				.collect(Collectors.toSet());
		model.addAttribute("appliedJobIds", appliedJobIds);

		if ("self_apply".equals(error)) {
			model.addAttribute("errorMessage", "You cannot apply to a job that you posted.");
		}

		return "jobs";
	}

	// VIEW JOB DETAILS
	@GetMapping("/jobs/{id}")
	public String viewJobDetails(@PathVariable Long id, Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Job job = jobService.getJobById(id);
		if (job == null) {
			return "redirect:/jobs";
		}

		boolean isApplied = appService.alreadyApplied(user, job);
		boolean isOwner = job.getPostedBy() != null && (job.getPostedBy().getId().equals(user.getId()) || "ADMIN".equals(user.getRole()));

		model.addAttribute("job", job);
		model.addAttribute("isApplied", isApplied);
		model.addAttribute("isOwner", isOwner);

		return "job-details";
	}

	// POST JOB PAGE
	@GetMapping("/post-job")
	public String createJobPage(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null || (!"ALUMNI".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/jobs";
		}

		model.addAttribute("job", new Job());
		return "post-job";
	}

	// SAVE JOB
	@PostMapping("/save-job")
	public String saveJob(@ModelAttribute Job job, HttpSession session, Model model) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null || (!"ALUMNI".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/jobs";
		}

		if (job.getTitle() == null || job.getTitle().trim().isEmpty() ||
			job.getCompany() == null || job.getCompany().trim().isEmpty()) {
			model.addAttribute("job", job);
			model.addAttribute("error", "Job title and company name are required.");
			return "post-job";
		}

		job.setPostedBy(user);
		jobService.saveJob(job);

		return "redirect:/jobs";
	}

	// APPLY JOB with optional resume upload
	@RequestMapping(value = "/apply/{jobId}", method = {RequestMethod.GET, RequestMethod.POST})
	public String applyJob(@PathVariable Long jobId,
			@RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
			HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Job job = jobService.getJobById(jobId);
		if (job == null) {
			return "redirect:/jobs";
		}

		// Prevent job poster from applying to own job
		if (job.getPostedBy() != null && job.getPostedBy().getId().equals(user.getId())) {
			return "redirect:/jobs?error=self_apply";
		}

		if (!appService.alreadyApplied(user, job)) {
			Application app = new Application();
			app.setJob(job);
			app.setUser(user);
			app.setStatus("APPLIED");
			app.setAppliedDate(LocalDate.now().toString());

			// Process Resume Upload if attached
			if (resumeFile != null && !resumeFile.isEmpty()) {
				try {
					String origName = resumeFile.getOriginalFilename();
					String ext = "";
					if (origName != null && origName.contains(".")) {
						ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
					}

					List<String> allowedExtensions = List.of("pdf", "doc", "docx");
					if (allowedExtensions.contains(ext) && resumeFile.getSize() <= 10 * 1024 * 1024) {
						String uploadDir = System.getProperty("user.dir") + "/uploads/resumes/";
						File uploadPath = new File(uploadDir);
						if (!uploadPath.exists()) {
							uploadPath.mkdirs();
						}

						String fileName = UUID.randomUUID().toString() + "." + ext;
						resumeFile.transferTo(new File(uploadPath, fileName));
						app.setResume(fileName);
					}
				} catch (Exception e) {
					// Fallback to saving application if I/O fails
				}
			}

			appService.apply(app);
		}

		return "redirect:/my-applications";
	}

	// RECRUITER APPLICANT REVIEW PORTAL
	@GetMapping("/jobs/{id}/applications")
	public String viewJobApplicants(@PathVariable Long id, Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Job job = jobService.getJobById(id);
		if (job == null) {
			return "redirect:/jobs";
		}

		// Only job poster or admin can review applicants
		if (job.getPostedBy() == null || (!job.getPostedBy().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole()))) {
			return "redirect:/jobs";
		}

		List<Application> applications = appService.getApplicationsByJob(job);
		model.addAttribute("job", job);
		model.addAttribute("applications", applications);

		return "job-applicants";
	}

	// UPDATE APPLICATION STATUS
	@PostMapping("/jobs/application/{id}/status")
	public String updateApplicationStatus(@PathVariable Long id, 
										  @RequestParam String status,
										  @RequestParam Long jobId,
										  HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		Job job = jobService.getJobById(jobId);
		if (job != null && (job.getPostedBy().getId().equals(user.getId()) || "ADMIN".equals(user.getRole()))) {
			appService.updateStatus(id, status);
		}

		return "redirect:/jobs/" + jobId + "/applications";
	}

	// MY APPLICATIONS
	@GetMapping("/my-applications")
	public String myApplications(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedUser");
		if (user == null) {
			return "redirect:/login";
		}

		model.addAttribute("apps", appService.getUserApplications(user));
		return "my-applications";
	}
}
