package com.ssa.lms.ai.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [기능 4] AI 학습진단 결과.
 *
 * <p><b>질문이 있을 때만 갱신한다.</b> 훈련생이 오늘 아무것도 안 물었으면 어제와
 * 달라질 게 없는데 모델을 부르면 돈만 나간다. 그래서 "오늘 새 질문이 있는 과정"만
 * 다시 분석하고, 나머지는 지난 결과를 그대로 쓴다.</p>
 *
 * @param analyzedAt 마지막 분석일. <b>분석한 적이 없으면 null</b>
 * @param questionCount 분석에 쓴 질문 수 — 표본 크기를 감추면 판단의 무게를 알 수 없다
 * @param topics     질문이 몰린 주제. 여러 명이 같은 곳에서 막히면 수업 문제다
 * @param rows       훈련생별 진단
 */
public record DiagnosisView(
        LocalDate analyzedAt,
        int questionCount,
        List<Topic> topics,
        List<Row> rows
) {

    /**
     * @param students 이 주제를 물어본 훈련생 수
     * @param count    질문 건수
     */
    public record Topic(String label, int count, int students) {}

    /**
     * @param level    high / mid / low — 시급도
     * @param weak     취약 영역
     * @param evidence 진단 근거. 근거 없이 "보완 필요"라고만 하면 강사가 판단할 수 없다
     * @param task     추천 과제 <b>제목만</b>. 실제 내용은 [AI로 과제 만들기]로 생성한다
     */
    public record Row(Long traineeId, String name, Long courseId, String course,
                      String level, String levelLabel,
                      List<String> weak, String evidence, String task) {}

    public static DiagnosisView empty() {
        return new DiagnosisView(null, 0, List.of(), List.of());
    }

    /** 분석할 질문이 하나도 없었는지 — 화면이 "아직 질문 없음"을 보여준다. */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
