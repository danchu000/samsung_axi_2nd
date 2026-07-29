package com.ssa.lms.dashboard;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.dashboard.dto.DashboardMetrics;
import com.ssa.lms.dashboard.service.DashboardMetricsService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대시보드 지표 계산을 고정한다.
 *
 * <p>여기서 잡으려는 것은 <b>조용히 틀린 숫자</b>다. 화면은 멀쩡히 그려지는데 값이
 * 틀리면 아무도 눈치채지 못하고, 그 숫자로 운영 판단을 하게 된다.</p>
 */
@SpringBootTest
@Transactional
class DashboardMetricsServiceTest {

    @Autowired DashboardMetricsService metricsService;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired UserRepository userRepository;

    private Course newCourse(String tag, LocalDate start, LocalDate end) {
        return courseRepository.save(Course.builder()
                .courseCode(tag + "-" + System.nanoTime())
                .courseName(tag)
                .startDate(start)
                .endDate(end)
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .build());
    }

    private User newTrainee() {
        return userRepository.save(User.builder()
                .loginId("m-" + System.nanoTime())
                .password("{noop}test").name("지표검증")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build());
    }

    private void enroll(Course c, EnrollmentStatus status, double progress) {
        Enrollment e = enrollmentRepository.save(Enrollment.builder()
                .trainee(newTrainee()).course(c).status(status).build());
        e.updateProgressRate(progress);
        enrollmentRepository.flush();
    }

    @Test
    @DisplayName("평균 진도율은 승인·수료 상태만 센다")
    void 진도율은_유효한_수강만() {
        Course c = newCourse("진도검증", LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        enroll(c, EnrollmentStatus.APPROVED, 80);
        enroll(c, EnrollmentStatus.APPROVED, 40);
        enroll(c, EnrollmentStatus.REJECTED, 0);    // 반려 — 세면 평균이 내려간다
        enroll(c, EnrollmentStatus.CANCELLED, 0);

        DashboardMetrics m = metricsService.of(List.of(c));

        assertThat(m.courses()).hasSize(1);
        assertThat(m.courses().get(0).progress())
                .as("반려·취소를 세면 평균이 실제보다 낮게 나온다")
                .isEqualTo(60);
        assertThat(m.courses().get(0).trainees()).isEqualTo(2);
    }

    @Test
    @DisplayName("수강생이 없는 과정은 목록에서 뺀다 — 0%로 그리면 '진도 안 나감'으로 읽힌다")
    void 수강생_없으면_제외() {
        Course c = newCourse("빈과정", LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        assertThat(metricsService.of(List.of(c)).courses()).isEmpty();
    }

    @Test
    @DisplayName("기간 경과율 — 시작 전 0, 중간, 종료 후 100")
    void 기간_경과율() {
        Course before = newCourse("시작전", LocalDate.now().plusDays(10), LocalDate.now().plusDays(20));
        Course mid = newCourse("진행중", LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        Course after = newCourse("종료됨", LocalDate.now().minusDays(20), LocalDate.now().minusDays(10));
        for (Course c : List.of(before, mid, after)) enroll(c, EnrollmentStatus.APPROVED, 50);

        List<DashboardMetrics.CourseProgress> rows = metricsService.of(List.of(before, mid, after)).courses();

        assertThat(find(rows, "시작전").elapsed()).isZero();
        assertThat(find(rows, "진행중").elapsed()).isBetween(45, 55);
        assertThat(find(rows, "종료됨").elapsed()).isEqualTo(100);
    }

    @Test
    @DisplayName("기간이 하루도 안 되는 과정은 기준선을 그리지 않도록 -1 을 준다")
    void 기간_계산불가면_기준선_없음() {
        // 시작일 == 종료일. 날짜는 DB 에서 필수(not null)라 null 은 들어올 수 없지만,
        // 같은 날짜로 잘못 입력된 데이터는 실제로 생길 수 있다. 이때 0으로 나누면 안 된다.
        LocalDate day = LocalDate.now();
        Course c = newCourse("기간없음", day, day);
        enroll(c, EnrollmentStatus.APPROVED, 30);

        assertThat(metricsService.of(List.of(c)).courses().get(0).elapsed())
                .as("계산할 수 없는 기준선을 0 이나 100 으로 그리면 거짓 정보가 된다")
                .isEqualTo(-1);
    }

    @Test
    @DisplayName("이수 판정 이력이 없으면 빈 목록 — 0으로 채우지 않는다")
    void 판정_없으면_빈목록() {
        Course c = newCourse("판정없음", LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        enroll(c, EnrollmentStatus.APPROVED, 90);

        assertThat(metricsService.of(List.of(c)).completion())
                .as("판정 전 인원을 미충족으로 세면 실제보다 나쁘게 보인다")
                .isEmpty();
    }

    @Test
    @DisplayName("과정이 없으면 빈 지표")
    void 과정_없음() {
        DashboardMetrics m = metricsService.of(List.of());
        assertThat(m.courses()).isEmpty();
        assertThat(m.completion()).isEmpty();
    }

    private DashboardMetrics.CourseProgress find(List<DashboardMetrics.CourseProgress> rows, String name) {
        return rows.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }
}
