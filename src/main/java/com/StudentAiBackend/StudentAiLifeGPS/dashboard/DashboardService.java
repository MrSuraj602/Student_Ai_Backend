package com.StudentAiBackend.StudentAiLifeGPS.dashboard;

import com.StudentAiBackend.StudentAiLifeGPS.entity.*;
import com.StudentAiBackend.StudentAiLifeGPS.planner.PlannerService;
import com.StudentAiBackend.StudentAiLifeGPS.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudySessionRepository studySessionRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final CareerReadinessRepository careerReadinessRepository;
    private final ActivityLogRepository activityLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserSkillRepository userSkillRepository;
    private final PlannerService plannerService;

    public Map<String, Object> getSummary(User user) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("username", user.getUsername());
        summary.put("level", user.getLevel());
        summary.put("xp", user.getXp());
        summary.put("streak", user.getStreak());
        summary.put("strengths", Optional.ofNullable(user.getStrengths()).orElse(Collections.emptyList()));
        summary.put("weaknesses", Optional.ofNullable(user.getWeaknesses()).orElse(Collections.emptyList()));
        summary.put("recommendedDomains", Optional.ofNullable(user.getRecommendedDomains()).orElse(Collections.emptyList()));
        summary.put("careerReadyScore", user.getCareerReadyScore());
        summary.put("activeMission", user.getActiveMission());

        Map<String, Object> xp = getXp(user);
        summary.put("xpSummary", xp);

        Map<String, Object> roadmap = getRoadmap(user);
        summary.put("roadmap", roadmap);

        Map<String, Object> study = getStudyStats(user);
        summary.put("studyStats", study);

        return summary;
    }

    public Map<String, Object> getSkills(User user) {
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        Map<String, Double> radarValues = new LinkedHashMap<>();
        for (UserSkill skill : skills) {
            radarValues.put(skill.getSkillName(), (double) estimateProgress(skill));
        }

        List<StudySession> sessions = studySessionRepository.findByUserOrderBySessionDateDesc(user);

        List<Map<String, Object>> radar = radarValues.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("subject", entry.getKey());
                    item.put("A", Math.round(entry.getValue()));
                    item.put("B", 70);
                    item.put("fullMark", 100);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> skillsPayload = new HashMap<>();
        skillsPayload.put("radarData", radar);
        skillsPayload.put("productivityData", generateProductivityData(sessions));
        skillsPayload.put("distributionData", generateDistribution(skills));
        skillsPayload.put("skillGrowthData", generateSkillGrowth(sessions));
        skillsPayload.put("masteryData", generateMasteryData(skills));
        skillsPayload.put("activityLog", generateActivityLog(user));
        return skillsPayload;
    }

    private List<Map<String, Object>> generateProductivityData(List<StudySession> sessions) {
        Map<String, Double> dailyHours = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyHours.put(date.getDayOfWeek().name().substring(0, 3), 0.0);
        }

        for (StudySession session : sessions) {
            String key = session.getSessionDate().getDayOfWeek().name().substring(0, 3);
            if (dailyHours.containsKey(key)) {
                dailyHours.put(key, dailyHours.get(key) + session.getHours());
            }
        }

        return dailyHours.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("name", entry.getKey());
                    point.put("hours", Math.round(entry.getValue() * 10.0) / 10.0);
                    return point;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, String>> generateActivityLog(User user) {
        List<ActivityLog> logs = activityLogRepository.findByUserOrderByTimeDesc(user);
        if (logs.isEmpty()) {
            return List.of(
                    Map.of("action", "Welcome back to the study console", "time", "Just now", "xp", "+0 XP")
            );
        }

        return logs.stream()
                .limit(8)
                .map(log -> Map.of(
                        "action", log.getAction(),
                        "time", log.getTime().toString(),
                        "xp", log.getXp() != null ? log.getXp() : "+0 XP"
                ))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getHeatmap(User user) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(364);
        List<StudySession> sessions = studySessionRepository.findByUserAndSessionDateBetween(user, start, now);
        List<LoginHistory> logins = loginHistoryRepository.findByUserOrderByLoginTimeDesc(user);

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 365; i++) {
            LocalDate date = start.plusDays(i);
            final LocalDate day = date;
            double studyHours = sessions.stream()
                    .filter(s -> s.getSessionDate().isEqual(day))
                    .mapToDouble(StudySession::getHours)
                    .sum();
            long loginCount = logins.stream().filter(login -> login.getLoginTime().toLocalDate().isEqual(day)).count();
            int level = (int) Math.min(3, Math.round(Math.min(1.0, studyHours / 2.0 + loginCount * 0.5) * 3));
            days.add(Map.of("date", day.toString(), "level", level));
        }
        return Map.of("heatmap", days);
    }

    public Map<String, Object> getXp(User user) {
        List<XpTransaction> txns = xpTransactionRepository.findByUserOrderByCreatedAtDesc(user);
        int earned = txns.stream().mapToInt(XpTransaction::getAmount).sum();
        int currentLevel = user.getLevel();
        int currentXp = user.getXp();
        int xpForNext = currentLevel * 400;
        int progress = xpForNext == 0 ? 0 : Math.min(100, Math.round((currentXp / (float) xpForNext) * 100));

        Map<String, Object> xp = new HashMap<>();
        xp.put("currentXp", currentXp);
        xp.put("currentLevel", currentLevel);
        xp.put("nextLevelXp", xpForNext);
        xp.put("progressPercentage", progress);
        xp.put("transactions", txns.stream().map(tx -> Map.of(
                "action", tx.getAction(),
                "amount", tx.getAmount(),
                "metadata", tx.getMetadata(),
                "createdAt", tx.getCreatedAt().toString()
        )).collect(Collectors.toList()));
        return xp;
    }

    public Map<String, Object> getRoadmap(User user) {
        return Map.of("planner", plannerService.generatePlanner(user));
    }

    public Map<String, Object> getReadiness(User user) {
        Optional<CareerReadiness> readiness = careerReadinessRepository.findByUser(user);
        int score = readiness.map(CareerReadiness::getScore).orElse(user.getCareerReadyScore());
        return Map.of("careerReadiness", score);
    }

    public Map<String, Object> getAchievements(User user) {
        List<XpTransaction> txns = xpTransactionRepository.findByUserOrderByCreatedAtDesc(user);
        List<String> badges = new ArrayList<>();
        if (user.getLevel() >= 2) badges.add("Python Hero");
        if (user.getLevel() >= 3) badges.add("DSA Master");
        if (user.getLevel() >= 4) badges.add("Hackathon Warrior");
        if (user.getLevel() >= 5) badges.add("Open Source Champion");

        return Map.of(
                "badgeCount", badges.size(),
                "badges", badges,
                "recentActivities", txns.stream()
                        .limit(10)
                        .map(tx -> Map.of(
                                "action", tx.getAction(),
                                "amount", tx.getAmount(),
                                "metadata", tx.getMetadata(),
                                "createdAt", tx.getCreatedAt().toString()
                        )).collect(Collectors.toList())
        );
    }

    private Map<String, Object> getStudyStats(User user) {
        List<StudySession> sessions = studySessionRepository.findByUserOrderBySessionDateDesc(user);
        double totalHours = sessions.stream().mapToDouble(StudySession::getHours).sum();
        int sessionCount = sessions.size();
        LocalDate now = LocalDate.now();
        long weeklySessions = sessions.stream()
                .filter(s -> !s.getSessionDate().isBefore(now.minusWeeks(1)))
                .count();
        double averageHours = sessionCount == 0 ? 0 : totalHours / sessionCount;

        Map<String, Long> subjectCounts = sessions.stream()
                .filter(s -> s.getSubject() != null)
                .collect(Collectors.groupingBy(StudySession::getSubject, Collectors.counting()));

        List<String> topSubjects = subjectCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return Map.of(
                "totalHours", totalHours,
                "sessionCount", sessionCount,
                "weeklySessions", weeklySessions,
                "averageHours", Math.round(averageHours * 10.0) / 10.0,
                "streak", user.getStreak(),
                "topSubjects", topSubjects
        );
    }

    private int estimateProgress(UserSkill skill) {
        return switch (skill.getCurrentLevel().toLowerCase()) {
            case "beginner" -> 20;
            case "intermediate" -> 50;
            case "advanced" -> 80;
            default -> 10;
        };
    }

    private List<Map<String, Object>> generateDistribution(List<UserSkill> skills) {
        if (skills.isEmpty()) {
            return List.of();
        }
        double total = skills.stream().mapToDouble(this::estimateProgress).sum();
        return skills.stream()
                .map(skill -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", skill.getSkillName());
                    item.put("value", Math.round((estimateProgress(skill) / Math.max(total, 1)) * 100));
                    item.put("color", randomColor(skill.getSkillName()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> generateSkillGrowth(List<StudySession> sessions) {
        List<Map<String, Object>> growth = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            double sessionHours = sessions.stream()
                    .filter(s -> s.getSessionDate().getMonth().equals(month.getMonth()) && s.getSessionDate().getYear() == month.getYear())
                    .mapToDouble(StudySession::getHours)
                    .sum();
            int score = Math.min(100, (int) Math.round(sessionHours * 10 + i * 2));
            growth.add(Map.of("name", month.getMonth().name().substring(0, 3), "score", score));
        }
        return growth;
    }

    private List<Map<String, Object>> generateMasteryData(List<UserSkill> skills) {
        List<Map<String, Object>> mastery = new ArrayList<>();
        Map<String, List<UserSkill>> grouped = skills.stream()
                .collect(Collectors.groupingBy(skill -> {
                    String name = skill.getSkillName().toLowerCase();
                    if (name.contains("react") || name.contains("css") || name.contains("html") || name.contains("js")) {
                        return "Frontend";
                    }
                    if (name.contains("spring") || name.contains("java") || name.contains("sql") || name.contains("backend") || name.contains("docker") || name.contains("aws")) {
                        return "Backend";
                    }
                    if (name.contains("python") || name.contains("ml") || name.contains("machine learning") || name.contains("deep learning")) {
                        return "Data";
                    }
                    return "General";
                }));

        for (var entry : grouped.entrySet()) {
            Map<String, Object> section = new HashMap<>();
            section.put("name", entry.getKey());
            section.put("children", entry.getValue().stream()
                    .map(skill -> Map.of(
                            "name", skill.getSkillName(),
                            "size", estimateProgress(skill)
                    ))
                    .collect(Collectors.toList()));
            mastery.add(section);
        }
        return mastery;
    }

    private String randomColor(String seed) {
        int hash = Math.abs(seed.hashCode());
        return String.format("#%06x", hash % 0x1000000);
    }
}
