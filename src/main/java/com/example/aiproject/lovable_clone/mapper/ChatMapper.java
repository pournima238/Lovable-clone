package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.chat.ChatResponse;
import com.example.aiproject.lovable_clone.entity.ChatMessage;
import com.example.aiproject.lovable_clone.dto.chat.ChatEventResponse;
import com.example.aiproject.lovable_clone.entity.ChatEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);

    @Mapping(source = "chatEventType", target = "type")
    ChatEventResponse toChatEventResponse(ChatEvent chatEvent);
}
