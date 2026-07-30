package com.ssa.lms.dashboard.dto;

import java.util.List;

/**
 * 학습자 이탈(중도탈락) 예측 대시보드가 쓰는 지표.
 *
 * <p><b>지금은 전 항목이 표본이다</b> — {@code sample=true}. 훈련 데이터가 쌓이면
 * {@link com.ssa.lms.dashboard.service.DropoutPredictionService} 가 실제 예측값으로
 * 이 DTO 를 채우고 {@code sample=false} 로 내려준다. 화면은 sample 여부에 따라
 * 표본 배너/배지를 켜고 끈다 — 표본을 실제 수치로 착각하면 잘못된 개입을 하게 된다.</p>
 *
 * <p>화면(JS)이 그대로 읽을 수 있도록 필드 이름을 차트 입력과 일치시켰다
 * ({@link DashboardMetrics} 와 같은 원칙).</p>
 */
public record DropoutPredictionView(
        boolean sample,
        String modelNote,
        Summary summary,
        List<CourseRisk> courseRisks,
        Trend trend,
        List<Factor> factors,
        List<RiskTrainee> riskTrainees
) {

    /**
     * 상단 KPI.
     *
     * @param avgRisk 전체 학습자 평균 이탈 위험도(%)
     */
    public record Summary(int totalTrainees, int highRisk, int midRisk, int stable, int avgRisk) {
    }

    /** 과정별 평균 이탈 위험도(%). 위험도가 높은 과정이 먼저 오도록 정렬해 담는다. */
    public record CourseRisk(String name, int risk) {
    }

    /**
     * 주간 위험군 비율 추이. weeks 와 각 시리즈의 길이는 같아야 한다.
     * 값은 전체 학습자 대비 비율(%) — 인원수로 담으면 기수마다 축이 달라져 추이가 안 읽힌다.
     */
    public record Trend(List<String> weeks, List<Integer> high, List<Integer> mid, List<Integer> stable) {
    }

    /** 위험 요인 기여도(%). 모델 연동 후에는 feature importance 를 그대로 담는 자리다. */
    public record Factor(String label, int value) {
    }

    /**
     * 관리자가 지금 개입해야 할 위험 학습자 한 명.
     *
     * @param level      "고위험" / "중위험" — 화면 배지 텍스트
     * @param mainFactor 위험도를 끌어올린 주요 요인 요약 (예: "14일 미접속 · 진도 지연 32%p")
     * @param lastAccess 최근 접속 시점 표시 문자열
     */
    public record RiskTrainee(String name, String loginId, String courseName, int riskScore,
                              String level, String mainFactor, String lastAccess, int progressRate) {
    }

    /**
     * 모델 연동 전 표본. 값은 고정이다 — 새로고침마다 바뀌면 표본이 아니라 오작동으로 보인다.
     * 과정 이름은 관리자 대시보드 표본(admin/dashboard-charts.js)과 맞춰 두 화면이 한 이야기로 읽히게 했다.
     */
    public static DropoutPredictionView sampleView() {
        return new DropoutPredictionView(
                true,
                "학습 활동 데이터(접속·진도·출결·제출)를 기반으로 이탈 위험을 분석합니다. 데이터가 축적될수록 예측 정확도가 향상됩니다.",
                new Summary(48, 5, 9, 34, 27),
                List.of(
                        new CourseRisk("백엔드 심화", 46),
                        new CourseRisk("데이터 분석 실무", 41),
                        new CourseRisk("프론트엔드 실무", 25),
                        new CourseRisk("클라우드 풀스택", 22),
                        new CourseRisk("AI 서비스 개발", 18)
                ),
                new Trend(
                        List.of("6/2", "6/9", "6/16", "6/23", "6/30", "7/7", "7/14", "7/21"),
                        List.of(4, 4, 6, 6, 8, 8, 10, 10),
                        List.of(13, 15, 15, 17, 17, 19, 19, 19),
                        List.of(83, 81, 79, 77, 75, 73, 71, 71)
                ),
                List.of(
                        new Factor("장기 미접속", 34),
                        new Factor("진도 지연", 27),
                        new Factor("과제 미제출", 18),
                        new Factor("출석 저조", 13),
                        new Factor("시험 미응시", 8)
                ),
                List.of(
                        new RiskTrainee("김민준", "sample01", "백엔드 심화", 87, "고위험",
                                "14일 미접속 · 진도 지연 32%p", "14일 전", 18),
                        new RiskTrainee("이서연", "sample02", "데이터 분석 실무", 81, "고위험",
                                "과제 3건 미제출 · 출석률 64%", "9일 전", 27),
                        new RiskTrainee("박도현", "sample03", "백엔드 심화", 76, "고위험",
                                "진도 지연 28%p · 시험 1건 미응시", "6일 전", 22),
                        new RiskTrainee("최지우", "sample04", "데이터 분석 실무", 73, "고위험",
                                "11일 미접속 · 과제 2건 미제출", "11일 전", 35),
                        new RiskTrainee("정하은", "sample05", "프론트엔드 실무", 70, "고위험",
                                "출석률 58% · 진도 지연 21%p", "4일 전", 41),
                        new RiskTrainee("강시우", "sample06", "클라우드 풀스택", 62, "중위험",
                                "진도 지연 18%p", "3일 전", 44),
                        new RiskTrainee("윤서준", "sample07", "백엔드 심화", 55, "중위험",
                                "과제 1건 미제출 · 출석률 78%", "2일 전", 51),
                        new RiskTrainee("임예린", "sample08", "AI 서비스 개발", 48, "중위험",
                                "5일 미접속", "5일 전", 63)
                )
        );
    }
}
