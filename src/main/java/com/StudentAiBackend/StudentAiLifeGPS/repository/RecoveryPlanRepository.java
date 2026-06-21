package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.RecoveryPlan;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecoveryPlanRepository extends JpaRepository<RecoveryPlan, Long> {
    Optional<RecoveryPlan> findByUser(User user);
    void deleteByUser(User user);
}
