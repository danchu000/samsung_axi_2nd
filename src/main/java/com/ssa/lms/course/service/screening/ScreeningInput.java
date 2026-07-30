package com.ssa.lms.course.service.screening;

/**
 * 적합도 산출에 필요한 신청자 평가 입력값.
 *
 * <p>실제 운영에서는 수강신청서(전공·경력·자격증)와 면접 결과 입력에서 채워질 값이다.
 * 현재는 제안용 시범 화면이라 {@link MockApplicantCatalog} 가 예시 값을 제공한다.</p>
 *
 * @param education      최종학력
 * @param majorName      전공명 (화면 표기용)
 * @param majorRelation  전공과 훈련직종의 관계
 * @param careerNote     경력·자격증 요약 (화면 표기용)
 * @param careerRelation 경력·자격증과 훈련직종의 관계
 * @param courseRound    수강횟수 (1 = 1회차)
 * @param interview      면접전형 점수
 */
public record ScreeningInput(EducationLevel education,
                             String majorName, RelationLevel majorRelation,
                             String careerNote, RelationLevel careerRelation,
                             int courseRound,
                             InterviewScores interview) {
}
