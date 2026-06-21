package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.RoadmapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import java.util.List;
import java.util.Optional;

public interface RoadmapNodeRepository extends JpaRepository<RoadmapNode, Long> {
    List<RoadmapNode> findByUser(User user);
    Optional<RoadmapNode> findByUserAndNodeId(User user, String nodeId);
    void deleteByUser(User user);
}

