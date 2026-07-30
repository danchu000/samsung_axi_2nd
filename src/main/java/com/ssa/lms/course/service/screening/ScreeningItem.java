package com.ssa.lms.course.service.screening;

/**
 * 선발 편람의 평가 항목 한 줄 — 상세 패널에서 배점표와 같은 형태로 보여준다.
 *
 * @param name  세부내역 (예: "학력")
 * @param max   배정점수 (예: 10)
 * @param point 획득점수 (예: 9)
 * @param note  평가근거 (예: "4년제 대학 졸업 → 초대졸이상")
 */
public record ScreeningItem(String name, int max, int point, String note) {
}
