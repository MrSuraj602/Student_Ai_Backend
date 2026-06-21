package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.StudentPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentPersonaRepository extends JpaRepository<StudentPersona, Long> {
    Optional<StudentPersona> findByUser(User user);
    void deleteByUser(User user);
}
