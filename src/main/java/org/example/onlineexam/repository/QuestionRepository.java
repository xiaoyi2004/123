package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT COUNT(pq) > 0 FROM PaperQuestion pq WHERE pq.questionId = ?1")
    boolean existsInPaper(Long questionId);

    List<Question> findByType(String type);

    Page<Question> findByTypeAndSubject(String type, String subject, Pageable pageable);
    Page<Question> findByType(String type, Pageable pageable);
    Page<Question> findBySubject(String subject, Pageable pageable);

    @Query("select distinct q.subject from Question q where q.subject is not null and q.subject <> '' order by q.subject")
    List<String> findDistinctSubjects();

    // 按知识点查询
    Page<Question> findByKnowledgePointId(Long knowledgePointId, Pageable pageable);
    Page<Question> findByTypeAndKnowledgePointId(String type, Long knowledgePointId, Pageable pageable);

    // 智能组卷：多条件查询（难度区间、知识点列表）
    @Query("SELECT q FROM Question q WHERE q.type = :type AND q.subject = :subject " +
            "AND q.difficulty BETWEEN :minDiff AND :maxDiff " +
            "AND (:knowledgePointIds IS NULL OR q.knowledgePointId IN :knowledgePointIds)")
    List<Question> findByTypeAndSubjectAndDifficultyBetweenAndKnowledgePointIdIn(
            @Param("type") String type,
            @Param("subject") String subject,
            @Param("minDiff") int minDiff,
            @Param("maxDiff") int maxDiff,
            @Param("knowledgePointIds") List<Long> knowledgePointIds);

    // 简化版本（无知识点）
    @Query("SELECT q FROM Question q WHERE q.type = :type AND q.subject = :subject AND q.difficulty BETWEEN :minDiff AND :maxDiff")
    List<Question> findByTypeAndSubjectAndDifficultyBetween(
            @Param("type") String type,
            @Param("subject") String subject,
            @Param("minDiff") int minDiff,
            @Param("maxDiff") int maxDiff);
}