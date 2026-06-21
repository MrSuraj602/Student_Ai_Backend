package com.StudentAiBackend.StudentAiLifeGPS.service;

import com.StudentAiBackend.StudentAiLifeGPS.entity.*;
import com.StudentAiBackend.StudentAiLifeGPS.repository.*;
import com.StudentAiBackend.StudentAiLifeGPS.entity.ReadinessSnapshot;
import com.StudentAiBackend.StudentAiLifeGPS.repository.ReadinessSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserGoalRepository userGoalRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final StudySessionRepository studySessionRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final CareerReadinessRepository careerReadinessRepository;
    private final ReadinessSnapshotRepository readinessSnapshotRepository;
    private final RecoveryPlanRepository recoveryPlanRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RoadmapNodeRepository roadmapNodeRepository;
    private final ObjectMapper objectMapper;
    private final StudentPersonaRepository studentPersonaRepository;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public Map<String, Object> getStudentProfileState(User user) {
        User persistentUser = userRepository.findById(user.getId())
                .orElse(user);
        updateSkillProgression(persistentUser);
        Map<String, Object> state = new LinkedHashMap<>();

        // 1. Basic details
        Map<String, Object> basicDetails = new HashMap<>();
        basicDetails.put("username", persistentUser.getUsername());
        basicDetails.put("email", persistentUser.getEmail());
        basicDetails.put("role", persistentUser.getRole());
        basicDetails.put("level", persistentUser.getLevel());
        basicDetails.put("xp", persistentUser.getXp());
        basicDetails.put("coins", persistentUser.getCoins());
        basicDetails.put("streak", persistentUser.getStreak());
        basicDetails.put("activeMission", persistentUser.getActiveMission());
        basicDetails.put("diagnosticComplete", persistentUser.isDiagnosticComplete());
        
        // Expose onboarding academic detail items
        basicDetails.put("fullName", persistentUser.getFullName());
        basicDetails.put("age", persistentUser.getAge());
        basicDetails.put("country", persistentUser.getCountry());
        basicDetails.put("educationCategory", persistentUser.getEducationCategory());
        basicDetails.put("preferredLearningTime", persistentUser.getPreferredLearningTime());
        basicDetails.put("deadlineType", persistentUser.getDeadlineType());

        if (persistentUser.getAcademicProfile() != null) {
            AcademicProfile ap = persistentUser.getAcademicProfile();
            basicDetails.put("board", ap.getBoard());
            basicDetails.put("stream", ap.getStream());
            basicDetails.put("branch", ap.getBranch());
            basicDetails.put("semester", ap.getSemester());
            basicDetails.put("year", ap.getYear());
            basicDetails.put("currentRole", ap.getCurrentRole());
            basicDetails.put("experience", ap.getExperience());
            basicDetails.put("className", ap.getClassName());
            basicDetails.put("medicalTrack", ap.getMedicalTrack());
            basicDetails.put("lawTrack", ap.getLawTrack());
        }

        // Fetch Student Persona
        Optional<StudentPersona> personaOpt = studentPersonaRepository.findByUser(persistentUser);
        if (personaOpt.isPresent()) {
            StudentPersona sp = personaOpt.get();
            basicDetails.put("personaName", sp.getPersonaName());
            basicDetails.put("identityTitle", sp.getIdentityTitle());
            basicDetails.put("traits", sp.getTraits());
            basicDetails.put("powerLevel", sp.getPowerLevel());
            basicDetails.put("currentArc", sp.getCurrentArc());
            basicDetails.put("finalGoal", sp.getFinalGoal());
            basicDetails.put("currentQuest", sp.getCurrentQuest());
            basicDetails.put("weakness", sp.getWeakness());
            basicDetails.put("aiAdvice", sp.getAiAdvice());
            basicDetails.put("personaSummary", sp.getPersonaSummary());
            basicDetails.put("risk", sp.getRisk());
            basicDetails.put("consistency", sp.getConsistency());
            basicDetails.put("confidence", sp.getConfidence());
            basicDetails.put("successProbability", sp.getSuccessProbability());
            basicDetails.put("studyStrategy", sp.getStudyStrategy());
        }
        state.put("basicDetails", basicDetails);

        // 2. Career goals & Target Career
        List<UserGoal> goals = userGoalRepository.findByUser(persistentUser);
        state.put("careerGoals", goals.stream().map(UserGoal::getGoalName).collect(Collectors.toList()));
        state.put("targetCareer", persistentUser.getTargetCareer() != null ? persistentUser.getTargetCareer() : (goals.isEmpty() ? "Student Explorer" : goals.get(0).getGoalName()));

        // 3. Selected Skills
        List<UserSkill> skills = userSkillRepository.findByUser(persistentUser);
        List<Map<String, Object>> skillList = skills.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("skillName", s.getSkillName());
            map.put("currentLevel", s.getCurrentLevel());
            map.put("targetLevel", s.getTargetLevel());
            map.put("status", s.getStatus());
            map.put("progress", estimateProgress(s.getCurrentLevel()));
            map.put("remainingEffort", calculateRemainingEffort(s));
            return map;
        }).collect(Collectors.toList());
        state.put("selectedSkills", skillList);

        // 4. Available study hours / schedule
        List<UserSchedule> scheduleList = userScheduleRepository.findByUser(persistentUser);
        List<Map<String, Object>> scheds = scheduleList.stream().map(sc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("day", sc.getDay());
            map.put("hours", sc.getAvailableHours());
            return map;
        }).collect(Collectors.toList());
        state.put("availableHours", scheds);

        // 5. Preferences & Projects interested
        state.put("learningPreferences", persistentUser.getLearningPreferences() != null ? persistentUser.getLearningPreferences() : Collections.emptyList());
        state.put("targetCompletionDate", persistentUser.getTargetCompletionDate() != null ? persistentUser.getTargetCompletionDate() : LocalDate.now().plusWeeks(10).toString());
        state.put("projectsInterestedIn", persistentUser.getInterestedProjects() != null ? persistentUser.getInterestedProjects() : Collections.emptyList());

        // 6. Completed Nodes
        List<UserNodeProgress> completedProgress = userNodeProgressRepository.findByUser(persistentUser).stream()
                .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
        List<String> completedNodeIds = completedProgress.stream()
                .map(p -> p.getRoadmapNode().getNodeId())
                .collect(Collectors.toList());
        state.put("completedNodes", completedNodeIds);

        // 7. Study Sessions
        List<StudySession> sessions = studySessionRepository.findByUserOrderBySessionDateDesc(persistentUser);
        List<Map<String, Object>> sessList = sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", s.getSessionDate().toString());
            map.put("subject", s.getSubject());
            map.put("hours", s.getHours());
            return map;
        }).collect(Collectors.toList());
        state.put("studySessions", sessList);

        // 8. Strengths & Weaknesses
        state.put("strengths", persistentUser.getStrengths() != null ? persistentUser.getStrengths() : Collections.emptyList());
        state.put("weaknesses", persistentUser.getWeaknesses() != null ? persistentUser.getWeaknesses() : Collections.emptyList());
        state.put("recommendedDomains", persistentUser.getRecommendedDomains() != null ? persistentUser.getRecommendedDomains() : Collections.emptyList());

        // 9. XP transactions
        List<XpTransaction> txns = xpTransactionRepository.findByUserOrderByCreatedAtDesc(persistentUser);
        List<Map<String, Object>> txnList = txns.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("action", t.getAction());
            map.put("amount", t.getAmount());
            map.put("metadata", t.getMetadata());
            map.put("createdAt", t.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
        state.put("xpHistory", txnList);

        // 10. Achievements / Badges
        List<String> badges = persistentUser.getDefeatedBosses() != null ? new ArrayList<>(persistentUser.getDefeatedBosses()) : new ArrayList<>();
        if (completedNodeIds.contains("python") && !badges.contains("Python Hero")) {
            badges.add("Python Hero");
        }
        if (persistentUser.getLevel() >= 3 && !badges.contains("DSA Master")) {
            badges.add("DSA Master");
        }
        if (persistentUser.getLevel() >= 5 && !badges.contains("Architect Elite")) {
            badges.add("Architect Elite");
        }
        state.put("unlockedBadges", badges);

        // 11. Career Readiness Report
        CareerReadiness currentReadiness = refreshCareerReadiness(persistentUser, false);
        Map<String, Object> crReport = buildCareerReadinessReport(persistentUser, currentReadiness);
        state.put("careerReadiness", crReport);

        // 12. Bosses Defeated Arena details - derived from Roadmap nodes
        List<RoadmapNode> roadmapNodes = roadmapNodeRepository.findByUser(persistentUser);
        List<Map<String, Object>> bossList = new ArrayList<>();
        List<String> listDefeated = persistentUser.getDefeatedBosses() != null ? persistentUser.getDefeatedBosses() : Collections.emptyList();
        
        if (roadmapNodes.isEmpty()) {
            // Local fallback nodes or default list if roadmap is not loaded yet
            String[] bossKeys = {"react", "java", "dsa", "python", "ml"};
            String[] bossNames = {"React Sentinel", "Java Sentinel", "DSA Guardian", "Python Sentinel", "ML Guardian"};
            int[] rewards = {260, 280, 300, 240, 320};

            for (int i = 0; i < bossKeys.length; i++) {
                Map<String, Object> boss = new HashMap<>();
                String bid = bossKeys[i];
                boss.put("id", bid);
                boss.put("name", bossNames[i]);
                boss.put("xpReward", rewards[i]);

                String status = "Locked";
                if (listDefeated.contains(bid)) {
                    status = "Defeated";
                } else {
                    if (i == 0 || listDefeated.contains(bossKeys[i - 1])) {
                        status = "Ready";
                    }
                }
                boss.put("status", status);
                bossList.add(boss);
            }
        } else {
            boolean foundFirstReady = false;
            for (int i = 0; i < roadmapNodes.size(); i++) {
                RoadmapNode node = roadmapNodes.get(i);
                String bid = node.getNodeId();
                Map<String, Object> boss = new HashMap<>();
                boss.put("id", bid);
                boss.put("name", node.getBossName() != null ? node.getBossName() : node.getTitle() + " Sentinel");
                boss.put("xpReward", node.getXpReward() > 0 ? node.getXpReward() * 2 : 200);

                String status = "Locked";
                if (completedNodeIds.contains(node.getNodeId()) || listDefeated.contains(bid)) {
                    status = "Defeated";
                } else {
                    if (!foundFirstReady) {
                        status = "Ready";
                        foundFirstReady = true;
                    }
                }
                boss.put("status", status);
                bossList.add(boss);
            }
        }
        state.put("bosses", bossList);

        // 13. Dynamic Recovery check and plan compile
        state.put("recoveryPlan", getOrGenerateRecoveryPlan(persistentUser, sessions, skills, crReport));

        // 14. Roadmap Nodes from database
        List<Map<String, Object>> rNodes = roadmapNodes.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nodeId", n.getNodeId());
            map.put("title", n.getTitle());
            map.put("bossName", n.getBossName());
            map.put("xpReward", n.getXpReward());
            map.put("description", n.getDescription());
            map.put("bossMission", n.isBossMission());
            map.put("difficulty", n.getDifficulty());
            map.put("hours", n.getHours());
            map.put("resources", n.getResources() != null ? n.getResources() : Collections.emptyList());
            map.put("projects", n.getProjects() != null ? n.getProjects() : Collections.emptyList());
            map.put("interviewQuestions", n.getInterviewQuestions() != null ? n.getInterviewQuestions() : Collections.emptyList());
            map.put("miniChallenges", n.getMiniChallenges() != null ? n.getMiniChallenges() : Collections.emptyList());
            return map;
        }).collect(Collectors.toList());
        state.put("roadmapNodes", rNodes);

        return state;
    }

    private Map<String, Object> getOrGenerateRecoveryPlan(User user, List<StudySession> sessions, List<UserSkill> skills, Map<String, Object> crReport) {
        Map<String, Object> planMap = new HashMap<>();
        if (crReport == null) {
            crReport = Collections.emptyMap();
        }
        
        // 1. Calculate inactivity
        long inactivityDays = 0;
        if (!sessions.isEmpty()) {
            LocalDate lastSessionDate = sessions.get(0).getSessionDate();
            inactivityDays = ChronoUnit.DAYS.between(lastSessionDate, LocalDate.now());
        } else {
            // assume 14 days if no study sessions logged at all
            inactivityDays = 14;
        }

        // Check if there are weak skills
        List<String> weakSkillsList = skills.stream()
                .filter(s -> "beginner".equalsIgnoreCase(s.getCurrentLevel()))
                .map(UserSkill::getSkillName)
                .collect(Collectors.toList());

        boolean triggerRecovery = inactivityDays >= 7 || !weakSkillsList.isEmpty();

        if (!triggerRecovery) {
            // mark as inactive in DB if exists
            recoveryPlanRepository.findByUser(user).ifPresent(rp -> {
                if (rp.isActive()) {
                    rp.setActive(false);
                    recoveryPlanRepository.save(rp);
                }
            });
            planMap.put("active", false);
            planMap.put("headline", "Recovery State Normal");
            planMap.put("suggestions", List.of("Maintain your consistency!", "Practice 15 minutes daily."));
            planMap.put("timelineDelay", "No delay detected");
            planMap.put("motivation", "You are doing great! Keep building your profile.");
            return planMap;
        }

        // Query or generate recovery plan
        Optional<RecoveryPlan> rpOpt = recoveryPlanRepository.findByUser(user);
        if (rpOpt.isPresent()) {
            RecoveryPlan rp = rpOpt.get();
            // Re-generate if active is false or if the plan is stale (created > 2 days ago)
            if (!rp.isActive() || ChronoUnit.DAYS.between(rp.getUpdatedAt(), LocalDateTime.now()) >= 2) {
                rp = generateAndSaveRecoveryPlan(user, inactivityDays, weakSkillsList, crReport, rp);
            }
            planMap.put("active", rp.isActive());
            planMap.put("headline", rp.getHeadline());
            planMap.put("suggestions", rp.getSuggestions() != null ? rp.getSuggestions() : Collections.emptyList());
            planMap.put("timelineDelay", rp.getTimelineDelay());
            planMap.put("motivation", rp.getMotivation());
        } else {
            RecoveryPlan rp = generateAndSaveRecoveryPlan(user, inactivityDays, weakSkillsList, crReport, null);
            planMap.put("active", rp.isActive());
            planMap.put("headline", rp.getHeadline());
            planMap.put("suggestions", rp.getSuggestions() != null ? rp.getSuggestions() : Collections.emptyList());
            planMap.put("timelineDelay", rp.getTimelineDelay());
            planMap.put("motivation", rp.getMotivation());
        }

        return planMap;
    }

    private RecoveryPlan generateAndSaveRecoveryPlan(User user, long inactivityDays, List<String> weakSkills, Map<String, Object> crReport, RecoveryPlan existing) {
        RecoveryPlan plan = existing != null ? existing : new RecoveryPlan();
        plan.setUser(user);
        plan.setActive(true);
        plan.setUpdatedAt(LocalDateTime.now());

        String prompt = String.format(
                "The student has been inactive for %d days. Target career: %s. Weak Skills: %s. Career Readiness Score: %s.\n" +
                "Generate a failure recovery plan for the student. Suggest calendar/schedule changes (e.g. reduce workload by 20%%), alternate micro-projects, timeline delays, and motivational counseling.\n" +
                "Format the response strictly as valid JSON matching this exact layout:\n" +
                "{\n" +
                "  \"headline\": \"Failed Consistency Recovery Plan\",\n" +
                "  \"timelineDelay\": \"Estimated delay: 8 days\",\n" +
                "  \"motivation\": \"Consistency builds momentum. Start with a 15-minute challenge today!\",\n" +
                "  \"suggestions\": [\n" +
                "    \"Reschedule Java practice to weekends\",\n" +
                "    \"Reduce daily hours to 1 hour this week\",\n" +
                "    \"Build a simple Expense Tracker calculator instead of a complex database application\"\n" +
                "  ]\n" +
                "}\n" +
                "Return ONLY valid JSON.",
                inactivityDays,
                user.getTargetCareer() != null ? user.getTargetCareer() : "Software Developer",
                String.join(", ", weakSkills),
                crReport.getOrDefault("score", 50).toString()
        );

        Map<String, Object> gResult = null;
        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.startsWith("${")) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "user");
                msg.put("content", prompt);

                Map<String, Object> body = new HashMap<>();
                body.put("model", "llama-3.3-70b-versatile");
                body.put("messages", List.of(msg));
                body.put("temperature", 0.3);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity("https://api.groq.com/openai/v1/chat/completions", request, Map.class);
                Map<?, ?> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> messageObj = (Map<?, ?>) choice.get("message");
                        String content = (String) messageObj.get("content");
                        content = content.replace("```json", "").replace("```", "").trim();
                        gResult = objectMapper.readValue(content, Map.class);
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq Recovery generation failed: " + e.getMessage());
            }
        }

        if (gResult != null) {
            plan.setHeadline((String) gResult.getOrDefault("headline", "AI Consistency Recovery"));
            plan.setTimelineDelay((String) gResult.getOrDefault("timelineDelay", "Estimated delay: 7 days"));
            plan.setMotivation((String) gResult.getOrDefault("motivation", "Consistency is the root of progress."));
            plan.setSuggestions((List<String>) gResult.getOrDefault("suggestions", List.of("Reduce workload by 15% this week")));
        } else {
            // Local fallback
            plan.setHeadline("Inactivity Recovery Plan");
            plan.setTimelineDelay("Estimated delay: " + (inactivityDays > 10 ? "10" : "6") + " days");
            plan.setMotivation("Every developer faces setbacks. Re-start with small 10-minute coding sprints to regain the streak!");
            plan.setSuggestions(List.of(
                    "Shift complex coding assignments to next week",
                    "Reduce daily available hours by 20% to build consistency",
                    "Suggested project: Simple CLI task manager in Python"
            ));
        }

        return recoveryPlanRepository.save(plan);
    }

    private int estimateProgress(String level) {
        return switch (Optional.ofNullable(level).orElse("beginner").toLowerCase()) {
            case "beginner" -> 20;
            case "intermediate" -> 55;
            case "advanced" -> 85;
            default -> 10;
        };
    }

    private int calculateRemainingEffort(UserSkill skill) {
        int current = levelRank(skill.getCurrentLevel());
        int target = levelRank(skill.getTargetLevel());
        return Math.max(1, (target - current) * 12);
    }

    private int levelRank(String level) {
        return switch (Optional.ofNullable(level).orElse("beginner").toLowerCase()) {
            case "beginner" -> 1;
            case "intermediate" -> 2;
            case "advanced" -> 3;
            default -> 1;
        };
    }

    public CareerReadiness refreshCareerReadiness(User user, boolean recordSnapshot) {
        Map<String, Object> metrics = calculateCareerReadinessMetrics(user);

        CareerReadiness readiness = careerReadinessRepository.findByUser(user).orElse(new CareerReadiness());
        readiness.setUser(user);
        readiness.setScore(((Number) metrics.get("score")).intValue());
        readiness.setInternshipReady(((Number) metrics.get("internshipReady")).intValue());
        readiness.setPlacementReady(((Number) metrics.get("placementReady")).intValue());
        readiness.setInterviewReady(((Number) metrics.get("interviewReady")).intValue());
        readiness.setConfidence(((Number) metrics.get("confidence")).intValue());
        readiness.setEstimatedTimeline((String) metrics.get("estimatedTimeline"));
        readiness.setWeakAreas((List<String>) metrics.get("weakAreas"));
        readiness.setImprovementSuggestions((List<String>) metrics.get("improvementSuggestions"));
        readiness.setProjectsNeeded((List<String>) metrics.get("projectsNeeded"));
        readiness.setUpdatedAt(LocalDateTime.now());
        readiness = careerReadinessRepository.save(readiness);

        if (recordSnapshot) {
            recordReadinessSnapshot(user, readiness, "AUTO_REFRESH");
        }

        return readiness;
    }

    public Map<String, Object> calculateCareerReadinessMetrics(User user) {
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        List<String> defeatedBosses = user.getDefeatedBosses() != null ? user.getDefeatedBosses() : Collections.emptyList();
        List<StudySession> sessions = studySessionRepository.findByUserOrderBySessionDateDesc(user);
        List<UserNodeProgress> completedProgress = userNodeProgressRepository.findByUser(user).stream()
                .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
        List<RoadmapNode> roadmapNodes = roadmapNodeRepository.findByUser(user);

        int skillScore = skills.isEmpty() ? 30 : (int) Math.round(skills.stream()
                .mapToInt(s -> getLevelScore(s.getCurrentLevel()))
                .average().orElse(30));
        int bossScore = Math.min(100, defeatedBosses.size() * 20);
        int roadmapScore = roadmapNodes.isEmpty() ? 0 : (int) Math.round((completedProgress.size() / (double) roadmapNodes.size()) * 100.0);
        int consistencyScore = calculateStudyConsistency(sessions);
        int diagnosticScore = user.isDiagnosticComplete() ? 85 : 40;

        int internshipReady = clamp((int) Math.round(skillScore * 0.34 + roadmapScore * 0.25 + consistencyScore * 0.20 + bossScore * 0.11 + diagnosticScore * 0.10));
        int placementReady = clamp((int) Math.round(skillScore * 0.28 + bossScore * 0.28 + roadmapScore * 0.22 + consistencyScore * 0.12 + diagnosticScore * 0.10));
        int interviewReady = clamp((int) Math.round(skillScore * 0.22 + bossScore * 0.33 + consistencyScore * 0.22 + roadmapScore * 0.13 + diagnosticScore * 0.10));
        int confidence = clamp((internshipReady + placementReady + interviewReady) / 3);
        int score = clamp((int) Math.round((internshipReady + placementReady + interviewReady) / 3.0));

        String estimatedTimeline;
        if (score >= 85) {
            estimatedTimeline = "Ready for placement-ready experiences in 1-2 weeks.";
        } else if (score >= 70) {
            estimatedTimeline = "Ready in 3-4 weeks with focused practice.";
        } else if (score >= 55) {
            estimatedTimeline = "Ready in 5-8 weeks with guided projects and boss drills.";
        } else {
            estimatedTimeline = "Needs 2+ months of structured skills training and project work.";
        }

        List<String> weakAreas = new ArrayList<>(user.getWeaknesses() != null ? user.getWeaknesses() : Collections.emptyList());
        List<String> lowSkillNames = skills.stream()
                .filter(s -> getLevelScore(s.getCurrentLevel()) < 60)
                .map(UserSkill::getSkillName)
                .collect(Collectors.toList());
        for (String lowSkill : lowSkillNames) {
            String candidate = lowSkill + " fundamentals";
            if (!weakAreas.contains(candidate)) {
                weakAreas.add(candidate);
            }
        }
        if (weakAreas.isEmpty()) {
            weakAreas.add("Adaptive learning and time management");
        }

        List<String> improvementSuggestions = buildImprovementSuggestions(user, skillScore, bossScore, roadmapScore, consistencyScore, diagnosticScore, lowSkillNames);
        List<String> projectsNeeded = buildProjectSuggestions(lowSkillNames);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("score", score);
        metrics.put("confidence", confidence);
        metrics.put("internshipReady", internshipReady);
        metrics.put("placementReady", placementReady);
        metrics.put("interviewReady", interviewReady);
        metrics.put("estimatedTimeline", estimatedTimeline);
        metrics.put("weakAreas", weakAreas);
        metrics.put("improvementSuggestions", improvementSuggestions);
        metrics.put("projectsNeeded", projectsNeeded);
        metrics.put("skillScore", skillScore);
        metrics.put("bossScore", bossScore);
        metrics.put("roadmapScore", roadmapScore);
        metrics.put("consistencyScore", consistencyScore);
        metrics.put("diagnosticScore", diagnosticScore);
        return metrics;
    }

    public Map<String, Object> buildReadinessBreakdown(User user, CareerReadiness readiness) {
        Map<String, Object> metrics = calculateCareerReadinessMetrics(user);
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("Skill Mastery", Map.of(
                "score", metrics.get("skillScore"),
                "description", "Average current skill level across selected topics."));
        breakdown.put("Boss Combat", Map.of(
                "score", metrics.get("bossScore"),
                "description", "Progress from defeated boss battles and challenge completion."));
        breakdown.put("Roadmap Progress", Map.of(
                "score", metrics.get("roadmapScore"),
                "description", "How many guided roadmap nodes you have completed."));
        breakdown.put("Study Consistency", Map.of(
                "score", metrics.get("consistencyScore"),
                "description", "Recent study session momentum over the last two weeks."));
        breakdown.put("Diagnostic Signal", Map.of(
                "score", metrics.get("diagnosticScore"),
                "description", "Completeness of core diagnostics and confidence inputs."));
        return breakdown;
    }

    public List<Map<String, Object>> buildReadinessHistory(User user) {
        return readinessSnapshotRepository.findByUserOrderBySnapshotAtDesc(user).stream()
                .limit(6)
                .map(snapshot -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", snapshot.getSnapshotType());
                    map.put("score", snapshot.getScore());
                    map.put("confidence", snapshot.getConfidence());
                    map.put("internshipReady", snapshot.getInternshipReady());
                    map.put("placementReady", snapshot.getPlacementReady());
                    map.put("interviewReady", snapshot.getInterviewReady());
                    map.put("date", snapshot.getSnapshotAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<String> buildImprovementSuggestions(User user, int skillScore, int bossScore, int roadmapScore, int consistencyScore, int diagnosticScore, List<String> lowSkillNames) {
        List<String> suggestions = new ArrayList<>();
        if (!lowSkillNames.isEmpty()) {
            suggestions.add("Level up " + String.join(", ", lowSkillNames) + " through hands-on mini-projects.");
        }
        if (skillScore < 55) {
            suggestions.add("Spend 3 focused hours this week on fundamentals and reuse practice examples.");
        }
        if (bossScore < 60) {
            suggestions.add("Complete more boss challenges to build placement and interview readiness.");
        }
        if (roadmapScore < 60) {
            suggestions.add("Finish priority roadmap nodes to unlock stronger skill progression.");
        }
        if (consistencyScore < 50) {
            suggestions.add("Log daily study sessions and keep your streak active.");
        }
        if (!user.isDiagnosticComplete()) {
            suggestions.add("Complete the cognitive diagnostics quiz to unlock a tailored preparation plan.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Keep practicing regularly and expand your portfolio with integrated projects.");
        }
        return suggestions;
    }

    private List<String> buildProjectSuggestions(List<String> lowSkillNames) {
        List<String> projects = new ArrayList<>();
        if (!lowSkillNames.isEmpty()) {
            for (String skill : lowSkillNames) {
                projects.add("Build a small portfolio project using " + skill + " to reinforce core concepts.");
            }
        }
        if (projects.size() < 3) {
            projects.add("Create an end-to-end capstone project that ties your selected skills together.");
            projects.add("Document your learning progress with a project showcase or case study.");
        }
        return projects.subList(0, Math.min(projects.size(), 3));
    }

    private int calculateStudyConsistency(List<StudySession> sessions) {
        LocalDate today = LocalDate.now();
        double hours = sessions.stream()
                .filter(s -> s.getSessionDate() != null && !s.getSessionDate().isBefore(today.minusDays(13)))
                .mapToDouble(StudySession::getHours)
                .sum();
        return Math.min(100, (int) Math.round(hours * 8));
    }

    private Map<String, Object> buildCareerReadinessReport(User user, CareerReadiness readiness) {
        Map<String, Object> report = new HashMap<>();
        report.put("score", readiness.getScore());
        report.put("internshipReady", readiness.getInternshipReady());
        report.put("placementReady", readiness.getPlacementReady());
        report.put("interviewReady", readiness.getInterviewReady());
        report.put("confidence", readiness.getConfidence());
        report.put("estimatedTimeline", readiness.getEstimatedTimeline());
        report.put("weakAreas", readiness.getWeakAreas() != null ? readiness.getWeakAreas() : Collections.emptyList());
        report.put("improvementSuggestions", readiness.getImprovementSuggestions() != null ? readiness.getImprovementSuggestions() : Collections.emptyList());
        report.put("projectsNeeded", readiness.getProjectsNeeded() != null ? readiness.getProjectsNeeded() : Collections.emptyList());
        report.put("breakdown", buildReadinessBreakdown(user, readiness));
        report.put("history", buildReadinessHistory(user));
        return report;
    }

    private int getLevelScore(String level) {
        return switch (Optional.ofNullable(level).orElse("beginner").toLowerCase()) {
            case "beginner" -> 30;
            case "intermediate" -> 60;
            case "advanced" -> 90;
            default -> 30;
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public void recordReadinessSnapshot(User user, CareerReadiness readiness, String snapshotType) {
        ReadinessSnapshot snapshot = new ReadinessSnapshot();
        snapshot.setUser(user);
        snapshot.setSnapshotType(snapshotType);
        snapshot.setScore(readiness.getScore());
        snapshot.setConfidence(readiness.getConfidence());
        snapshot.setInternshipReady(readiness.getInternshipReady());
        snapshot.setPlacementReady(readiness.getPlacementReady());
        snapshot.setInterviewReady(readiness.getInterviewReady());
        snapshot.setEstimatedTimeline(readiness.getEstimatedTimeline());
        snapshot.setWeakAreas(readiness.getWeakAreas());
        snapshot.setSnapshotAt(LocalDateTime.now());
        readinessSnapshotRepository.save(snapshot);
    }

    public void updateSkillProgression(User user) {
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        List<String> listDefeated = user.getDefeatedBosses() != null ? user.getDefeatedBosses() : Collections.emptyList();
        List<StudySession> sessions = studySessionRepository.findByUserOrderBySessionDateDesc(user);
        
        List<UserNodeProgress> completedProgress = userNodeProgressRepository.findByUser(user).stream()
                .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
        List<String> completedNodeIds = completedProgress.stream()
                .map(p -> p.getRoadmapNode().getNodeId().toLowerCase())
                .collect(Collectors.toList());
        List<String> completedNodeTitles = completedProgress.stream()
                .map(p -> p.getRoadmapNode().getTitle().toLowerCase())
                .collect(Collectors.toList());

        for (UserSkill s : skills) {
            String skillName = s.getSkillName().toLowerCase();
            
            boolean bossDefeated = listDefeated.contains(skillName);
            
            double studyHours = 0;
            for (StudySession sess : sessions) {
                if (sess.getSubject() != null && sess.getSubject().toLowerCase().contains(skillName)) {
                    studyHours += sess.getHours();
                }
            }
            
            long completedNodesCount = 0;
            for (String nodeId : completedNodeIds) {
                if (nodeId.contains(skillName)) {
                    completedNodesCount++;
                }
            }
            for (String title : completedNodeTitles) {
                if (title.contains(skillName)) {
                    completedNodesCount++;
                }
            }

            int score = 0;
            if (bossDefeated) score += 40;
            score += Math.min(40, studyHours * 5);
            score += Math.min(20, completedNodesCount * 10);

            String newLevel = "beginner";
            if (score >= 70) {
                newLevel = "advanced";
            } else if (score >= 35) {
                newLevel = "intermediate";
            }

            if (!newLevel.equalsIgnoreCase(s.getCurrentLevel())) {
                s.setCurrentLevel(newLevel);
                userSkillRepository.save(s);
            }
        }
    }
}
