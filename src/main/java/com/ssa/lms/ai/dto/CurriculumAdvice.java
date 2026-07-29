package com.ssa.lms.ai.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [기능 2] 맞춤 커리큘럼 추천 결과.
 *
 * <p><b>추천 대상은 원내 개설 과정으로 못 박는다.</b> 외부 강의를 섞으면 훈련생이
 * 어디서 신청하는지 알 수 없고, 훈련기관이 남의 강의를 권하는 꼴이 된다.
 * 그래서 모델에게 <b>과정 목록을 주고 그 안에서만 고르게</b> 하고, 돌아온 답도
 * 실제 과정 id 와 대조해 없는 과정은 버린다.</p>
 *
 * @param ok        생성 성공 여부
 * @param analyzedAt 분석 기준일
 * @param stats     판단에 쓴 내 학습 데이터 (실제 값)
 * @param recommend 추천 과정
 * @param message   실패 시 안내
 */
public record CurriculumAdvice(
        boolean ok,
        LocalDate analyzedAt,
        List<Stat> stats,
        List<Recommend> recommend,
        String message
) {

    /** 추천의 근거가 된 실제 수치. 추천만 던지면 "왜 나한테?"가 되고 안 누른다. */
    public record Stat(String label, String value, String sub) {}

    /**
     * @param courseId 실제 과정 id — 눌러서 신청할 수 있어야 추천이 의미가 있다
     * @param fit      적합도(%)
     * @param reasons  추천 이유. 이유 없는 추천은 광고로 읽힌다
     * @param applied  이미 신청한 과정인지 — 모르고 또 신청하려다 헛걸음한다
     */
    public record Recommend(Long courseId, String name, String period, String seats,
                            int fit, List<String> reasons, boolean applied) {}

    public static CurriculumAdvice fail(String message) {
        return new CurriculumAdvice(false, LocalDate.now(), List.of(), List.of(), message);
    }
}
