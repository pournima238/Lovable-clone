package com.example.intelligence_service.mapper;

import com.example.intelligence_service.dto.chat.ChatResponse;
import com.example.intelligence_service.entity.ChatMessage;
import com.example.intelligence_service.dto.chat.ChatEventResponse;
import com.example.intelligence_service.entity.ChatEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);

    @Mapping(source = "chatEventType", target = "type")
    ChatEventResponse toChatEventResponse(ChatEvent chatEvent);
}
