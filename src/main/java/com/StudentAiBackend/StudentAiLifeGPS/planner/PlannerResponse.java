package com.StudentAiBackend.StudentAiLifeGPS.planner;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PlannerResponse {
    private List<String> goals;
    private List<Map<String, Object>> skills;
    private List<Map<String, Object>> schedule;
    private List<Map<String, Object>> milestones;
    private List<Map<String, Object>> projects;
    private List<Map<String, Object>> weeklyPlan;
    private List<Map<String, Object>> todayTasks;
    private List<Map<String, Object>> upcomingMilestones;
    private List<Map<String, Object>> skillProgress;
    private List<String> aiSuggestions;
    private double roadmapProgress;
    private int completionProgress;
    private String estimatedCompletionDate;
}
