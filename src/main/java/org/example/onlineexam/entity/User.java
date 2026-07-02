package org.example.onlineexam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    private String password;
    private String realName;
    private String role;
    private String className;
    private String studentNo; // 学号/工号
    private Integer status = 1; // 默认启用
    private String phone;
    private String email;
    @Column(name = "clazz_id")
    private Long clazzId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}