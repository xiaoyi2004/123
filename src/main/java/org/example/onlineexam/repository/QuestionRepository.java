package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
