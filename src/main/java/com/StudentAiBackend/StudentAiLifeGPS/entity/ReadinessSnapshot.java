package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "readiness_snapshot")
@Data
public class ReadinessSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String snapshotType;
    private int score;
    private int internshipReady;
    private int placementReady;
    private int interviewReady;
    private int confidence;
    private String estimatedTimeline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "readiness_snapshot_weak_areas", joinColumns = @JoinColumn(name = "snapshot_id"))
    @Column(name = "weak_area")
    private List<String> weakAreas;

    private LocalDateTime snapshotAt = LocalDateTime.now();
}
