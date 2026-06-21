package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.DailyTask;
import com.StudentAiBackend.StudentAiLifeGPS.entity.DailyTaskModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyTaskModuleRepository extends JpaRepository<DailyTaskModule, Long> {
    List<DailyTaskModule> findByDailyTaskOrderByOrderIndexAsc(DailyTask dailyTask);
    void deleteByDailyTask(DailyTask dailyTask);
}
