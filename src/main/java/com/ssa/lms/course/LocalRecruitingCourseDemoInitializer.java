package com.ssa.lms.course;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.entity.Subject;
import com.ssa.lms.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * local 프로필 모집중 과정 시연 시드 ({@code @Order(21)}).
 *
 * <p>{@code CourseDemoDataInitializer}(@Order(20)) 가 만드는 모집중 과정은 2개뿐이고 그중 하나는
 * trainee1 이 이미 신청한 상태라, 훈련생 "나의 과정 &gt; 모집중 과정 신청" 목록에 실제로 뜨는 건
 * 1개다. 신청 → 취소 → 재신청을 눌러 보면 목록이 금세 바닥나 시연이 끊긴다. 여기서 신청 가능한
 * 과정을 3개 더 깔아 둔다.</p>
 *
 * <p><b>trainee1 의 수강신청(Enrollment)을 만들지 않는다</b> — {@code EnrollmentService.openCoursesFor}
 * 가 이미 신청·승인·수료한 과정을 빼고 보여주므로, 신청을 미리 심으면 목록에서 사라져 이 시드의
 * 목적이 없어진다. 담당 강사도 지정하지 않는다(모집 단계라 배정 전).</p>
 *
 * <p><b>⚠ 시작일을 COURSE-2026-003(현재 +30일)보다 뒤로 두지 말 것.</b>
 * {@code ExamListScopeTest} 는 "강사 미담당 과정" 중 <b>시작일 내림차순 첫 번째</b>를 골라
 * 관리자에게는 시험·과제가 보여야 한다고 검증한다({@code CourseQueryService.findAllCourseOptions()}
 * 가 시작일 내림차순). 지금 그 자리는 시험·과제가 심어진 COURSE-2026-003 이다. 여기 과정들은
 * 담당 강사가 없고 시험·과제도 없으므로, 시작일이 003 보다 미래가 되는 순간 그 자리를 빼앗아
 * 해당 테스트가 "데이터가 없다"며 깨진다. 아래 날짜는 모두 +30일보다 앞이다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(21)
@RequiredArgsConstructor
public class LocalRecruitingCourseDemoInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (courseRepository.findByCourseCode("COURSE-2026-004").isPresent()) {
            return;   // 이미 생성됨
        }

        Course ai = Course.builder()
                .courseCode("COURSE-2026-004")
                .courseName("생성형 AI 서비스 개발 과정 (LLM/RAG)")
                .cohort("1기").category("AI")
                .description("LLM API 연동과 RAG 검색 증강을 다루는 모집중 과정")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusMonths(4))
                .capacity(25).status(CourseStatus.RECRUITING).completionProgressRate(80)
                .build();
        Subject aiSubject = Subject.builder()
                .name("LLM 활용 기초").description("프롬프트/토큰/비용").orderNo(1).build();
        aiSubject.addSession(Session.builder().seq(1).name("1차시 - LLM 개요").learningMinutes(60).build());
        aiSubject.addSession(Session.builder().seq(2).name("2차시 - 프롬프트 설계").learningMinutes(60).build());
        ai.addSubject(aiSubject);
        courseRepository.save(ai);

        Course backend = Course.builder()
                .courseCode("COURSE-2026-005")
                .courseName("백엔드 개발자 심화 (Spring Boot/JPA)")
                .cohort("3기").category("웹개발")
                .description("JPA 연관관계와 쿼리 튜닝 중심의 모집중 과정")
                .startDate(LocalDate.now().plusDays(17))
                .endDate(LocalDate.now().plusMonths(5))
                .capacity(30).status(CourseStatus.RECRUITING).completionProgressRate(80)
                .build();
        Subject backendSubject = Subject.builder()
                .name("JPA 심화").description("연관관계/영속성 컨텍스트").orderNo(1).build();
        backendSubject.addSession(Session.builder().seq(1).name("1차시 - 엔티티 매핑").learningMinutes(60).build());
        backendSubject.addSession(Session.builder().seq(2).name("2차시 - 지연 로딩과 N+1").learningMinutes(60).build());
        backend.addSubject(backendSubject);
        courseRepository.save(backend);

        Course security = Course.builder()
                .courseCode("COURSE-2026-006")
                .courseName("정보보안 실무 (모의해킹/취약점 진단)")
                .cohort("1기").category("보안")
                .description("웹 취약점 진단과 보안 조치를 실습하는 모집중 과정")
                .startDate(LocalDate.now().plusDays(24))
                .endDate(LocalDate.now().plusMonths(4))
                .capacity(20).status(CourseStatus.RECRUITING).completionProgressRate(80)
                .build();
        courseRepository.save(security);

        log.info("[local] 모집중 과정 시연 시드 생성 완료 — COURSE-2026-004/005/006 (trainee1 미신청)");
    }
}
