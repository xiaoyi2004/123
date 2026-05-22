package org.example.onlineexam.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.example.onlineexam.entity.Question;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionImportService {

    public List<Question> parseQuestions(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) throw new RuntimeException("文件名为空");
        String content;
        if (fileName.endsWith(".txt")) {
            content = new String(file.getBytes(), "UTF-8");
        } else if (fileName.endsWith(".docx")) {
            content = parseDocx(file.getInputStream());
        } else {
            throw new RuntimeException("仅支持 .txt 或 .docx 文件");
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
        // 按两个以上换行符分割题目块
        String[] blocks = text.split("\\n\\s*\\n");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            Question q = new Question();
            // 智能识别题型
            String type = detectType(block);
            q.setType(type);
            // 提取题干（第一行去除数字序号）
            String[] lines = block.split("\\n");
            String titleLine = lines[0].replaceFirst("^\\d+[.、]\\s*", "").trim();
            q.setTitle(titleLine);

            // 根据题型解析选项、答案
            if ("single".equals(type) || "judge".equals(type)) {
                parseChoice(block, q, "single".equals(type));
            } else if ("fill".equals(type)) {
                parseFill(block, q);
            } else if ("essay".equals(type)) {
                parseEssay(block, q);
            }
            parseAnalysis(block, q);
            list.add(q);
        }
        return list;
    }

    private String detectType(String block) {
        if (block.contains("A.") && block.contains("B.") && (block.contains("C.") || block.contains("D.")))
            return "single";
        if (block.contains("正确") && block.contains("错误")) return "judge";
        if (block.contains("填空") || block.contains("______")) return "fill";
        if (block.contains("简答") || block.contains("论述")) return "essay";
        // 默认单选
        return "single";
    }

    private void parseChoice(String block, Question q, boolean isSingle) {
        // 提取选项 A. xxx B. xxx ...
        Pattern p = Pattern.compile("([A-D])\\.\\s*([^\\n]+)");
        Matcher m = p.matcher(block);
        while (m.find()) {
            String letter = m.group(1);
            String text = m.group(2).trim();
            switch (letter) {
                case "A": q.setOptionA(text); break;
                case "B": q.setOptionB(text); break;
                case "C": q.setOptionC(text); break;
                case "D": q.setOptionD(text); break;
            }
        }
        // 答案通常在最后一行 如 答案：A  或 正确答案：B
        String lastLine = block.substring(block.lastIndexOf("\n") + 1);
        String ans = extractAnswer(lastLine);
        q.setAnswer(ans != null ? ans : "A");
    }

    private void parseFill(String block, Question q) {
        // 答案在最后一行 答案：xxxx
        String lastLine = block.substring(block.lastIndexOf("\n") + 1);
        String ans = extractAnswer(lastLine);
        q.setAnswer(ans != null ? ans : "");
    }

    private void parseEssay(String block, Question q) {
        String lastLine = block.substring(block.lastIndexOf("\n") + 1);
        String ans = extractAnswer(lastLine);
        q.setAnswer(ans != null ? ans : "");
    }

    private void parseAnalysis(String block, Question q) {
        for (String line : block.split("\n")) {
            if (line.contains("解析：")) { q.setAnalysis(line.substring(line.indexOf("解析：") + 3).trim()); return; }
            if (line.contains("答案解析：")) { q.setAnalysis(line.substring(line.indexOf("答案解析：") + 5).trim()); return; }
        }
    }

    private String extractAnswer(String line) {
        if (line.contains("答案：")) {
            return line.split("答案：")[1].trim();
        }
        if (line.contains("正确答案：")) {
            return line.split("正确答案：")[1].trim();
        }
        return null;
    }
}