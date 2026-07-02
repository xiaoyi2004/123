package org.example.onlineexam.repository;

import org.example.onlineexam.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findByStudentIdAndExamId(Long studentId, Long examId);
    StudentAnswer findByStudentIdAndExamIdAndQuestionId(Long studentId, Long examId, Long questionId);
    void deleteByStudentIdAndExamId(Long studentId, Long examId);
}