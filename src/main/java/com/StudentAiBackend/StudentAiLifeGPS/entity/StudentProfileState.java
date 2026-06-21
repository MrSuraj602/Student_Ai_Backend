package com.StudentAiBackend.StudentAiLifeGPS.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileState {
    private boolean initialized;
    private String profileStatus;
    private boolean isFirstLogin;
    private Object persona;
    private Object dashboard;
    private Object planner;
    private Object roadmap;
    private Object careerReadiness;
    private Object recoveryPlan;
    private List<?> tasks;
    private List<?> bosses;
    private List<?> badges;
    private List<?> studySessions;
    private List<?> activityLogs;
    private Object mentorContext;
    private int xp;
    private int level;
    private int coins;
    private int streak;

    // Extra fields when initialized is true (to prevent breaking frontend store mappings)
    private Map<String, Object> basicDetails;
    private List<String> careerGoals;
    private String targetCareer;
    private List<?> selectedSkills;
    private List<?> availableHours;
    private List<String> learningPreferences;
    private String targetCompletionDate;
    private List<String> projectsInterestedIn;
    private List<String> completedNodes;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedDomains;
    private List<?> xpHistory;
    private List<String> unlockedBadges;
    private List<?> roadmapNodes;
}
