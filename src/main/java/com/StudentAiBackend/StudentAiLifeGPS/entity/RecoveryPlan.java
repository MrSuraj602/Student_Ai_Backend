package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recovery_plans")
@Data
public class RecoveryPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean active = false;
    private String headline;
    private String timelineDelay;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recovery_suggestions", joinColumns = @JoinColumn(name = "recovery_id"))
    private List<String> suggestions;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
