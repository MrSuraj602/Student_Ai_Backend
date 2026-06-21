package com.StudentAiBackend.StudentAiLifeGPS.controller;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/state")
    public ResponseEntity<?> getProfileState(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(profileService.getProfileState(principal));
    }
}
