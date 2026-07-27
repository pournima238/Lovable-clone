package com.example.intelligence_service.service.impl;

import com.example.intelligence_service.dto.chat.StreamResponse;
import com.example.intelligence_service.entity.*;
import com.example.common_lib.enums.ChatEventType;
import com.example.common_lib.enums.MessageRole;
import com.example.common_lib.error.ResourceNotFoundException;
import com.example.intelligence_service.llm.LlmResponseParser;
import com.example.intelligence_service.llm.PromptUtils;
import com.example.intelligence_service.llm.advisors.FileTreeContextAdvisor;
import com.example.intelligence_service.client.AccountClient;
import com.example.intelligence_service.client.WorkspaceClient;
import com.example.intelligence_service.llm.tools.CodeGenerationTools;
import com.example.intelligence_service.repository.*;
import com.example.common_lib.security.AuthUtil;
import com.example.intelligence_service.service.AiGenerationService;
import com.example.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final WorkspaceClient workspaceClient;
    private final AccountClient accountClient;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final LlmResponseParser llmResponseParser;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;

    private static final Pattern FILE_TAG_PATTERN =
            Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);

    @Override
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = createChatSessionIfNotExists(projectId, userId);

        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(workspaceClient, projectId);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .filter(response -> response != null
                        && response.getResult() != null
                        && response.getResult().getOutput() != null)
                .doOnNext(response -> {
                    String content = response.getResult().getOutput().getText();

                    if (content != null && !content.isEmpty() && endTime.get() == 0) {
                        endTime.set(System.currentTimeMillis());
                    }

                    if (response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    fullResponseBuffer.append(content != null ? content : "");
                })
                .doOnComplete(() -> {
                    Schedulers.boundedElastic().schedule(() -> {
                        long duration = (endTime.get() - startTime.get()) / 1000;
                        finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration, usageRef.get());
                    });
                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId, error))
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    return new StreamResponse(text != null ? text : "");
                });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration, Usage usage) {
        Long projectId = chatSession.getId().getProjectId();

        if (usage != null) {
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
        }

        // Save the User message
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(usage.getPromptTokens())
                        .build()
        );

        ChatMessage assistantChatMessage = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .content("Assistant Message here...")
                .chatSession(chatSession)
                .tokensUsed(usage.getCompletionTokens())
                .build();

        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventList.addFirst(ChatEvent.builder()
                .chatEventType(ChatEventType.THOUGHT)
                .chatMessage(assistantChatMessage)
                .content("Thought for " + duration + "s")
                .sequenceOrder(0)
                .build());

        chatEventList.stream()
                .filter(e -> e.getChatEventType() == ChatEventType.FILE_EDIT)
                .forEach(e -> {
                    try {
                        workspaceClient.saveFile(projectId, new WorkspaceClient.SaveFileRequest(e.getFilePath(), e.getContent()));
                        log.info("Successfully saved AI generated file {} to MinIO for project {}", e.getFilePath(), projectId);
                    } catch (Exception ex) {
                        log.error("Failed to save AI generated file {} for project {}: {}", e.getFilePath(), projectId, ex.getMessage(), ex);
                    }
                });

        chatEventRepository.saveAll(chatEventList);
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if (chatSession == null) {
            try {
                workspaceClient.getProject(projectId);
            } catch (Exception e) {
                log.error("Failed to verify project {} via WorkspaceClient for userId {}: {}", projectId, userId, e.getMessage(), e);
                throw new ResourceNotFoundException("Project", projectId.toString());
            }

            try {
                accountClient.getUser(userId);
            } catch (Exception e) {
                log.error("Failed to verify user {} via AccountClient: {}", userId, e.getMessage(), e);
                throw new ResourceNotFoundException("User", userId.toString());
            }

            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }
}
