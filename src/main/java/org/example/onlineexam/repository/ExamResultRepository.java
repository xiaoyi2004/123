package org.example.onlineexam.repository;

import org.example.onlineexam.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    // 根据学生ID查询所有考试记录
    List<ExamResult> findByStudentId(Long studentId);

    // 根据考试ID查询所有成绩记录（用于教师查看、统计、导出等）
    List<ExamResult> findByExamId(Long examId);

    // 根据批阅状态查询（待批阅/已批阅）
    List<ExamResult> findByGradeStatus(String gradeStatus);

    // 检查学生是否已参加某考试（用于防止重复提交，除非允许重考）
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);

    // 查询某学生某次考试的记录（用于更新或检查）
    Optional<ExamResult> findByStudentIdAndExamId(Long studentId, Long examId);
}