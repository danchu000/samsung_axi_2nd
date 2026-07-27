package com.ssa.lms.assignment.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 훈련생 과제 제출 폼 (trainee/assignments.html 제출 모달).
 *
 * 유형별 필수값 검증(파일/링크/텍스트)은 서비스가 한다 — 화면 JS 검증만 믿으면 안 된다.
 */
@Getter
@Setter
public class SubmissionForm {

    private Long courseAssignmentId;

    private String contentText;

    private String linkUrl;

    private List<MultipartFile> files = new ArrayList<>();
}
