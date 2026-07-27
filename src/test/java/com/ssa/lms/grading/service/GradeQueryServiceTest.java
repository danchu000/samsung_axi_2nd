package com.ssa.lms.grading.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.grading.dto.GradeSummary;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.repository.GradeRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 개발자 A의 이수 판정이 호출하는 계약이라 동작을 고정해 둔다.
 * 여기 기대값이 바뀌면 A의 이수 로직도 같이 바뀌어야 한다.
 */
@SpringBootTest
@Transactional
class GradeQueryServiceTest {

    @Autowired GradeQueryService gradeQueryService;
    @Autowired GradeRepository gradeRepository;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;

    private User trainee;
    private Course course;

    @BeforeEach
    void setUp() {
        trainee = userRepository.save(User.builder()
                .loginId("grade-test-" + System.nanoTime())
                .password("{noop}test")
                .name("성적테스트")
                .role(Role.TRAINEE)
                .status(UserStatus.ACTIVE)
                .build());

        course = courseRepository.save(Course.builder()
                .courseCode("COURSE-TEST-" + System.nanoTime())
                .courseName("성적 계약 테스트 과정")
                .cohort("1기")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .build());
    }

    private Grade saveGrade(Grade.EvalType type, long refId, Integer score,
                            Boolean passed, Grade.GradeStatus status) {
        Grade grade = Grade.builder()
                .user(trainee)
                .course(course)
                .evalType(type)
                .evalRefId(refId)
                .status(status)
                .build();
        if (status == Grade.GradeStatus.GRADED || status == Grade.GradeStatus.CONFIRMED) {
            grade.applyScore(score, 0, passed != null && passed ? 0 : score + 1, null, LocalDateTime.now());
            if (status == Grade.GradeStatus.CONFIRMED) {
                grade.confirm(null, LocalDateTime.now());
            }
        }
        return gradeRepository.save(grade);
    }

    @Test
    @DisplayName("확정된 성적만 돌려준다 — 채점완료 상태는 이수 판정에 쓰이면 안 된다")
    void findConfirmedGrades_확정만() {
        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        saveGrade(Grade.EvalType.ASSIGNMENT, 2L, 90, true, Grade.GradeStatus.GRADED);
        saveGrade(Grade.EvalType.EXAM, 3L, null, null, Grade.GradeStatus.UNGRADED);

        List<GradeSummary> result =
                gradeQueryService.findConfirmedGrades(trainee.getId(), course.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).evalRefId()).isEqualTo(1L);
        assertThat(result.get(0).isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("성적이 하나도 없으면 '전부 확정'이 아니다 — 평가 없는 과정이 자동 이수되면 안 된다")
    void hasAllRequiredGradesConfirmed_성적없음() {
        assertThat(gradeQueryService.hasAllRequiredGradesConfirmed(trainee.getId(), course.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("미확정 성적이 하나라도 남아 있으면 false")
    void hasAllRequiredGradesConfirmed_일부미확정() {
        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        saveGrade(Grade.EvalType.ASSIGNMENT, 2L, 70, true, Grade.GradeStatus.GRADING);

        assertThat(gradeQueryService.hasAllRequiredGradesConfirmed(trainee.getId(), course.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("전부 확정이면 true")
    void hasAllRequiredGradesConfirmed_전부확정() {
        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        saveGrade(Grade.EvalType.ASSIGNMENT, 2L, 70, true, Grade.GradeStatus.CONFIRMED);

        assertThat(gradeQueryService.hasAllRequiredGradesConfirmed(trainee.getId(), course.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("확정 성적 중 불합격이 있으면 hasFailedGrade 가 true")
    void hasFailedGrade() {
        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        assertThat(gradeQueryService.hasFailedGrade(trainee.getId(), course.getId())).isFalse();

        saveGrade(Grade.EvalType.ASSIGNMENT, 2L, 30, false, Grade.GradeStatus.CONFIRMED);
        assertThat(gradeQueryService.hasFailedGrade(trainee.getId(), course.getId())).isTrue();
    }

    @Test
    @DisplayName("확정 성적 평균 — 없으면 null")
    void averageConfirmedScore() {
        assertThat(gradeQueryService.averageConfirmedScore(trainee.getId(), course.getId()))
                .isNull();

        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        saveGrade(Grade.EvalType.ASSIGNMENT, 2L, 60, true, Grade.GradeStatus.CONFIRMED);

        assertThat(gradeQueryService.averageConfirmedScore(trainee.getId(), course.getId()))
                .isEqualTo(70.0);
    }

    @Test
    @DisplayName("다른 과정의 성적은 섞이지 않는다 — 훈련별 데이터 독립 수집 요건")
    void 과정별_격리() {
        Course other = courseRepository.save(Course.builder()
                .courseCode("COURSE-OTHER-" + System.nanoTime())
                .courseName("다른 과정")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .capacity(10)
                .status(CourseStatus.IN_PROGRESS)
                .build());

        saveGrade(Grade.EvalType.EXAM, 1L, 80, true, Grade.GradeStatus.CONFIRMED);
        gradeRepository.save(Grade.builder()
                .user(trainee).course(other)
                .evalType(Grade.EvalType.EXAM).evalRefId(99L)
                .status(Grade.GradeStatus.CONFIRMED).build());

        assertThat(gradeQueryService.findConfirmedGrades(trainee.getId(), course.getId()))
                .hasSize(1)
                .allSatisfy(g -> assertThat(g.courseId()).isEqualTo(course.getId()));
    }
}
