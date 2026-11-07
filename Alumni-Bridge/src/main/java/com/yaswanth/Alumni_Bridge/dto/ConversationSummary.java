package com.yaswanth.Alumni_Bridge.dto;

import com.yaswanth.Alumni_Bridge.entity.Message;
import com.yaswanth.Alumni_Bridge.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummary {
    private User partner;
    private Message latestMessage;
    private long unreadCount;
}
