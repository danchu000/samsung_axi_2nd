package com.ssa.lms.content.web;

/**
 * 콘텐츠 등록/수정 폼의 차시 선택 옵션. 과정 선택에 따라 클라이언트에서 필터링할 수 있도록
 * courseId 를 함께 담는다.
 */
public record SessionOption(Long id, Long courseId, String label) {
}
