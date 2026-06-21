package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.UserNodeProgress;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNodeProgressRepository extends JpaRepository<UserNodeProgress, Long> {
    List<UserNodeProgress> findByUser(User user);
    Optional<UserNodeProgress> findByUserAndRoadmapNode_NodeId(User user, String nodeId);
    void deleteByUser(User user);
}
