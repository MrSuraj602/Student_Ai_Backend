package com.StudentAiBackend.StudentAiLifeGPS.chat;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.RoadmapNode;
import com.StudentAiBackend.StudentAiLifeGPS.repository.RoadmapNodeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RoadmapNodeRepository roadmapNodeRepository;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        try {
            String reply = chatService.getMentorResponse(request.getMessage());
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/node/{nodeId}/message")
    public ResponseEntity<?> sendNodeMessage(
            @AuthenticationPrincipal User principal,
            @PathVariable String nodeId,
            @RequestBody ChatRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Optional<RoadmapNode> nodeOpt = roadmapNodeRepository.findByUserAndNodeId(principal, nodeId);
            if (nodeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Node not found"));
            }
            String reply = chatService.getNodeMentorResponse(principal, nodeOpt.get(), request.getMessage());
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

@Data
class ChatRequest {
    private String message;
}
