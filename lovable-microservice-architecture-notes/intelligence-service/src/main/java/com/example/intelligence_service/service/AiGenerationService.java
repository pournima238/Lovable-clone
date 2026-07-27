package com.example.intelligence_service.service;

import aj.org.objectweb.asm.commons.Remapper;

import com.example.intelligence_service.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long projectId);
}
