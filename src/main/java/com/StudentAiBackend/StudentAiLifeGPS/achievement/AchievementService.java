package com.StudentAiBackend.StudentAiLifeGPS.achievement;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserRepository userRepository;

    public List<LeaderboardDto> getLeaderboard() {
        List<User> users = userRepository.findAll();
        
        if (users.isEmpty()) {
            return List.of(
                new LeaderboardDto("alex_matrix", 45, 18200, 34, List.of("Python Hero", "DSA Master", "Hackathon Warrior")),
                new LeaderboardDto("tanya_dev", 38, 15400, 21, List.of("Python Hero", "DSA Master")),
                new LeaderboardDto("aryan_ml", 25, 9800, 12, List.of("Python Hero")),
                new LeaderboardDto("student_gps", 12, 4800, 8, List.of("Python Hero")),
                new LeaderboardDto("matrix_freshman", 8, 3200, 4, List.of())
            );
        }

        return users.stream()
                .sorted((u1, u2) -> {
                    if (u2.getLevel() != u1.getLevel()) {
                        return Integer.compare(u2.getLevel(), u1.getLevel());
                    }
                    return Integer.compare(u2.getXp(), u1.getXp());
                })
                .map(user -> {
                    List<String> badges = new ArrayList<>();
                    if (user.getLevel() >= 2) badges.add("Python Hero");
                    if (user.getLevel() >= 3) badges.add("DSA Master");
                    if (user.getLevel() >= 4) badges.add("Hackathon Warrior");
                    if (user.getLevel() >= 5) badges.add("Open Source Champion");
                    
                    return new LeaderboardDto(
                            user.getUsername(),
                            user.getLevel(),
                            user.getXp(),
                            user.getStreak(),
                            badges
                    );
                })
                .collect(Collectors.toList());
    }
}
