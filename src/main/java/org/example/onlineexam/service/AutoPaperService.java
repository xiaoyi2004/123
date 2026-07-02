package org.example.onlineexam.service;

import org.example.onlineexam.entity.Paper;
import org.example.onlineexam.entity.PaperQuestion;
import org.example.onlineexam.entity.Question;
import org.example.onlineexam.repository.PaperQuestionRepository;
import org.example.onlineexam.repository.PaperRepository;
import org.example.onlineexam.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AutoPaperService {

    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;

    public AutoPaperService(QuestionRepository questionRepository,
                            PaperRepository paperRepository,
                            PaperQuestionRepository paperQuestionRepository) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
    }

    public Paper generatePaper(Map<String, Object> config) throws Exception {
        String subject = (String) config.get("subject");
        String paperName = (String) config.get("paperName");
        int totalScore = (int) config.get("totalScore");

        // 解析题型分布：键为题型，值为题目数量
        Map<String, Integer> typeCountMap = new HashMap<>();
        Object typeObj = config.get("typeCountMap");
        if (typeObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) typeObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = entry.getKey().toString();
                Integer value = Integer.valueOf(entry.getValue().toString());
                typeCountMap.put(key, value);
            }
        }

        // 解析难度分布：键为难度级别，值为题目数量
        Map<Integer, Integer> difficultyMap = new HashMap<>();
        Object diffObj = config.get("difficultyMap");
        if (diffObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) diffObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Integer key = Integer.valueOf(entry.getKey().toString());
                Integer value = Integer.valueOf(entry.getValue().toString());
                difficultyMap.put(key, value);
            }
        }

        // 知识点ID列表
        List<Long> knowledgePointIds = new ArrayList<>();
        Object kpObj = config.get("knowledgePointIds");
        if (kpObj instanceof List) {
            List<?> rawList = (List<?>) kpObj;
            for (Object item : rawList) {
                knowledgePointIds.add(Long.valueOf(item.toString()));
            }
        }

        // 根据条件从题库筛选题目
        List<Question> pool = new ArrayList<>();
        for (Map.Entry<String, Integer> typeEntry : typeCountMap.entrySet()) {
            String type = typeEntry.getKey();
            int count = typeEntry.getValue();
            // 按难度分布选取题目
            for (Map.Entry<Integer, Integer> diffEntry : difficultyMap.entrySet()) {
                int diff = diffEntry.getKey();
                int diffCount = diffEntry.getValue();
                List<Question> candidates;
                if (knowledgePointIds != null && !knowledgePointIds.isEmpty()) {
                    candidates = questionRepository.findByTypeAndSubjectAndDifficultyBetweenAndKnowledgePointIdIn(
                            type, subject, diff, diff, knowledgePointIds);
                } else {
                    candidates = questionRepository.findByTypeAndSubjectAndDifficultyBetween(
                            type, subject, diff, diff);
                }
                Collections.shuffle(candidates);
                int take = Math.min(diffCount, candidates.size());
                pool.addAll(candidates.subList(0, take));
            }
        }

        if (pool.isEmpty()) {
            throw new Exception("题库中无符合条件的题目，请调整配置");
        }

        // 创建试卷
        Paper paper = new Paper();
        paper.setName(paperName);
        paper.setSubject(subject);
        paper.setTotalScore(totalScore);
        paperRepository.save(paper);

        // 将选中的题目加入试卷
        int order = 1;
        int scorePerQuestion = totalScore / pool.size();
        for (Question q : pool) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(q.getId());
            pq.setScore(scorePerQuestion);
            pq.setSortOrder(order++);
            paperQuestionRepository.save(pq);
        }
        return paper;
    }
}