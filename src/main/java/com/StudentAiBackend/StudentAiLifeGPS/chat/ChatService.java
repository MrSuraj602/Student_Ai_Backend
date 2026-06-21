package com.StudentAiBackend.StudentAiLifeGPS.chat;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.RoadmapNode;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private final Map<String, List<Map<String, String>>> nodeChatHistory = new ConcurrentHashMap<>();

    public String getMentorResponse(String userQuery) {
        String systemPrompt = "You are StudentAI LifeGPS AI Counselor, an empathetic and highly experienced academic advisor. " +
                "Guide the student through educational matrices, streams (PCM/PCB/Commerce/Arts), coding roadmaps (OOP, DSA, web build), and exam failures. " +
                "Keep responses structured with markdown lists and bold text. Keep it concise, engaging and supportive.";

        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.startsWith("${")) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);

                Map<String, Object> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", userQuery);

                Map<String, Object> body = new HashMap<>();
                body.put("model", "llama-3.3-70b-versatile");
                body.put("messages", List.of(sysMsg, userMsg));
                body.put("temperature", 0.7);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity("https://api.groq.com/openai/v1/chat/completions", request, Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> messageObj = (Map<?, ?>) choice.get("message");
                        return (String) messageObj.get("content");
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq AI Mentor call failed: " + e.getMessage());
            }
        }

        // Catch-all Fallback Counselor
        String text = userQuery.toLowerCase();
        if (text.contains("oop") || text.contains("object")) {
            return "**Object-Oriented Programming (OOP)** is structured around four primary pillars:\n\n" +
                    "- **Encapsulation**: Bundling state variables and wrapping methods inside class frameworks.\n" +
                    "- **Inheritance**: Derived subclasses acquiring properties of parent classes.\n" +
                    "- **Polymorphism**: Interoperable functions responding uniquely across subclasses.\n" +
                    "- **Abstraction**: Hiding structural implementation details from final execution layers.\n\n" +
                    "Check out the **OOP Principles Node** in your RPG Skill Tree to test these concepts!";
        } else if (text.contains("dsa") || text.contains("data structure") || text.contains("algorithm")) {
            return "To master **Data Structures and Algorithms (DSA)**, target this phased agenda:\n\n" +
                    "1. **Linear Matrices**: Arrays, Linked Lists, Stacks, Queues.\n" +
                    "2. **Non-Linear Nodes**: Binary Trees, Heaps, Graphs.\n" +
                    "3. **Optimization routines**: BFS/DFS search traversals, recursion, and dynamic memoization.\n\n" +
                    "Try completing Leetcode challenges and checking items in your **DSA RPG Node**!";
        } else if (text.contains("jee") || text.contains("exam") || text.contains("failed")) {
            return "A disappointing exam score is merely a redirection, not a dead end. Consider these structural steps:\n\n" +
                    "- **Alternative entrances**: Direct focus on VITEEE, WBJEE, COMEDK, or CUET.\n" +
                    "- **Certifications & Open Source**: Build a strong software portfolio on GitHub. Direct coding builds always trump college tier marks in technical recruiting.\n\n" +
                    "Access the **Failure Recovery Module** in your dashboard for a custom step-by-step fallback map.";
        } else if (text.contains("motivate") || text.contains("motivation")) {
            return "Remember: *\"Every expert developer you admire was once a confused student staring at a compile error they did not understand. Success is not a single leap—it is a sequence of daily commits.\"* Keep your streak active, and let's clear the next RPG node!";
        }

        return "I have indexed your inquiry. As your educational counselor, I recommend checking your **RPG Skill Tree** or completing the **Diagnostic Quiz** to map out your core domain targets. What specific subject can we troubleshoot next?";
    }

    public String getNodeMentorResponse(User user, RoadmapNode node, String userQuery) {
        String key = user.getId() + "_" + node.getNodeId();
        List<Map<String, String>> history = nodeChatHistory.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        String systemPrompt = String.format(
            "You are StudentAI LifeGPS AI Node Mentor. The student is currently studying the roadmap node: '%s' (Difficulty: %s).\n" +
            "Description: %s.\n" +
            "Answer the student's questions about this topic. Provide clean code snippets, explain best practices, and trace conceptual steps.\n" +
            "Keep the responses highly relevant to '%s' and formatted in Markdown.",
            node.getTitle(), node.getDifficulty(), node.getDescription(), node.getTitle()
        );

        history.add(Map.of("role", "user", "content", userQuery));

        if (history.size() > 10) {
            history.remove(0);
        }

        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.startsWith("${")) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));
                for (Map<String, String> msg : history) {
                    messages.add(new HashMap<>(msg));
                }

                Map<String, Object> body = new HashMap<>();
                body.put("model", "llama-3.3-70b-versatile");
                body.put("messages", messages);
                body.put("temperature", 0.6);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity("https://api.groq.com/openai/v1/chat/completions", request, Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> messageObj = (Map<?, ?>) choice.get("message");
                        String reply = (String) messageObj.get("content");
                        history.add(Map.of("role", "assistant", "content", reply));
                        return reply;
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq Node Mentor call failed: " + e.getMessage());
            }
        }

        String reply = "Here is an explanation of " + node.getTitle() + " matching your query '" + userQuery + "'. Practicing core projects and mini-challenges for this node will build your skills.";
        history.add(Map.of("role", "assistant", "content", reply));
        return reply;
    }
}
