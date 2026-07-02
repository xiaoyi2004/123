package org.example.onlineexam.repository;

import org.example.onlineexam.entity.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long> {

    // 根据名称查询（用于导入时匹配知识点）
    List<KnowledgePoint> findByName(String name);

    // 根据课程ID查询
    List<KnowledgePoint> findByCourseId(Long courseId);

    // 根据父节点ID查询子节点
    List<KnowledgePoint> findByParentId(Long parentId);

    // 根据名称和课程ID查询（更精确）
    List<KnowledgePoint> findByNameAndCourseId(String name, Long courseId);
}