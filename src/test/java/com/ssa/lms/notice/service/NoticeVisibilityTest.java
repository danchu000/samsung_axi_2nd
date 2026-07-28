package com.ssa.lms.notice.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.course.service.CourseQueryService;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공지 노출 범위가 A 의 수강 판정 기준과 같은지 고정한다.
 *
 * <p><b>배경:</b> {@code NoticeVisibilityService.traineeCourseIds()} 가 수강 상태를 보지 않아
 * 신청만 하고 반려·취소된 과정의 공지까지 보였다. A 의
 * {@code CourseQueryService.findUserIdsByCourseId} 는 APPROVED·COMPLETED 만 인정한다.</p>
 *
 * <p>기준이 갈리면 <b>"공지는 보이는데 과제는 안 보이는"</b> 상태가 된다.
 * 사용자에게는 시스템이 고장 난 것으로 보인다.</p>
 */
@SpringBootTest
@Transactional
class NoticeVisibilityTest {

    @Autowired NoticeVisibilityService noticeVisibilityService;
    @Autowired CourseQueryService courseQueryService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;

    private Course newCourse(String tag) {
        return courseRepository.save(Course.builder()
                .courseCode(tag + "-" + System.nanoTime())
                .courseName(tag + " 과정")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .build());
    }

    @Test
    @DisplayName("반려·취소된 수강 과정의 공지는 보이지 않는다")
    void 반려취소는_제외() {
        User trainee = userRepository.save(User.builder()
                .loginId("vis-" + System.nanoTime())
                .password("{noop}test").name("노출검증")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build());

        Course approved = newCourse("APPROVED");
        Course rejected = newCourse("REJECTED");
        Course cancelled = newCourse("CANCELLED");
        Course applied = newCourse("APPLIED");

        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(approved).status(EnrollmentStatus.APPROVED).build());
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(rejected).status(EnrollmentStatus.REJECTED).build());
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(cancelled).status(EnrollmentStatus.CANCELLED).build());
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(applied).status(EnrollmentStatus.APPLIED).build());
        enrollmentRepository.flush();

        assertThat(noticeVisibilityService.traineeCourseIds(trainee.getId()))
                .as("반려·취소·신청중 과정의 공지가 보이면 안 된다")
                .containsExactly(approved.getId());
    }

    @Test
    @DisplayName("A 의 수강 판정과 결과가 일치한다 — 기준이 갈리면 안 된다")
    void A와_기준_일치() {
        User trainee = userRepository.save(User.builder()
                .loginId("vis2-" + System.nanoTime())
                .password("{noop}test").name("기준검증")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build());

        Course course = newCourse("MATCH");
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(course).status(EnrollmentStatus.APPROVED).build());
        enrollmentRepository.flush();

        boolean visibleToNotice = noticeVisibilityService.traineeCourseIds(trainee.getId())
                .contains(course.getId());
        boolean enrolledPerA = courseQueryService.findUserIdsByCourseId(course.getId())
                .contains(trainee.getId());

        assertThat(visibleToNotice)
                .as("공지 노출 기준과 A 의 수강생 명단 기준이 다르면 "
                        + "'공지는 보이는데 과제는 안 보이는' 상태가 된다")
                .isEqualTo(enrolledPerA);
    }
}
