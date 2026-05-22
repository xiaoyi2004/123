package org.example.onlineexam.repository;

import org.example.onlineexam.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    List<PaperQuestion> findByPaperIdOrderBySortOrderAsc(Long paperId);
    boolean existsByQuestionId(Long questionId);
    @Transactional
    void deleteByPaperId(Long paperId);
}
