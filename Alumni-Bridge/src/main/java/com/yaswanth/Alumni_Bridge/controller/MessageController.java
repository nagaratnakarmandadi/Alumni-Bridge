package com.yaswanth.Alumni_Bridge.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yaswanth.Alumni_Bridge.dto.ConversationSummary;
import com.yaswanth.Alumni_Bridge.entity.Message;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.MessageService;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MessageController {

    @Autowired
    private MessageService msgService;

    @Autowired
    private UserService userService;

    // INBOX (Grouped by conversation thread)
    @GetMapping("/messages")
    public String inbox(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<ConversationSummary> conversations = msgService.getRecentConversations(user);
        model.addAttribute("conversations", conversations);

        return "messages";
    }

    // CHAT PAGE
    @GetMapping("/chat/{id}")
    public String chat(@PathVariable Long id, Model model, HttpSession session) {
        User current = (User) session.getAttribute("loggedUser");
        if (current == null) {
            return "redirect:/login";
        }

        User other = userService.getUserById(id);
        if (other == null) {
            return "redirect:/alumni";
        }

        // Prevent self-chat
        if (current.getId().equals(other.getId())) {
            return "redirect:/messages";
        }

        // Mark incoming messages as seen
        msgService.markAsSeen(current, other);

        List<Message> messages = msgService.getConversation(current, other);

        model.addAttribute("messages", messages);
        model.addAttribute("receiver", other);
        model.addAttribute("current", current);

        return "chat";
    }

    // SEND MESSAGE
    @PostMapping("/send-message")
    public String sendMessage(@RequestParam Long receiverId,
                              @RequestParam String content,
                              HttpSession session) {
        User sender = (User) session.getAttribute("loggedUser");
        if (sender == null) {
            return "redirect:/login";
        }

        User receiver = userService.getUserById(receiverId);
        if (receiver != null && content != null && !content.trim().isEmpty()) {
            msgService.sendMessage(sender, receiver, content.trim());
        }

        return "redirect:/chat/" + receiverId;
    }
}
