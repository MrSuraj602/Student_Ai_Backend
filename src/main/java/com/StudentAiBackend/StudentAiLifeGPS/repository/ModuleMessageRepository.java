package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.ModuleMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleMessageRepository extends JpaRepository<ModuleMessage, Long> {
    List<ModuleMessage> findByModuleIdAndUserIdOrderByCreatedAtAsc(Long moduleId, Long userId);
    void deleteByUserId(Long userId);
}
