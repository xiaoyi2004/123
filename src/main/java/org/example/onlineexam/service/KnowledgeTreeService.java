package org.example.onlineexam.service;

import org.example.onlineexam.entity.KnowledgePoint;
import org.example.onlineexam.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeTreeService {
    private final KnowledgePointRepository repository;

    public KnowledgeTreeService(KnowledgePointRepository repository) {
        this.repository = repository;
    }

    // 获取某课程的知识树（递归构建完整树）
    public List<KnowledgePoint> getTreeByCourse(Long courseId) {
        List<KnowledgePoint> all;
        if (courseId == null) {
            all = repository.findAll();
        } else {
            all = repository.findByCourseId(courseId);
        }

        // 过滤掉 null 或 id 为 null 的无效节点
        all = all.stream()
                .filter(p -> p != null && p.getId() != null)
                .collect(Collectors.toList());

        // 构建父子映射
        Map<Long, List<KnowledgePoint>> parentMap = all.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() > 0)
                .collect(Collectors.groupingBy(KnowledgePoint::getParentId));

        // 根节点（parentId 为 null 或 0）
        List<KnowledgePoint> roots = all.stream()
                .filter(p -> p.getParentId() == null || p.getParentId() == 0)
                .collect(Collectors.toList());

        // 递归填充 children
        for (KnowledgePoint root : roots) {
            fillChildren(root, parentMap);
        }

        // 返回非空根节点列表（再次确保）
        return roots.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void fillChildren(KnowledgePoint node, Map<Long, List<KnowledgePoint>> parentMap) {
        if (node == null) return;
        List<KnowledgePoint> children = parentMap.get(node.getId());
        if (children != null) {
            // 过滤掉 children 中的 null
            children = children.stream().filter(Objects::nonNull).collect(Collectors.toList());
            node.setChildren(children);
            for (KnowledgePoint child : children) {
                fillChildren(child, parentMap);
            }
        } else {
            node.setChildren(new ArrayList<>());
        }
    }

    // 获取所有子节点ID（递归）
    public Set<Long> getDescendantIds(Long nodeId) {
        Set<Long> ids = new HashSet<>();
        ids.add(nodeId);
        List<KnowledgePoint> children = repository.findByParentId(nodeId);
        for (KnowledgePoint child : children) {
            ids.addAll(getDescendantIds(child.getId()));
        }
        return ids;
    }
}