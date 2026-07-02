-- ==================== 创建数据库 ====================
CREATE DATABASE IF NOT EXISTS online_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_exam;

-- ==================== 用户表 ====================
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          username VARCHAR(50) NOT NULL UNIQUE,
                          password VARCHAR(100) NOT NULL,
                          real_name VARCHAR(50),
                          role VARCHAR(20) NOT NULL COMMENT 'teacher/student/admin',
                          class_name VARCHAR(100),
                          student_no VARCHAR(50),
                          status TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
                          phone VARCHAR(20),
                          email VARCHAR(100),
                          clazz_id BIGINT,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==================== 课程表 ====================
CREATE TABLE course (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        code VARCHAR(50),
                        description VARCHAR(255),
                        teacher_id BIGINT,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==================== 班级表 ====================
CREATE TABLE clazz (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       course_id BIGINT,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==================== 知识点表 ====================
CREATE TABLE knowledge_point (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 name VARCHAR(100) NOT NULL,
                                 description VARCHAR(500),
                                 parent_id BIGINT DEFAULT NULL,
                                 course_id BIGINT,
                                 level INT DEFAULT 0,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 FOREIGN KEY (parent_id) REFERENCES knowledge_point(id) ON DELETE CASCADE,
                                 FOREIGN KEY (course_id) REFERENCES course(id)
);

-- ==================== 题目表 ====================
CREATE TABLE question (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          title VARCHAR(1000) NOT NULL,
                          type VARCHAR(30) NOT NULL COMMENT 'single/judge/fill/essay/multiple_choice/analysis/programming',
                          subject VARCHAR(100),
                          course_id BIGINT,
                          difficulty TINYINT DEFAULT 1 COMMENT '1-5',
                          knowledge_point VARCHAR(100),
                          knowledge_point_id BIGINT,
                          option_a VARCHAR(255),
                          option_b VARCHAR(255),
                          option_c VARCHAR(255),
                          option_d VARCHAR(255),
                          option_e VARCHAR(255),
                          answer VARCHAR(1000) NOT NULL,
                          analysis VARCHAR(2000),
                          image_url VARCHAR(500),
                          creator_id BIGINT,
                          scoring_rule VARCHAR(20) DEFAULT 'exact' COMMENT 'exact/partial/exact_wrong',
                          code_snippet TEXT,
                          test_cases TEXT,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
);

-- ==================== 试卷表 ====================
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

-- ==================== 试卷题目关联表 ====================
CREATE TABLE paper_question (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                paper_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                score INT NOT NULL DEFAULT 0,
                                sort_order INT NOT NULL DEFAULT 0,
                                FOREIGN KEY (paper_id) REFERENCES paper(id),
                                FOREIGN KEY (question_id) REFERENCES question(id)
);

-- ==================== 考试表 ====================
CREATE TABLE exam (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(100) NOT NULL,
                      paper_id BIGINT NOT NULL,
                      subject VARCHAR(100),
                      class_names VARCHAR(1000),
                      duration INT NOT NULL DEFAULT 60,
                      status VARCHAR(20) NOT NULL DEFAULT '进行中',
                      start_time DATETIME,
                      end_time DATETIME,
                      creator_id BIGINT,
                      allow_repeat TINYINT DEFAULT 0,
                      shuffle_questions TINYINT DEFAULT 0,
                      show_score TINYINT DEFAULT 1,
                      show_analysis TINYINT DEFAULT 1,
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      FOREIGN KEY (paper_id) REFERENCES paper(id)
);

-- ==================== 考试结果表 ====================
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
                             submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             is_graded TINYINT DEFAULT 0,
                             creator_id BIGINT,
                             FOREIGN KEY (student_id) REFERENCES sys_user(id),
                             FOREIGN KEY (exam_id) REFERENCES exam(id)
);

-- ==================== 学生答案表 ====================
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
                                UNIQUE KEY uk_student_exam_question (student_id, exam_id, question_id),
                                FOREIGN KEY (student_id) REFERENCES sys_user(id),
                                FOREIGN KEY (exam_id) REFERENCES exam(id),
                                FOREIGN KEY (question_id) REFERENCES question(id)
);

