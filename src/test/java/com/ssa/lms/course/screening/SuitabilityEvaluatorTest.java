package com.ssa.lms.course.screening;

import com.ssa.lms.course.service.screening.EducationLevel;
import com.ssa.lms.course.service.screening.InterviewScores;
import com.ssa.lms.course.service.screening.MockApplicantCatalog;
import com.ssa.lms.course.service.screening.RelationLevel;
import com.ssa.lms.course.service.screening.ScreeningInput;
import com.ssa.lms.course.service.screening.SuitabilityEvaluator;
import com.ssa.lms.course.service.screening.SuitabilityGrade;
import com.ssa.lms.course.service.screening.SuitabilityResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 훈련생 선발 편람 배점(서류 40 + 면접 60)에 따른 적합도 산출 검증. */
class SuitabilityEvaluatorTest {

    private final SuitabilityEvaluator evaluator = new SuitabilityEvaluator();

    private ScreeningInput input(EducationLevel edu, RelationLevel major, RelationLevel career,
                                 int round, InterviewScores interview) {
        return new ScreeningInput(edu, "전공", major, "경력", career, round, interview);
    }

    @Test
    void 만점_신청자는_100점_아주적합() {
        SuitabilityResult r = evaluator.evaluate(input(EducationLevel.GRADUATE,
                RelationLevel.SAME, RelationLevel.SAME, 1, new InterviewScores(15, 15, 15, 15)));

        assertThat(r.documentScore()).isEqualTo(40);
        assertThat(r.interviewScore()).isEqualTo(60);
        assertThat(r.totalScore()).isEqualTo(100);
        assertThat(r.percent()).isEqualTo(100);
        assertThat(r.grade()).isEqualTo(SuitabilityGrade.VERY_SUITABLE);
    }

    @Test
    void 서류_항목별_배점이_편람과_같다() {
        SuitabilityResult r = evaluator.evaluate(input(EducationLevel.UNIVERSITY,
                RelationLevel.RELATED, RelationLevel.DIFFERENT, 2, new InterviewScores(13, 12, 11, 10)));

        // 학력 9(초대졸이상) + 전공 9(관련) + 경력 8(다른) + 수강횟수 9(2회차) = 35
        assertThat(r.documentScore()).isEqualTo(35);
        assertThat(r.documentItems()).extracting("name")
                .containsExactly("학력", "전공", "경력 및 자격증", "수강횟수");
        assertThat(r.documentItems()).extracting("max").containsOnly(10);
        assertThat(r.interviewItems()).extracting("max").containsOnly(15);
        assertThat(r.interviewScore()).isEqualTo(46);
        assertThat(r.totalScore()).isEqualTo(81);
    }

    @Test
    void 면접_점수는_상중하로_표기된다() {
        SuitabilityResult r = evaluator.evaluate(input(EducationLevel.HIGH_SCHOOL,
                RelationLevel.DIFFERENT, RelationLevel.DIFFERENT, 1, new InterviewScores(13, 10, 9, 15)));

        assertThat(r.interviewItems().get(0).note()).startsWith("상");
        assertThat(r.interviewItems().get(1).note()).startsWith("중");
        assertThat(r.interviewItems().get(2).note()).startsWith("하");
        assertThat(r.interviewItems().get(3).note()).startsWith("상");
    }

    @Test
    void 적합도_구간이_4단계로_나뉜다() {
        assertThat(SuitabilityGrade.of(100)).isEqualTo(SuitabilityGrade.VERY_SUITABLE);
        assertThat(SuitabilityGrade.of(90)).isEqualTo(SuitabilityGrade.VERY_SUITABLE);
        assertThat(SuitabilityGrade.of(89)).isEqualTo(SuitabilityGrade.SUITABLE);
        assertThat(SuitabilityGrade.of(75)).isEqualTo(SuitabilityGrade.SUITABLE);
        assertThat(SuitabilityGrade.of(74)).isEqualTo(SuitabilityGrade.UNSUITABLE);
        assertThat(SuitabilityGrade.of(60)).isEqualTo(SuitabilityGrade.UNSUITABLE);
        assertThat(SuitabilityGrade.of(59)).isEqualTo(SuitabilityGrade.NOT_SUITABLE);
        assertThat(SuitabilityGrade.of(0)).isEqualTo(SuitabilityGrade.NOT_SUITABLE);
    }

    @Test
    void 예시_신청자가_4단계를_모두_보여준다() {
        MockApplicantCatalog catalog = new MockApplicantCatalog();

        assertThat(catalog.applicants()).hasSize(10);
        assertThat(catalog.applicants().stream()
                .map(m -> evaluator.evaluate(m.input()).grade())
                .distinct())
                .containsExactlyInAnyOrder(SuitabilityGrade.VERY_SUITABLE, SuitabilityGrade.SUITABLE,
                        SuitabilityGrade.UNSUITABLE, SuitabilityGrade.NOT_SUITABLE);

        // 화면 요약 카드가 한쪽으로 쏠리지 않게 구성한 분포 (아주 적합 2 / 적합 4 / 미적합 2 / 부적합 2)
        assertThat(catalog.applicants().stream()
                .map(m -> evaluator.evaluate(m.input()).totalScore())
                .toList())
                .containsExactly(98, 92, 87, 86, 84, 79, 74, 72, 58, 53);
    }
}
