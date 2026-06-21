package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.CareerReadiness;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareerReadinessRepository extends JpaRepository<CareerReadiness, Long> {
    Optional<CareerReadiness> findByUser(User user);
}
