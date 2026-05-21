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
            String studentAnswer = answers.get(String.valueOf(q.getId()));
            if (studentAnswer == null) studentAnswer = "";
            studentAnswer = studentAnswer.trim();
            String correctAnswer = q.getAnswer() == null ? "" : q.getAnswer().trim();

            if ("essay".equals(q.getType())) {
                if (containsAnyKeyword(studentAnswer, correctAnswer)) {
                    total += q.getScore();
                }
            } else {
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