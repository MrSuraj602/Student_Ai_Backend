package com.StudentAiBackend.StudentAiLifeGPS.career;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @PostMapping("/recommend-careers")
    public ResponseEntity<?> recommendCareers(@AuthenticationPrincipal User principal, @RequestBody Map<String, Object> profile) {
        System.out.println("[CareerController] recommendCareers called");
        try {
            Map<String, Object> recommendations = careerService.recommendCareers(profile);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/suggest-skills")
    public ResponseEntity<?> suggestSkills(@AuthenticationPrincipal User principal, @RequestParam String career) {
        try {
            return ResponseEntity.ok(Map.of("suggestedSkills", careerService.suggestSkills(career)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/boss/{bossId}/challenge")
    public ResponseEntity<?> getBossChallenge(@AuthenticationPrincipal User principal, @PathVariable String bossId) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> challenge = careerService.generateBossChallenge(principal, bossId);
            return ResponseEntity.ok(challenge);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/boss/{bossId}/evaluate")
    public ResponseEntity<?> evaluateBossFight(@AuthenticationPrincipal User principal, @PathVariable String bossId, @RequestBody Map<String, Object> submission) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> report = careerService.evaluateBossSubmission(principal, bossId, submission);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

