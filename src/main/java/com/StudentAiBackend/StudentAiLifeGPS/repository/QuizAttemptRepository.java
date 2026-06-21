package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizAttempt;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserOrderByTakenAtDesc(User user);
}
