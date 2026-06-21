package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "daily_task_modules")
@Data
public class DailyTaskModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_task_id", nullable = false)
    private DailyTask dailyTask;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private int duration;
    private String difficulty;
    private int xpReward;
    private boolean completed;
    private int orderIndex;
}
