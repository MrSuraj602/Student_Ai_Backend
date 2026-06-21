package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_personas")
@Data
public class StudentPersona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // Student Identity Fields
    private String personaName;               // e.g. "Disciplined Builder"
    private String identityTitle;             // e.g. "The Night Builder"
    private String traits;                    // e.g. "Project Driven, Fast Learner"
    private int powerLevel;                   // e.g. 76
    private String currentArc;                // e.g. "Backend Engineering"
    private String finalGoal;                 // e.g. "AI Engineer"
    private String currentQuest;              // e.g. "Build Spring APIs"
    private String weakness;                  // e.g. "Communication"
    
    @Column(columnDefinition = "TEXT")
    private String aiAdvice;                  // e.g. "Ship one project/week"
    
    @Column(columnDefinition = "TEXT")
    private String personaSummary;

    // AI Analysis Fields
    private String risk;                      // Burnout Risk: Low, Moderate, High
    private String consistency;               // e.g. "80%"
    private String confidence;                // e.g. "75%"
    private String successProbability;        // e.g. "82%"
    private String studyStrategy;             // e.g. "Pomodoro, Flashcards"

    private LocalDateTime updatedAt;
}
