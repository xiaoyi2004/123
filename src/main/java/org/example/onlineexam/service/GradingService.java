package org.example.onlineexam.service;

import org.example.onlineexam.entity.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradingService {

    public int gradeObjective(List<Question> questions, Map<Long, Integer> scoreMap, Map<String, String> answers) {
        int total = 0;
        for (Question q : questions) {
            String type = q.getType();
            if (type == null) continue;
            String studentAnswer = answers.getOrDefault(String.valueOf(q.getId()), "").trim();
            String correctAnswer = q.getAnswer() == null ? "" : q.getAnswer().trim();
            int fullScore = scoreMap.getOrDefault(q.getId(), 0);
            int got = 0;
            switch (type) {
                case "single":
                case "judge":
                    if (studentAnswer.equalsIgnoreCase(correctAnswer)) got = fullScore;
                    break;
                case "fill":
                    // 简单完全匹配，可扩展忽略大小写等
                    if (studentAnswer.equalsIgnoreCase(correctAnswer)) got = fullScore;
                    break;
                case "multiple_choice":
                    got = gradeMultipleChoice(q, studentAnswer, fullScore);
                    break;
                default:
                    // 主观题不在此评分
                    continue;
            }
            total += got;
        }
        return total;
    }

    // 多选题评分
    private int gradeMultipleChoice(Question q, String studentAnswer, int fullScore) {
        if (studentAnswer == null || studentAnswer.isBlank()) return 0;
        String correct = q.getAnswer() == null ? "" : q.getAnswer().trim();
        Set<String> correctSet = Arrays.stream(correct.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        Set<String> studentSet = Arrays.stream(studentAnswer.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        if (studentSet.equals(correctSet)) return fullScore;
        // 根据评分规则
        String rule = q.getScoringRule() != null ? q.getScoringRule() : "exact";
        if ("partial".equals(rule) && correctSet.containsAll(studentSet) && !studentSet.isEmpty()) {
            // 少选得一半分
            return fullScore / 2;
        }
        if ("exact_wrong".equals(rule)) {
            // 错选不得分，少选得一半
            if (correctSet.containsAll(studentSet) && !studentSet.isEmpty()) return fullScore / 2;
            // 有错误选项不得分
            return 0;
        }
        return 0;
    }

    public boolean hasSubjective(List<Question> questions) {
        return questions.stream().anyMatch(q -> {
            String type = q.getType();
            return "essay".equals(type) || "analysis".equals(type) || "programming".equals(type);
        });
    }
}