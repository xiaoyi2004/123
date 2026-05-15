CREATE DATABASE IF NOT EXISTS online_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_exam;

DROP TABLE IF EXISTS exam_result;
DROP TABLE IF EXISTS paper_question;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS paper;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          username VARCHAR(50) NOT NULL UNIQUE,
                          password VARCHAR(100) NOT NULL,
                          real_name VARCHAR(50),
                          role VARCHAR(20) NOT NULL COMMENT 'teacher/student'
);

CREATE TABLE question (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          title VARCHAR(500) NOT NULL,
                          type VARCHAR(20) NOT NULL COMMENT 'single/judge/fill',
                          option_a VARCHAR(255),
                          option_b VARCHAR(255),
                          option_c VARCHAR(255),
                          option_d VARCHAR(255),
                          answer VARCHAR(255) NOT NULL,
                          score INT NOT NULL DEFAULT 5
);

CREATE TABLE paper (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       description VARCHAR(255)
);

CREATE TABLE paper_question (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                paper_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL
);

CREATE TABLE exam (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(100) NOT NULL,
                      paper_id BIGINT NOT NULL,
                      duration INT NOT NULL DEFAULT 60,
                      status VARCHAR(20) NOT NULL DEFAULT '进行中'
);

CREATE TABLE exam_result (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             student_id BIGINT NOT NULL,
                             exam_id BIGINT NOT NULL,
                             score INT NOT NULL DEFAULT 0,
                             answers_json TEXT,
                             submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_user(username,password,real_name,role) VALUES
                                                           ('teacher1','123456','教师一','teacher'),
                                                           ('student1','123456','学生一','student');

INSERT INTO question(title,type,option_a,option_b,option_c,option_d,answer,score) VALUES
                                                                                      ('Java 中哪个关键字用于定义类？','single','class','public','static','void','A',5),
                                                                                      ('Spring Boot 可以简化 Spring 应用开发。','judge','正确','错误',NULL,NULL,'A',5),
                                                                                      ('HTML 的中文名称是？','fill',NULL,NULL,NULL,NULL,'超文本标记语言',10);
