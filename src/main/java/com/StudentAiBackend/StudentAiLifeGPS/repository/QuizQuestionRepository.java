package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
}
