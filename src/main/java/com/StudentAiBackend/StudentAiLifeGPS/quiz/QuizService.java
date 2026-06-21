package com.StudentAiBackend.StudentAiLifeGPS.quiz;

import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizAnswer;
import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizAttempt;
import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizQuestion;
import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizResult;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.XpTransaction;
import com.StudentAiBackend.StudentAiLifeGPS.repository.QuizAttemptRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.QuizQuestionRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.QuizResultRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.XpTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class QuizService {

    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Transactional
    public Map<String, Object> evaluateQuiz(User user, List<AnswerDto> answers) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an experienced education counselor analyzing a diagnostic test. ");
        promptBuilder.append("The student has responded to 50 questions across categories: Math, Programming, Communication, Creativity, Leadership, Logical Thinking, Learning Style. ");
        promptBuilder.append("Here are the student's rating values (from 1=Strongly Disagree to 5=Strongly Agree) for these items:\n");
        for (AnswerDto ans : answers) {
            promptBuilder.append(String.format("- Q%d (Val: %d)\n", ans.getQuestionId(), ans.getRating()));
        }
        promptBuilder.append("\nDetermine:\n");
        promptBuilder.append("1. Level: Beginner, Intermediate, or Advanced.\n");
        promptBuilder.append("2. Confidence Score (0 to 100).\n");
        promptBuilder.append("3. Strengths: List primary categories matching higher scores.\n");
        promptBuilder.append("4. Weaknesses: List categories matching lower scores.\n");
        promptBuilder.append("5. Suitable Domains: E.g., AI, Data Science, Fullstack, Finance, Design.\n");
        promptBuilder.append("Format the response strictly as valid JSON matching this exact layout:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"level\": \"Intermediate\",\n");
        promptBuilder.append("  \"confidence\": 92,\n");
        promptBuilder.append("  \"strengths\": [\"Math\", \"Logical Thinking\"],\n");
        promptBuilder.append("  \"weaknesses\": [\"Communication\"],\n");
        promptBuilder.append("  \"recommendedDomains\": [\"AI\", \"Data Science\"]\n");
        promptBuilder.append("}\n");
        promptBuilder.append("Return ONLY the JSON. No other conversational text.");

        Map<String, Object> responseMap = null;

        // Call Groq API if key is set
        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.startsWith("${")) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", promptBuilder.toString());

                Map<String, Object> body = new HashMap<>();
                body.put("model", "llama-3.3-70b-versatile");
                body.put("messages", List.of(message));
                body.put("temperature", 0.2);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity("https://api.groq.com/openai/v1/chat/completions", request, Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> messageObj = (Map<?, ?>) choice.get("message");
                        String content = (String) messageObj.get("content");
                        
                        // Parse JSON content
                        content = content.replace("```json", "").replace("```", "").trim();
                        responseMap = objectMapper.readValue(content, Map.class);
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq API Call failed: " + e.getMessage());
            }
        }

        // Fallback local evaluator
        if (responseMap == null) {
            responseMap = new HashMap<>();
            
            int programmingSum = 0;
            int mathSum = 0;
            int communicationSum = 0;

            for (AnswerDto ans : answers) {
                if (ans.getQuestionId() >= 8 && ans.getQuestionId() <= 14) {
                    programmingSum += ans.getRating();
                } else if (ans.getQuestionId() >= 15 && ans.getQuestionId() <= 21) {
                    mathSum += ans.getRating();
                } else if (ans.getQuestionId() >= 22 && ans.getQuestionId() <= 28) {
                    communicationSum += ans.getRating();
                }
            }

            String level = "Beginner";
            int confidence = 85;
            List<String> strengths = new java.util.ArrayList<>();
            List<String> weaknesses = new java.util.ArrayList<>();
            List<String> domains = new java.util.ArrayList<>();

            if (programmingSum >= 28 || mathSum >= 28) {
                level = "Advanced";
                confidence = 94;
            } else if (programmingSum >= 18 || mathSum >= 18) {
                level = "Intermediate";
                confidence = 90;
            }

            if (mathSum >= programmingSum && mathSum >= communicationSum) {
                strengths.add("Math");
                strengths.add("Logical Thinking");
                domains.add("AI");
                domains.add("Data Science");
                if (communicationSum < 20) {
                    weaknesses.add("Communication");
                } else {
                    weaknesses.add("Creativity");
                }
            } else if (programmingSum >= mathSum && programmingSum >= communicationSum) {
                strengths.add("Programming");
                strengths.add("Logical Thinking");
                domains.add("AI Engineering");
                domains.add("Fullstack Development");
                if (communicationSum < 20) {
                    weaknesses.add("Communication");
                } else {
                    weaknesses.add("Leadership");
                }
            } else {
                strengths.add("Communication");
                strengths.add("Creativity");
                domains.add("Product Management");
                domains.add("UI/UX Design");
                if (mathSum < 20) {
                    weaknesses.add("Math");
                } else {
                    weaknesses.add("Programming");
                }
            }

            responseMap.put("level", level);
            responseMap.put("confidence", confidence);
            responseMap.put("strengths", strengths);
            responseMap.put("weaknesses", weaknesses);
            responseMap.put("recommendedDomains", domains);
        }

        // Save attempt and answers
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        List<QuizAnswer> savedAnswers = new ArrayList<>();
        for (AnswerDto answerDto : answers) {
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setRating(answerDto.getRating());
            quizQuestionRepository.findById((long) answerDto.getQuestionId()).ifPresent(answer::setQuestion);
            savedAnswers.add(answer);
        }
        attempt.setAnswers(savedAnswers);
        quizAttemptRepository.save(attempt);

        // Save quiz evaluation result for analytics and dashboard
        QuizResult quizResult = new QuizResult();
        quizResult.setAttempt(attempt);
        quizResult.setLevel((String) responseMap.getOrDefault("level", "Beginner"));
        Object confidenceValue = responseMap.get("confidence");
        quizResult.setConfidenceScore(confidenceValue instanceof Number ? ((Number) confidenceValue).intValue() : 0);
        quizResult.setStrengths((List<String>) responseMap.getOrDefault("strengths", new ArrayList<>()));
        quizResult.setWeaknesses((List<String>) responseMap.getOrDefault("weaknesses", new ArrayList<>()));
        quizResult.setRecommendedDomains((List<String>) responseMap.getOrDefault("recommendedDomains", new ArrayList<>()));
        quizResult.setLearningStyle((String) responseMap.getOrDefault("learningStyle", "Balanced"));
        quizResultRepository.save(quizResult);

        // Sync and save User stats
        user.setDiagnosticComplete(true);
        user.setStrengths((List<String>) responseMap.get("strengths"));
        user.setWeaknesses((List<String>) responseMap.get("weaknesses"));
        user.setRecommendedDomains((List<String>) responseMap.get("recommendedDomains"));
        if (confidenceValue instanceof Number) {
            user.setCareerReadyScore(((Number) confidenceValue).intValue());
        }

        int xpAward = calculateXpAward((String) responseMap.getOrDefault("level", "Beginner"));
        user.setXp(user.getXp() + xpAward);
        if (xpAward > 0) {
            XpTransaction transaction = new XpTransaction();
            transaction.setUser(user);
            transaction.setAction("Quiz Completion");
            transaction.setAmount(xpAward);
            transaction.setMetadata("Diagnostic quiz completed: " + quizResult.getLevel());
            xpTransactionRepository.save(transaction);
        }

        userRepository.save(user);

        responseMap.put("xpAward", xpAward);
        responseMap.put("updatedLevel", user.getLevel());
        responseMap.put("updatedXp", user.getXp());
        responseMap.put("diagnosticComplete", true);
        responseMap.put("strengths", quizResult.getStrengths());
        responseMap.put("weaknesses", quizResult.getWeaknesses());
        responseMap.put("recommendedDomains", quizResult.getRecommendedDomains());

        return responseMap;
    }

    private int calculateXpAward(String level) {
        return switch (level.toLowerCase()) {
            case "advanced" -> 200;
            case "intermediate" -> 120;
            default -> 80;
        };
    }
}
