package com.StudentAiBackend.StudentAiLifeGPS.quiz;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswers(@AuthenticationPrincipal User principal, @RequestBody List<AnswerDto> answers) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> evaluation = quizService.evaluateQuiz(principal, answers);
            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
