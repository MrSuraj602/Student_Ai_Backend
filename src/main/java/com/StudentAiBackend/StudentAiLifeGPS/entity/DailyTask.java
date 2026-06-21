package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "daily_tasks")
@Data
public class DailyTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String dayOfWeek;
    private String taskTitle;
    private int hours;
    private String roadmapNodeId;
    private boolean completed;
}
