package com.ssa.lms.assignment;

import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.entity.AssignmentCriteria;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.repository.AssignmentRepository;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.exam.entity.Difficulty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * local 프로필 전용 과제 시드 (개발자 B).
 *
 * 기존 정적 화면의 더미 배열을 옮긴 것이다:
 *  - static/js/assignments.js 의 assignmentData (관리자 과제 목록)
 *  - trainee/assignments.html 인라인 TASKS (훈련생 카드)
 *  - admin-evaluation-assignment-add.html 의 assignments 더미 (콘텐츠 은행 후보)
 *
 * A의 {@code LocalDataInitializer}, B의 {@code LocalQuestionDataInitializer} 와
 * 파일을 나눠 두어 서로 충돌하지 않는다.
 *
 * <p><b>CommandLineRunner 를 쓰지 않는 이유</b> — A의 {@code LocalDataInitializer}(사용자·과정 시드)에
 * {@code @Order} 가 없어 LOWEST_PRECEDENCE 로 잡힌다. 여기서 CommandLineRunner 를 쓰면 A보다
 * 먼저 돌아 Course/User 가 아직 없는 상태가 되고, 배정할 과정을 못 찾아 시드가 조용히 반쪽만 만들어진다.
 * {@code ApplicationReadyEvent} 는 모든 CommandLineRunner 가 끝난 뒤에 발생하므로 안전하다.</p>
 *
 * <p>MySQL(dev 프로필)용 data.sql 이관은 스키마 확정 후 별도로 진행한다.</p>
 *
 * <p><b>가드는 마커(과제 제목) 기반이다.</b> {@code @EventListener} 는 {@code @Order} 가 지정된
 * 리스너가 무순서(LOWEST_PRECEDENCE) 리스너보다 <b>먼저</b> 실행된다. 그래서
 * {@code LocalAssignmentVolumeDataInitializer}(@Order(100))가 이 시더보다 먼저 돌아 과제 정의를
 * 만들면, 과거의 {@code assignmentRepository.count() > 0} 가드는 이 시드를 통째로 건너뛰게 했다
 * (과제 정의 3종과 AssignmentCriteria 가 사라지는 버그). 지금은 "내가 만드는 과제 정의"의
 * 존재 여부로만 판정한다.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAssignmentDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커 과제 제목. */
    private static final String MARKER_TITLE = "REST API 구현 과제";

    private final AssignmentRepository assignmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final AssignmentLookupRepository lookupRepository;

    @PersistenceContext
    private EntityManager em;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        // 볼륨 시더가 먼저 과제를 만들어도 이 시드는 죽으면 안 된다 — count 가드 금지.
        Long already = em.createQuery(
                        "select count(a) from Assignment a where a.title = :t", Long.class)
                .setParameter("t", MARKER_TITLE)
                .getSingleResult();
        if (already > 0) {
            return;
        }

        Assignment a1 = Assignment.builder()
                .title(MARKER_TITLE)
                .description("Spring Boot 로 게시판 CRUD REST API 를 구현하고 소스와 설명을 제출하세요.")
                .defaultSubmissionType(CourseAssignment.SubmissionType.DOCUMENT_TEXT)
                .maxScore(100)
                .category("KDT")
                .difficulty(Difficulty.MEDIUM)
                .status(Assignment.AssignmentStatus.ACTIVE)
                .build();

        Assignment a2 = Assignment.builder()
                .title("React 컴포넌트 실습")
                .description("재사용 가능한 컴포넌트 3개를 만들고 배포 링크를 제출하세요.")
                .defaultSubmissionType(CourseAssignment.SubmissionType.LINK)
                .maxScore(100)
                .category("KDT")
                .difficulty(Difficulty.EASY)
                .status(Assignment.AssignmentStatus.ACTIVE)
                .build();

        Assignment a3 = Assignment.builder()
                .title("데이터 전처리 정리")
                .description("결측치 처리와 정규화 과정을 10줄 이내로 정리해 제출하세요.")
                .defaultSubmissionType(CourseAssignment.SubmissionType.TEXT)
                .maxScore(50)
                .category("고교위탁")
                .difficulty(Difficulty.HARD)
                .status(Assignment.AssignmentStatus.ARCHIVED)
                .build();

        assignmentRepository.saveAll(List.of(a1, a2, a3));

        // A의 시드 과정/강사가 있어야 배정을 만들 수 있다. 없으면 정의만 넣고 끝낸다.
        Long courseId = firstId("select c.id from Course c order by c.id asc");
        Long instructorId = firstId(
                "select u.id from User u where u.role = com.ssa.lms.user.entity.Role.INSTRUCTOR order by u.id asc");
        if (courseId == null || instructorId == null) {
            log.info("[local] 과제 정의 {}건 생성 (배정 대상 과정/강사 없음)", assignmentRepository.count());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        CourseAssignment ca1 = CourseAssignment.builder()
                .course(lookupRepository.courseRef(courseId))
                .assignment(a1)
                .submissionType(CourseAssignment.SubmissionType.DOCUMENT_TEXT)
                .startAt(now.minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .endAt(now.plusDays(7).withHour(23).withMinute(59).withSecond(0).withNano(0))
                .allowLate(true)
                .allowResubmit(true)
                .maxResubmit(2)
                .autoGrading(false)
                .score(100)
                .passScore(70)
                .grader(lookupRepository.userRef(instructorId))
                .status(CourseAssignment.CourseAssignmentStatus.OPEN)
                .build();
        ca1.addCriteria(criteria(1, "요구사항 충족", 50));
        ca1.addCriteria(criteria(2, "코드 품질", 30));
        ca1.addCriteria(criteria(3, "문서화", 20));

        CourseAssignment ca2 = CourseAssignment.builder()
                .course(lookupRepository.courseRef(courseId))
                .assignment(a2)
                .submissionType(CourseAssignment.SubmissionType.LINK)
                .startAt(now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                .endAt(now.plusDays(14).withHour(23).withMinute(59).withSecond(0).withNano(0))
                .allowLate(false)
                .allowResubmit(false)
                .maxResubmit(0)
                .autoGrading(false)
                .score(100)
                .passScore(60)
                .grader(lookupRepository.userRef(instructorId))
                .status(CourseAssignment.CourseAssignmentStatus.OPEN)
                .build();

        courseAssignmentRepository.saveAll(List.of(ca1, ca2));
        log.info("[local] 과제 시드 — 정의 {}건, 배정 {}건",
                assignmentRepository.count(), courseAssignmentRepository.count());
    }

    private AssignmentCriteria criteria(int seq, String content, int score) {
        return AssignmentCriteria.builder().seq(seq).content(content).score(score).build();
    }

    /** A 소유 리포지토리를 주입하지 않기 위해 읽기 전용 JPQL 로만 조회한다. */
    private Long firstId(String jpql) {
        return em.createQuery(jpql, Long.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
