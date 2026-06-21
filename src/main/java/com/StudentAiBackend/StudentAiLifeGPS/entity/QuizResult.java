package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_results")
@Data
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    private String level;
    private int confidenceScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_result_strengths", joinColumns = @JoinColumn(name = "quiz_result_id"))
    private List<String> strengths;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_result_weaknesses", joinColumns = @JoinColumn(name = "quiz_result_id"))
    private List<String> weaknesses;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_result_domains", joinColumns = @JoinColumn(name = "quiz_result_id"))
    private List<String> recommendedDomains;

    private String learningStyle;
    private LocalDateTime createdAt = LocalDateTime.now();
}
