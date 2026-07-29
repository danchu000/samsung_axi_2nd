package com.ssa.lms.ai;

import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.dto.DiagnosisView;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import com.ssa.lms.ai.service.AiDiagnosisService;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * [기능 4] <b>비용 방어선</b>을 고정한다.
 *
 * <p>강사가 진단 화면을 새로고침할 때마다 모델을 부르면, 훈련생이 아무것도 안 물은 날에도
 * 요금이 나간다. "질문이 없으면 부르지 않는다"는 규칙은 눈에 안 보여서 리팩터링 중
 * 조용히 사라지기 쉽다. 그래서 테스트로 못 박는다.</p>
 */
class AiDiagnosisCostGuardTest {

    private AiClient aiClient;
    private AiUsageLogRepository usageRepository;
    private CourseQueryService courseQueryService;
    private CourseRepository courseRepository;
    private AiDiagnosisService service;

    private static final long TEACHER = 5L;

    @BeforeEach
    void setUp() {
        aiClient = mock(AiClient.class);
        usageRepository = mock(AiUsageLogRepository.class);
        courseQueryService = mock(CourseQueryService.class);
        courseRepository = mock(CourseRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        service = new AiDiagnosisService(aiClient, usageRepository,
                courseQueryService, courseRepository, userRepository);
    }

    @Test
    @DisplayName("담당 과정이 없으면 모델을 부르지 않는다")
    void 담당_과정_없으면_호출_안함() {
        when(courseRepository.findAll()).thenReturn(List.of());

        assertThat(service.forInstructor(TEACHER).isEmpty()).isTrue();
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("기간 내 훈련생 질문이 0건이면 모델을 부르지 않는다 — 여기가 비용 방어선이다")
    void 질문_없으면_호출_안함() {
        com.ssa.lms.course.entity.Course c = mock(com.ssa.lms.course.entity.Course.class);
        when(c.getId()).thenReturn(1L);
        when(courseRepository.findAll()).thenReturn(List.of(c));
        when(courseQueryService.isInstructorOf(anyLong(), anyLong())).thenReturn(true);
        when(usageRepository.findQuestionsSince(anyList(), any())).thenReturn(List.of());

        DiagnosisView v = service.forInstructor(TEACHER);

        assertThat(v.isEmpty()).isTrue();
        assertThat(v.analyzedAt())
                .as("분석하지 않았는데 날짜를 남기면 최신 진단으로 오해한다")
                .isNull();
        verify(aiClient, never())
                .ask(any());
    }

    @Test
    @DisplayName("로그인 정보가 없으면 아무것도 하지 않는다")
    void 사용자_없으면_호출_안함() {
        assertThat(service.forInstructor(null).isEmpty()).isTrue();
        verify(aiClient, never()).ask(any());
    }

    @Test
    @DisplayName("질문 본문은 저장 길이를 넘기면 잘라 담는다 — 진단에는 앞부분이면 충분하다")
    void 질문은_잘라_담는다() {
        AiUsageLog logEntry = AiUsageLog.success("QNA", 1L, "m", 1, 1, 1, 1, 1)
                .withQuestion(3L, "가".repeat(AiUsageLog.MAX_QUESTION_CHARS + 100));

        assertThat(logEntry.getQuestion()).hasSize(AiUsageLog.MAX_QUESTION_CHARS);
        assertThat(logEntry.getCourseId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("보존기간이 지나면 질문 내용만 지우고 통계는 남긴다")
    void 내용만_지운다() {
        AiUsageLog logEntry = AiUsageLog.success("QNA", 1L, "m", 10, 20, 5, 100, 200)
                .withQuestion(3L, "트랜잭션이 뭔가요?");

        logEntry.forgetQuestion();

        assertThat(logEntry.getQuestion()).isNull();
        assertThat(logEntry.getInputTokens())
                .as("이력을 통째로 지우면 '지난달에 얼마나 썼나'를 못 본다")
                .isEqualTo(10);
        assertThat(logEntry.getOutputTokens()).isEqualTo(20);
    }
}
