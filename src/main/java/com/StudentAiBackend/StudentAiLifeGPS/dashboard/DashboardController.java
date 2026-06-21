package com.StudentAiBackend.StudentAiLifeGPS.dashboard;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getSummary(principal));
    }

    @GetMapping("/skills")
    public ResponseEntity<?> getSkills(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getSkills(principal));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmap(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getHeatmap(principal));
    }

    @GetMapping("/xp")
    public ResponseEntity<?> getXp(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getXp(principal));
    }

    @GetMapping("/roadmap")
    public ResponseEntity<?> getRoadmap(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getRoadmap(principal));
    }

    @GetMapping("/readiness")
    public ResponseEntity<?> getReadiness(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getReadiness(principal));
    }

    @GetMapping("/achievements")
    public ResponseEntity<?> getAchievements(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(dashboardService.getAchievements(principal));
    }
}
