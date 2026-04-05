package com.example.aiproject.lovable_clone.entity;

import com.example.aiproject.lovable_clone.enums.PreviewStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level= AccessLevel.PRIVATE)
public class Preview {
    Long id;
    Project project;
    String namespace;//isolated resourses in kubernetes
    String podName;//smallest unit in kubernetes
    String previewUrl;
    PreviewStatus previewStatus;
    Instant startedAt;
    Instant terminatedAt;
    Instant createdAt;
}
