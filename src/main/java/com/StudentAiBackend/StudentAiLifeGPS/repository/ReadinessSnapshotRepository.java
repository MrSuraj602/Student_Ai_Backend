package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.ReadinessSnapshot;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadinessSnapshotRepository extends JpaRepository<ReadinessSnapshot, Long> {
    List<ReadinessSnapshot> findByUserOrderBySnapshotAtDesc(User user);
}
