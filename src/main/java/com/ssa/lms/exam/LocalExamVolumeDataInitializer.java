package com.ssa.lms.exam;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.repository.ExamRepository;
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
 * local 프로필 <b>시험 목록 페이징 검증용</b> 볼륨 시드 (개발자 B).
 *
 * <p><b>왜 따로 있나:</b> {@link LocalExamDataInitializer} 는 시험 2건만 심는다. 한 페이지(10건)도
 * 안 차서 서버 페이징이 도는지, 강사가 담당하지 않는 과정 시험이 정말 빠지는지 확인할 수 없다.
 * 기존 시드는 화면 더미를 옮긴 것이라 의미가 다르므로 건드리지 않고 볼륨만 여기서 덧붙인다.</p>
 *
 * <p><b>과정 배분이 핵심이다.</b> 강사 권한이 쿼리 단계로 내려갔는지 확인하려면
 * <em>강사가 담당하지 않는 과정</em>의 시험이 반드시 있어야 한다. instructor1(김강사)은
 * COURSE-2026-001 / 002 담당이고 COURSE-2026-003(클라우드 인프라 입문) 은 담당이 아니다.
 * 그래서 003 에도 시험을 심는다 — 관리자와 강사의 총 건수가 달라야 정상이다.</p>
 *
 * <p>{@code @EventListener(ApplicationReadyEvent.class)} 를 쓰는 이유는
 * {@link LocalExamDataInitializer} 와 같다 (A의 CommandLineRunner 시드 이후에 돌아야 한다).
 * 같은 이벤트 리스너끼리는 {@code @Order} 로 순서를 정하는데, 기존 시험 시드가
 * {@code examRepository.count() > 0} 로 스스로를 막으므로 이 파일이 <b>나중에</b> 돌아야 한다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalExamVolumeDataInitializer {

    /** 이 시드가 만든 시험임을 알아보는 표식. 두 번 돌지 않게 하는 데도 쓴다. */
    private static final String MARKER = "[샘플]";

    private final ExamRepository examRepository;

    @PersistenceContext
    private EntityManager em;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!em.createQuery("select count(e) from Exam e where e.examName like :m", Long.class)
                .setParameter("m", MARKER + "%")
                .getSingleResult().equals(0L)) {
            return;
        }

        Course c1 = courseByCode("COURSE-2026-001");   // instructor1 담당
        Course c2 = courseByCode("COURSE-2026-002");   // instructor1 담당
        Course c3 = courseByCode("COURSE-2026-003");   // 담당 강사 없음 — 강사에게 보이면 안 된다
        if (c1 == null || c2 == null || c3 == null) {
            log.warn("[local] 데모 과정이 없어 시험 볼륨 시드를 건너뛴다");
            return;
        }
        Long instructorId = firstId(
                "select u.id from User u where u.loginId = 'instructor1'");

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<Exam> exams = new ArrayList<>();

        // 담당 과정(001) 8건 — 상태를 섞어 상태 필터도 같이 검증할 수 있게 한다
        for (int i = 1; i <= 8; i++) {
            exams.add(exam(c1, instructorId, MARKER + " 풀스택 단원평가 " + i,
                    i % 3 == 0 ? Exam.ExamStatus.CLOSED : Exam.ExamStatus.SCHEDULED,
                    base.plusDays(i)));
        }
        // 담당 과정(002) 3건
        for (int i = 1; i <= 3; i++) {
            exams.add(exam(c2, instructorId, MARKER + " 데이터분석 단원평가 " + i,
                    Exam.ExamStatus.SCHEDULED, base.plusDays(10 + i)));
        }
        // 미담당 과정(003) 3건 — 강사 목록에 나오면 권한 사고다
        for (int i = 1; i <= 3; i++) {
            exams.add(exam(c3, null, MARKER + " 클라우드 단원평가 " + i,
                    i == 1 ? Exam.ExamStatus.CLOSED : Exam.ExamStatus.SCHEDULED,
                    base.plusDays(20 + i)));
        }

        examRepository.saveAll(exams);
        log.info("[local] 시험 볼륨 시드 {}건 추가 (총 {}건) — 001:8 / 002:3 / 003:3(강사 미담당)",
                exams.size(), examRepository.count());
    }

    /* ===== 내부 ===== */

    private Exam exam(Course course, Long instructorId, String name,
                      Exam.ExamStatus status, LocalDateTime windowStart) {
        return Exam.builder()
                .examName(name)
                .examType(Exam.ExamType.UNIT)
                .course(course)
                .instructor(instructorId == null ? null
                        : em.getReference(com.ssa.lms.user.entity.User.class, instructorId))
                .timeLimitMin(30)
                .autoScore(100).manualScore(0).totalScore(100).passScore(60)
                .randomOrder(false)
                .retakeAllowed(false).maxAttempts(1)
                .windowStart(windowStart)
                .windowEnd(windowStart.plusHours(1))
                .requireIdentityVerification(false)
                .proctorEnabled(false).requireWebcam(false)
                .blockTabSwitch(false).blockCopyPaste(false)
                .note("페이징·권한 검증용 샘플 시험")
                .status(status)
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
