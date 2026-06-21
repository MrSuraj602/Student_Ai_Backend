package com.StudentAiBackend.StudentAiLifeGPS.planner;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.ModuleMessage;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @PostMapping("/onboarding")
    public ResponseEntity<?> saveOnboarding(@AuthenticationPrincipal User principal, @RequestBody PlannerRequest request) {
        System.out.println("START PROFILE");
        System.out.println(request);
        if (principal == null) {
            System.err.println("[PlannerController] Unauthorized access request");
            return ResponseEntity.status(401).body("User not authenticated");
        }
        System.out.println("USER");
        System.out.println(principal);
        System.out.println(principal.getId());
        System.out.println(principal.getEmail());
        
        try {
            System.out.println("[PlannerController] Payload values: Goals=" + request.getGoals() + 
                               ", Skills=" + request.getSkills() + 
                               ", Schedule=" + request.getSchedule());
            plannerService.saveOnboarding(principal, request);
            System.out.println("[PlannerController] Onboarding completed successfully.");
            return ResponseEntity.ok("Onboarding data saved successfully");
        } catch (Exception e) {
            System.err.println("[PlannerController] Critical Exception in saveOnboarding: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("PlannerService exception: " + e.getMessage());
        }
    }

    @PostMapping("/orchestrate-onboarding")
    public ResponseEntity<?> orchestrateOnboarding(@AuthenticationPrincipal User principal, @RequestBody PlannerRequest request) {
        System.out.println("[PlannerController] orchestrateOnboarding called");
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        try {
            System.out.println("[PlannerController] Starting single-orchestration onboarding for user: " + principal.getUsername());
            plannerService.orchestrateOnboarding(principal, request);
            return ResponseEntity.ok(Map.of("message", "Onboarding data orchestrated and saved successfully"));
        } catch (Exception e) {
            System.err.println("[PlannerController] Exception in orchestrateOnboarding: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Orchestration failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getPlanner(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(plannerService.generatePlanner(principal));
    }

    @PostMapping("/task/{taskId}/module/{moduleId}/toggle")
    public ResponseEntity<?> toggleModule(
            @AuthenticationPrincipal User principal,
            @PathVariable Long taskId,
            @PathVariable Long moduleId) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            return ResponseEntity.ok(plannerService.toggleTaskModule(principal, taskId, moduleId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/task/{taskId}/module/{moduleId}/chat")
    public ResponseEntity<List<ModuleMessage>> getModuleChat(
            @AuthenticationPrincipal User principal,
            @PathVariable Long taskId,
            @PathVariable Long moduleId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(plannerService.getModuleChat(principal, moduleId));
    }

    @PostMapping("/task/{taskId}/module/{moduleId}/chat")
    public ResponseEntity<?> sendModuleChatMessage(
            @AuthenticationPrincipal User principal,
            @PathVariable Long taskId,
            @PathVariable Long moduleId,
            @RequestBody MessageRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            ModuleMessage reply = plannerService.sendMessageToModuleChat(principal, moduleId, request.getMessage());
            return ResponseEntity.ok(reply);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class MessageRequest {
        private String message;
    }
}

