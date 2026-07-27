package com.ssa.lms.content.service;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.entity.Progress;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.content.repository.ProgressRepository;
import com.ssa.lms.content.web.LearningContentView;
import com.ssa.lms.content.web.ProgressRequest;
import com.ssa.lms.content.web.ProgressResponse;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 훈련생 학습 진도 — 기존 프론트 JS 진도 로직(동영상 재생 위치 / 문서 도달 페이지)을 서버에 영속화한다.
 * 이수 판정 조회는 {@link ProgressQueryServiceImpl} 가 이 도메인의 데이터를 읽어 계약({@link ProgressQueryService})을 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    /** 콘텐츠 1개를 "완료"로 간주하는 진도율 임계값(%). */
    public static final int COMPLETION_RATE = 90;

    private final ProgressRepository progressRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    /** 특정 콘텐츠에 대한 내 진도(없으면 0/미완료). */
    public ProgressResponse getProgress(Long userId, Long contentId) {
        Progress p = progressRepository.findByUserIdAndContentId(userId, contentId).orElse(null);
        return ProgressResponse.of(contentId, p);
    }

    /**
     * 진도 갱신 — 콘텐츠 유형에 따라 재생 위치/도달 페이지를 반영한다.
     * 전체 길이/페이지는 요청 값 우선, 없으면 콘텐츠 등록값 사용.
     */
    @Transactional
    public ProgressResponse record(Long userId, Long contentId, ProgressRequest req) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        Progress progress = getOrCreate(userId, content);

        if (content.getType() == ContentType.VIDEO) {
            int position = req.getPositionSeconds() != null ? req.getPositionSeconds() : 0;
            Integer duration = req.getDurationSeconds() != null
                    ? req.getDurationSeconds() : content.getDurationSeconds();
            progress.updateVideoProgress(position, duration, COMPLETION_RATE);
        } else {
            int page = req.getPageReached() != null ? req.getPageReached() : 0;
            Integer total = req.getTotalPages() != null ? req.getTotalPages() : content.getPageCount();
            progress.updateDocumentProgress(page, total, COMPLETION_RATE);
        }
        syncEnrollmentProgress(userId, content.getCourse().getId());
        return ProgressResponse.of(contentId, progress);
    }

    /** 수동 완료 처리(완료 버튼 등). */
    @Transactional
    public ProgressResponse complete(Long userId, Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        Progress progress = getOrCreate(userId, content);
        progress.markCompleted();
        syncEnrollmentProgress(userId, content.getCourse().getId());
        return ProgressResponse.of(contentId, progress);
    }

    /**
     * 과정 학습 콘텐츠 + 내 진도 목록 (활성 콘텐츠, 배치 순서). 차시별 아코디언/이어보기 화면용.
     */
    public List<LearningContentView> learningContents(Long userId, Long courseId) {
        List<Content> contents =
                contentRepository.findByCourseIdAndStatusOrderByOrderNoAscIdAsc(courseId, ContentStatus.ACTIVE);
        Map<Long, Progress> byContent = progressRepository.findByUserIdAndCourseId(userId, courseId).stream()
                .collect(Collectors.toMap(p -> p.getContent().getId(), Function.identity(), (a, b) -> a));
        return contents.stream()
                .map(c -> LearningContentView.of(c, byContent.get(c.getId())))
                .toList();
    }

    /**
     * 내가 학습 중인(승인/수료) 과정들의 콘텐츠 + 진도 전체 목록. 훈련생 "학습 콘텐츠" 화면용.
     */
    public List<LearningContentView> myLearningContents(Long userId) {
        List<LearningContentView> all = new ArrayList<>();
        for (Course course : enrolledCourses(userId)) {
            all.addAll(learningContents(userId, course.getId()));
        }
        return all;
    }

    /** 내가 학습 중인(승인/수료) 과정 목록. */
    public List<Course> enrolledCourses(Long userId) {
        return enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(userId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .map(Enrollment::getCourse)
                .toList();
    }

    /**
     * 과정 진도율(0~100) — 활성 콘텐츠 진도율의 평균(진도 기록이 없는 콘텐츠는 0으로 간주).
     * {@link ProgressQueryServiceImpl#completedRatio} 가 이 값을 그대로 사용한다.
     */
    public int courseProgressRatio(Long userId, Long courseId) {
        long total = contentRepository.countByCourseIdAndStatus(courseId, ContentStatus.ACTIVE);
        if (total == 0) {
            return 0;
        }
        long sum = progressRepository.sumProgressRate(userId, courseId, ContentStatus.ACTIVE);
        return (int) Math.min(100, Math.round((double) sum / total));
    }

    /**
     * 과정의 필수 콘텐츠(활성)를 모두 이수했는지. 필수 콘텐츠가 하나도 없으면 false
     * (아직 이수 대상이 구성되지 않은 것으로 본다).
     */
    public boolean hasCompletedAllRequired(Long userId, Long courseId) {
        long required = progressRepository.countRequiredContents(courseId, ContentStatus.ACTIVE);
        if (required == 0) {
            return false;
        }
        long completed = progressRepository.countCompletedRequired(userId, courseId, ContentStatus.ACTIVE);
        return completed >= required;
    }

    /**
     * 콘텐츠 진도 변화 시 Enrollment.progressRate(전체 진도율)를 함께 갱신한다(같은 A 도메인, 필드가 이 용도로 예약됨).
     * 최종 수료(이수) 판정 — 진도 + 출결 + 성적 — 및 상태 전이는 이수 트랙(P2-B) 소관이므로 여기서는 진도율만 확정한다.
     */
    private void syncEnrollmentProgress(Long userId, Long courseId) {
        enrollmentRepository.findByTraineeIdAndCourseId(userId, courseId)
                .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .ifPresent(enrollment -> enrollment.updateProgressRate(courseProgressRatio(userId, courseId)));
    }

    private Progress getOrCreate(Long userId, Content content) {
        return progressRepository.findByUserIdAndContentId(userId, content.getId())
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return progressRepository.save(
                            Progress.builder().user(user).content(content).build());
                });
    }
}
