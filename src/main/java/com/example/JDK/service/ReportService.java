package com.example.JDK.service;

import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.Report;
import com.example.JDK.entity.User;
import com.example.JDK.repository.ReportRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public void reportPost(User reporter, Post post, String reason) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setPost(post);
        report.setReason(reason);
        reportRepository.save(report);
    }

    public void reportComment(User reporter, Comment comment, String reason) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setComment(comment);
        report.setPost(comment.getPost()); // 어떤 게시글의 댓글인지 연결
        report.setReason(reason);
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<Report> findAll() {
        return reportRepository.findAllForAdmin(); // ✅ fetch join 사용
    }

    @Transactional
    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
    }


}
