package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.service.AiGenerationService;
import reactor.core.publisher.Flux;

public class AiGenerationServiceImpl implements AiGenerationService {
    @Override
    public Flux<String> streamResponse(String message, Long projectId) {
        return null;
    }
}
