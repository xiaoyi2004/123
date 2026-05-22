package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByStatusAndStartTimeBeforeAndEndTimeAfter(String status, LocalDateTime now1, LocalDateTime now2);
    // 在 ExamRepository.java 中添加
    List<Exam> findByStatus(String status);
    boolean existsByPaperId(Long paperId);
}
