package com.ssa.lms.course.service.screening;

/**
 * 제안용 시범 화면에 표시하는 예시 신청자 1건 — DB 에 저장되지 않는 화면 표시 전용 데이터다.
 *
 * @param name            신청자명
 * @param loginId         아이디
 * @param courseCode      과정코드
 * @param courseName      과정명
 * @param appliedDaysAgo  신청일시 (오늘로부터 며칠 전) — 화면이 항상 최근 신청처럼 보이게 상대일로 둔다
 * @param input           평가 입력값
 */
public record MockApplicant(String name, String loginId, String courseCode, String courseName,
                            int appliedDaysAgo, ScreeningInput input) {
}
