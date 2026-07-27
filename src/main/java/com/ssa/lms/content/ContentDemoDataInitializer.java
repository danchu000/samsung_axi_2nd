package com.ssa.lms.content;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.entity.Progress;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.content.repository.ProgressRepository;
import com.ssa.lms.content.web.ProgressRequest;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * local 프로필 콘텐츠 도메인 데모 시드 (P2-A 트랙, {@code @Order(30)}).
 *
 * <p>기본 시더({@code @Order(0)})·과정 시더({@code @Order(20)})가 만든 데이터에 의존한다.
 * {@code LocalDataInitializer} 는 동결 대상이라 수정하지 않고 여기에 콘텐츠/진도를 덧붙인다(PARALLEL-P2.md).</p>
 *
 * <p>파일 URL 은 로컬 데모용 플레이스홀더(공개 샘플/정적 이미지)다. 실제 업로드 시 강사 등록으로 교체된다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(30)
@RequiredArgsConstructor
public class ContentDemoDataInitializer implements CommandLineRunner {

    private static final String SAMPLE_VIDEO_URL =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final String SAMPLE_DOC_URL = "/static/img/sample-class.png";

    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Course course = courseRepository.findByCourseCode("COURSE-2026-002").orElse(null);
        if (course == null) {
            log.warn("[local] 과정 COURSE-2026-002 가 없어 콘텐츠 데모 시드를 건너뜁니다.");
            return;
        }
        if (contentRepository.countByCourseId(course.getId()) > 0) {
            return; // 이미 생성됨
        }

        List<Session> sessions =
                sessionRepository.findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(course.getId());
        Session s1 = sessions.isEmpty() ? null : sessions.get(0);
        Session s2 = sessions.size() > 1 ? sessions.get(1) : s1;

        Content video = contentRepository.save(Content.builder()
                .course(course).session(s1).type(ContentType.VIDEO)
                .title("파이썬 환경설정 (VOD)").description("설치와 개발환경 세팅 영상")
                .fileUrl(SAMPLE_VIDEO_URL).originalFileName("BigBuckBunny.mp4")
                .durationSeconds(600).orderNo(1).required(true).status(ContentStatus.ACTIVE)
                .build());

        contentRepository.save(Content.builder()
                .course(course).session(s2).type(ContentType.DOCUMENT)
                .title("자료구조 요약 (문서)").description("리스트/딕셔너리 정리 자료")
                .fileUrl(SAMPLE_DOC_URL).originalFileName("data-structures.png")
                .pageCount(1).orderNo(2).required(true).status(ContentStatus.ACTIVE)
                .build());

        // trainee1 의 부분 진도 데모 (영상 40%)
        User trainee = userRepository.findByLoginId("trainee1").orElse(null);
        if (trainee != null) {
            Progress p = progressRepository.save(Progress.builder().user(trainee).content(video).build());
            ProgressRequest req = new ProgressRequest();
            req.setPositionSeconds(240);
            req.setDurationSeconds(600);
            p.updateVideoProgress(req.getPositionSeconds(), req.getDurationSeconds(),
                    com.ssa.lms.content.service.ProgressService.COMPLETION_RATE);
        }

        log.info("[local] 콘텐츠 도메인 데모 시드 생성 완료 — VOD/문서 콘텐츠 2건 (COURSE-2026-002)");
    }
}
