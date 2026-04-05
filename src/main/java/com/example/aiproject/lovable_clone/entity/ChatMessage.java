package com.example.aiproject.lovable_clone.entity;

import com.example.aiproject.lovable_clone.enums.MessageRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
@Getter
@Setter
@FieldDefaults(level= AccessLevel.PRIVATE)
public class ChatMessage {
    Long id;
    ChatSession chatSession;
    String content;
    String toolCalls; //JSON Array of Tools Called
    Integer tokenUsed;
    Instant createdAt;
    MessageRole role;
}
