package com.StudentAiBackend.StudentAiLifeGPS.auth;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
}
