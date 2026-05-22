package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findBySubject(String subject);
}
