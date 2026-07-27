package com.ssa.lms.support.dto;

/**
 * 학습 지원 요약 카드 값.
 *
 * 화면 대응: admin-support-tutoring.html 상단 카드 6개 (튜터링 탭 / Q&A 탭 각각).
 * 기존에는 하드코딩된 숫자("7", "1.8시간", ...)였다.
 *
 * @param waitingCount    처리 대기 / 미답변 건수
 * @param avgFirstResponse "1.8시간" 형태. 응답 이력이 없으면 "-"
 * @param inProgressCount 진행 중 건수
 * @param recentCount     최근 7일 신규 건수
 * @param topSummary      요약 문자열 (튜터별 처리 현황 / 조회수 TOP 제목)
 * @param topSubDesc      요약 보조 설명 (조회수 98회 등)
 * @param noResponseCount 기준 시간(24h) 넘도록 무응답인 건수
 */
public record SupportStats(
        long waitingCount,
        String avgFirstResponse,
        long inProgressCount,
        long recentCount,
        String topSummary,
        String topSubDesc,
        long noResponseCount
) {

    public static SupportStats empty() {
        return new SupportStats(0, "-", 0, 0, "-", "", 0);
    }
}
