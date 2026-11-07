package com.yaswanth.Alumni_Bridge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.ApplicationService;
import com.yaswanth.Alumni_Bridge.service.EventService;
import com.yaswanth.Alumni_Bridge.service.JobService;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService appService;

    @Autowired
    private EventService eventService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) {
            return "redirect:/login";
        }

        // Accurately count Alumni
        model.addAttribute("alumniCount", userService.countAlumni());
        model.addAttribute("jobCount", jobService.getAllJobs().size());
        model.addAttribute("eventCount", eventService.getAllEvents().size());
        model.addAttribute("applicationCount", appService.getUserApplications(user).size());

        return "dashboard";
    }
}
