package org.example.onlineexam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long paperId;
    private String subject;

    @Column(length = 1000)
    private String classNames;

    private Integer duration;
    private String status;          // 未开始/进行中/已结束
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long creatorId;
    private Integer allowRepeat;     // 0否 1是
    private Integer shuffleQuestions;
    private Integer showScore;
    private Integer showAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---------- getter / setter ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getClassNames() { return classNames; }
    public void setClassNames(String classNames) { this.classNames = classNames; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Integer getAllowRepeat() { return allowRepeat; }
    public void setAllowRepeat(Integer allowRepeat) { this.allowRepeat = allowRepeat; }

    public Integer getShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(Integer shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }

    public Integer getShowScore() { return showScore; }
    public void setShowScore(Integer showScore) { this.showScore = showScore; }

    public Integer getShowAnalysis() { return showAnalysis; }
    public void setShowAnalysis(Integer showAnalysis) { this.showAnalysis = showAnalysis; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}