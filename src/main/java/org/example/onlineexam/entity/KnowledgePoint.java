package org.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "knowledge_point")
public class KnowledgePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 500)
    private String description; // 知识点描述

    private Long parentId;

    private Long courseId;

    private Integer level;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 非数据库字段，用于树形结构展示（初始化为空列表避免 NPE）
    @Transient
    private List<KnowledgePoint> children = new ArrayList<>();
}