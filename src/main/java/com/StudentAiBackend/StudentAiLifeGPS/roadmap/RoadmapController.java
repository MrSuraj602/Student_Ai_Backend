package com.StudentAiBackend.StudentAiLifeGPS.roadmap;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.RoadmapNode;
import com.StudentAiBackend.StudentAiLifeGPS.entity.UserNodeProgress;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.RoadmapNodeRepository;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserNodeProgressRepository;
import com.StudentAiBackend.StudentAiLifeGPS.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final UserRepository userRepository;
    private final RoadmapNodeRepository roadmapNodeRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @PostMapping("/node/{nodeId}/complete")
    @Transactional
    public ResponseEntity<?> completeNode(@AuthenticationPrincipal User principal, @PathVariable String nodeId, @RequestBody CompleteNodeRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setXp(user.getXp() + request.getXpReward());
            user.setCoins(user.getCoins() + (request.getXpReward() / 2));
            
            // Level calculation checks
            int requiredXp = user.getLevel() * 400;
            if (user.getXp() >= requiredXp) {
                user.setXp(user.getXp() - requiredXp);
                user.setLevel(user.getLevel() + 1);
            }

            userRepository.save(user);

            // Fetch node and save user node progress
            Optional<RoadmapNode> nodeOpt = roadmapNodeRepository.findByUserAndNodeId(user, nodeId);
            if (nodeOpt.isPresent()) {
                RoadmapNode node = nodeOpt.get();
                UserNodeProgress progress = userNodeProgressRepository.findByUserAndRoadmapNode_NodeId(user, nodeId)
                        .orElse(new UserNodeProgress());
                progress.setUser(user);
                progress.setRoadmapNode(node);
                progress.setStatus("completed");
                progress.setCompletedAt(LocalDateTime.now());
                userNodeProgressRepository.save(progress);
            }

            profileService.refreshCareerReadiness(user, true);

            Map<String, Object> res = new HashMap<>();
            res.put("message", "RPG Skill Node indexed successfully");
            res.put("xp", user.getXp());
            res.put("level", user.getLevel());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/node/{nodeId}/elaborate")
    public ResponseEntity<?> elaborateNode(@AuthenticationPrincipal User principal, @PathVariable String nodeId) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        Optional<RoadmapNode> nodeOpt = roadmapNodeRepository.findByUserAndNodeId(principal, nodeId);
        if (nodeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Node not found"));
        }

        RoadmapNode node = nodeOpt.get();
        String prompt = String.format(
            "Elaborate on the learning topic: '%s' (difficulty: %s, estimated: %d hours).\n" +
            "Description: %s.\n" +
            "Target Career Path: %s.\n" +
            "Please provide a comprehensive guide formatted in Markdown with the following sections:\n" +
            "1. **Core Concept Explanation**: Elaborate the concept clearly with professional context.\n" +
            "2. **Code Examples**: Provide code snippets or architectural diagrams demonstrating practical usage.\n" +
            "3. **Best Practices**: List 3-4 professional best practices for implementation.\n" +
            "4. **Common Mistakes**: Describe typical pitfalls and how to avoid them.\n" +
            "5. **Recommended Resources**: Provide links to official documentation or top tutorial sites.\n" +
            "Output ONLY the markdown response.",
            node.getTitle(),
            node.getDifficulty(),
            node.getHours(),
            node.getDescription(),
            principal.getTargetCareer() != null ? principal.getTargetCareer() : "Software Developer"
        );

        String resultMarkdown = "";
        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.startsWith("${")) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);

                Map<String, Object> body = new HashMap<>();
                body.put("model", "llama-3.3-70b-versatile");
                body.put("messages", List.of(message));
                body.put("temperature", 0.5);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity("https://api.groq.com/openai/v1/chat/completions", request, Map.class);
                Map<?, ?> response = responseEntity.getBody();

                if (response != null && response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        Map<?, ?> messageObj = (Map<?, ?>) choice.get("message");
                        resultMarkdown = (String) messageObj.get("content");
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq Node elaboration call failed: " + e.getMessage());
            }
        }

        if (resultMarkdown.isEmpty()) {
            // Local fallback elaboration
            resultMarkdown = String.format(
                "# Concept Elaboration: %s\n\n" +
                "## Core Concept Explanation\n" +
                "This node covers **%s** under your career goal to become a **%s**. It represents an essential milestone with a estimated practice requirement of %d hours.\n\n" +
                "## Code Examples\n" +
                "```python\n" +
                "# Typical demonstration pattern\n" +
                "def demonstration():\n" +
                "    print('Demonstrating dynamic concept execution for %s')\n" +
                "    return True\n" +
                "demonstration()\n" +
                "```\n\n" +
                "## Best Practices\n" +
                "- Maintain code style consistency and comment logic blocks.\n" +
                "- Write modular functions with high unit-test coverage.\n" +
                "- Use logging instead of prints to trace application metrics.\n\n" +
                "## Common Mistakes\n" +
                "- Hardcoding config parameters. Use environment variables.\n" +
                "- Neglecting error boundaries and exceptions handling.\n\n" +
                "## Recommended Resources\n" +
                "- [Official Documentation](https://docs.python.org/3/)\n" +
                "- [StudentAI LifeGPS Knowledge Matrix](http://localhost:3000/roadmap)\n",
                node.getTitle(), node.getTitle(), 
                principal.getTargetCareer() != null ? principal.getTargetCareer() : "Software Developer", 
                node.getHours(), node.getTitle()
            );
        }

        return ResponseEntity.ok(Map.of("elaboration", resultMarkdown));
    }
}

@Data
class CompleteNodeRequest {
    private int xpReward;
}

