package com.ssa.lms.content.web;

/** 훈련생 학습 화면의 과정 선택 옵션 (id + 과정명) — lazy 프록시 노출을 피하기 위한 경량 뷰. */
public record CourseOption(Long id, String courseName) {
}
