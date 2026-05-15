package org.example.onlineexam.service;

import org.example.onlineexam.entity.Question;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class GradingService {

    public int grade(List<Question> questions, Map<String, String> answers) {
        int total = 0;
        for (Question q : questions) {
            String studentAnswer = answers.getOrDefault(String.valueOf(q.getId()), "").trim();
            String correctAnswer = q.getAnswer() == null ? "" : q.getAnswer().trim();

            if ("essay".equals(q.getType())) {
                // 简答题：学生答案包含任意关键词即得分
                if (containsAnyKeyword(studentAnswer, correctAnswer)) {
                    total += q.getScore();
                }
            } else {
                // 原有判分（精确匹配）
                if (studentAnswer.equalsIgnoreCase(correctAnswer)) {
                    total += q.getScore();
                }
            }
        }
        return total;
    }

    private boolean containsAnyKeyword(String studentAnswer, String keywordStr) {
        if (studentAnswer.isEmpty()) return false;
        String[] keywords = keywordStr.split("[，,]+");  // 支持中英文逗号
        for (String kw : keywords) {
            if (studentAnswer.contains(kw.trim())) {
                return true;
            }
        }
        return false;
    }
}