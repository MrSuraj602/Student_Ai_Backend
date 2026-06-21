package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class AcademicProfile {
    private String board;         // CBSE, ICSE, State Board
    private String stream;        // PCM, PCB, Commerce, Arts, Humanities
    private String branch;        // CSE, IT, AI, Mechanical, Civil, ECE
    private String semester;      // 1-8
    private String year;          // 1-5
    private String currentRole;   // Current designation
    private String experience;    // Years of experience
    private String className;     // 11th, 12th
    private String medicalTrack;  // MBBS, BDS, BAMS, BHMS
    private String lawTrack;      // LLB, BA LLB, BBA LLB
}
