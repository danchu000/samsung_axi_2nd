package com.ssa.lms.course.service.screening;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 훈련생 선발 편람(서류전형 40점 + 면접전형 60점)에 따라 신청자 적합도를 산출한다.
 *
 * <p>배점은 편람 그대로다.</p>
 * <ul>
 *   <li>서류 — 학력 10 / 전공 10 / 경력 및 자격증 10 / 수강횟수 10</li>
 *   <li>면접 — 훈련의욕 15 / 수강능력 15 / 적성 15 / 취업의지 15</li>
 * </ul>
 *
 * <p>산식 자체는 실제 선발에 그대로 쓸 수 있게 구현했다. 현재 화면이 쓰는 입력값만
 * 제안용 예시 데이터({@link MockApplicantCatalog})이며, 수강신청서·면접 결과 입력이
 * 붙으면 이 클래스는 수정 없이 재사용된다.</p>
 */
@Service
public class SuitabilityEvaluator {

    /** 수강횟수 배점 — 편람: 1회차(10) 2회차(9) 3회차(8). 4회차 이상은 7점으로 이어 내린다. */
    static int courseRoundPoint(int round) {
        return switch (Math.max(round, 1)) {
            case 1 -> 10;
            case 2 -> 9;
            case 3 -> 8;
            default -> 7;
        };
    }

    public SuitabilityResult evaluate(ScreeningInput in) {
        List<ScreeningItem> documents = List.of(
                new ScreeningItem("학력", 10, in.education().getPoint(),
                        in.education().getLabel() + " → " + in.education().getTier()),
                new ScreeningItem("전공", 10, in.majorRelation().getPoint(),
                        in.majorName() + " → " + in.majorRelation().getLabel()),
                new ScreeningItem("경력 및 자격증", 10, in.careerRelation().getPoint(),
                        in.careerNote() + " → " + in.careerRelation().getLabel()),
                new ScreeningItem("수강횟수", 10, courseRoundPoint(in.courseRound()),
                        in.courseRound() + "회차"));

        InterviewScores iv = in.interview();
        List<ScreeningItem> interviews = List.of(
                interviewItem("훈련의욕 및 직종에 대한 이해도", iv.motivation(), "교육과정에 대한 기초지식 파악"),
                interviewItem("훈련 수강능력 및 수료 가능성", iv.capability(), "교육과정에 대한 수강목적 파악"),
                interviewItem("직종에 대한 적성 및 선호도", iv.aptitude(), "교육과정에 대한 적성 파악"),
                interviewItem("취업의지 및 취업 시급성", iv.employment(), "취업에 대한 의지 및 진로 파악"));

        int documentScore = sum(documents);
        int interviewScore = sum(interviews);
        int total = documentScore + interviewScore;
        int percent = Math.round(total * 100f / SuitabilityResult.TOTAL_MAX);

        return new SuitabilityResult(documents, interviews, documentScore, interviewScore,
                total, percent, SuitabilityGrade.of(percent));
    }

    private ScreeningItem interviewItem(String name, int point, String method) {
        return new ScreeningItem(name, InterviewScores.ITEM_MAX, point,
                InterviewScores.tierOf(point) + "(" + point + "점) · " + method);
    }

    private int sum(List<ScreeningItem> items) {
        return items.stream().mapToInt(ScreeningItem::point).sum();
    }
}
