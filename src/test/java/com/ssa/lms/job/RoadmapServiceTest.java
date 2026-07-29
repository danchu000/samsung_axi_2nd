package com.ssa.lms.job;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.job.dto.RoadmapView;
import com.ssa.lms.job.entity.JobPosting;
import com.ssa.lms.job.repository.JobPostingRepository;
import com.ssa.lms.job.service.RoadmapService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [기능 1] 직무 로드맵 집계 규칙을 고정한다.
 *
 * <p>여기서 막으려는 것은 <b>거짓 통계</b>다. 화면은 멀쩡히 그려지는데 "공고 68%가 요구"가
 * 틀리면 훈련생이 그 숫자로 진로를 정한다.</p>
 */
@SpringBootTest
@Transactional
class RoadmapServiceTest {

    @Autowired RoadmapService roadmapService;
    @Autowired JobPostingRepository jobPostingRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ContentRepository contentRepository;
    @Autowired UserRepository userRepository;

    private User newTrainee() {
        return userRepository.save(User.builder()
                .loginId("rm-" + System.nanoTime()).password("{noop}t").name("로드맵검증")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build());
    }

    private Course courseWith(String... materialTitles) {
        Course c = courseRepository.save(Course.builder()
                .courseCode("RM-" + System.nanoTime()).courseName("로드맵과정")
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().plusDays(10))
                .capacity(30).status(CourseStatus.IN_PROGRESS).build());
        for (String t : materialTitles) {
            contentRepository.save(Content.builder()
                    .course(c).type(ContentType.DOCUMENT).title(t)
                    .orderNo(1).required(true).status(ContentStatus.ACTIVE).build());
        }
        return c;
    }

    private void posting(String group, String keywords, String experience) {
        jobPostingRepository.save(JobPosting.builder()
                .externalId("ext-" + System.nanoTime())
                .jobGroup(group).companyName("회사").title("공고")
                .keywords(keywords).experienceLevel(experience)
                .postingDate(LocalDate.now().minusDays(1))
                .collectedAt(LocalDate.now())
                .build());
    }

    @Test
    @DisplayName("표본이 5건 미만인 직무는 아예 빼 버린다 — 3건 중 2건도 67%가 된다")
    void 표본이_작으면_직무를_뺀다() {
        User me = newTrainee();
        for (int i = 0; i < 4; i++) posting("backend", "Java,Spring", "신입");

        RoadmapView v = roadmapService.forTrainee(me.getId());

        assertThat(v.collectedAt()).as("수집 이력은 있어야 한다").isNotNull();
        assertThat(v.jobs().stream().filter(j -> j.id().equals("backend")))
                .as("표본이 작은 통계는 없는 것보다 나쁘다")
                .isEmpty();
    }

    @Test
    @DisplayName("요구 비율은 공고 '건수' 기준 — 한 공고가 같은 키워드를 두 번 써도 1건")
    void 요구비율은_공고건수_기준() {
        User me = newTrainee();
        // Docker 를 5건 중 4건이 요구 → 80%
        for (int i = 0; i < 4; i++) posting("backend", "Java,Docker,Docker", "신입");
        posting("backend", "Java", "신입");

        RoadmapView.Job job = job(roadmapService.forTrainee(me.getId()), "backend");

        RoadmapView.Demand docker = demand(job, "Docker");
        assertThat(docker.count()).as("중복 키워드를 세면 비율이 부풀려진다").isEqualTo(4);
        assertThat(docker.percent()).isEqualTo(80);
        assertThat(demand(job, "Java").percent()).isEqualTo(100);
    }

    @Test
    @DisplayName("내 학습 자료에 있는 역량은 보유로, 없으면 부족으로 분류한다")
    void 보유_부족_분류() {
        User me = newTrainee();
        Course c = courseWith("Docker 로 배포하기");
        enroll(me, c);
        for (int i = 0; i < 6; i++) posting("backend", "Docker,Kubernetes", "신입");

        RoadmapView.Job job = job(roadmapService.forTrainee(me.getId()), "backend");

        assertThat(job.have()).contains("Docker");
        assertThat(job.lack()).contains("Kubernetes");
        assertThat(demand(job, "Docker").mine()).isTrue();
    }

    @Test
    @DisplayName("학습 단계 — 아직 없는 역량 중 가장 많이 요구되는 것이 '지금 할 일'")
    void 다음_단계는_요구빈도_1위() {
        User me = newTrainee();
        for (int i = 0; i < 6; i++) posting("backend", "Kubernetes", "신입");
        for (int i = 0; i < 6; i++) posting("backend", "Redis", "신입");
        // Kubernetes 6건 / Redis 6건 + Kubernetes 만 3건 더 → Kubernetes 가 앞선다
        for (int i = 0; i < 3; i++) posting("backend", "Kubernetes", "신입");

        RoadmapView.Job job = job(roadmapService.forTrainee(me.getId()), "backend");

        RoadmapView.Step current = job.steps().stream()
                .filter(s -> "current".equals(s.status())).findFirst().orElseThrow();
        assertThat(current.title()).isEqualTo("Kubernetes");
        assertThat(current.meta())
                .as("근거 숫자 없이 순서만 주면 왜 그 순서인지 알 수 없다")
                .contains("%").contains("건");
    }

    @Test
    @DisplayName("경력은 평균이 아니라 최빈값 — 구간 문자열은 평균을 낼 수 없다")
    void 경력은_최빈값() {
        User me = newTrainee();
        for (int i = 0; i < 4; i++) posting("backend", "Java", "신입");
        for (int i = 0; i < 2; i++) posting("backend", "Java", "3~5년");

        assertThat(job(roadmapService.forTrainee(me.getId()), "backend").avgCareer())
                .isEqualTo("신입");
    }

    @Test
    @DisplayName("수집 이력이 없으면 날짜를 지어내지 않고 null 을 준다")
    void 수집_전에는_날짜가_없다() {
        RoadmapView v = roadmapService.forTrainee(newTrainee().getId());
        assertThat(v.collectedAt()).isNull();
        assertThat(v.jobs()).isEmpty();
    }

    private void enroll(User u, Course c) {
        enrollmentRepository.save(Enrollment.builder()
                .trainee(u).course(c).status(EnrollmentStatus.APPROVED).build());
        enrollmentRepository.flush();
    }

    private RoadmapView.Job job(RoadmapView v, String id) {
        return v.jobs().stream().filter(j -> j.id().equals(id)).findFirst().orElseThrow();
    }

    private RoadmapView.Demand demand(RoadmapView.Job job, String label) {
        return job.demands().stream().filter(d -> d.label().equals(label)).findFirst().orElseThrow();
    }
}
