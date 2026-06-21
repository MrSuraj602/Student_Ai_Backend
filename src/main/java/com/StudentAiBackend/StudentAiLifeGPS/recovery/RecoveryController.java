package com.StudentAiBackend.StudentAiLifeGPS.recovery;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class RecoveryController {

    @PostMapping("/plan")
    public ResponseEntity<?> getRecoveryPlan(@RequestBody RecoveryRequest request) {
        Map<String, Object> plan = new HashMap<>();
        
        if ("JEE".equalsIgnoreCase(request.getRoadblock())) {
            plan.put("headline", "Failed JEE Mains Recovery Plan");
            plan.put("actionItems", List.of("Target COMEDK, VITEEE and boards quotas", "Focus on projects portfolio"));
        } else if ("Placement".equalsIgnoreCase(request.getRoadblock())) {
            plan.put("headline", "Failed Placement Recovery Plan");
            plan.put("actionItems", List.of("Solve 150 Leetcode questions", "Establish LinkedIn outreach"));
        } else {
            plan.put("headline", "Low CGPA Recovery Plan");
            plan.put("actionItems", List.of("Obtain cloud certifications", "Apply to startups without limits"));
        }

        return ResponseEntity.ok(plan);
    }
}

@Data
class RecoveryRequest {
    private String roadblock;
}
