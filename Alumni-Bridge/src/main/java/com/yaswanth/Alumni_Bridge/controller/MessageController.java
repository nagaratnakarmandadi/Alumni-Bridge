package com.yaswanth.Alumni_Bridge.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.yaswanth.Alumni_Bridge.dto.ConversationSummary;
import com.yaswanth.Alumni_Bridge.dto.MessageResponseDto;
import com.yaswanth.Alumni_Bridge.entity.Message;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.service.MessageService;
import com.yaswanth.Alumni_Bridge.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MessageController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

    // CHAT PAGE (HTML)
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

    // SEND MESSAGE (Traditional Form Fallback)
    @PostMapping("/send-message")
    public String sendMessage(@RequestParam Long receiverId,
                              @RequestParam String content,
                              HttpSession session) {
        User sender = (User) session.getAttribute("loggedUser");
        if (sender == null) {
            return "redirect:/login";
        }

        User receiver = userService.getUserById(receiverId);
        if (receiver != null && content != null && !content.trim().isEmpty() && !sender.getId().equals(receiver.getId())) {
            msgService.sendMessage(sender, receiver, content.trim());
        }

        return "redirect:/chat/" + receiverId;
    }

    // REST API: GET CONVERSATION (AJAX Polling)
    @GetMapping("/api/messages/{otherUserId}")
    @ResponseBody
    public ResponseEntity<?> getConversationApi(@PathVariable Long otherUserId, HttpSession session) {
        User current = (User) session.getAttribute("loggedUser");
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        User other = userService.getUserById(otherUserId);
        if (other == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Mark incoming messages as seen
        msgService.markAsSeen(current, other);

        List<Message> messages = msgService.getConversation(current, other);
        List<MessageResponseDto> dtos = new ArrayList<>();

        for (Message m : messages) {
            boolean isMine = m.getSender() != null && m.getSender().getId().equals(current.getId());
            String timeStr = (m.getTimestamp() != null) ? m.getTimestamp().format(TIME_FORMATTER) : "";
            dtos.add(new MessageResponseDto(
                m.getId(),
                m.getSender() != null ? m.getSender().getId() : null,
                m.getSender() != null ? m.getSender().getName() : "Unknown",
                m.getReceiver() != null ? m.getReceiver().getId() : null,
                m.getContent(),
                timeStr,
                isMine,
                m.isSeen()
            ));
        }

        return ResponseEntity.ok(dtos);
    }

    // REST API: SEND MESSAGE (Fetch API)
    @PostMapping("/api/messages/send")
    @ResponseBody
    public ResponseEntity<?> sendMessageApi(@RequestParam Long receiverId,
                                            @RequestParam String content,
                                            HttpSession session) {
        User sender = (User) session.getAttribute("loggedUser");
        if (sender == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content cannot be empty");
        }

        if (sender.getId().equals(receiverId)) {
            return ResponseEntity.badRequest().body("Cannot send message to yourself");
        }

        User receiver = userService.getUserById(receiverId);
        if (receiver == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipient not found");
        }

        Message saved = msgService.sendMessage(sender, receiver, content.trim());
        String timeStr = (saved.getTimestamp() != null) ? saved.getTimestamp().format(TIME_FORMATTER) : "";

        MessageResponseDto dto = new MessageResponseDto(
            saved.getId(),
            sender.getId(),
            sender.getName(),
            receiver.getId(),
            saved.getContent(),
            timeStr,
            true,
            false
        );

        return ResponseEntity.ok(dto);
    }
}
