package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "user_role")
    private String role; // ROLE_STUDENT or ROLE_ADMIN

    private int level = 1;
    private int xp = 0;
    private int coins = 0;
    private int streak = 0;
    private String activeMission = "Complete Cognitive Diagnostics Quiz";

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "board", column = @Column(name = "academic_board")),
        @AttributeOverride(name = "stream", column = @Column(name = "academic_stream")),
        @AttributeOverride(name = "branch", column = @Column(name = "academic_branch")),
        @AttributeOverride(name = "semester", column = @Column(name = "academic_semester")),
        @AttributeOverride(name = "year", column = @Column(name = "academic_year")),
        @AttributeOverride(name = "currentRole", column = @Column(name = "academic_current_role")),
        @AttributeOverride(name = "experience", column = @Column(name = "academic_experience")),
        @AttributeOverride(name = "className", column = @Column(name = "academic_class_name")),
        @AttributeOverride(name = "medicalTrack", column = @Column(name = "academic_medical_track")),
        @AttributeOverride(name = "lawTrack", column = @Column(name = "academic_law_track"))
    })
    private AcademicProfile academicProfile;

    private String fullName;
    private Integer age;
    private String country;
    private String educationCategory;
    private String preferredLearningTime;
    private String deadlineType;

    private String targetCareer;
    private String targetCompletionDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_learning_preferences", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> learningPreferences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interested_projects", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> interestedProjects = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_defeated_bosses", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> defeatedBosses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_strengths", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> strengths = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_weaknesses", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> weaknesses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recommended_domains", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> recommendedDomains = new ArrayList<>();

    private boolean diagnosticComplete = false;
    private int careerReadyScore = 0;

    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean active = false;
}

