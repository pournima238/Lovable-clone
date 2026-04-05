package com.example.aiproject.lovable_clone.entity;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
//after writing this no need to write private for all attributes
@Getter
@Setter
public class User {
    Long id;
    String email;
    String passwordHash;
    String name;
    String avatarUrl;
    Instant createdAt;
    Instant updatedAt;
    Instant deletedAt; //to do soft delete
}
