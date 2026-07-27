package com.ssa.lms.course;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * local 프로필 과정 도메인 데모 시드 (A-2 트랙, {@code @Order(20)}).
 *
 * <p>기본 시더({@code LocalDataInitializer @Order(0)})가 만든 계정/데모 과정에 의존한다.
 * {@code LocalDataInitializer} 는 동결 대상이므로 수정하지 않고 여기에 도메인 데이터를 덧붙인다
 * (협업 규칙: PARALLEL.md §공유 파일 규칙 3).</p>
 *
 * <ul>
 *   <li>모집중 과정 2개 추가 — 훈련생 수강신청/관리자 승인 흐름 데모용</li>
 *   <li>과정 하나에는 trainee1 의 신청(APPLIED)을 넣어 관리자 승인 대상이 존재하게 한다</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("local")
@Order(20)
@RequiredArgsConstructor
public class CourseDemoDataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (courseRepository.findByCourseCode("COURSE-2026-002").isPresent()) {
            return;   // 이미 생성됨
        }
        User instructor = userRepository.findByLoginId("instructor1").orElse(null);
        User trainee = userRepository.findByLoginId("trainee1").orElse(null);
        if (instructor == null || trainee == null) {
            log.warn("[local] 기본 시드 계정이 없어 과정 데모 시드를 건너뜁니다.");
            return;
        }

        // 모집중 과정 1 — trainee1 이 이미 신청(APPLIED)한 상태 → 관리자 승인 데모
        Course recruiting1 = Course.builder()
                .courseCode("COURSE-2026-002")
                .courseName("데이터 분석 실무 (Python/Pandas)")
                .cohort("2기").category("데이터")
                .description("데이터 수집·전처리·시각화 실무 모집중 과정")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusMonths(4))
                .capacity(25).status(CourseStatus.RECRUITING).completionProgressRate(80)
                .build();
        Subject s1 = Subject.builder().name("파이썬 기초").description("문법/자료구조").orderNo(1).build();
        s1.addSession(Session.builder().seq(1).name("1차시 - 환경설정").learningMinutes(60).build());
        s1.addSession(Session.builder().seq(2).name("2차시 - 자료구조").learningMinutes(60).build());
        recruiting1.addSubject(s1);
        courseRepository.save(recruiting1);
        courseInstructorRepository.save(CourseInstructor.builder()
                .course(recruiting1).instructor(instructor).primaryInstructor(true).build());
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(recruiting1)
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now().minusDays(1)).build());

        // 모집중 과정 2 — trainee1 미신청 → 훈련생 "모집중 과정 신청" 데모
        Course recruiting2 = Course.builder()
                .courseCode("COURSE-2026-003")
                .courseName("클라우드 인프라 입문 (AWS)")
                .cohort("1기").category("클라우드")
                .description("EC2/S3/VPC 기초를 다루는 모집중 과정")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusMonths(5))
                .capacity(20).status(CourseStatus.RECRUITING).completionProgressRate(80)
                .build();
        courseRepository.save(recruiting2);

        log.info("[local] 과정 도메인 데모 시드 생성 완료 — 모집중 과정 COURSE-2026-002/003");
    }
}
