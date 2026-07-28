package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.dto.CourseOption;
import com.ssa.lms.assignment.dto.UserOption;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 과제 화면이 필요로 하는 A 도메인 참조 데이터(과정 셀렉트, 채점자 셀렉트, 훈련생 이름) 조회.
 *
 * <p><b>왜 여기에 있나</b> — A 소유 리포지토리({@code CourseRepository},
 * {@code UserRepository})를 직접 주입하지 않기로 한 규칙(CLAUDE.md)을 지키면서도
 * 화면을 띄우려면 읽을 방법이 필요했다. {@code CourseQueryService} 에는 지금
 * {@code findUserIdsByCourseId} / {@code isInstructorOf} 두 개뿐이라 과정 목록·이름을
 * 얻을 수 없다.</p>
 *
 * <p><b>임시 조치다.</b> A에게 아래 3개를 {@code CourseQueryService} 에 추가해 달라고
 * 요청해 둔 상태이고, 추가되면 이 클래스는 통째로 지우고 그 서비스를 호출한다:
 * <ul>
 *   <li>선택 가능한 과정 목록 (id, courseCode, courseName, cohort)</li>
 *   <li>과정 담당 강사 목록 (채점자 셀렉트)</li>
 *   <li>userId → 표시용 이름/로그인ID 매핑 (미제출자 목록)</li>
 * </ul>
 * 그 전까지는 읽기 전용 JPQL 만 쓰고 A 엔티티를 수정하지 않는다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AssignmentLookupRepository {

    private final com.ssa.lms.course.service.CourseQueryService courseQueryService;

    @PersistenceContext
    private EntityManager em;

    /** 과정 셀렉트 옵션 — 최근 시작 과정 우선. */
    /**
     * 과정 셀렉트 옵션 — A 의 {@code CourseQueryService.findAllCourseOptions()} 로 위임한다.
     *
     * <p>예전에는 A 가 이 조회를 제공하지 않아 여기서 직접 JPQL 을 돌렸다.
     * 이제 공식 조회가 생겼으므로 단일 출처로 모은다. 반환 타입만 화면용으로 감싼다.</p>
     */
    public List<CourseOption> findCourseOptions() {
        return courseQueryService.findAllCourseOptions().stream()
                .map(c -> new CourseOption(c.id(), c.courseCode(), c.courseName(), c.cohort()))
                .toList();
    }

    public List<UserOption> findInstructorOptions() {
        return em.createQuery("""
                        select new com.ssa.lms.assignment.dto.UserOption(u.id, u.loginId, u.name)
                        from User u
                        where u.role = com.ssa.lms.user.entity.Role.INSTRUCTOR
                        order by u.name asc
                        """, UserOption.class)
                .getResultList();
    }

    /** userId → 표시용 정보. 미제출자 목록처럼 id 만 있는 상태에서 이름을 붙일 때 쓴다. */
    public Map<Long, UserOption> findUserOptions(Collection<Long> userIds) {
        Map<Long, UserOption> result = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        em.createQuery("""
                        select new com.ssa.lms.assignment.dto.UserOption(u.id, u.loginId, u.name)
                        from User u
                        where u.id in :ids
                        order by u.name asc
                        """, UserOption.class)
                .setParameter("ids", userIds)
                .getResultList()
                .forEach(o -> result.put(o.id(), o));
        return result;
    }

    /**
     * FK 만 세우면 되는 자리에 쓰는 프록시 참조.
     * A 소유 엔티티를 읽거나 고치지 않고 연관만 걸기 위한 것이다.
     */
    public com.ssa.lms.course.entity.Course courseRef(Long courseId) {
        return em.getReference(com.ssa.lms.course.entity.Course.class, courseId);
    }

    public com.ssa.lms.user.entity.User userRef(Long userId) {
        return em.getReference(com.ssa.lms.user.entity.User.class, userId);
    }

    /** 훈련생이 수강 중(승인/수료)인 과정 id 목록. */
    public List<Long> findEnrolledCourseIds(Long traineeId) {
        return em.createQuery("""
                        select e.course.id from Enrollment e
                        where e.trainee.id = :traineeId
                          and e.status in (com.ssa.lms.course.entity.EnrollmentStatus.APPROVED,
                                           com.ssa.lms.course.entity.EnrollmentStatus.COMPLETED)
                        """, Long.class)
                .setParameter("traineeId", traineeId)
                .getResultList();
    }
}
