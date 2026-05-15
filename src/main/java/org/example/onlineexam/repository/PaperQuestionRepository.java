package org.example.onlineexam.repository;

import org.example.onlineexam.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    // 按试卷ID和顺序排序获取题目关联
    List<PaperQuestion> findByPaperIdOrderBySortOrderAsc(Long paperId);
    // 检查题目是否被任何试卷使用（用于删除前校验）
    boolean existsByQuestionId(Long questionId);
}