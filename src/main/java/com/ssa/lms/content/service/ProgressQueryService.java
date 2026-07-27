package com.ssa.lms.content.service;

/**
 * 진도 조회 계약 — <b>P2-A(콘텐츠/진도) 가 제공, P2-B(이수) 가 소비</b> (PARALLEL-P2.md).
 *
 * <p>이수 판정은 진도(이 인터페이스) + 출결(P2-B 내부) + 성적(B의 GradeQueryService, 미제공)을 읽는다.
 * 두 트랙이 독립 컴파일/부팅되도록 인터페이스와 fallback 빈({@link ProgressContractConfig})을
 * base 에 둔다. P2-A 가 실제 {@code @Service} 구현체를 추가하면 fallback 은 자동으로 물러난다.</p>
 *
 * <p>시그니처가 더 필요하면 P2-A·P2-B 합의 후 여기에 추가한다(인터페이스 변경은 양쪽 영향 → 알림 필수).</p>
 */
public interface ProgressQueryService {

    /** 해당 과정에서 사용자의 진도율(0~100). */
    int completedRatio(Long userId, Long courseId);

    /** 필수 콘텐츠를 모두 이수했는지. */
    boolean hasCompletedAll(Long userId, Long courseId);
}
