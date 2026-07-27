package com.ssa.lms.course.service;

import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * B 도메인(시험/과제/설문 등)이 호출하는 과정 조회 계약 (a-requests.md P0-4).
 * B 는 A 소유 리포지토리를 직접 쓰지 말고 이 서비스만 호출할 것.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    /**
     * 과정 수강생 명단 — 응시 대상자, 미제출자 목록, 설문 배포 대상 산출용.
     * 승인(APPROVED)·수료(COMPLETED) 상태만 포함한다 (신청/반려/취소 제외).
     */
    public List<Long> findUserIdsByCourseId(Long courseId) {
        return enrollmentRepository.findTraineeIdsByCourseIdAndStatusIn(
                courseId, List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED));
    }

    /** 강사가 해당 과정 담당인지 — 권한정의서 △(담당 과정 한정) 판정용. */
    public boolean isInstructorOf(Long userId, Long courseId) {
        return courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, userId);
    }
}
