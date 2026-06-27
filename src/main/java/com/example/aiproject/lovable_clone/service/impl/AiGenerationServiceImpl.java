package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.llm.PromptUtils;
import com.example.aiproject.lovable_clone.llm.advisors.FileTreeContextAdvisor;
import com.example.aiproject.lovable_clone.llm.tools.CodeGenerationTools;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.AiGenerationService;
import com.example.aiproject.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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
    private final ProjectFileService projectFileService;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;

    private static final Pattern FILE_TAG_PATTERN =
            Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);

    @Override
    public Flux<String> streamResponse(String userMessage, Long projectId) {
        Long userId = authUtil.getCurrentUserId();
//        log.info("=== streamResponse called === projectId={} userId={} message={}",
//                projectId, userId, userMessage);
        createChatSessionIfNotExists(projectId, userId);

        AtomicReference<StringBuilder> bufferRef = new AtomicReference<>(new StringBuilder());
        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService, projectId);

        return Flux.defer(() -> {
//            log.info("=== Flux subscribed, calling Groq ===");  // ← moved here
            return chatClient.prompt()
                    .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                    .user(userMessage)
                    .tools(codeGenerationTools)
                    .advisors(fileTreeContextAdvisor)
                    .advisors(advisorSpec -> advisorSpec.params(advisorParams))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        if (chunk != null) {
//                            log.info("=== chunk received length: {} ===", chunk.length());
                            bufferRef.get().append(chunk);
                        }
                    })
                    .doOnComplete(() -> {
//                        log.info("=== doOnComplete fired ===");
                        String fullResponse = bufferRef.get().toString();
                        if (fullResponse.isBlank()) {
                            log.warn("Empty response for project {}", projectId);
                            return;
                        }
                        Schedulers.boundedElastic().schedule(() -> {
                            try {
                                parseAndSaveFiles(fullResponse, projectId);
                            } catch (Exception e) {
                                log.error("Failed to save files for project {}: {}", projectId, e.getMessage(), e);
                            }
                        });
                    })
                    .doOnError(error ->
                            log.error("=== doOnError fired === class={} message={}",
                                    error.getClass().getName(), error.getMessage(), error))
                    .onErrorResume(error -> {
                        log.error("=== onErrorResume fired === class={} message={}",
                                error.getClass().getName(), error.getMessage());
                        String message = switch (error.getClass().getSimpleName()) {
                            case "InterruptedIOException", "OpenAIIoException" ->
                                    "[ERROR] Stream timed out. Please try again.";
                            case "StreamResetException" -> "[ERROR] Connection reset by server. Please try again.";
                            default -> "[ERROR] Something went wrong: " + error.getMessage();
                        };
                        return Flux.just(message);
                    })
                    .filter(text -> text != null && !text.isEmpty());
            // ← removed .retry(1)
        });
    }

    private void parseAndSaveFiles(String fullResponse, Long projectId) {
        Matcher matcher = FILE_TAG_PATTERN.matcher(fullResponse);
        int filesSaved = 0;
        while (matcher.find()) {
            String filePath = matcher.group(1);
            String fileContent = matcher.group(2).trim();
            try {
                projectFileService.saveFile(projectId, filePath, fileContent);
                filesSaved++;
                log.debug("Saved file {} for project {}", filePath, projectId);
            } catch (Exception e) {
                log.error("Failed to save file {} for project {}: {}",
                        filePath, projectId, e.getMessage(), e);
            }
        }
        log.info("Saved {}/{} files for project {}", filesSaved,
                filesSaved, projectId);
    }

    private void createChatSessionIfNotExists(Long projectId, Long userId) {
        // TODO: implement session persistence
    }
}