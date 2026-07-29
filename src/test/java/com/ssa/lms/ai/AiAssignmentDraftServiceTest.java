package com.ssa.lms.ai;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.AssignmentDraft;
import com.ssa.lms.ai.service.AiAssignmentDraftService;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
 * [기능 4] AI 과제 초안 생성 규칙을 고정한다.
 *
 * <p>모델을 실제로 부르지 않는다 — 테스트가 API 를 때리면 돈이 나가고 결과도 매번 달라진다.</p>
 */
class AiAssignmentDraftServiceTest {

    private AiClient aiClient;
    private CourseQueryService courseQueryService;
    private AiAssignmentDraftService service;

    private static final long TEACHER = 7L;
    private static final long COURSE = 3L;

    @BeforeEach
    void setUp() {
        aiClient = mock(AiClient.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        courseQueryService = mock(CourseQueryService.class);

        when(contentRepository.findByCourseIdAndStatusOrderByOrderNoAscIdAsc(anyLong(), any()))
                .thenReturn(List.of());
        when(courseRepository.findById(anyLong())).thenReturn(Optional.empty());

        service = new AiAssignmentDraftService(aiClient, contentRepository,
                courseRepository, courseQueryService);
    }

    private void mine() {
        when(courseQueryService.isInstructorOf(TEACHER, COURSE)).thenReturn(true);
    }

    private void modelSays(String text) {
        when(aiClient.ask(any(AiRequest.class))).thenReturn(AiAnswer.success(text, 10, 20));
    }

    @Test
    @DisplayName("담당하지 않는 과정은 만들 수 없다 — 모델도 부르지 않는다")
    void 남의_과정_차단() {
        when(courseQueryService.isInstructorOf(anyLong(), anyLong())).thenReturn(false);

        AssignmentDraft d = service.draft(TEACHER, COURSE, "트랜잭션", 3);

        assertThat(d.ok()).isFalse();
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("보완 영역이 비면 모델을 부르기 전에 막는다 — 토큰은 비용이다")
    void 영역_없으면_호출_안함() {
        mine();
        assertThat(service.draft(TEACHER, COURSE, "  ", 1).ok()).isFalse();
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("제목·설명·평가기준을 나눠 담는다")
    void 세_구획으로_나눈다() {
        mine();
        modelSays("""
                제목: 트랜잭션 격리수준 실습
                설명: 계좌 이체 예제를 만들어 격리수준별 차이를 직접 확인합니다.
                제출은 코드와 실행 결과 캡처입니다.
                평가기준: 격리수준 4종 구현 (40점)
                결과 분석 (30점)
                코드 품질 (30점)
                """);

        AssignmentDraft d = service.draft(TEACHER, COURSE, "트랜잭션", 3);

        assertThat(d.ok()).isTrue();
        assertThat(d.title()).isEqualTo("트랜잭션 격리수준 실습");
        assertThat(d.description()).contains("계좌 이체").doesNotContain("평가기준");
        assertThat(d.criteria()).contains("40점").contains("30점");
    }

    @Test
    @DisplayName("형식이 어긋나도 버리지 않는다 — 빈 칸보다 고쳐 쓸 초안이 낫다")
    void 형식이_어긋나도_살린다() {
        mine();
        modelSays("트랜잭션 격리수준을 직접 실습해 보는 과제를 추천합니다.");

        AssignmentDraft d = service.draft(TEACHER, COURSE, "트랜잭션·동시성", 2);

        assertThat(d.ok()).isTrue();
        assertThat(d.title()).isEqualTo("트랜잭션·동시성 보완 과제");
        assertThat(d.description()).contains("트랜잭션 격리수준");
    }

    @Test
    @DisplayName("출력 상한을 반드시 지정한다 — 응답이 길어지면 그대로 비용이다")
    void 출력_상한을_건다() {
        mine();
        modelSays("제목: x");
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);

        service.draft(TEACHER, COURSE, "트랜잭션", 1);

        verify(aiClient).ask(captor.capture());
        assertThat(captor.getValue().maxOutputTokens()).isNotNull().isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("모델 실패는 안내 문구로 내려간다")
    void 실패는_안내로() {
        mine();
        when(aiClient.ask(any())).thenReturn(AiAnswer.disabled());

        AssignmentDraft d = service.draft(TEACHER, COURSE, "트랜잭션", 1);

        assertThat(d.ok()).isFalse();
        assertThat(d.message()).isNotBlank();
    }
}
