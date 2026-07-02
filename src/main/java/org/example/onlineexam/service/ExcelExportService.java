package org.example.onlineexam.service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.onlineexam.entity.Exam;
import org.example.onlineexam.entity.ExamResult;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.ExamResultRepository;
import org.example.onlineexam.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private final ExamResultRepository examResultRepository;
    private final UserRepository userRepository;

    public ExcelExportService(ExamResultRepository examResultRepository, UserRepository userRepository) {
        this.examResultRepository = examResultRepository;
        this.userRepository = userRepository;
    }

    public void exportExamResults(Long examId, Exam exam, HttpServletResponse response) throws IOException {
        List<ExamResult> results = examResultRepository.findByExamId(examId);
        Map<Long, User> studentMap = userRepository.findByRole("student")
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("成绩");
        Row header = sheet.createRow(0);
        String[] columns = {"学号", "姓名", "班级", "客观分", "主观分", "总分", "是否及格", "提交时间"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (ExamResult r : results) {
            User s = studentMap.get(r.getStudentId());
            if (s == null) continue;
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.getStudentNo() == null ? "" : s.getStudentNo());
            row.createCell(1).setCellValue(s.getRealName());
            row.createCell(2).setCellValue(s.getClassName());
            row.createCell(3).setCellValue(r.getObjectiveScore() == null ? 0 : r.getObjectiveScore());
            row.createCell(4).setCellValue(r.getSubjectiveScore() == null ? 0 : r.getSubjectiveScore());
            row.createCell(5).setCellValue(r.getScore() == null ? 0 : r.getScore());
            row.createCell(6).setCellValue(r.getScore() != null && r.getScore() >= 60 ? "及格" : "不及格");
            row.createCell(7).setCellValue(r.getSubmitTime() == null ? "" : r.getSubmitTime().toString());
        }

        // 自动列宽
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exam_results.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}