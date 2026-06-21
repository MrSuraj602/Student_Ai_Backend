package com.StudentAiBackend.StudentAiLifeGPS.auth;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            String msg = authService.signup(request.getUsername(), request.getEmail(), request.getPassword());
            Map<String, String> res = new HashMap<>();
            res.put("message", msg);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            AuthResponse response = authService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String msg = authService.forgotPassword(request.getEmail());
            Map<String, String> res = new HashMap<>();
            res.put("message", msg);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(principal);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User principal, @RequestBody User profileUpdates) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (profileUpdates.getUsername() != null) user.setUsername(profileUpdates.getUsername());
        if (profileUpdates.getStrengths() != null) user.setStrengths(profileUpdates.getStrengths());
        if (profileUpdates.getWeaknesses() != null) user.setWeaknesses(profileUpdates.getWeaknesses());
        if (profileUpdates.getRecommendedDomains() != null) user.setRecommendedDomains(profileUpdates.getRecommendedDomains());
        if (profileUpdates.getCareerReadyScore() > 0) user.setCareerReadyScore(profileUpdates.getCareerReadyScore());
        
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }
}

@Data
class SignupRequest {
    private String username;
    private String email;
    private String password;
}

@Data
class VerifyOtpRequest {
    private String email;
    private String otp;
}

@Data
class LoginRequest {
    private String email;
    private String password;
}

@Data
class ForgotPasswordRequest {
    private String email;
}
