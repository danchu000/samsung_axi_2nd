package com.ssa.lms.content.entity;

/**
 * 콘텐츠 유형 — Phase 2(A)는 학습 콘텐츠인 <b>VOD(동영상)/문서</b>만 소유한다.
 * (시험/문제/과제 유형은 B 도메인(exam/assignment)이 별도 엔티티로 관리 —
 * 콘텐츠와의 연결은 Exam 이 content_id(nullable) 를 갖는 방향, a-requests.md P2.)
 */
public enum ContentType {

    VIDEO("동영상"),
    DOCUMENT("문서");

    private final String label;

    ContentType(String label) {
        this.label = label;
    }

    /** 화면 표기용 한글 라벨 (프론트 typeLabel 과 일치) */
    public String getLabel() {
        return label;
    }
}
