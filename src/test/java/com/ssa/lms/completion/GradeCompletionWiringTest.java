package com.ssa.lms.completion;

import com.ssa.lms.completion.service.grade.GradeCompletionProvider;
import com.ssa.lms.completion.service.grade.GradeQueryServiceAdapter;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.repository.GradeRepository;
import com.ssa.lms.grading.service.GradeQueryService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A 의 이수 판정이 B 의 성적을 실제로 읽는지 고정한다.
 *
 * <p><b>왜 이 테스트가 필요한가:</b> A 는 {@code Grade} 를 직접 읽으면 안 된다. 채점 규칙
 * (재응시 시 어느 회차를 반영할지, 확정 전 점수를 쓸지)이 전부 B 쪽에 있어서, 직접 읽으면
 * 틀린 값으로 수료 판정을 하게 된다. 그래서 A 는 {@link GradeCompletionProvider} 포트만 알고,
 * 구현체 {@link GradeQueryServiceAdapter} 가 B 의 {@link GradeQueryService} 로 위임한다.</p>
 *
 * <p>B 의 grading 패키지가 없던 시절 A 는 fallback(항상 false)으로 단독 부팅했다.
 * <b>통합 후에도 fallback 이 살아 있으면 모든 학생이 성적 미달로 판정된다</b> — 조용히 틀린
 * 결과가 나오는 종류라 테스트로 잡아야 한다.</p>
 */
@SpringBootTest
@Transactional
class GradeCompletionWiringTest {

    @Autowired GradeCompletionProvider provider;
    @Autowired GradeRepository gradeRepository;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;

    @Test
    @DisplayName("이수 판정이 fallback 이 아니라 B 의 GradeQueryService 로 연결돼 있다")
    void 어댑터가_연결됨() {
        assertThat(provider)
                .as("fallback 이 남아 있으면 모든 학생이 성적 미달로 판정된다")
                .isInstanceOf(GradeQueryServiceAdapter.class);
    }

    @Test
    @DisplayName("확정 성적이 있으면 이수 요건 충족으로 넘어간다 — 실제 값이 흐르는지")
    void 확정성적이_이수판정에_반영된다() {
        User trainee = userRepository.save(User.builder()
                .loginId("wiring-" + System.nanoTime())
                .password("{noop}test")
                .name("연결검증")
                .role(Role.TRAINEE)
                .status(UserStatus.ACTIVE)
                .build());
        Course course = courseRepository.save(Course.builder()
                .courseCode("WIRING-" + System.nanoTime())
                .courseName("성적 연결 검증 과정")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .capacity(10)
                .status(com.ssa.lms.course.entity.CourseStatus.IN_PROGRESS)
                .build());

        // 성적이 하나도 없으면 false — "평가가 없어서 자동 이수" 를 막는 정책
        assertThat(provider.gradesConfirmed(trainee.getId(), course.getId()))
                .as("성적 행이 없는데 true 면 평가 없이 수료된다")
                .isFalse();

        Grade grade = gradeRepository.save(Grade.builder()
                .user(trainee).course(course)
                .evalType(Grade.EvalType.EXAM).evalRefId(1L)
                .status(Grade.GradeStatus.UNGRADED)
                .build());
        gradeRepository.flush();

        // 아직 확정 전 — 미확정이 남아 있으면 false
        assertThat(provider.gradesConfirmed(trainee.getId(), course.getId()))
                .as("확정 전 점수로 수료 판정하면 안 된다")
                .isFalse();

        grade.applyScore(80, 0, 60, null, LocalDateTime.now());
        grade.confirm(null, LocalDateTime.now());
        gradeRepository.flush();

        assertThat(provider.gradesConfirmed(trainee.getId(), course.getId()))
                .as("확정됐는데 false 면 수료 대상자가 탈락한다")
                .isTrue();
    }
}
