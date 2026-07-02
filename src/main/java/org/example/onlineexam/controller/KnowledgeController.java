package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.KnowledgePoint;
import org.example.onlineexam.repository.KnowledgePointRepository;
import org.example.onlineexam.service.KnowledgeTreeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/knowledge")
public class KnowledgeController {
    private final KnowledgePointRepository repository;
    private final KnowledgeTreeService treeService;

    public KnowledgeController(KnowledgePointRepository repository, KnowledgeTreeService treeService) {
        this.repository = repository;
        this.treeService = treeService;
    }

    @GetMapping("/tree/{courseId}")
    @ResponseBody
    public List<KnowledgePoint> getTree(@PathVariable Long courseId) {
        return treeService.getTreeByCourse(courseId);
    }

    // 管理页面
    @GetMapping("/manage")
    public String manage(Model model) {
        List<KnowledgePoint> all = repository.findAll();
        model.addAttribute("allNodes", all);
        return "knowledge_manage";
    }

    @PostMapping("/save")
    public String save(KnowledgePoint node) {
        repository.save(node);
        return "redirect:/knowledge/manage";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/knowledge/manage";
    }
}