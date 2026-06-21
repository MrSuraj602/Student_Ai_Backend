package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.StudySession;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUserAndSessionDateBetween(User user, LocalDate start, LocalDate end);
    List<StudySession> findByUserOrderBySessionDateDesc(User user);
}
