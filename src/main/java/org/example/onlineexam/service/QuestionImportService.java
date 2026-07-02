package org.example.onlineexam.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.example.onlineexam.entity.KnowledgePoint;
import org.example.onlineexam.entity.Question;
import org.example.onlineexam.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionImportService {

    private final KnowledgePointRepository knowledgePointRepository;

    public QuestionImportService(KnowledgePointRepository knowledgePointRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
    }

    public List<Question> parseQuestions(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) throw new RuntimeException("文件名为空");
        String content;
        if (fileName.endsWith(".txt")) {
            content = new String(file.getBytes(), "UTF-8");
        } else if (fileName.endsWith(".docx")) {
            content = parseDocx(file.getInputStream());
        } else {
            throw new RuntimeException("仅支持 .txt 或 .docx 文件（暂不支持 Excel）");
        }
        return parseTextToQuestions(content);
    }

    private String parseDocx(InputStream is) throws Exception {
        XWPFDocument doc = new XWPFDocument(is);
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            sb.append(p.getText()).append("\n");
        }
        doc.close();
        return sb.toString();
    }

    private List<Question> parseTextToQuestions(String text) {
        List<Question> list = new ArrayList<>();
        String[] blocks = text.split("\\n\\s*\\n");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            Question q = new Question();

            // 1. 提取所有标记字段
            Map<String, String> fields = extractFields(block);

            // 2. 题型
            String type = fields.getOrDefault("题型", fields.getOrDefault("题目类型", ""));
            q.setType(normalizeType(type));

            // 3. 科目
            q.setSubject(fields.get("科目"));

            // 4. 知识点
            String knowledge = fields.get("知识点");
            if (knowledge != null && !knowledge.isEmpty()) {
                List<KnowledgePoint> kps = knowledgePointRepository.findByName(knowledge);
                if (!kps.isEmpty()) {
                    q.setKnowledgePointId(kps.get(0).getId());
                    q.setKnowledgePoint(kps.get(0).getName());
                } else {
                    q.setKnowledgePoint(knowledge);
                }
            }

            // 5. 难度
            String diffStr = fields.get("难度");
            if (diffStr != null) {
                try {
                    q.setDifficulty(Math.min(5, Math.max(1, Integer.parseInt(diffStr.trim()))));
                } catch (NumberFormatException ignored) {}
            }

            // 6. 答案
            q.setAnswer(fields.getOrDefault("答案", fields.getOrDefault("参考答案", "")));

            // 7. 解析
            q.setAnalysis(fields.getOrDefault("解析", fields.getOrDefault("答案解析", "")));

            // 8. ★★★ 提取题干：优先使用“题干：”标签，若无则取删除元数据行后的第一行 ★★★
            String title = fields.get("题干");  // 如果有“题干：”标签
            if (title == null) {
                // 无标签，尝试删除元数据行后取第一行
                String cleanBlock = removeMarkupLines(block);
                String[] lines = cleanBlock.split("\\n");
                if (lines.length > 0) {
                    title = lines[0].replaceFirst("^\\d+[.、]\\s*", "").trim();
                }
            }
            q.setTitle(title != null ? title : "");

            // 9. 提取选项（用于选择题）
            parseOptions(block, q);  // 注意使用原block，因为选项可能在标记行中

            // 10. 如果答案仍未获取，尝试从最后一行提取
            if (q.getAnswer() == null || q.getAnswer().isEmpty()) {
                String lastLine = block.substring(block.lastIndexOf("\n") + 1);
                String ans = extractAnswerFromLine(lastLine);
                if (ans != null) q.setAnswer(ans);
            }

            list.add(q);
        }
        return list;
    }

    // 提取所有标记字段
    private Map<String, String> extractFields(String block) {
        Map<String, String> map = new HashMap<>();
        Pattern p = Pattern.compile("^(?i)(题干|题型|题目类型|科目|课程|知识点|知识|难度|难易|答案|参考答案|解析|答案解析)[:：]\\s*(.*)$", Pattern.MULTILINE);
        Matcher m = p.matcher(block);
        while (m.find()) {
            String key = m.group(1).trim();
            String value = m.group(2).trim();
            // 统一键名
            if (key.contains("题型") || key.contains("题目类型")) key = "题型";
            else if (key.contains("科目") || key.contains("课程")) key = "科目";
            else if (key.contains("知识点") || key.contains("知识")) key = "知识点";
            else if (key.contains("难度") || key.contains("难易")) key = "难度";
            else if (key.contains("答案") || key.contains("参考答案")) key = "答案";
            else if (key.contains("解析") || key.contains("答案解析")) key = "解析";
            // 题干直接保留
            map.put(key, value);
        }
        return map;
    }

    // 删除所有标记行（用于无标签时取题干）
    private String removeMarkupLines(String block) {
        String[] lines = block.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.matches("(?i)^(题干|题型|题目类型|科目|课程|知识点|知识|难度|难易|答案|参考答案|解析|答案解析)[:：].*")) {
                continue;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // 解析选项（从原block中提取A-E行）
    private void parseOptions(String block, Question q) {
        Pattern p = Pattern.compile("([A-E])\\.\\s*([^\\n]+)");
        Matcher m = p.matcher(block);
        while (m.find()) {
            String letter = m.group(1);
            String text = m.group(2).trim();
            switch (letter) {
                case "A": q.setOptionA(text); break;
                case "B": q.setOptionB(text); break;
                case "C": q.setOptionC(text); break;
                case "D": q.setOptionD(text); break;
                case "E": q.setOptionE(text); break;
            }
        }
    }

    private String extractAnswerFromLine(String line) {
        if (line.contains("答案：")) return line.split("答案：")[1].trim();
        if (line.contains("正确答案：")) return line.split("正确答案：")[1].trim();
        if (line.contains("参考答案：")) return line.split("参考答案：")[1].trim();
        return null;
    }

    private String normalizeType(String type) {
        if (type == null) return "essay";
        type = type.trim().toLowerCase();
        if (type.contains("单选") || type.contains("single")) return "single";
        if (type.contains("多选") || type.contains("multiple")) return "multiple_choice";
        if (type.contains("判断") || type.contains("judge")) return "judge";
        if (type.contains("填空") || type.contains("fill")) return "fill";
        if (type.contains("简答") || type.contains("essay")) return "essay";
        if (type.contains("分析") || type.contains("analysis")) return "analysis";
        if (type.contains("编程") || type.contains("programming")) return "programming";
        return "essay";
    }
}