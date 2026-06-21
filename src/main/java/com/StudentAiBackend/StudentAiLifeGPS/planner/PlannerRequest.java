package com.StudentAiBackend.StudentAiLifeGPS.planner;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PlannerRequest {
    private List<String> goals;
    private List<SkillDto> skills;
    
    @JsonAlias({"schedule", "availability"})
    private List<ScheduleDto> schedule;

    private List<String> learningPreferences;
    private String deadline;
    private Map<String, Object> profile;

    @Data
    public static class SkillDto {
        private String skillName;
        private String currentLevel;
        private String targetLevel;
        private String status;
    }

    @Data
    public static class ScheduleDto {
        private String day;
        
        @JsonAlias({"hours", "availableHours"})
        private int availableHours;
    }
}

