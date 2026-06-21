package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "roadmap_nodes")
@Data
public class RoadmapNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int xpReward;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    private boolean bossMission = false;
    private String bossName;

    private String difficulty;
    private int hours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roadmap_node_resources", joinColumns = @JoinColumn(name = "node_id"))
    private List<String> resources;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roadmap_node_projects", joinColumns = @JoinColumn(name = "node_id"))
    private List<String> projects;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roadmap_node_interview_questions", joinColumns = @JoinColumn(name = "node_id"))
    private List<String> interviewQuestions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roadmap_node_mini_challenges", joinColumns = @JoinColumn(name = "node_id"))
    private List<String> miniChallenges;
}

