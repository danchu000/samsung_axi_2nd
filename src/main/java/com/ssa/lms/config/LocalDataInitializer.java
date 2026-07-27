package com.ssa.lms.config;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import com.ssa.lms.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * local 프로필 전용 <b>기본(core) 시드</b> — 개발용 계정 3종과 데모 과정 1개.
 * 계정: admin / instructor1 / trainee1, 비밀번호는 모두 "1234".
 *
 * <p><b>이 파일은 병렬 작업 중 동결(freeze)한다.</b> 도메인별 시드가 필요하면 이 파일을 수정하지 말고
 * 별도의 {@code @Component @Profile("local") @Order(n>0)} CommandLineRunner 를 새로 추가할 것.
 * {@code @Order(0)} 이라 항상 가장 먼저 실행되므로, 도메인 시더는 여기서 만든 계정/데모 과정에
 * 의존해도 된다. (협업 규칙: PARALLEL.md)</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(0)
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .loginId("admin").password(passwordEncoder.encode("1234"))
                .name("관리자").role(Role.ADMIN).status(UserStatus.ACTIVE)
                .email("admin@ssa.local").phone("010-0000-0000").birthDate("1990-01-01")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build());

        User instructor = userRepository.save(User.builder()
                .loginId("instructor1").password(passwordEncoder.encode("1234"))
                .name("김강사").role(Role.INSTRUCTOR).status(UserStatus.ACTIVE)
                .email("instructor1@ssa.local").phone("010-1111-1111").birthDate("1985-03-15")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build());

        User trainee = userRepository.save(User.builder()
                .loginId("trainee1").password(passwordEncoder.encode("1234"))
                .name("이훈련").role(Role.TRAINEE).status(UserStatus.ACTIVE)
                .email("trainee1@ssa.local").phone("010-2222-2222").birthDate("1999-07-07")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build());

        Course course = Course.builder()
                .courseCode("COURSE-2026-001")
                .courseName("클라우드 기반 풀스택 개발자 양성과정")
                .cohort("1기")
                .category("웹개발")
                .description("Spring Boot + AWS 풀스택 개발자 양성 데모 과정")
                .startDate(LocalDate.now().minusDays(7))
                .endDate(LocalDate.now().plusMonths(5))
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .completionProgressRate(80)
                .build();

        Subject subject = Subject.builder().name("백엔드 기초").description("Java/Spring 입문").orderNo(1).build();
        subject.addSession(Session.builder().seq(1).name("1차시 - 개발환경 구축")
                .lessonDate(LocalDate.now().minusDays(6)).learningMinutes(60).build());
        subject.addSession(Session.builder().seq(2).name("2차시 - Java 문법 기초")
                .lessonDate(LocalDate.now().minusDays(5)).learningMinutes(60).build());
        course.addSubject(subject);
        courseRepository.save(course);

        courseInstructorRepository.save(CourseInstructor.builder()
                .course(course).instructor(instructor).primaryInstructor(true).build());

        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(course)
                .status(EnrollmentStatus.APPROVED).appliedAt(LocalDateTime.now().minusDays(8))
                .build());

        log.info("[local] 시드 데이터 생성 완료 — 계정: admin / instructor1 / trainee1 (pw: 1234)");
    }
}
