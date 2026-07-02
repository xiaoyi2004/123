package org.example.onlineexam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_result")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long examId;
    private Integer score;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String gradeStatus;          // 待批阅 / 已批阅
    private Integer isGraded;            // 0未评 1已评

    @Column(columnDefinition = "TEXT")
    private String answersJson;

    @Column(columnDefinition = "TEXT")
    private String subjectiveScoresJson;

    private LocalDateTime submitTime;
    private Long creatorId;              // 评分教师ID

    // ---------- getter / setter ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getObjectiveScore() { return objectiveScore; }
    public void setObjectiveScore(Integer objectiveScore) { this.objectiveScore = objectiveScore; }

    public Integer getSubjectiveScore() { return subjectiveScore; }
    public void setSubjectiveScore(Integer subjectiveScore) { this.subjectiveScore = subjectiveScore; }

    public String getGradeStatus() { return gradeStatus; }
    public void setGradeStatus(String gradeStatus) { this.gradeStatus = gradeStatus; }

    public Integer getIsGraded() { return isGraded; }
    public void setIsGraded(Integer isGraded) { this.isGraded = isGraded; }

    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }

    public String getSubjectiveScoresJson() { return subjectiveScoresJson; }
    public void setSubjectiveScoresJson(String subjectiveScoresJson) { this.subjectiveScoresJson = subjectiveScoresJson; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
}