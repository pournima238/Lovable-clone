package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.llm.PromptUtils;
import com.example.aiproject.lovable_clone.llm.advisors.FileTreeContextAdvisor;
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
        createChatSessionIfNotExists(projectId, userId);

        AtomicReference<StringBuilder> bufferRef = new AtomicReference<>(new StringBuilder());
        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .advisors(fileTreeContextAdvisor)          // ← move outside, pass directly
                .advisors(advisorSpec ->
                        advisorSpec.params(advisorParams))
                .stream()
                .content()  // ← switch to .content() which returns Flux<String> directly
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        bufferRef.get().append(chunk);
                    }
                })
                .doOnComplete(() -> {
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
                        log.error("Stream error for project {} user {}: {}",
                                projectId, userId, error.getMessage(), error))
                .onErrorResume(error -> {
                    // Now this is Flux<String> → Flux<String>, no type mismatch
                    String message = switch (error.getClass().getSimpleName()) {
                        case "InterruptedIOException", "OpenAIIoException" ->
                                "[ERROR] Stream timed out. Please try again.";
                        case "StreamResetException" -> "[ERROR] Connection reset by server. Please try again.";
                        default -> "[ERROR] Something went wrong: " + error.getMessage();
                    };
                    return Flux.just(message);
                })
                .filter(text -> text != null && !text.isEmpty())
                .retry(1);
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