package org.example.onlineexam.service;

import org.example.onlineexam.entity.PaperQuestion;
import org.example.onlineexam.entity.Question;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class GradingService {
    public int gradeObjective(List<Question> questions, Map<Long, Integer> scoreMap, Map<String, String> answers) {
        int total = 0;
        for (Question q : questions) {
            if ("essay".equals(q.getType())) continue;
            String studentAnswer = answers.getOrDefault(String.valueOf(q.getId()), "").trim();
            String correctAnswer = q.getAnswer() == null ? "" : q.getAnswer().trim();
            if (studentAnswer.equalsIgnoreCase(correctAnswer)) {
                total += scoreMap.getOrDefault(q.getId(), 0);
            }
        }
        return total;
    }

    public boolean hasSubjective(List<Question> questions) {
        return questions.stream().anyMatch(q -> "essay".equals(q.getType()));
    }
}
