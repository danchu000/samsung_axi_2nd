package com.ssa.lms.ai.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 호출 기록.
 *
 * <p><b>왜 남기나</b>
 * <ol>
 *   <li><b>비용</b> — 토큰은 곧 돈이다. 어느 기능이 얼마나 쓰는지 모르면 요금이 터진 뒤에야 안다</li>
 *   <li><b>한도</b> — 하루 상한을 세려면 오늘 몇 번 불렀는지 알아야 한다</li>
 *   <li><b>추적</b> — "AI가 이상한 답을 했다"는 신고가 들어왔을 때 무엇을 물었고 뭐라 답했는지
 *       확인할 수 있어야 한다</li>
 * </ol>
 *
 * <p><b>개인정보 주의:</b> 질문 본문에 이름·연락처가 섞여 들어올 수 있어
 * {@code prompt}/{@code answer} 는 저장하지 않고 <b>길이만</b> 남긴다.
 * 내용까지 보관하려면 암호화 컨버터를 붙이고 보존기간을 정한 뒤에 해야 한다.</p>
 *
 * <p>이력 테이블이라 soft delete 를 쓰지 않는다 — 지워지면 기록의 의미가 없다.</p>
 */
@Entity
@Table(name = "ai_usage_log", indexes = {
        @Index(name = "idx_ai_usage_called_at", columnList = "called_at"),
        @Index(name = "idx_ai_usage_user", columnList = "user_id, called_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 기능이 불렀는지 — QNA / CURRICULUM / ROADMAP / DIAGNOSIS */
    @Column(name = "purpose", nullable = false, length = 40)
    private String purpose;

    /** 호출한 사용자. 배치 호출이면 null. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    /** 실패 사유 코드. 성공이면 null. */
    @Column(name = "fail_reason", length = 40)
    private String failReason;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    /** 응답까지 걸린 시간(ms). 느려지는 추세를 보려면 필요하다. */
    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs;

    /** 본문은 저장하지 않고 길이만 — 개인정보가 섞일 수 있다. */
    @Column(name = "prompt_chars", nullable = false)
    private int promptChars;

    @Column(name = "answer_chars", nullable = false)
    private int answerChars;

    private AiUsageLog(String purpose, Long userId, String model, boolean success,
                       String failReason, int inputTokens, int outputTokens,
                       long elapsedMs, int promptChars, int answerChars) {
        this.purpose = purpose;
        this.userId = userId;
        this.model = model;
        this.calledAt = LocalDateTime.now();
        this.success = success;
        this.failReason = failReason;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.elapsedMs = elapsedMs;
        this.promptChars = promptChars;
        this.answerChars = answerChars;
    }

    public static AiUsageLog success(String purpose, Long userId, String model,
                                     int inputTokens, int outputTokens,
                                     long elapsedMs, int promptChars, int answerChars) {
        return new AiUsageLog(purpose, userId, model, true, null,
                inputTokens, outputTokens, elapsedMs, promptChars, answerChars);
    }

    public static AiUsageLog failure(String purpose, Long userId, String model,
                                     String failReason, long elapsedMs, int promptChars) {
        return new AiUsageLog(purpose, userId, model, false, failReason,
                0, 0, elapsedMs, promptChars, 0);
    }
}
