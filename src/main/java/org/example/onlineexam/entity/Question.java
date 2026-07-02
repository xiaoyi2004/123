package org.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "question")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String title;

    private String type;

    private String subject;

    private Long courseId;

    private Integer difficulty;

    private String knowledgePoint;        // 文本存储（兼容旧数据）

    private Long knowledgePointId;        // 关联知识树ID

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;

    @Column(length = 1000)
    private String answer;

    @Column(length = 2000)
    private String analysis;

    private String imageUrl;

    private Long creatorId;

    private String scoringRule;           // exact / partial / exact_wrong

    @Column(columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(columnDefinition = "TEXT")
    private String testCases;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}