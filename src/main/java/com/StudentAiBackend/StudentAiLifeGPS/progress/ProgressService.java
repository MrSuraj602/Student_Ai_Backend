package com.StudentAiBackend.StudentAiLifeGPS.progress;

import com.StudentAiBackend.StudentAiLifeGPS.entity.ActivityLog;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ActivityLogRepository activityLogRepository;

    public Map<String, Object> getAnalytics(User user) {
        Map<String, Object> data = new HashMap<>();

        // 1. Radar Chart Data
        data.put("radarData", List.of(
            Map.of("subject", "Programming", "A", 85, "B", 60),
            Map.of("subject", "Math", "A", 90, "B", 70),
            Map.of("subject", "Communication", "A", 65, "B", 80),
            Map.of("subject", "Leadership", "A", 70, "B", 65),
            Map.of("subject", "Creativity", "A", 75, "B", 85)
        ));

        // 2. Productivity Trend Data
        data.put("productivityData", List.of(
            Map.of("name", "Mon", "hours", 4.5),
            Map.of("name", "Tue", "hours", 6.2),
            Map.of("name", "Wed", "hours", 5.0),
            Map.of("name", "Thu", "hours", 7.8),
            Map.of("name", "Fri", "hours", 4.0),
            Map.of("name", "Sat", "hours", 8.5),
            Map.of("name", "Sun", "hours", 9.0)
        ));

        // 3. Skill Growth Data
        data.put("skillGrowthData", List.of(
            Map.of("name", "Jan", "score", 30),
            Map.of("name", "Feb", "score", 45),
            Map.of("name", "Mar", "score", 55),
            Map.of("name", "Apr", "score", 62),
            Map.of("name", "May", "score", 78),
            Map.of("name", "Jun", "score", 88)
        ));

        // 4. Distribution Data
        data.put("distributionData", List.of(
            Map.of("name", "Technical", "value", 40, "color", "#3B82F6"),
            Map.of("name", "Analytical", "value", 25, "color", "#8B5CF6"),
            Map.of("name", "Soft Skills", "value", 20, "color", "#22C55E"),
            Map.of("name", "Creative", "value", 15, "color", "#EC4899")
        ));

        // 5. Mastery Data
        data.put("masteryData", List.of(
            Map.of("name", "Frontend", "children", List.of(
                Map.of("name", "React", "size", 100),
                Map.of("name", "Tailwind", "size", 60)
            )),
            Map.of("name", "Backend", "children", List.of(
                Map.of("name", "Spring Boot", "size", 90),
                Map.of("name", "Java", "size", 80)
            )),
            Map.of("name", "Databases", "children", List.of(
                Map.of("name", "MySQL", "size", 70),
                Map.of("name", "SQLite", "size", 40)
            ))
        ));

        // 6. Logged activities (fallback to default if DB is empty)
        List<ActivityLog> dbLogs = activityLogRepository.findByUserOrderByTimeDesc(user);
        if (dbLogs.isEmpty()) {
            data.put("activityLog", List.of(
                Map.of("action", "Signed up on Matrix OS", "time", "4 days ago", "xp", "+100 XP"),
                Map.of("action", "Maintained 4-day Streak", "time", "2 days ago", "xp", "+50 XP"),
                Map.of("action", "Attempted Diagnostic Quiz", "time", "1 day ago", "xp", "+150 XP")
            ));
        } else {
            List<Map<String, Object>> logList = dbLogs.stream()
                .limit(10)
                .map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("action", log.getAction());
                    map.put("time", "Just now");
                    map.put("xp", log.getXp());
                    return map;
                })
                .collect(Collectors.toList());
            data.put("activityLog", logList);
        }

        return data;
    }

    public void logActivity(User user, String action, String xp) {
        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setXp(xp);
        log.setUser(user);
        activityLogRepository.save(log);
    }
}
