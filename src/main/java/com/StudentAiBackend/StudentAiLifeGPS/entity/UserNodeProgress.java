package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_node_progress")
@Data
public class UserNodeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private RoadmapNode roadmapNode;

    @Column(nullable = false)
    private String status;

    private LocalDateTime completedAt;
}
