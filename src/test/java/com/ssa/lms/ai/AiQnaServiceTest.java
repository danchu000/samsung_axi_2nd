package com.ssa.lms.ai;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.AiQnaAnswer;
import com.ssa.lms.ai.service.AiQnaService;
import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 학습 도우미의 <b>안전장치</b>를 고정한다.
 *
 * <p>모델을 실제로 부르지 않는다 — 테스트가 API 를 때리면 돈이 나가고 결과도 매번 달라져서
 * 실패를 믿을 수 없게 된다. 그래서 {@link AiClient} 를 가짜로 꽂고, 이 서비스가 지켜야 할
 * 규칙만 검증한다.</p>
 *
 * <p>여기서 막으려는 사고
 * <ul>
 *   <li>수강하지 않는 과정으로 물어 <b>남의 커리큘럼이 새는 것</b></li>
 *   <li>인용하지도 않은 자료가 <b>근거처럼 붙는 것</b></li>
 *   <li>권한·입력이 잘못됐는데 <b>모델을 먼저 불러 돈이 나가는 것</b></li>
 * </ul>
 */
class AiQnaServiceTest {

    private AiClient aiClient;
    private EnrollmentRepository enrollmentRepository;
    private ContentRepository contentRepository;
    private AiQnaService service;

    private static final long ME = 1L;
    private static final long COURSE = 10L;

    @BeforeEach
    void setUp() {
        aiClient = mock(AiClient.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        contentRepository = mock(ContentRepository.class);
        service = new AiQnaService(aiClient, enrollmentRepository, contentRepository);
    }

    private void enrolled(EnrollmentStatus status) {
        Enrollment e = mock(Enrollment.class);
        when(e.getStatus()).thenReturn(status);
        when(enrollmentRepository.findByTraineeIdAndCourseId(ME, COURSE)).thenReturn(Optional.of(e));
    }

    private Content material(long id, String title) {
        Content c = mock(Content.class);
        when(c.getId()).thenReturn(id);
        when(c.getTitle()).thenReturn(title);
        when(c.getType()).thenReturn(ContentType.DOCUMENT);
        return c;
    }

    private void materials(Content... list) {
        when(contentRepository.findByCourseIdAndStatusOrderByOrderNoAscIdAsc(COURSE, ContentStatus.ACTIVE))
                .thenReturn(List.of(list));
    }

    private void modelSays(String text) {
        when(aiClient.ask(any(AiRequest.class))).thenReturn(AiAnswer.success(text, 10, 20));
    }

    @Test
    @DisplayName("수강하지 않는 과정은 질문할 수 없다 — 모델도 부르지 않는다")
    void 미수강_과정_차단() {
        when(enrollmentRepository.findByTraineeIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        AiQnaAnswer a = service.ask(ME, COURSE, "이 과정 커리큘럼 알려줘");

        assertThat(a.ok()).isFalse();
        assertThat(a.reason()).isEqualTo("NOT_ENROLLED");
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("승인 전(신청) 수강도 차단한다 — 승인 나기 전엔 자료를 볼 권한이 없다")
    void 신청중_수강_차단() {
        enrolled(EnrollmentStatus.APPLIED);

        AiQnaAnswer a = service.ask(ME, COURSE, "질문");

        assertThat(a.reason()).isEqualTo("NOT_ENROLLED");
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("빈 질문·과도하게 긴 질문은 모델을 부르기 전에 막는다 — 토큰은 비용이다")
    void 입력_검증이_먼저() {
        enrolled(EnrollmentStatus.APPROVED);

        assertThat(service.ask(ME, COURSE, "   ").reason()).isEqualTo("EMPTY_QUESTION");
        assertThat(service.ask(ME, COURSE, "가".repeat(1001)).reason()).isEqualTo("TOO_LONG");
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("학습 자료가 없어도 답한다 — 단, 일반 지식임을 표시한다")
    void 자료_없으면_일반지식으로_답한다() {
        enrolled(EnrollmentStatus.APPROVED);
        materials();
        modelSays("<일반지식>\n트랜잭션은 하나의 작업 단위입니다.");

        AiQnaAnswer a = service.ask(ME, COURSE, "트랜잭션이 뭔가요?");

        assertThat(a.ok())
                .as("자료가 없는 건 훈련생 잘못이 아닌데 아무 답도 못 받으면 안 된다")
                .isTrue();
        assertThat(a.general()).isTrue();
        assertThat(a.answer())
                .as("태그가 그대로 보이면 무슨 말인지 알 수 없다")
                .doesNotContain("<일반지식>");
        assertThat(a.sources())
                .as("일반 지식 답변에 자료 링크를 달면 근거가 아닌 것을 근거로 보이게 한다")
                .isEmpty();
    }

    @Test
    @DisplayName("자료로 답한 경우에는 일반지식 표시가 붙지 않는다")
    void 자료_기반_답변은_일반지식이_아니다() {
        enrolled(EnrollmentStatus.APPROVED);
        materials(material(101, "3주차 트랜잭션"));
        modelSays("3주차에서 다뤘어요 [자료 1].");

        AiQnaAnswer a = service.ask(ME, COURSE, "트랜잭션이 뭔가요?");

        assertThat(a.general()).isFalse();
        assertThat(a.sources()).hasSize(1);
    }

    @Test
    @DisplayName("답변이 인용한 자료만 링크로 붙는다")
    void 인용한_자료만_붙는다() {
        enrolled(EnrollmentStatus.APPROVED);
        materials(material(101, "1주차 자료"), material(102, "2주차 자료"), material(103, "3주차 자료"));
        modelSays("이 내용은 2주차에서 다뤘어요 [자료 2]. 함께 보면 좋아요 [자료 2].");

        AiQnaAnswer a = service.ask(ME, COURSE, "질문");

        assertThat(a.ok()).isTrue();
        assertThat(a.sources())
                .as("인용하지 않은 자료까지 붙이면 '근거가 이만큼 있다'는 착각을 준다")
                .containsExactly(new AiQnaAnswer.Source("2주차 자료", "/trainee/contents/102/play"));
    }

    @Test
    @DisplayName("범위 밖 번호를 인용하면 무시한다 — 없는 자료 링크를 만들면 404 로 간다")
    void 잘못된_인용번호는_버린다() {
        enrolled(EnrollmentStatus.APPROVED);
        materials(material(101, "1주차 자료"));
        modelSays("자료를 참고하세요 [자료 9] [자료 0] [자료 1]");

        AiQnaAnswer a = service.ask(ME, COURSE, "질문");

        assertThat(a.sources()).hasSize(1);
        assertThat(a.sources().get(0).href()).isEqualTo("/trainee/contents/101/play");
    }

    @Test
    @DisplayName("모델 호출이 실패하면 안내 문구를 그대로 돌려주고 자료는 붙이지 않는다")
    void 실패는_안내로_내려간다() {
        enrolled(EnrollmentStatus.APPROVED);
        materials(material(101, "1주차 자료"));
        when(aiClient.ask(any())).thenReturn(AiAnswer.disabled());

        AiQnaAnswer a = service.ask(ME, COURSE, "질문");

        assertThat(a.ok()).isFalse();
        assertThat(a.reason()).isEqualTo("DISABLED");
        assertThat(a.answer()).isNotBlank();
        assertThat(a.sources())
                .as("실패한 답변에 자료를 달면 답이 있는 것처럼 보인다")
                .isEmpty();
    }
}
