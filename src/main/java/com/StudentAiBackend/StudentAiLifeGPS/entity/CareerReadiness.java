package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "career_readiness")
@Data
public class CareerReadiness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int score;
    
    private int internshipReady;
    private int placementReady;
    private int interviewReady;
    private int confidence;
    private String estimatedTimeline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_readiness_weak_areas", joinColumns = @JoinColumn(name = "readiness_id"))
    private List<String> weakAreas;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_readiness_suggestions", joinColumns = @JoinColumn(name = "readiness_id"))
    private List<String> improvementSuggestions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_readiness_projects", joinColumns = @JoinColumn(name = "readiness_id"))
    private List<String> projectsNeeded;

    private LocalDateTime updatedAt = LocalDateTime.now();
}

