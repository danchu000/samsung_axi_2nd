package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.Qna;

/**
 * Q&A 검색 조건.
 *
 * 화면 셀렉트는 "전체"를 빈 문자열 또는 "전체" 문자열로 넘긴다.
 * 컨트롤러에서 그대로 받아 여기서 null 로 정규화한다 (null = 조건 미적용).
 *
 * <p><b>keyword 는 제목/작성자명에만 적용된다.</b> 질문 본문(content)은 AES-256
 * 암호문으로 저장돼 LIKE 검색이 불가능하다 — 내역서 개인정보 요건이라 컨버터를 떼지 않는다.</p>
 */
public record QnaSearchCond(
        String keyword,
        String status,
        String category,
        Long courseId,
        Long assigneeId,
        /** "미배정" 필터 */
        String manager
) {

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    public Qna.QnaStatus statusOrNull() {
        if (isAll(status)) {
            return null;
        }
        return switch (status) {
            case "미답변", "대기", "WAITING" -> Qna.QnaStatus.WAITING;
            case "진행중", "IN_PROGRESS" -> Qna.QnaStatus.IN_PROGRESS;
            case "답변완료", "ANSWERED" -> Qna.QnaStatus.ANSWERED;
            case "종료", "CLOSED" -> Qna.QnaStatus.CLOSED;
            default -> throw new IllegalArgumentException("알 수 없는 상태 값: " + status);
        };
    }

    public Qna.QnaCategory categoryOrNull() {
        if (isAll(category)) {
            return null;
        }
        return switch (category) {
            case "수업", "LECTURE" -> Qna.QnaCategory.LECTURE;
            case "진도", "PROGRESS" -> Qna.QnaCategory.PROGRESS;
            case "기타", "ETC" -> Qna.QnaCategory.ETC;
            default -> throw new IllegalArgumentException("알 수 없는 유형 값: " + category);
        };
    }

    public Long courseIdOrNull() {
        return courseId;
    }

    /** 담당자 필터가 "미배정"이면 assignee is null 조건으로 바꾼다. */
    public boolean unassignedOnly() {
        return "미배정".equals(manager);
    }

    public Long assigneeIdOrNull() {
        return unassignedOnly() ? null : assigneeId;
    }

    /**
     * 조회자 본인 범위로 좁힌다 — 강사는 <b>본인이 담당자인 질문만</b> 본다.
     *
     * <p>{@code assigneeId} 가 null 이면(관리자) 조건을 그대로 둔다.
     * 담당자 필터({@code manager})는 함께 비운다: "미배정"이 들어오면
     * {@link #assigneeIdOrNull()} 이 assignee 조건을 버려 스코프가 통째로 풀리기 때문이다.</p>
     */
    public QnaSearchCond scopedTo(Long assigneeId) {
        return assigneeId == null ? this
                : new QnaSearchCond(keyword, status, category, courseId, assigneeId, null);
    }
}
