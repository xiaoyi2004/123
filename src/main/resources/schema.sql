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
                          role VARCHAR(20) NOT NULL COMMENT 'teacher/student',
                          class_name VARCHAR(100),
                          student_no VARCHAR(50)
);

CREATE TABLE question (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          title VARCHAR(1000) NOT NULL,
                          type VARCHAR(20) NOT NULL COMMENT 'single/judge/fill/essay',
                          subject VARCHAR(100),
                          option_a VARCHAR(255),
                          option_b VARCHAR(255),
                          option_c VARCHAR(255),
                          option_d VARCHAR(255),
                          answer VARCHAR(1000) NOT NULL,
                          analysis VARCHAR(2000),
                          image_url VARCHAR(500)
);

CREATE TABLE paper (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       subject VARCHAR(100),
                       description VARCHAR(255)
);

CREATE TABLE paper_question (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                paper_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                score INT NOT NULL DEFAULT 0,
                                sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE exam (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(100) NOT NULL,
                      paper_id BIGINT NOT NULL,
                      subject VARCHAR(100),
                      class_names VARCHAR(1000),
                      duration INT NOT NULL DEFAULT 60,
                      status VARCHAR(20) NOT NULL DEFAULT '进行中',
                      start_time DATETIME,
                      end_time DATETIME
);

CREATE TABLE exam_result (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             student_id BIGINT NOT NULL,
                             exam_id BIGINT NOT NULL,
                             score INT NOT NULL DEFAULT 0,
                             objective_score INT NOT NULL DEFAULT 0,
                             subjective_score INT NOT NULL DEFAULT 0,
                             grade_status VARCHAR(20) NOT NULL DEFAULT '已批阅',
                             answers_json TEXT,
                             subjective_scores_json TEXT,
                             submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_user(username,password,real_name,role,class_name,student_no) VALUES
                                                                                 ('teacher1','123456','教师一','teacher',NULL,NULL),
                                                                                 ('student1','123456','学生一','student','计科2301','2023001'),
                                                                                 ('student2','123456','学生二','student','计科2302','2023002');

INSERT INTO question(title,type,subject,option_a,option_b,option_c,option_d,answer,analysis) VALUES
                                                                                                 ('Java 中哪个关键字用于定义类？','single','Java','class','public','static','void','A','class 用于声明类。'),
                                                                                                 ('Spring Boot 可以简化 Spring 应用开发。','judge','Java','正确','错误',NULL,NULL,'A','Spring Boot 通过自动配置简化开发。'),
                                                                                                 ('HTML 的中文名称是？','fill','Web',NULL,NULL,NULL,NULL,'超文本标记语言','HTML 是 HyperText Markup Language。'),
                                                                                                 ('请简述 MVC 三层职责。','essay','Java',NULL,NULL,NULL,NULL,'模型负责数据，视图负责展示，控制器负责请求分发。','按职责分离回答即可。');
