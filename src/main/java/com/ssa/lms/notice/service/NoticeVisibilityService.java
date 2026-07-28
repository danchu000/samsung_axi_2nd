package com.ssa.lms.notice.service;

import com.ssa.lms.course.entity.CourseInstructor;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자가 볼 수 있는 과정 범위를 구한다 (공지 노출 판정용).
 *
 * A의 CourseQueryService 는 "과정 → 사용자" 방향(findUserIdsByCourseId)만 제공한다.
 * 공지 목록은 "사용자 → 과정" 역방향이 필요해서 여기서 조회한다.
 * A에게 CourseQueryService.findCourseIdsByUserId 추가를 요청해 두었고, 들어오면 이 클래스는 걷어낸다.
 * (docs/a-requests.md 참고)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeVisibilityService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    /**
     * 훈련생이 수강 중인 과정 id.
     *
     * <p><b>승인(APPROVED)·수료(COMPLETED) 상태만 센다.</b> 예전에는 상태를 보지 않아
     * 신청만 하고 반려(REJECTED)·취소(CANCELLED)된 과정의 공지까지 보였다.
     * A 의 {@code CourseQueryService.findUserIdsByCourseId} 도 같은 두 상태만 인정하므로
     * 기준을 맞춘다 — 한쪽만 다르면 "공지는 보이는데 과제는 안 보이는" 상태가 된다.</p>
     */
    public List<Long> traineeCourseIds(Long userId) {
        return enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(userId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .map(Enrollment::getCourse)
                .map(c -> c.getId())
                .distinct()
                .toList();
    }

    /** 강사가 담당하는 과정 id. */
    public List<Long> instructorCourseIds(Long userId) {
        return courseInstructorRepository.findByInstructorId(userId).stream()
                .map(CourseInstructor::getCourse)
                .map(c -> c.getId())
                .distinct()
                .toList();
    }

    /** 공지 상세도 목록과 동일한 노출 규칙을 적용한다. */
    public boolean canView(Long courseId, List<Long> visibleCourseIds) {
        return courseId == null || visibleCourseIds.contains(courseId);
    }
}
