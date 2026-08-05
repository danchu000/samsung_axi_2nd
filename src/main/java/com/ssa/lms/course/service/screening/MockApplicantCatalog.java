package com.ssa.lms.course.service.screening;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 수강신청 승인 화면의 <b>제안용 예시 신청자</b> 목록.
 *
 * <p>정부 제안 자료용 시범 화면이라 DB 에 가짜 훈련생·수강신청을 넣지 않고 화면 표시 전용으로만 둔다.
 * 실제 승인 대기 건은 별도 영역에 그대로 표시되고, 이 목록의 승인/반려 버튼은 DB 를 건드리지 않는다.</p>
 *
 * <p>적합도 4단계가 화면에 모두 나타나도록 아주 적합 2 / 적합 4 / 미적합 2 / 부적합 2 로 구성했다.
 * 점수는 {@link SuitabilityEvaluator} 가 편람 배점대로 실제 계산한다 (여기 하드코딩된 총점은 없다).</p>
 *
 * <p><b>이름을 실명형으로 되돌리지 말 것.</b> 이 목록에는 {@code @Profile} 이 없어 운영에서도 그대로
 * 렌더된다. 예전에는 "김서연·박지훈" 같은 실명형이라 관리자가 실제 신청자로 오인할 수 있었다 —
 * 한눈에 예시임이 드러나도록 "예시 신청자 A~J" 로 둔다(화면 상단 안내 배너와 짝이다).</p>
 */
@Component
public class MockApplicantCatalog {

    private static final String DATA_CODE = "DATA-2026-01";
    private static final String DATA_NAME = "빅데이터 분석 및 시각화 전문가 양성과정";
    private static final String CLOUD_CODE = "CLOUD-2026-01";
    private static final String CLOUD_NAME = "클라우드 기반 백엔드 개발자 양성과정";
    private static final String AI_CODE = "AI-2026-01";
    private static final String AI_NAME = "AI 서비스 개발 실무과정";

    private static final List<MockApplicant> APPLICANTS = List.of(
            // ── 아주 적합 (98점) ──
            new MockApplicant("예시 신청자 A", "sample_a", DATA_CODE, DATA_NAME, 1,
                    new ScreeningInput(EducationLevel.GRADUATE,
                            "데이터사이언스 석사", RelationLevel.SAME,
                            "데이터 분석 경력 3년, ADsP·SQLD 보유", RelationLevel.SAME,
                            1, new InterviewScores(15, 14, 15, 14))),
            // ── 아주 적합 (92점) ──
            new MockApplicant("예시 신청자 B", "sample_b", CLOUD_CODE, CLOUD_NAME, 1,
                    new ScreeningInput(EducationLevel.UNIVERSITY,
                            "컴퓨터공학", RelationLevel.SAME,
                            "백엔드 개발 경력 2년, 정보처리기사 보유", RelationLevel.RELATED,
                            1, new InterviewScores(14, 13, 14, 13))),
            // ── 적합 (87점) ──
            new MockApplicant("예시 신청자 C", "sample_c", AI_CODE, AI_NAME, 2,
                    new ScreeningInput(EducationLevel.UNIVERSITY,
                            "정보통계학", RelationLevel.RELATED,
                            "웹 서비스 운영 경력 1년, 정보처리기사 보유", RelationLevel.RELATED,
                            1, new InterviewScores(13, 12, 13, 12))),
            // ── 적합 (86점) ──
            new MockApplicant("예시 신청자 D", "sample_d", DATA_CODE, DATA_NAME, 2,
                    new ScreeningInput(EducationLevel.JUNIOR_COLLEGE,
                            "빅데이터과", RelationLevel.SAME,
                            "유통 물류 사무 경력 2년, 자격증 없음", RelationLevel.DIFFERENT,
                            2, new InterviewScores(13, 13, 12, 12))),
            // ── 적합 (84점) ──
            new MockApplicant("예시 신청자 E", "sample_e", CLOUD_CODE, CLOUD_NAME, 3,
                    new ScreeningInput(EducationLevel.UNIVERSITY_ENROLLED,
                            "소프트웨어학과", RelationLevel.RELATED,
                            "교내 프로젝트 다수, 리눅스마스터 2급 보유", RelationLevel.RELATED,
                            1, new InterviewScores(12, 13, 11, 12))),
            // ── 적합 (79점) ──
            new MockApplicant("예시 신청자 F", "sample_f", AI_CODE, AI_NAME, 4,
                    new ScreeningInput(EducationLevel.JUNIOR_COLLEGE,
                            "전자공학과", RelationLevel.RELATED,
                            "생산관리 경력 3년, 자격증 없음", RelationLevel.DIFFERENT,
                            2, new InterviewScores(11, 12, 10, 11))),
            // ── 미적합 (74점) ──
            new MockApplicant("예시 신청자 G", "sample_g", DATA_CODE, DATA_NAME, 5,
                    new ScreeningInput(EducationLevel.HIGH_SCHOOL,
                            "이과 계열(고졸)", RelationLevel.RELATED,
                            "서비스직 경력 4년, 자격증 없음", RelationLevel.DIFFERENT,
                            2, new InterviewScores(10, 11, 10, 9))),
            // ── 미적합 (72점) ──
            new MockApplicant("예시 신청자 H", "sample_h", CLOUD_CODE, CLOUD_NAME, 6,
                    new ScreeningInput(EducationLevel.JUNIOR_COLLEGE,
                            "관광경영과", RelationLevel.DIFFERENT,
                            "여행사 근무 경력 5년, 자격증 없음", RelationLevel.DIFFERENT,
                            3, new InterviewScores(10, 10, 9, 10))),
            // ── 부적합 (58점) ──
            new MockApplicant("예시 신청자 I", "sample_i", AI_CODE, AI_NAME, 7,
                    new ScreeningInput(EducationLevel.HIGH_SCHOOL,
                            "인문 계열(고졸)", RelationLevel.DIFFERENT,
                            "단기 아르바이트, 자격증 없음", RelationLevel.DIFFERENT,
                            3, new InterviewScores(7, 6, 7, 6))),
            // ── 부적합 (53점) ──
            new MockApplicant("예시 신청자 J", "sample_j", DATA_CODE, DATA_NAME, 8,
                    new ScreeningInput(EducationLevel.HIGH_SCHOOL,
                            "인문 계열(고졸)", RelationLevel.DIFFERENT,
                            "경력·자격증 없음", RelationLevel.DIFFERENT,
                            4, new InterviewScores(6, 5, 6, 5))));

    public List<MockApplicant> applicants() {
        return APPLICANTS;
    }
}
