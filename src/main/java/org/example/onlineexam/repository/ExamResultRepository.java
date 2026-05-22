package org.example.onlineexam.repository;

import org.example.onlineexam.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudentId(Long studentId);
    List<ExamResult> findByGradeStatus(String gradeStatus);
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);
    Optional<ExamResult> findByStudentIdAndExamId(Long studentId, Long examId);
}
