-- =============================================
-- 数据库初始化脚本
-- 数据库：online_exam
-- 字符集：utf8mb4
-- 说明：包含所有表及初始数据
-- =============================================

CREATE DATABASE IF NOT EXISTS online_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_exam;

-- 删除已存在的表（按依赖顺序）
DROP TABLE IF EXISTS student_answer;
DROP TABLE IF EXISTS exam_result;
DROP TABLE IF EXISTS paper_question;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS paper;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS clazz;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS sys_user;

-- ==============================
-- 1. 用户表 sys_user
-- ==============================
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          username VARCHAR(50) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          real_name VARCHAR(50),
                          role VARCHAR(20) NOT NULL COMMENT 'admin/teacher/student',
                          class_name VARCHAR(100),
                          student_no VARCHAR(50) COMMENT '学号或工号',
                          status TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
                          phone VARCHAR(20),
                          email VARCHAR(100),
                          clazz_id BIGINT COMMENT '关联班级ID',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 2. 课程表 course
-- ==============================
CREATE TABLE course (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        code VARCHAR(50),
                        description VARCHAR(255),
                        teacher_id BIGINT COMMENT '负责教师ID',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 3. 班级表 clazz
-- ==============================
CREATE TABLE clazz (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       course_id BIGINT COMMENT '关联课程ID',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 4. 题目表 question
-- ==============================
CREATE TABLE question (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          title VARCHAR(1000) NOT NULL,
                          type VARCHAR(20) NOT NULL COMMENT 'single/judge/fill/essay',
                          subject VARCHAR(100),
                          course_id BIGINT COMMENT '关联课程ID',
                          difficulty TINYINT DEFAULT 1 COMMENT '1-5',
                          knowledge_point VARCHAR(100),
                          option_a VARCHAR(255),
                          option_b VARCHAR(255),
                          option_c VARCHAR(255),
                          option_d VARCHAR(255),
                          answer VARCHAR(1000) NOT NULL,
                          analysis VARCHAR(2000) COMMENT '答案解析',
                          image_url VARCHAR(500),
                          creator_id BIGINT COMMENT '创建人ID',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 5. 试卷表 paper
-- ==============================
CREATE TABLE paper (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       subject VARCHAR(100),
                       description VARCHAR(255),
                       creator_id BIGINT,
                       total_score INT DEFAULT 0,
                       pass_score INT DEFAULT 60,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 6. 试卷题目关联表 paper_question
-- ==============================
CREATE TABLE paper_question (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                paper_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                score INT NOT NULL DEFAULT 0,
                                sort_order INT NOT NULL DEFAULT 0,
                                FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE,
                                FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);

-- ==============================
-- 7. 考试表 exam
-- ==============================
CREATE TABLE exam (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(100) NOT NULL,
                      paper_id BIGINT NOT NULL,
                      subject VARCHAR(100),
                      class_names VARCHAR(1000) COMMENT '逗号分隔的班级名称',
                      duration INT NOT NULL DEFAULT 60,
                      status VARCHAR(20) NOT NULL DEFAULT '进行中' COMMENT '未开始/进行中/已结束',
                      start_time DATETIME,
                      end_time DATETIME,
                      creator_id BIGINT,
                      allow_repeat TINYINT DEFAULT 0 COMMENT '0不允许 1允许',
                      shuffle_questions TINYINT DEFAULT 0 COMMENT '0顺序 1乱序',
                      show_score TINYINT DEFAULT 1 COMMENT '0不显示 1显示',
                      show_analysis TINYINT DEFAULT 1 COMMENT '0不显示 1显示',
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE
);

-- ==============================
-- 8. 考试结果表 exam_result
-- ==============================
CREATE TABLE exam_result (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             student_id BIGINT NOT NULL,
                             exam_id BIGINT NOT NULL,
                             score INT NOT NULL DEFAULT 0,
                             objective_score INT NOT NULL DEFAULT 0,
                             subjective_score INT NOT NULL DEFAULT 0,
                             grade_status VARCHAR(20) NOT NULL DEFAULT '已批阅' COMMENT '待批阅/已批阅',
                             answers_json TEXT COMMENT 'JSON格式答案',
                             subjective_scores_json TEXT COMMENT 'JSON格式主观题得分',
                             submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             is_graded TINYINT DEFAULT 0 COMMENT '0未评完 1已评完',
                             creator_id BIGINT COMMENT '评分人ID',
                             FOREIGN KEY (exam_id) REFERENCES exam(id) ON DELETE CASCADE
);

-- ==============================
-- 9. 学生答案表 student_answer（用于保存中间状态和恢复）
-- ==============================
CREATE TABLE student_answer (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                student_id BIGINT NOT NULL,
                                exam_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                answer TEXT,
                                is_correct TINYINT DEFAULT 0,
                                score INT DEFAULT 0,
                                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                UNIQUE KEY uk_student_exam_question (student_id, exam_id, question_id)
);

-- ==============================
-- 10. 公告表 announcement
-- ==============================
CREATE TABLE announcement (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              title VARCHAR(200) NOT NULL,
                              content TEXT,
                              publisher_id BIGINT,
                              status TINYINT DEFAULT 0 COMMENT '0草稿 1发布 2下线',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==============================
-- 11. 操作日志表 operation_log
-- ==============================
CREATE TABLE operation_log (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT,
                               username VARCHAR(50),
                               module VARCHAR(50),
                               action VARCHAR(50),
                               request_path VARCHAR(200),
                               request_params TEXT,
                               ip VARCHAR(50),
                               result TINYINT DEFAULT 1 COMMENT '1成功 0失败',
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 初始数据
-- =============================================

-- 默认管理员
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJ9tPmzq4Xh2nK7p5m9C8wE6f3Lk4S', '系统管理员', 'admin', 1);

-- 默认教师
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
    ('teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJ9tPmzq4Xh2nK7p5m9C8wE6f3Lk4S', '张老师', 'teacher', 1);

-- 默认学生
INSERT INTO sys_user (username, password, real_name, role, class_name, student_no, status) VALUES
                                                                                               ('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJ9tPmzq4Xh2nK7p5m9C8wE6f3Lk4S', '李明', 'student', '计科2301', '2023001', 1),
                                                                                               ('student2', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrJ9tPmzq4Xh2nK7p5m9C8wE6f3Lk4S', '王芳', 'student', '计科2302', '2023002', 1);

-- 默认课程
INSERT INTO course (name, code, description, teacher_id) VALUES
                                                             ('Java程序设计', 'CS101', 'Java基础与面向对象', 1),
                                                             ('Web前端开发', 'CS201', 'HTML/CSS/JavaScript', 1);

-- 默认班级
INSERT INTO clazz (name, course_id) VALUES
                                        ('计科2301', 1),
                                        ('计科2302', 1),
                                        ('计科2303', 2);

-- 示例题目（客观题）
INSERT INTO question (title, type, subject, option_a, option_b, option_c, option_d, answer, analysis, creator_id) VALUES
                                                                                                                      ('Java中，以下哪个关键字用于定义类？', 'single', 'Java', 'class', 'public', 'static', 'void', 'A', 'class关键字用于声明类。', 1),
                                                                                                                      ('Spring Boot 可以自动配置 Spring 应用？', 'judge', 'Java', '正确', '错误', NULL, NULL, 'A', 'Spring Boot通过自动配置简化开发。', 1),
                                                                                                                      ('HTML 的中文全称是？', 'fill', 'Web', NULL, NULL, NULL, NULL, '超文本标记语言', 'HTML是HyperText Markup Language的缩写。', 1);

-- 示例试卷
INSERT INTO paper (name, subject, creator_id, total_score, pass_score) VALUES
    ('Java基础测试', 'Java', 1, 15, 10);

-- 试卷题目关联（每题5分）
INSERT INTO paper_question (paper_id, question_id, score, sort_order) VALUES
                                                                          (1, 1, 5, 1),
                                                                          (1, 2, 5, 2),
                                                                          (1, 3, 5, 3);

-- 示例考试
INSERT INTO exam (name, paper_id, subject, class_names, duration, status, start_time, end_time, creator_id, allow_repeat, shuffle_questions, show_score, show_analysis) VALUES
    ('Java随堂测验', 1, 'Java', '计科2301,计科2302', 30, '进行中', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 0, 0, 1, 1);

-- 示例公告
INSERT INTO announcement (title, content, publisher_id, status) VALUES
    ('关于考试安排的通知', '请各位同学按时参加Java随堂测验，考试时间为30分钟。', 1, 1);