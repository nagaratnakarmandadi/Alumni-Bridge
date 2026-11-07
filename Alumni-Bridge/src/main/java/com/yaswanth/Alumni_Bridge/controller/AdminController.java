package com.yaswanth.Alumni_Bridge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ApplicationService appService;

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    // DASHBOARD with analytics
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalAlumni", userService.countAlumni());
        model.addAttribute("totalJobs", jobService.getAllJobs().size());
        model.addAttribute("totalEvents", eventService.getAllEvents().size());
        model.addAttribute("totalApplications", appService.getAllApplications().size());

        return "admin-dashboard";
    }

    // USERS MANAGEMENT
    @GetMapping("/users")
    public String users(@RequestParam(required = false) String error, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        model.addAttribute("users", userService.getAllUsers());
        if ("self_action".equals(error)) {
            model.addAttribute("errorMessage", "Action denied: You cannot delete or block your own administrator account.");
        }
        return "admin-users";
    }

    @RequestMapping(value = "/delete-user/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        // Prevent admin self-deletion
        if (user.getId().equals(id)) {
            return "redirect:/admin/users?error=self_action";
        }

        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @RequestMapping(value = "/toggle-block/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String blockUser(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        // Prevent admin self-blocking
        if (user.getId().equals(id)) {
            return "redirect:/admin/users?error=self_action";
        }

        userService.toggleBlockUser(id);
        return "redirect:/admin/users";
    }

    // JOBS MANAGEMENT
    @GetMapping("/jobs")
    public String jobs(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        model.addAttribute("jobs", jobService.getAllJobs());
        return "admin-jobs";
    }

    @RequestMapping(value = "/delete-job/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteJob(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        jobService.deleteJob(id);
        return "redirect:/admin/jobs";
    }

    // EVENTS MANAGEMENT
    @GetMapping("/events")
    public String events(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        model.addAttribute("events", eventService.getAllEvents());
        return "admin-events";
    }

    @RequestMapping(value = "/delete-event/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteEvent(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        eventService.deleteEvent(id);
        return "redirect:/admin/events";
    }

    // APPLICATION OVERSIGHT
    @GetMapping("/applications")
    public String applications(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (!isAdmin(user)) return "redirect:/dashboard";

        model.addAttribute("applications", appService.getAllApplications());
        return "admin-applications";
    }
}
