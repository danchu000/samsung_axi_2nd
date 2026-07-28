package com.ssa.lms.assignment;

import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.repository.AssignmentRepository;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.exam.entity.Difficulty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * local 프로필 <b>과제 목록 페이징 + 독려 리마인드 검증용</b> 볼륨 시드 (개발자 B).
 *
 * <p>두 가지를 동시에 만든다.</p>
 * <ol>
 *   <li><b>페이징</b> — {@link LocalAssignmentDataInitializer} 는 배정 2건뿐이라 한 페이지도 안 찬다.
 *       12건을 덧붙여 2페이지가 되게 하고, 강사 미담당 과정(COURSE-2026-003)에도 배정을 심어
 *       강사 목록에서 빠지는지 확인할 수 있게 한다.</li>
 *   <li><b>리마인드 대상</b> — {@code ReminderService} 는 마감이
 *       {@code [now+lead, now+lead+1h)} 구간에 드는 것만 잡는다. 기존 시드의 마감(7일/14일 뒤)은
 *       어느 구간에도 안 걸려서 스케줄러를 돌려도 아무것도 안 나간다. 세 단계
 *       (24시간 전 / 1시간 전 / 마감 후)에 각각 하나씩 걸리도록 마감을 <b>실행 시각 기준 상대값</b>으로
 *       잡는다. 30분 여유를 둔 것은 스케줄러가 부팅 직후 몇 초 뒤에 돌기 때문이다.</li>
 * </ol>
 *
 * <p>기존 시드 파일은 화면 더미를 옮긴 것이라 의미가 달라 건드리지 않는다.
 * {@code @EventListener(ApplicationReadyEvent.class)} 를 쓰는 이유는 같은 패키지의 기존 시드와 같다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalAssignmentVolumeDataInitializer {

    /** 이 시드가 만든 과제 정의 제목. 두 번 돌지 않게 하는 표식으로도 쓴다. */
    private static final String SAMPLE_TITLE = "[샘플] 주차별 실습 과제";

    private final AssignmentRepository assignmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;

    @PersistenceContext
    private EntityManager em;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        Long already = em.createQuery(
                        "select count(a) from Assignment a where a.title = :t", Long.class)
                .setParameter("t", SAMPLE_TITLE)
                .getSingleResult();
        if (already > 0) {
            return;
        }

        Course c1 = courseByCode("COURSE-2026-001");   // instructor1 담당
        Course c2 = courseByCode("COURSE-2026-002");   // instructor1 담당
        Course c3 = courseByCode("COURSE-2026-003");   // 담당 강사 없음
        if (c1 == null || c2 == null || c3 == null) {
            log.warn("[local] 데모 과정이 없어 과제 볼륨 시드를 건너뛴다");
            return;
        }
        Long instructorId = firstId("select u.id from User u where u.loginId = 'instructor1'");

        Assignment definition = assignmentRepository.save(Assignment.builder()
                .title(SAMPLE_TITLE)
                .description("페이징·리마인드 검증용 샘플 과제 정의")
                .defaultSubmissionType(CourseAssignment.SubmissionType.TEXT)
                .maxScore(100)
                .category("KDT")
                .difficulty(Difficulty.EASY)
                .status(Assignment.AssignmentStatus.ACTIVE)
                .build());

        LocalDateTime now = LocalDateTime.now();
        List<CourseAssignment> rows = new ArrayList<>();

        // --- 리마인드 3단계에 각각 걸리는 배정 (모두 담당 과정 001, 상태 OPEN) ---
        rows.add(assign(c1, definition, instructorId,
                now.minusDays(2), now.plusHours(24).plusMinutes(30)));   // 마감 24시간 전
        rows.add(assign(c1, definition, instructorId,
                now.minusDays(2), now.plusHours(1).plusMinutes(30)));    // 마감 1시간 전
        rows.add(assign(c1, definition, instructorId,
                now.minusDays(5), now.minusDays(1)));                    // 마감 지남 — 독려

        // --- 페이징용 볼륨 ---
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c1, definition, instructorId,
                    now.minusDays(i), now.plusDays(20 + i)));
        }
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c2, definition, instructorId,
                    now.minusDays(i), now.plusDays(30 + i)));
        }
        // 강사 미담당 과정 — 강사 목록에 나오면 권한 사고다
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c3, definition, null,
                    now.minusDays(i), now.plusDays(40 + i)));
        }

        courseAssignmentRepository.saveAll(rows);
        log.info("[local] 과제 볼륨 시드 {}건 추가 (총 {}건) — 리마인드 3단계 대상 포함, 003 은 강사 미담당",
                rows.size(), courseAssignmentRepository.count());
    }

    /* ===== 내부 ===== */

    private CourseAssignment assign(Course course, Assignment definition, Long graderId,
                                    LocalDateTime startAt, LocalDateTime endAt) {
        return CourseAssignment.builder()
                .course(course)
                .assignment(definition)
                .submissionType(CourseAssignment.SubmissionType.TEXT)
                .startAt(startAt)
                .endAt(endAt)
                .allowLate(true)
                .allowResubmit(false)
                .maxResubmit(0)
                .autoGrading(false)
                .score(100)
                .passScore(60)
                .grader(graderId == null ? null
                        : em.getReference(com.ssa.lms.user.entity.User.class, graderId))
                .status(CourseAssignment.CourseAssignmentStatus.OPEN)
                .build();
    }

    /** A 소유 리포지토리를 주입하지 않기 위해 읽기 전용 JPQL 로만 조회한다. */
    private Course courseByCode(String code) {
        return em.createQuery("select c from Course c where c.courseCode = :code", Course.class)
                .setParameter("code", code)
                .getResultStream().findFirst().orElse(null);
    }

    private Long firstId(String jpql) {
        return em.createQuery(jpql, Long.class)
                .setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
    }
}
