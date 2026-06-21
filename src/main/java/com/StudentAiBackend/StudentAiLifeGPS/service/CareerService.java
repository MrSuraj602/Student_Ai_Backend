package com.StudentAiBackend.StudentAiLifeGPS.service;

import com.StudentAiBackend.StudentAiLifeGPS.entity.CareerReadiness;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.XpTransaction;
import com.StudentAiBackend.StudentAiLifeGPS.repository.CareerReadinessRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.XpTransactionRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserSkillRepository;
import com.StudentAiBackend.StudentAiLifeGPS.entity.UserSkill;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final CareerReadinessRepository careerReadinessRepository;
    private final UserRepository userRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ObjectMapper objectMapper;
    private final ProfileService profileService;
    private final UserSkillRepository userSkillRepository;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public Map<String, Object> generateBossChallenge(User user, String bossId) {
        String bossName = getBossNameById(user, bossId);
        String profileContext = buildStudentProfileContext(user);
        String prompt = String.format(
            "You are an AI challenge generator. You must generate a highly customized, unique boss battle assessment for the student.\n" +
            "The student's profile context is:\n" +
            "%s\n\n" +
            "Generate a customized assessment battle for the boss: '%s' (bossId: '%s').\n" +
            "Follow these rules strictly:\n" +
            "1. Adjust the difficulty of the questions dynamically based on the student's profile (XP, skill levels, completed roadmap nodes). If their skill levels are high or they have completed many nodes, make the questions significantly more difficult and advanced.\n" +
            "2. Different users must receive completely different, unique questions. Choose creative scenarios, MCQs, and coding prompts tailored to their strengths/weaknesses and career goals. Avoid generic templates.\n" +
            "3. The challenge must contain exactly 4 interactive questions of different types related to the skill '%s':\n" +
            "   - MCQ (Single choice)\n" +
            "   - MultipleChoice (Checkbox/Multiple options select)\n" +
            "   - Scenario (Describe a system design problem or debugging decision)\n" +
            "   - Coding (Write a small function or logic piece)\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"bossId\": \"%s\",\n" +
            "  \"bossName\": \"%s\",\n" +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"id\": \"q1\",\n" +
            "      \"type\": \"MCQ\",\n" +
            "      \"prompt\": \"Question prompt...\",\n" +
            "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"q2\",\n" +
            "      \"type\": \"MultipleChoice\",\n" +
            "      \"prompt\": \"Question prompt...\",\n" +
            "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"q3\",\n" +
            "      \"type\": \"Scenario\",\n" +
            "      \"prompt\": \"Scenario prompt...\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"q4\",\n" +
            "      \"type\": \"Coding\",\n" +
            "      \"prompt\": \"Coding prompt...\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Return ONLY valid JSON. Do not include markdown code block characters like ```json.",
            profileContext, bossName, bossId, bossId, bossId, bossName
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
                body.put("temperature", 0.8);

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
                System.err.println("Groq Boss Challenge generation failed: " + e.getMessage());
            }
        }

        if (gResult != null) {
            return gResult;
        }

        // Local fallback challenge if Groq fails
        return getFallbackChallenge(bossId);
    }

    @Transactional
    public Map<String, Object> evaluateBossSubmission(User user, String bossId, Map<String, Object> submission) {
        User persistentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String bossName = getBossNameById(persistentUser, bossId);
        Map<String, Object> challenge = getFallbackChallenge(bossId); // simple fallback description context
        
        String prompt = String.format(
            "You are an AI grader. Grade the student's submission for the boss battle: '%s' (bossId: '%s').\n" +
            "The submission answers list:\n" +
            "%s\n" +
            "Evaluate their answers and compile a career readiness score. Provide:\n" +
            "- score: final grade from 0 to 100\n" +
            "- confidence: skill confidence score from 0 to 100\n" +
            "- internshipReady: internship readiness percentage from 0 to 100\n" +
            "- placementReady: placement readiness percentage from 0 to 100\n" +
            "- interviewReady: interview readiness percentage from 0 to 100\n" +
            "- weakAreas: list of weak skills or concepts they failed\n" +
            "- projectsNeeded: list of projects they should build to reinforce these skills\n" +
            "- estimatedTimeline: description of when they will be ready\n" +
            "- suggestions: list of direct suggestions\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"score\": 82,\n" +
            "  \"confidence\": 80,\n" +
            "  \"internshipReady\": 75,\n" +
            "  \"placementReady\": 60,\n" +
            "  \"interviewReady\": 70,\n" +
            "  \"weakAreas\": [\"weakness 1\", \"weakness 2\"],\n" +
            "  \"projectsNeeded\": [\"project 1\", \"project 2\"],\n" +
            "  \"estimatedTimeline\": \"Ready in 5-6 weeks with focused practice.\",\n" +
            "  \"suggestions\": [\"suggestion 1\", \"suggestion 2\"]\n" +
            "}\n" +
            "Return ONLY JSON.",
            bossName, bossId, submission.toString()
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
                body.put("temperature", 0.2);

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
                System.err.println("Groq Boss Challenge evaluation failed: " + e.getMessage());
            }
        }

        int score = 75;
        int confidence = 70;
        int internshipReady = 65;
        int placementReady = 55;
        int interviewReady = 60;
        List<String> weakAreas = List.of("Concept structures", "Whiteboard syntax");
        List<String> projectsNeeded = List.of("Build integrated React/Java prototypes");
        List<String> suggestions = List.of("Revise hook behaviors and recursion parameters");
        String timeline = "FTE Ready in 8-10 weeks with continuous study sessions.";

        if (gResult != null) {
            score = ((Number) gResult.getOrDefault("score", 75)).intValue();
            confidence = ((Number) gResult.getOrDefault("confidence", 70)).intValue();
            internshipReady = ((Number) gResult.getOrDefault("internshipReady", 65)).intValue();
            placementReady = ((Number) gResult.getOrDefault("placementReady", 55)).intValue();
            interviewReady = ((Number) gResult.getOrDefault("interviewReady", 60)).intValue();
            weakAreas = (List<String>) gResult.getOrDefault("weakAreas", weakAreas);
            projectsNeeded = (List<String>) gResult.getOrDefault("projectsNeeded", projectsNeeded);
            suggestions = (List<String>) gResult.getOrDefault("suggestions", suggestions);
            timeline = (String) gResult.getOrDefault("estimatedTimeline", timeline);
        }

        boolean passed = score >= 60;

        if (passed) {
            // Mark boss as defeated in User entity
            List<String> defeated = persistentUser.getDefeatedBosses() != null 
                    ? new ArrayList<>(persistentUser.getDefeatedBosses()) : new ArrayList<>();
            if (!defeated.contains(bossId)) {
                defeated.add(bossId);
                persistentUser.setDefeatedBosses(defeated);
                
                // Add XP and check Level upgrades
                int xpReward = getBossRewardXp(persistentUser, bossId);
                persistentUser.setXp(persistentUser.getXp() + xpReward);
                persistentUser.setCoins(persistentUser.getCoins() + (xpReward / 2));
                
                int requiredXp = persistentUser.getLevel() * 400;
                if (persistentUser.getXp() >= requiredXp) {
                    persistentUser.setXp(persistentUser.getXp() - requiredXp);
                    persistentUser.setLevel(persistentUser.getLevel() + 1);
                }
                
                // Save XP transaction
                XpTransaction transaction = new XpTransaction();
                transaction.setUser(persistentUser);
                transaction.setAction("Boss Defeated");
                transaction.setAmount(xpReward);
                transaction.setMetadata("Defeated " + bossName + "!");
                xpTransactionRepository.save(transaction);
            }
            persistentUser.setActiveMission("Complete next level node on the roadmap!");
            userRepository.save(persistentUser);
            profileService.updateSkillProgression(persistentUser);
        }

        CareerReadiness readiness = profileService.refreshCareerReadiness(persistentUser, true);
        score = readiness.getScore();
        confidence = readiness.getConfidence();
        internshipReady = readiness.getInternshipReady();
        placementReady = readiness.getPlacementReady();
        interviewReady = readiness.getInterviewReady();
        weakAreas = readiness.getWeakAreas();
        projectsNeeded = readiness.getProjectsNeeded();
        suggestions = readiness.getImprovementSuggestions();
        timeline = readiness.getEstimatedTimeline();

        Map<String, Object> report = new HashMap<>();
        report.put("bossId", bossId);
        report.put("bossName", bossName);
        report.put("passed", passed);
        report.put("score", score);
        report.put("confidence", confidence);
        report.put("internshipReady", internshipReady);
        report.put("placementReady", placementReady);
        report.put("interviewReady", interviewReady);
        report.put("weakAreas", weakAreas);
        report.put("projectsNeeded", projectsNeeded);
        report.put("suggestions", suggestions);
        report.put("estimatedTimeline", timeline);
        report.put("updatedLevel", persistentUser.getLevel());
        report.put("updatedXp", persistentUser.getXp());

        return report;
    }

    private String getBossNameById(User user, String bossId) {
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        for (UserSkill s : skills) {
            if (s.getSkillName().equalsIgnoreCase(bossId)) {
                return s.getSkillName() + " Boss";
            }
        }
        return bossId.substring(0, 1).toUpperCase() + bossId.substring(1) + " Boss";
    }

    private String getBossNameById(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "react" -> "React Boss";
            case "java" -> "Java Boss";
            case "dsa" -> "DSA Boss";
            case "python" -> "Python Boss";
            case "ml" -> "ML Boss";
            default -> "FTE Coding Boss";
        };
    }

    private int getBossRewardXp(User user, String bossId) {
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        for (UserSkill s : skills) {
            if (s.getSkillName().equalsIgnoreCase(bossId)) {
                int rank = levelRank(s.getCurrentLevel());
                return 200 + rank * 40;
            }
        }
        return 250;
    }

    private int levelRank(String level) {
        return switch (Optional.ofNullable(level).orElse("beginner").toLowerCase()) {
            case "beginner" -> 1;
            case "intermediate" -> 2;
            case "advanced" -> 3;
            default -> 1;
        };
    }

    private int getBossRewardXp(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "react" -> 260;
            case "java" -> 280;
            case "dsa" -> 300;
            case "python" -> 240;
            case "ml" -> 320;
            default -> 250;
        };
    }

    private Map<String, Object> getFallbackChallenge(String bossId) {
        Map<String, Object> challenge = new HashMap<>();
        challenge.put("bossId", bossId);
        challenge.put("bossName", getBossNameById(bossId));
        
        List<Map<String, Object>> questions = new ArrayList<>();
        
        Map<String, Object> q1 = new HashMap<>();
        q1.put("id", "q1");
        q1.put("type", "MCQ");
        q1.put("prompt", "What is the primary role of indexing in database optimization routines?");
        q1.put("options", List.of("Speeds up read query operations", "Compiles class structures", "Deletes duplicates", "Injects security protocols"));
        questions.add(q1);

        Map<String, Object> q2 = new HashMap<>();
        q2.put("id", "q2");
        q2.put("type", "MultipleChoice");
        q2.put("prompt", "Select all valid methods to allocate session cookies securely in Web APIs.");
        q2.put("options", List.of("HttpOnly attribute", "Secure attribute", "Storing plain in document.cookie", "Strict SameSite attribute"));
        questions.add(q2);

        Map<String, Object> q3 = new HashMap<>();
        q3.put("id", "q3");
        q3.put("type", "Scenario");
        q3.put("prompt", "Explain how you would handle an OutOfMemoryError in a Java spring-boot server receiving high throughput requests.");
        questions.add(q3);

        Map<String, Object> q4 = new HashMap<>();
        q4.put("id", "q4");
        q4.put("type", "Coding");
        q4.put("prompt", "Write a python function to check if a string is a palindrome. Keep space complexity O(1).");
        questions.add(q4);

        challenge.put("questions", questions);
        return challenge;
    }

    public Map<String, Object> generateAssessmentTest(User user) {
        String targetCareer = user.getTargetCareer() != null ? user.getTargetCareer() : "Software Development";
        String profileContext = buildStudentProfileContext(user);
        String prompt = String.format(
            "You are an AI assessment generator. You must generate a highly customized, unique diagnostic assessment test for the student.\n" +
            "The student's profile context is:\n" +
            "%s\n\n" +
            "Generate a comprehensive technical assessment test consisting of exactly 15 questions for a student pursuing: '%s'.\n" +
            "Follow these rules strictly:\n" +
            "1. Adjust the difficulty of the questions dynamically based on the student's profile (XP, skill levels, completed roadmap nodes). If their skill levels are high, make the questions significantly more difficult and advanced.\n" +
            "2. Different users must receive completely different, unique questions. Choose creative scenarios, MCQs, and short answer prompts tailored to their strengths/weaknesses and career goals. Avoid generic templates.\n" +
            "3. The test must contain a mix of question types (MCQ, Checkbox, Short Answer, Scenario).\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"id\": \"1\",\n" +
            "      \"type\": \"MCQ\",\n" +
            "      \"prompt\": \"Question prompt?\",\n" +
            "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Return ONLY valid JSON. Do not include markdown code block characters like ```json.",
            profileContext, targetCareer
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
                body.put("temperature", 0.8);

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
                System.err.println("Groq Assessment generation failed: " + e.getMessage());
            }
        }

        if (gResult != null) {
            return gResult;
        }

        return getFallbackAssessment(targetCareer);
    }

    private Map<String, Object> getFallbackAssessment(String targetCareer) {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, Object>> questions = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i);
            q.put("type", "MCQ");
            if (i == 1) {
                q.put("prompt", "What is the primary benefit of microservice architecture over monolithic systems?");
                q.put("options", List.of("Independent deployment and scaling", "Simpler debugging", "Faster single-process speeds", "Zero network dependencies"));
            } else if (i == 2) {
                q.put("prompt", "Which HTTP status code represents a resource creation success?");
                q.put("options", List.of("200 OK", "201 Created", "204 No Content", "400 Bad Request"));
            } else if (i == 3) {
                q.put("prompt", "What complexity class does binary search operate in for sorted arrays?");
                q.put("options", List.of("O(1)", "O(N)", "O(log N)", "O(N log N)"));
            } else if (i == 4) {
                q.put("prompt", "In git version control, which command moves changes from the staging area to the repository history?");
                q.put("options", List.of("git add", "git commit", "git push", "git checkout"));
            } else {
                q.put("prompt", "Which SQL constraint ensures data uniqueness across columns?");
                q.put("options", List.of("PRIMARY KEY", "FOREIGN KEY", "UNIQUE", "NOT NULL"));
            }
            questions.add(q);
        }

        for (int i = 6; i <= 10; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i);
            q.put("type", "MultipleChoice");
            if (i == 6) {
                q.put("prompt", "Select all valid relational database management systems (RDBMS).");
                q.put("options", List.of("MySQL", "PostgreSQL", "MongoDB", "Oracle DB"));
            } else if (i == 7) {
                q.put("prompt", "Select all design patterns commonly used in object-oriented software engineering.");
                q.put("options", List.of("Singleton", "Observer", "Factory Method", "Heap Sort"));
            } else if (i == 8) {
                q.put("prompt", "Which of the following are HTTP methods used in RESTful APIs?");
                q.put("options", List.of("GET", "POST", "FETCH", "DELETE"));
            } else if (i == 9) {
                q.put("prompt", "Select all characteristics of a stack data structure.");
                q.put("options", List.of("LIFO (Last In First Out)", "FIFO (First In First Out)", "Supports push/pop operations", "Maintains dynamic tail pointers"));
            } else {
                q.put("prompt", "Select all valid security measures for protecting user passwords.");
                q.put("options", List.of("Hashing with BCrypt", "Salting values", "Base64 encoding", "AES encryption with static local keys"));
            }
            questions.add(q);
        }

        for (int i = 11; i <= 13; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i);
            q.put("type", "Scenario");
            if (i == 11) {
                q.put("prompt", "Your team notices that database connections are getting exhausted during peak load. Describe your approach to diagnose and resolve this leakage.");
            } else if (i == 12) {
                q.put("prompt", "A third-party authentication API starts returning intermittent 504 Gateway Timeouts. How would you design a recovery strategy or circuit breaker to preserve user experience?");
            } else {
                q.put("prompt", "A user reports they can view other users' dashboard contents by modifying the URL ID parameter. Explain the security issue and how you would patch it.");
            }
            questions.add(q);
        }

        for (int i = 14; i <= 15; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i);
            q.put("type", "Short Answer");
            if (i == 14) {
                q.put("prompt", "What does MVC stand for in software architecture?");
            } else {
                q.put("prompt", "Explain the concept of 'hoisting' in JavaScript.");
            }
            questions.add(q);
        }

        res.put("questions", questions);
        return res;
    }

    @Transactional
    public Map<String, Object> evaluateAssessmentSubmission(User user, Map<String, Object> submission) {
        User persistentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String prompt = String.format(
            "You are an AI grader. Grade the student's submission for a comprehensive career readiness assessment.\n" +
            "The submission answers list:\n" +
            "%s\n" +
            "Evaluate their answers and compile a career readiness score. Provide:\n" +
            "- score: final grade from 0 to 100\n" +
            "- confidence: skill confidence score from 0 to 100\n" +
            "- internshipReady: internship readiness percentage from 0 to 100\n" +
            "- placementReady: placement readiness percentage from 0 to 100\n" +
            "- interviewReady: interview readiness percentage from 0 to 100\n" +
            "- weakAreas: list of weak skills or concepts they failed\n" +
            "- projectsNeeded: list of projects they should build to reinforce these skills\n" +
            "- estimatedTimeline: description of when they will be ready\n" +
            "- suggestions: list of direct suggestions\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"score\": 82,\n" +
            "  \"confidence\": 80,\n" +
            "  \"internshipReady\": 75,\n" +
            "  \"placementReady\": 60,\n" +
            "  \"interviewReady\": 70,\n" +
            "  \"weakAreas\": [\"weakness 1\", \"weakness 2\"],\n" +
            "  \"projectsNeeded\": [\"project 1\", \"project 2\"],\n" +
            "  \"estimatedTimeline\": \"Ready in 5-6 weeks with focused practice.\",\n" +
            "  \"suggestions\": [\"suggestion 1\", \"suggestion 2\"]\n" +
            "}\n" +
            "Return ONLY JSON.",
            submission.toString()
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
                body.put("temperature", 0.2);

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
                System.err.println("Groq Assessment evaluation failed: " + e.getMessage());
            }
        }

        int score = 70;
        int confidence = 70;
        int internshipReady = 60;
        int placementReady = 50;
        int interviewReady = 55;
        List<String> weakAreas = List.of("System scalability", "Relational normalizations");
        List<String> projectsNeeded = List.of("Build relational microservices", "Implement high throughput middleware");
        List<String> suggestions = List.of("Revise indexing principles and concurrency control");
        String timeline = "Ready in 6-8 weeks with targeted study blocks.";

        if (gResult != null) {
            score = ((Number) gResult.getOrDefault("score", 70)).intValue();
            confidence = ((Number) gResult.getOrDefault("confidence", 70)).intValue();
            internshipReady = ((Number) gResult.getOrDefault("internshipReady", 60)).intValue();
            placementReady = ((Number) gResult.getOrDefault("placementReady", 50)).intValue();
            interviewReady = ((Number) gResult.getOrDefault("interviewReady", 55)).intValue();
            weakAreas = (List<String>) gResult.getOrDefault("weakAreas", weakAreas);
            projectsNeeded = (List<String>) gResult.getOrDefault("projectsNeeded", projectsNeeded);
            suggestions = (List<String>) gResult.getOrDefault("suggestions", suggestions);
            timeline = (String) gResult.getOrDefault("estimatedTimeline", timeline);
        }

        int xpReward = 150;
        persistentUser.setXp(persistentUser.getXp() + xpReward);
        persistentUser.setCoins(persistentUser.getCoins() + (xpReward / 2));
        persistentUser.setDiagnosticComplete(true);
        
        int requiredXp = persistentUser.getLevel() * 400;
        if (persistentUser.getXp() >= requiredXp) {
            persistentUser.setXp(persistentUser.getXp() - requiredXp);
            persistentUser.setLevel(persistentUser.getLevel() + 1);
        }
        
        XpTransaction transaction = new XpTransaction();
        transaction.setUser(persistentUser);
        transaction.setAction("Assessment Completed");
        transaction.setAmount(xpReward);
        transaction.setMetadata("Completed Career Readiness Assessment!");
        xpTransactionRepository.save(transaction);
        
        persistentUser.setActiveMission("Complete next level node on the roadmap!");
        userRepository.save(persistentUser);
        profileService.updateSkillProgression(persistentUser);

        CareerReadiness readiness = profileService.refreshCareerReadiness(persistentUser, true);
        score = readiness.getScore();
        confidence = readiness.getConfidence();
        internshipReady = readiness.getInternshipReady();
        placementReady = readiness.getPlacementReady();
        interviewReady = readiness.getInterviewReady();
        weakAreas = readiness.getWeakAreas();
        projectsNeeded = readiness.getProjectsNeeded();
        suggestions = readiness.getImprovementSuggestions();
        timeline = readiness.getEstimatedTimeline();

        Map<String, Object> report = new HashMap<>();
        report.put("passed", true);
        report.put("score", score);
        report.put("confidence", confidence);
        report.put("internshipReady", internshipReady);
        report.put("placementReady", placementReady);
        report.put("interviewReady", interviewReady);
        report.put("weakAreas", weakAreas);
        report.put("projectsNeeded", projectsNeeded);
        report.put("suggestions", suggestions);
        report.put("estimatedTimeline", timeline);
        report.put("updatedLevel", persistentUser.getLevel());
        report.put("updatedXp", persistentUser.getXp());
        report.put("xpEarned", xpReward);

        return report;
    }

    private String buildStudentProfileContext(User user) {
        try {
            Map<String, Object> state = profileService.getStudentProfileState(user);
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("careerGoal", state.get("targetCareer"));
            profile.put("selectedSkills", state.get("selectedSkills"));
            profile.put("completedRoadmapNodes", state.get("completedNodes"));
            profile.put("studySessions", state.get("studySessions"));
            
            Map<String, Object> basicDetails = (Map<String, Object>) state.get("basicDetails");
            profile.put("xp", basicDetails != null ? basicDetails.get("xp") : user.getXp());
            profile.put("weakAreas", state.get("weaknesses"));
            profile.put("strengths", state.get("strengths"));
            profile.put("careerReadiness", state.get("careerReadiness"));
            profile.put("availability", state.get("availableHours"));
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
        } catch (Exception e) {
            System.err.println("Failed to build student profile context: " + e.getMessage());
            return "{ \"error\": \"Could not compile student profile context\" }";
        }
    }

    public Map<String, Object> recommendCareers(Map<String, Object> profile) {
        String prompt = String.format(
            "Recommend exactly 5 careers matching the student's profile context:\n%s\n" +
            "For each career, specify compatibility (integer percentage e.g. 92), whySuggested, difficulty, salary, scope, yearsNeeded, competitiveExams (list of strings), and roadmapComplexity.\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"recommendedCareers\": [\n" +
            "     {\n" +
            "       \"title\": \"MBBS Doctor\",\n" +
            "       \"compatibility\": 92,\n" +
            "       \"whySuggested\": \"Strong Biology and Chemistry scores, passion for medicine...\",\n" +
            "       \"difficulty\": \"Hard\",\n" +
            "       \"salary\": \"$100k - $250k\",\n" +
            "       \"scope\": \"Stable demand globally...\",\n" +
            "       \"yearsNeeded\": \"5.5 years\",\n" +
            "       \"competitiveExams\": [\"NEET\"],\n" +
            "       \"roadmapComplexity\": \"High\"\n" +
            "     }\n" +
            "  ]\n" +
            "}\n" +
            "Return ONLY valid JSON. Do not include markdown code block characters like ```json.",
            profile.toString()
        );

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
                body.put("temperature", 0.5);

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
                        int startIdx = content.indexOf('{');
                        int endIdx = content.lastIndexOf('}');
                        if (startIdx != -1 && endIdx != -1) {
                            content = content.substring(startIdx, endIdx + 1);
                        }
                        return objectMapper.readValue(content, Map.class);
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq career recommendation failed: " + e.getMessage());
            }
        }

        // Fallback local recommendations
        Map<String, Object> fallback = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("title", "AI Engineer");
        c1.put("compatibility", 85);
        c1.put("whySuggested", "High technology affinity and problem solving skills.");
        c1.put("difficulty", "Medium");
        c1.put("salary", "$110k - $160k");
        c1.put("scope", "Very High growth potential");
        c1.put("yearsNeeded", "4 years");
        c1.put("competitiveExams", List.of("JEE", "GATE"));
        c1.put("roadmapComplexity", "Medium");
        list.add(c1);
        fallback.put("recommendedCareers", list);
        return fallback;
    }

    public List<String> suggestSkills(String careerName) {
        String prompt = String.format(
            "Suggest exactly 6-8 core technical and domain skills for a student aiming for the career: '%s'.\n" +
            "Format the response strictly as valid JSON matching this exact layout:\n" +
            "{\n" +
            "  \"suggestedSkills\": [\"Skill1\", \"Skill2\", \"Skill3\"]\n" +
            "}\n" +
            "Return ONLY valid JSON. Do not include markdown code block characters like ```json.",
            careerName
        );

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
                body.put("temperature", 0.4);

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
                        int startIdx = content.indexOf('{');
                        int endIdx = content.lastIndexOf('}');
                        if (startIdx != -1 && endIdx != -1) {
                            content = content.substring(startIdx, endIdx + 1);
                        }
                        Map<String, Object> resMap = objectMapper.readValue(content, Map.class);
                        if (resMap.containsKey("suggestedSkills")) {
                            return (List<String>) resMap.get("suggestedSkills");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq skill suggestions failed: " + e.getMessage());
            }
        }

        // Fallback local skills mapping based on common matching
        String clean = careerName.toLowerCase();
        if (clean.contains("doctor") || clean.contains("medical") || clean.contains("surgeon") || clean.contains("biotech")) {
            return List.of("Biology", "Anatomy", "Chemistry", "NEET Preparation", "Physiology", "Diagnostic Skills");
        } else if (clean.contains("lawyer") || clean.contains("judge") || clean.contains("law")) {
            return List.of("Constitutional Law", "Criminal Law", "Legal Analysis", "Public Speaking", "Drafting & Writing", "Civil Procedure");
        } else if (clean.contains("ias") || clean.contains("ips") || clean.contains("upsc") || clean.contains("government")) {
            return List.of("History", "Polity & Constitution", "Economics", "Ethics & Integrity", "Current Affairs", "Essay Writing");
        } else if (clean.contains("engineer") || clean.contains("developer") || clean.contains("coder")) {
            return List.of("Data Structures & Algorithms", "Java", "Python", "SQL Databases", "Docker & APIs", "System Design");
        }
        return List.of("Critical Thinking", "Research Methodology", "Project Management", "Technical Writing", "Data Analysis");
    }
}
