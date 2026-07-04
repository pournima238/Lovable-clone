package com.example.aiproject.lovable_clone.service;


import com.example.aiproject.lovable_clone.dto.chat.ChatResponse;

import java.util.List;

public interface ChatService {

    List<ChatResponse> getProjectChatHistory(Long projectId);
}
