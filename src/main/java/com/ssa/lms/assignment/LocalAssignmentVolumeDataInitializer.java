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

    /**
     * 두 번 돌지 않게 하는 표식. 이 시드가 만드는 첫 과제 정의({@link #C1_DEFS} 의 0번) 제목과 같아야 한다.
     *
     * <p>예전에는 {@code "[샘플] 주차별 실습 과제"} 정의 <b>하나</b>에 배정 12건을 전부 매달았다.
     * 그래서 강사 대시보드 "오늘의 평가 업무" 카드에 똑같은 제목이 3건 반복돼 자동생성 티가 났다.
     * 제목은 배정이 아니라 <b>정의</b>에 있으므로, 문자열만 바꿔서는 해결되지 않는다.
     * 과정별로 정의를 나눠 만들고 배정이 서로 다른 정의를 참조하게 한다.
     * 가드는 기존 시드({@link LocalAssignmentDataInitializer} 의 "REST API 구현 과제") 와 같은
     * 방식으로 <b>실제 제목 하나를 표식으로 삼는다</b> — 기능은 그대로다.</p>
     */
    private static final String GUARD_TITLE = "1주차 개발 환경 구축 및 Git 기초 실습";

    /** 과제 정의 한 건. 화면에 보이는 제목·설명만 담는다. */
    private record Def(String title, String description) {
    }

    /** COURSE-2026-001(풀스택) 배정 6건이 쓸 정의. 0번 제목은 {@link #GUARD_TITLE} 와 일치해야 한다. */
    private static final List<Def> C1_DEFS = List.of(
            new Def("1주차 개발 환경 구축 및 Git 기초 실습",
                    "JDK·IDE 설치 화면과 저장소 초기 커밋 로그를 캡처해 제출하세요."),
            new Def("2주차 HTML/CSS 반응형 레이아웃 구현",
                    "제공된 시안을 데스크톱·모바일 두 폭으로 구현하고 소스를 제출하세요."),
            new Def("3주차 JavaScript 비동기 처리 실습",
                    "목록을 비동기로 불러와 렌더링하고 오류 처리까지 구현해 제출하세요."),
            new Def("4주차 REST API 설계 과제",
                    "게시판 도메인의 CRUD 엔드포인트를 설계하고 요청·응답 예시를 정리해 제출하세요."),
            new Def("5주차 JPA 연관관계 매핑 실습",
                    "1:N 관계를 매핑하고 조회 성능 이슈를 확인한 뒤 개선 방법을 정리해 제출하세요."),
            new Def("6주차 팀 프로젝트 중간 산출물 제출",
                    "요구사항 정의서와 화면 설계서를 팀별로 정리해 제출하세요.")
    );

    /** COURSE-2026-002(데이터분석) 배정 3건이 쓸 정의. */
    private static final List<Def> C2_DEFS = List.of(
            new Def("3주차 Pandas 데이터 전처리 실습",
                    "결측치·이상치 처리 과정을 노트북 파일로 정리해 제출하세요."),
            new Def("5주차 데이터 시각화 리포트 작성",
                    "핵심 지표 3개를 선정해 시각화하고 해석을 덧붙여 제출하세요."),
            new Def("7주차 머신러닝 모델 학습 및 평가",
                    "모델 두 개를 학습시켜 성능을 비교하고 선택 근거를 작성해 제출하세요.")
    );

    /** COURSE-2026-003(클라우드, 강사 미담당) 배정 3건이 쓸 정의. */
    private static final List<Def> C3_DEFS = List.of(
            new Def("2주차 컨테이너 이미지 빌드 실습",
                    "애플리케이션 이미지를 빌드하고 실행 로그를 첨부해 제출하세요."),
            new Def("4주차 배포 파이프라인 구성",
                    "빌드·테스트·배포 단계를 구성하고 실행 결과를 캡처해 제출하세요."),
            new Def("6주차 클라우드 인프라 구성도 작성",
                    "네트워크·컴퓨트·스토리지 구성을 도식화해 제출하세요.")
    );

    private final AssignmentRepository assignmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;

    @PersistenceContext
    private EntityManager em;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        Long already = em.createQuery(
                        "select count(a) from Assignment a where a.title = :t", Long.class)
                .setParameter("t", GUARD_TITLE)
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

        List<Assignment> c1Defs = saveDefs(C1_DEFS);
        List<Assignment> c2Defs = saveDefs(C2_DEFS);
        List<Assignment> c3Defs = saveDefs(C3_DEFS);

        LocalDateTime now = LocalDateTime.now();
        List<CourseAssignment> rows = new ArrayList<>();

        // --- 리마인드 3단계에 각각 걸리는 배정 (모두 담당 과정 001, 상태 OPEN) ---
        // 이 3건이 강사 대시보드 "오늘의 평가 업무" 에 뜨므로 정의를 반드시 서로 다르게 준다.
        rows.add(assign(c1, c1Defs.get(0), instructorId,
                now.minusDays(2), now.plusHours(24).plusMinutes(30)));   // 마감 24시간 전
        rows.add(assign(c1, c1Defs.get(1), instructorId,
                now.minusDays(2), now.plusHours(1).plusMinutes(30)));    // 마감 1시간 전
        rows.add(assign(c1, c1Defs.get(2), instructorId,
                now.minusDays(5), now.minusDays(1)));                    // 마감 지남 — 독려

        // --- 페이징용 볼륨 ---
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c1, c1Defs.get(2 + i), instructorId,
                    now.minusDays(i), now.plusDays(20 + i)));
        }
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c2, c2Defs.get(i - 1), instructorId,
                    now.minusDays(i), now.plusDays(30 + i)));
        }
        // 강사 미담당 과정 — 강사 목록에 나오면 권한 사고다
        for (int i = 1; i <= 3; i++) {
            rows.add(assign(c3, c3Defs.get(i - 1), null,
                    now.minusDays(i), now.plusDays(40 + i)));
        }

        courseAssignmentRepository.saveAll(rows);
        log.info("[local] 과제 볼륨 시드 {}건 추가 (총 {}건) — 리마인드 3단계 대상 포함, 003 은 강사 미담당",
                rows.size(), courseAssignmentRepository.count());
    }

    /* ===== 내부 ===== */

    /** 정의 목록을 저장한다. 제목·설명만 다르고 나머지 속성은 예전 단일 정의와 동일하게 둔다. */
    private List<Assignment> saveDefs(List<Def> defs) {
        List<Assignment> saved = new ArrayList<>();
        for (Def d : defs) {
            saved.add(assignmentRepository.save(Assignment.builder()
                    .title(d.title())
                    .description(d.description())
                    .defaultSubmissionType(CourseAssignment.SubmissionType.TEXT)
                    .maxScore(100)
                    .category("KDT")
                    .difficulty(Difficulty.EASY)
                    .status(Assignment.AssignmentStatus.ACTIVE)
                    .build()));
        }
        return saved;
    }

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
