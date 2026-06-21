package com.example.aiproject.lovable_clone.entity;

import com.example.aiproject.lovable_clone.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)

    })
    ChatSession chatSession;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MessageRole role;
    @Column(columnDefinition = "text", nullable = false)
    String content;
    String toolCalls; //JSON Array of Tools Called
    Integer tokenUsed = 0;
    @CreationTimestamp
    Instant createdAt;

}
