package com.yaswanth.Alumni_Bridge.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yaswanth.Alumni_Bridge.dto.ConversationSummary;
import com.yaswanth.Alumni_Bridge.entity.Message;
import com.yaswanth.Alumni_Bridge.entity.User;
import com.yaswanth.Alumni_Bridge.repository.MessageRepository;

@Service
public class MessageService {

	@Autowired
	private MessageRepository repo;

	public Message sendMessage(User sender, User receiver, String content) {
		if (sender == null || receiver == null) return null;
		if (sender.getId().equals(receiver.getId())) return null;
		if (content == null || content.trim().isEmpty()) return null;
		if (Boolean.TRUE.equals(sender.getBlocked()) || Boolean.TRUE.equals(receiver.getBlocked())) return null;

		Message msg = new Message();
		msg.setSender(sender);
		msg.setReceiver(receiver);
		msg.setContent(content.trim());
		msg.setTimestamp(LocalDateTime.now());
		msg.setSeen(false);

		return repo.save(msg);
	}

	public List<Message> getInbox(User user) {
		if (user == null) return Collections.emptyList();
		return repo.findByReceiver(user);
	}

	public List<ConversationSummary> getRecentConversations(User current) {
		if (current == null) return Collections.emptyList();

		List<Message> allMessages = repo.findBySenderOrReceiverOrderByTimestampDesc(current, current);
		Map<Long, ConversationSummary> map = new LinkedHashMap<>();

		for (Message m : allMessages) {
			User partner = m.getSender().getId().equals(current.getId()) ? m.getReceiver() : m.getSender();
			if (partner == null) continue;

			ConversationSummary summary = map.get(partner.getId());
			if (summary == null) {
				long unread = (!m.isSeen() && m.getReceiver().getId().equals(current.getId())) ? 1 : 0;
				map.put(partner.getId(), new ConversationSummary(partner, m, unread));
			} else {
				if (!m.isSeen() && m.getReceiver().getId().equals(current.getId())) {
					summary.setUnreadCount(summary.getUnreadCount() + 1);
				}
			}
		}

		return new ArrayList<>(map.values());
	}

	public List<Message> getConversation(User u1, User u2) {
		if (u1 == null || u2 == null) return Collections.emptyList();
		return repo.findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(u1, u2, u1, u2);
	}

	public void markAsSeen(User current, User other) {
		if (current == null || other == null) return;

		List<Message> messages = repo.findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(other, current,
				other, current);

		for (Message m : messages) {
			if (m.getReceiver().getId().equals(current.getId())) {
				m.setSeen(true);
			}
		}

		repo.saveAll(messages);
	}
}
