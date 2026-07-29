package com.ssa.lms.ai.entity;

import com.ssa.lms.common.converter.CryptoConverter;
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
 * <p><b>개인정보 처리</b><br>
 * 답변 본문은 저장하지 않는다(길이만). 질문 본문은 <b>학습 진단([기능 4])에 필요해</b>
 * 저장하되 다음을 지킨다.
 * <ul>
 *   <li>{@code CryptoConverter}(AES-256/GCM)로 <b>암호문 저장</b> — DB 를 직접 열어도 읽히지 않는다</li>
 *   <li>훈련생 질문({@code purpose=QNA})만 저장한다. 배치·강사 호출은 남기지 않는다</li>
 *   <li>{@link #MAX_QUESTION_CHARS} 를 넘으면 잘라 담는다 — 진단에는 앞부분이면 충분하다</li>
 *   <li>보존기간이 지난 기록은 {@code AiUsageLogCleaner} 가 질문 본문만 지운다.
 *       통계(건수·토큰)는 남기고 <b>내용만</b> 지운다</li>
 * </ul>
 * 암호문이라 <b>이 컬럼으로 DB 검색은 불가능</b>하다 (CLAUDE.md 규칙 7).
 * 진단은 하루치를 읽어 자바에서 처리한다.</p>
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

    /** 질문 대상 과정. 진단은 과정 단위로 하므로 필요하다. */
    @Column(name = "course_id")
    private Long courseId;

    /**
     * 훈련생 질문 본문 (암호화 저장). 진단 대상이 아닌 호출은 null.
     * 답변은 저장하지 않는다 — 진단에 필요한 것은 "무엇을 어려워하는가"이지 답이 아니다.
     */
    @Convert(converter = CryptoConverter.class)
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    /** 진단에 쓸 만큼만 담는다. 길게 담아도 분류 정확도는 안 올라가고 저장량만 는다. */
    public static final int MAX_QUESTION_CHARS = 500;

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

    /**
     * 진단용 질문 본문을 붙인다. 훈련생 Q&A 에서만 호출한다.
     * 길면 잘라 담는다 — 저장량이 늘어날 뿐 분류가 정확해지지는 않는다.
     */
    public AiUsageLog withQuestion(Long courseId, String question) {
        this.courseId = courseId;
        if (question != null && !question.isBlank()) {
            String q = question.trim();
            this.question = q.length() > MAX_QUESTION_CHARS
                    ? q.substring(0, MAX_QUESTION_CHARS) : q;
        }
        return this;
    }

    /** 보존기간이 지난 기록의 <b>내용만</b> 지운다. 통계는 남긴다. */
    public void forgetQuestion() {
        this.question = null;
    }
}
