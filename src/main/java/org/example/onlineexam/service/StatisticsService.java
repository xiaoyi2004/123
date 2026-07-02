package org.example.onlineexam.service;

import org.example.onlineexam.entity.ExamResult;
import org.example.onlineexam.repository.ExamResultRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatisticsService {

    private final ExamResultRepository examResultRepository;

    public StatisticsService(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    /**
     * 计算某场考试的统计信息
     */
    public Map<String, Object> getExamStatistics(Long examId) {
        List<ExamResult> results = examResultRepository.findByExamId(examId);
        Map<String, Object> stats = new HashMap<>();
        if (results.isEmpty()) {
            stats.put("average", 0);
            stats.put("max", 0);
            stats.put("min", 0);
            stats.put("passRate", 0);
            stats.put("segments", new LinkedHashMap<>());
            return stats;
        }

        // 使用 IntSummaryStatistics 收集统计信息
        IntSummaryStatistics summary = results.stream()
                .mapToInt(r -> r.getScore() == null ? 0 : r.getScore())
                .summaryStatistics();

        long passCount = results.stream().filter(r -> r.getScore() != null && r.getScore() >= 60).count();
        double passRate = (double) passCount / results.size();

        // 分数段
        Map<String, Integer> segments = new LinkedHashMap<>();
        segments.put("0-59", 0);
        segments.put("60-69", 0);
        segments.put("70-79", 0);
        segments.put("80-89", 0);
        segments.put("90-100", 0);
        for (ExamResult r : results) {
            int score = r.getScore() == null ? 0 : r.getScore();
            if (score < 60) segments.put("0-59", segments.get("0-59") + 1);
            else if (score < 70) segments.put("60-69", segments.get("60-69") + 1);
            else if (score < 80) segments.put("70-79", segments.get("70-79") + 1);
            else if (score < 90) segments.put("80-89", segments.get("80-89") + 1);
            else segments.put("90-100", segments.get("90-100") + 1);
        }

        stats.put("average", summary.getAverage());
        stats.put("max", summary.getMax());
        stats.put("min", summary.getMin());
        stats.put("passRate", passRate);
        stats.put("segments", segments);
        return stats;
    }

    /**
     * 计算每道题的正确率（仅适用于客观题）
     */
    public Map<Long, Double> getQuestionCorrectRate(Long examId) {
        // 需要从StudentAnswer表中统计，此处略，实际可扩展
        return new HashMap<>();
    }

    /**
     * 按班级统计
     */
    public Map<String, Object> getClassStatistics(Long examId, List<String> classNames) {
        // 略，实际可扩展
        return new HashMap<>();
    }
}