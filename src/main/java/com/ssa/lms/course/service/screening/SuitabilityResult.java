package com.ssa.lms.course.service.screening;

import java.util.List;

/**
 * 적합도 산출 결과 — 서류전형(40) + 면접전형(60) = 100점 만점.
 *
 * @param documentItems  서류전형 항목별 점수 (학력/전공/경력 및 자격증/수강횟수)
 * @param interviewItems 면접전형 항목별 점수 (4개 항목)
 * @param documentScore  서류전형 획득점수
 * @param interviewScore 면접전형 획득점수
 * @param totalScore     총점
 * @param percent        적합도(%) — 총점 / 총 배점
 * @param grade          4단계 등급
 */
public record SuitabilityResult(List<ScreeningItem> documentItems, List<ScreeningItem> interviewItems,
                                int documentScore, int interviewScore,
                                int totalScore, int percent, SuitabilityGrade grade) {

    public static final int DOCUMENT_MAX = 40;
    public static final int INTERVIEW_MAX = 60;
    public static final int TOTAL_MAX = DOCUMENT_MAX + INTERVIEW_MAX;

    public int documentMax() {
        return DOCUMENT_MAX;
    }

    public int interviewMax() {
        return INTERVIEW_MAX;
    }

    public int totalMax() {
        return TOTAL_MAX;
    }

    /** 화면 배지 클래스 (grade-very / grade-ok / grade-warn / grade-bad). */
    public String badgeClass() {
        return "grade-" + grade.getCssSuffix();
    }

    public String gradeLabel() {
        return grade.getLabel();
    }
}
