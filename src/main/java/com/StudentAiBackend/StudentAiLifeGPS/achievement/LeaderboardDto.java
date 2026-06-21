package com.StudentAiBackend.StudentAiLifeGPS.achievement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardDto {
    private String username;
    private int level;
    private int xp;
    private int streak;
    private List<String> badges;
}
