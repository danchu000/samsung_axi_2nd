package com.ssa.lms.course.service.screening;

/**
 * 면접전형 편람 4개 항목의 평가 점수 (각 15점 만점).
 *
 * <p>편람 평가방법: 상(15~13) / 중(12~10) / 하(9 이하).</p>
 *
 * @param motivation 훈련의욕 및 직종에 대한 이해도
 * @param capability 훈련 수강능력 및 수료 가능성
 * @param aptitude   직종에 대한 적성 및 선호도
 * @param employment 취업의지 및 취업 시급성
 */
public record InterviewScores(int motivation, int capability, int aptitude, int employment) {

    public static final int ITEM_MAX = 15;

    /** 점수 → 편람 등급 표기 (상/중/하). */
    public static String tierOf(int point) {
        if (point >= 13) {
            return "상";
        }
        if (point >= 10) {
            return "중";
        }
        return "하";
    }
}
