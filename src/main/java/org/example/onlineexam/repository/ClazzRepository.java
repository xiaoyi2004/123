package org.example.onlineexam.repository;

import org.example.onlineexam.entity.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {
    List<Clazz> findByCourseId(Long courseId);
}