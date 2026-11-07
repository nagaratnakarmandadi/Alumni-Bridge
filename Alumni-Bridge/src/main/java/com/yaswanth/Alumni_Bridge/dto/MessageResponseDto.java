package com.yaswanth.Alumni_Bridge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String content;
    private String timestamp;
    private boolean isMine;
    private boolean seen;
}
