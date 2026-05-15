package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT COUNT(pq) > 0 FROM PaperQuestion pq WHERE pq.questionId = ?1")
    boolean existsInPaper(Long questionId);
    // 添加方法
    List<Question> findByType(String type);
}