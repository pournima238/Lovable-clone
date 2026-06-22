package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.llm.PromptUtils;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.AiGenerationService;
import com.example.aiproject.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {
    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final ProjectFileService projectFileService;
    private final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);

    @Override
//    @PreAuthorize("@security.canEditProject(#projectId)")
    //only user who has permission to edit can access this fun logic
    public Flux<String> streamResponse(String userMessage, Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        createChatSessionIfNotExists(projectId, userId);
        StringBuilder fullResponseBuffer = new StringBuilder();
        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );
        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .advisors(advisorSpec -> {
                    advisorSpec.params(advisorParams);
                })
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String content = response.getResult().getOutput().getText(); // we are doing this to check the tokens used for this
                    fullResponseBuffer.append(content);
                })
                .doOnComplete(() -> {
                    Schedulers.boundedElastic().schedule(() -> {
                        parseAndSaveFiles(fullResponseBuffer.toString(), projectId);
                    });
                }).doOnError(error -> log.error("Error during streaming of project response"))
                .map(response -> response.getResult().getOutput().getText());
    }

    private void parseAndSaveFiles(String fullResponse, Long projectId) {
//        String dummy = """
//                <message>I am going to read the files and generate the code </message>
//                <file-path="src/App.jsx>
//                import App  from '.App.jsx'
//                .......
//                </file>
//                """;
        Matcher matcher = FILE_TAG_PATTERN.matcher(fullResponse);
        while (matcher.find()) {
            String filePath = matcher.group(1);
            String fileContent = matcher.group(2).trim();
            projectFileService.saveFile(projectId, filePath, fileContent);
        }
    }

    private void createChatSessionIfNotExists(Long projectId, Long userId) {
    }
}