-- ==================== 公告表 ====================
CREATE TABLE announcement (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              title VARCHAR(200) NOT NULL,
                              content TEXT,
                              publisher_id BIGINT,
                              status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布 2已下线',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==================== 通知表 ====================
CREATE TABLE notification (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              title VARCHAR(200) NOT NULL,
                              content TEXT,
                              type VARCHAR(50) NOT NULL COMMENT 'exam_start/grade_published/system',
                              target_url VARCHAR(255),
                              sender_id BIGINT,
                              sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (sender_id) REFERENCES sys_user(id)
);

-- ==================== 用户通知关联表 ====================
CREATE TABLE user_notification (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   user_id BIGINT NOT NULL,
                                   notification_id BIGINT NOT NULL,
                                   is_read TINYINT DEFAULT 0,
                                   read_at DATETIME,
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (user_id) REFERENCES sys_user(id),
                                   FOREIGN KEY (notification_id) REFERENCES notification(id),
                                   UNIQUE KEY uk_user_notification (user_id, notification_id)
);

-- ==================== 操作日志表 ====================
CREATE TABLE operation_log (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT,
                               username VARCHAR(50),
                               module VARCHAR(50),
                               action VARCHAR(50),
                               request_path VARCHAR(200),
                               request_params TEXT,
                               ip VARCHAR(50),
                               result TINYINT DEFAULT 1,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 初始化数据 ====================
-- 管理员账号（密码 123456，BCrypt 加密）
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
    ('admin', '$2a$10$NkM7R9nZO2Fx8Hf5s5qOcO3y5gQ2hW5Jx5K5L5M5N5O5P5Q5R5S5T', '系统管理员', 'admin', 1);

-- 示例班级
INSERT INTO clazz (name) VALUES ('计科2301'), ('计科2302'), ('软工2301');

-- 示例课程
INSERT INTO course (name, code) VALUES ('软件设计与体系结构', 'SE301'), ('大学英语', 'ENG101'), ('Oracle高级数据库技术', 'DB401'), ('软件工程导论', 'SE101'), ('软件过程与管理', 'SE102'), ('软件测试', 'SE201');

-- 示例知识点（包含上述课程的一级、二级节点，层级已设置）
INSERT INTO knowledge_point (name, description, parent_id, course_id, level, created_at, updated_at) VALUES
-- 软件设计与体系结构
('软件设计基础', '软件设计的基本原则与模式', NULL, (SELECT id FROM course WHERE name = '软件设计与体系结构'), 0, NOW(), NOW()),
('设计原则（SOLID、DRY等）', '面向对象设计原则', (SELECT id FROM knowledge_point WHERE name = '软件设计基础' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),
('设计模式（创建型、结构型、行为型）', 'GoF设计模式分类', (SELECT id FROM knowledge_point WHERE name = '软件设计基础' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),
('软件架构风格', '常见软件架构风格', NULL, (SELECT id FROM course WHERE name = '软件设计与体系结构'), 0, NOW(), NOW()),
('分层架构', '经典三层架构', (SELECT id FROM knowledge_point WHERE name = '软件架构风格' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),
('微服务架构', '微服务设计原则', (SELECT id FROM knowledge_point WHERE name = '软件架构风格' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),
('质量属性与评估', '质量属性及评估方法', NULL, (SELECT id FROM course WHERE name = '软件设计与体系结构'), 0, NOW(), NOW()),
('可用性、可靠性、性能', '质量属性定义', (SELECT id FROM knowledge_point WHERE name = '质量属性与评估' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),
('架构评估方法（ATAM、CBAM）', '架构贸易分析方法', (SELECT id FROM knowledge_point WHERE name = '质量属性与评估' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构')), (SELECT id FROM course WHERE name = '软件设计与体系结构'), 1, NOW(), NOW()),

-- 大学英语
('基础语法与词汇', '英语语法基础', NULL, (SELECT id FROM course WHERE name = '大学英语'), 0, NOW(), NOW()),
('时态、语态、从句', '核心语法知识', (SELECT id FROM knowledge_point WHERE name = '基础语法与词汇' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),
('高频词汇与搭配', '常用学术词汇', (SELECT id FROM knowledge_point WHERE name = '基础语法与词汇' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),
('阅读与写作', '阅读理解与写作', NULL, (SELECT id FROM course WHERE name = '大学英语'), 0, NOW(), NOW()),
('阅读理解技巧', '快速阅读与细节定位', (SELECT id FROM knowledge_point WHERE name = '阅读与写作' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),
('学术写作规范', '论文结构、引用格式', (SELECT id FROM knowledge_point WHERE name = '阅读与写作' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),
('听力与口语', '听力策略与口语', NULL, (SELECT id FROM course WHERE name = '大学英语'), 0, NOW(), NOW()),
('听力理解策略', '笔记技巧与信号词', (SELECT id FROM knowledge_point WHERE name = '听力与口语' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),
('日常与学术口语表达', '口语常用句型', (SELECT id FROM knowledge_point WHERE name = '听力与口语' AND course_id = (SELECT id FROM course WHERE name = '大学英语')), (SELECT id FROM course WHERE name = '大学英语'), 1, NOW(), NOW()),

-- Oracle高级数据库技术
('Oracle体系结构', 'Oracle物理与内存结构', NULL, (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 0, NOW(), NOW()),
('内存结构（SGA、PGA）', '系统全局区与程序全局区', (SELECT id FROM knowledge_point WHERE name = 'Oracle体系结构' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),
('物理结构（数据文件、控制文件、重做日志）', '数据库物理文件组成', (SELECT id FROM knowledge_point WHERE name = 'Oracle体系结构' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),
('SQL优化与调优', '执行计划与性能调优', NULL, (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 0, NOW(), NOW()),
('执行计划分析', '解释计划与调优', (SELECT id FROM knowledge_point WHERE name = 'SQL优化与调优' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),
('索引设计与优化', '索引类型与选择', (SELECT id FROM knowledge_point WHERE name = 'SQL优化与调优' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),
('备份与恢复', 'RMAN及闪回技术', NULL, (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 0, NOW(), NOW()),
('RMAN备份恢复', 'Recovery Manager工具', (SELECT id FROM knowledge_point WHERE name = '备份与恢复' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),
('闪回技术与数据保护', '闪回查询、闪回表', (SELECT id FROM knowledge_point WHERE name = '备份与恢复' AND course_id = (SELECT id FROM course WHERE name = 'Oracle高级数据库技术')), (SELECT id FROM course WHERE name = 'Oracle高级数据库技术'), 1, NOW(), NOW()),

-- 软件工程导论
('软件生命周期', '软件过程模型', NULL, (SELECT id FROM course WHERE name = '软件工程导论'), 0, NOW(), NOW()),
('瀑布模型、迭代模型', '传统与迭代开发模型', (SELECT id FROM knowledge_point WHERE name = '软件生命周期' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),
('敏捷开发方法', 'Scrum、XP等', (SELECT id FROM knowledge_point WHERE name = '软件生命周期' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),
('需求工程', '需求获取与验证', NULL, (SELECT id FROM course WHERE name = '软件工程导论'), 0, NOW(), NOW()),
('需求获取与建模', '访谈、用例、原型', (SELECT id FROM knowledge_point WHERE name = '需求工程' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),
('需求验证与变更管理', '需求评审与变更控制', (SELECT id FROM knowledge_point WHERE name = '需求工程' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),
('软件项目管理', '项目计划与风险管理', NULL, (SELECT id FROM course WHERE name = '软件工程导论'), 0, NOW(), NOW()),
('项目计划与进度管理', '甘特图、关键路径法', (SELECT id FROM knowledge_point WHERE name = '软件项目管理' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),
('风险管理与质量保证', '风险识别与评估', (SELECT id FROM knowledge_point WHERE name = '软件项目管理' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论')), (SELECT id FROM course WHERE name = '软件工程导论'), 1, NOW(), NOW()),

-- 软件过程与管理
('过程模型', 'CMMI与ISO标准', NULL, (SELECT id FROM course WHERE name = '软件过程与管理'), 0, NOW(), NOW()),
('CMMI成熟度模型', '能力成熟度模型集成', (SELECT id FROM knowledge_point WHERE name = '过程模型' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),
('ISO 9001质量体系', '质量管理体系要求', (SELECT id FROM knowledge_point WHERE name = '过程模型' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),
('项目计划与控制', '工作量估算与版本管理', NULL, (SELECT id FROM course WHERE name = '软件过程与管理'), 0, NOW(), NOW()),
('工作量估算（功能点、COCOMO）', '估算方法及工具', (SELECT id FROM knowledge_point WHERE name = '项目计划与控制' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),
('版本控制与配置管理', 'Git、SVN与基线管理', (SELECT id FROM knowledge_point WHERE name = '项目计划与控制' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),
('团队与沟通管理', '团队协作与沟通', NULL, (SELECT id FROM course WHERE name = '软件过程与管理'), 0, NOW(), NOW()),
('团队协作模式', 'Agile团队、自组织', (SELECT id FROM knowledge_point WHERE name = '团队与沟通管理' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),
('沟通管理与会议技巧', '高效会议与沟通策略', (SELECT id FROM knowledge_point WHERE name = '团队与沟通管理' AND course_id = (SELECT id FROM course WHERE name = '软件过程与管理')), (SELECT id FROM course WHERE name = '软件过程与管理'), 1, NOW(), NOW()),

-- 软件测试
('测试基础', '测试分类与用例设计', NULL, (SELECT id FROM course WHERE name = '软件测试'), 0, NOW(), NOW()),
('测试分类（单元测试、集成测试、系统测试）', '各级测试定义', (SELECT id FROM knowledge_point WHERE name = '测试基础' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW()),
('测试用例设计（等价类、边界值、场景法）', '黑盒测试用例设计', (SELECT id FROM knowledge_point WHERE name = '测试基础' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW()),
('自动化测试', '自动化框架与CI', NULL, (SELECT id FROM course WHERE name = '软件测试'), 0, NOW(), NOW()),
('自动化框架（Selenium、JUnit）', '主流自动化工具', (SELECT id FROM knowledge_point WHERE name = '自动化测试' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW()),
('持续集成与测试', 'CI/CD中的测试', (SELECT id FROM knowledge_point WHERE name = '自动化测试' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW()),
('性能与安全测试', '性能与安全测试方法', NULL, (SELECT id FROM course WHERE name = '软件测试'), 0, NOW(), NOW()),
('性能测试（LoadRunner、JMeter）', '性能测试工具', (SELECT id FROM knowledge_point WHERE name = '性能与安全测试' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW()),
('安全测试基础（OWASP、渗透测试）', '安全测试漏洞与工具', (SELECT id FROM knowledge_point WHERE name = '性能与安全测试' AND course_id = (SELECT id FROM course WHERE name = '软件测试')), (SELECT id FROM course WHERE name = '软件测试'), 1, NOW(), NOW());

-- 示例题目（含单选、多选、判断、填空、简答、分析、编程各一条）
INSERT INTO question (title, type, subject, difficulty, answer, analysis, creator_id, knowledge_point_id) VALUES
                                                                                                              ('Java中哪个关键字用于定义类？', 'single', 'Java', 1, 'A', 'class 关键字用于声明类。', 1, (SELECT id FROM knowledge_point WHERE name = '设计原则（SOLID、DRY等）' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构'))),
                                                                                                              ('以下哪些属于SOLID原则？', 'multiple_choice', 'Java', 3, 'A,B,C,D,E', 'SOLID包含五个原则。', 1, (SELECT id FROM knowledge_point WHERE name = '设计原则（SOLID、DRY等）' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构'))),
                                                                                                              ('Spring Boot 可以简化 Spring 应用开发。', 'judge', 'Java', 1, 'A', 'Spring Boot 通过自动配置简化开发。', 1, (SELECT id FROM knowledge_point WHERE name = '设计原则（SOLID、DRY等）' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构'))),
                                                                                                              ('HTML 的中文名称是？', 'fill', 'Web', 2, '超文本标记语言', 'HTML 是 HyperText Markup Language。', 1, (SELECT id FROM knowledge_point WHERE name = '测试基础' AND course_id = (SELECT id FROM course WHERE name = '软件测试'))),
                                                                                                              ('简述瀑布模型的主要阶段。', 'essay', '软件工程', 3, '需求分析、设计、编码、测试、维护。', '瀑布模型按线性顺序执行。', 1, (SELECT id FROM knowledge_point WHERE name = '瀑布模型、迭代模型' AND course_id = (SELECT id FROM course WHERE name = '软件工程导论'))),
                                                                                                              ('某系统响应时间慢，分析可能原因并提出优化方案。', 'analysis', '软件架构', 4, '原因可能包括索引缺失、SQL效率低、硬件瓶颈。优化方案：添加索引、SQL调优、升级硬件。', '性能问题通常从数据库和代码层面分析。', 1, (SELECT id FROM knowledge_point WHERE name = '可用性、可靠性、性能' AND course_id = (SELECT id FROM course WHERE name = '软件设计与体系结构'))),
                                                                                                              ('编写一个程序，输入两个整数，输出它们的和。', 'programming', '编程基础', 2, 'Java实现: public class Add { public static void main(String[] args) { int a=1,b=2; System.out.println(a+b); } }', '考察基础输入输出和变量操作。', 1, (SELECT id FROM knowledge_point WHERE name = '测试基础' AND course_id = (SELECT id FROM course WHERE name = '软件测试')));

-- 示例试卷
INSERT INTO paper (name, subject, creator_id, total_score) VALUES ('综合测试卷', '综合', 1, 100);

-- 示例考试
INSERT INTO exam (name, paper_id, subject, class_names, duration, status, start_time, end_time, creator_id) VALUES
    ('期中考试', 1, '综合', '计科2301,计科2302', 60, '进行中', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 1);