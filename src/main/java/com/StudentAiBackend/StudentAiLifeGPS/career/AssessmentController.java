package com.StudentAiBackend.StudentAiLifeGPS.career;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final CareerService careerService;

    @GetMapping("/start")
    public ResponseEntity<?> startAssessment(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> test = careerService.generateAssessmentTest(principal);
            return ResponseEntity.ok(test);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(@AuthenticationPrincipal User principal, @RequestBody Map<String, Object> submission) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> report = careerService.evaluateAssessmentSubmission(principal, submission);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
