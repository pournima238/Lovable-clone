package com.example.workspace_service.entity;


import com.example.common_lib.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "project_members") //basically db name in pgsql
public class ProjectMember {
    @EmbeddedId //useful while making composite key
    ProjectMemberId id;

    @ManyToOne
    @MapsId("projectId")
    Project project;

//    @ManyToOne
//    @MapsId("userId")
//    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProjectRole projectRole;
    Instant invitedAt;
    Instant acceptedAt;

}
