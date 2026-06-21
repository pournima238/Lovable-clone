package com.example.aiproject.lovable_clone.service;

import aj.org.objectweb.asm.commons.Remapper;
import reactor.core.publisher.Flux;

public interface AiGenerationService {
    Flux<String> streamResponse(String message, Long projectId);
}
