package com.ssa.lms.config;

import com.ssa.lms.assignment.dto.GradingForm;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.entity.Submission;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.assignment.repository.SubmissionRepository;
import com.ssa.lms.assignment.service.AssignmentGradingService;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.completion.entity.Completion;
import com.ssa.lms.completion.entity.CompletionResult;
import com.ssa.lms.completion.entity.ConfirmStatus;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.Progress;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.content.repository.ProgressRepository;
import com.ssa.lms.content.service.ProgressService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.grading.LocalGradingDataInitializer;
import com.ssa.lms.proctor.LocalProctorDataInitializer;
import com.ssa.lms.support.LocalSupportDataInitializer;
import com.ssa.lms.survey.LocalSurveyDataInitializer;
import com.ssa.lms.survey.dto.SurveySubmitForm;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.service.SurveyService;
import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * local 프로필 전용 <b>정부 제안서 화면 캡처용</b> 데모 시드.
 *
 * <p>기본 시드만으로는 캡처 화면이 허전하다 — COURSE-2026-001 의 승인 수강생이 2명뿐이라
 * 5초 뒤 {@code LocalProgressDemoDataInitializer} 가 demo_t 계정을 패딩하고(못생긴 로그인ID),
 * 학습기록(Progress)·과제 제출물(Submission)·설문 응답(SurveyResponse)은 아예 없다.
 * 여기서 실명 훈련생 계정과 학습·출결·제출·응답 데이터를 채워 관리자/강사/훈련생 화면이
 * 전부 "운영 중인 시스템"처럼 보이게 한다.</p>
 *
 * <p><b>실행 순서</b> — {@code @EventListener} 는 {@code @Order} 지정 리스너가 무순서 리스너보다
 * <b>먼저</b> 실행되고, 무순서 리스너끼리는 순서가 없다. 이 시더는 무순서로 두되, 의존하는
 * 무순서 시더들(채점→응시→시험 연쇄, 감독, 설문, Q&A)을 맨 앞에서 {@code ObjectProvider} 로
 * 명시 호출해 순서를 확정한다 — 각자 멱등 가드가 있어 이벤트로 다시 불려도 안전하다.
 * ({@code LocalGradingDataInitializer} 가 이미 쓰는 패턴이다.)</p>
 *
 * <p>{@code config.LocalDataInitializer} 는 동결 파일이라 손대지 않는다.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalProposalDemoDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커 계정. */
    private static final String MARKER_LOGIN_ID = "trainee7";
    private static final String COURSE_MAIN = "COURSE-2026-001";     // 진행중, instructor1 담당
    private static final String COURSE_RECRUIT = "COURSE-2026-002";  // 모집중, instructor1 담당

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final AccessLogRepository accessLogRepository;
    private final AttendanceService attendanceService;
    private final CompletionService completionService;
    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentGradingService assignmentGradingService;
    private final SurveyRepository surveyRepository;
    private final SurveyService surveyService;
    private final PasswordEncoder passwordEncoder;

    private final ObjectProvider<LocalGradingDataInitializer> gradingSeedProvider;
    private final ObjectProvider<LocalProctorDataInitializer> proctorSeedProvider;
    private final ObjectProvider<LocalSurveyDataInitializer> surveySeedProvider;
    private final ObjectProvider<LocalSupportDataInitializer> supportSeedProvider;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // 의존 시더를 먼저 확정한다 (클래스 주석 참고). 채점 시더는 응시→시험 시더를 연쇄 호출한다.
        gradingSeedProvider.ifAvailable(LocalGradingDataInitializer::seed);
        proctorSeedProvider.ifAvailable(LocalProctorDataInitializer::seed);
        surveySeedProvider.ifAvailable(LocalSurveyDataInitializer::seed);
        supportSeedProvider.ifAvailable(LocalSupportDataInitializer::seed);

        if (userRepository.findByLoginId(MARKER_LOGIN_ID).isPresent()) {
            return;
        }
        Course main = courseRepository.findByCourseCode(COURSE_MAIN).orElse(null);
        if (main == null) {
            log.warn("[local] 과정 {} 이 없어 제안서 데모 시드를 건너뛴다", COURSE_MAIN);
            return;
        }
        Course recruit = courseRepository.findByCourseCode(COURSE_RECRUIT).orElse(null);
        User trainee1 = userRepository.findByLoginId("trainee1").orElse(null);
        User instructor = userRepository.findByLoginId("instructor1").orElse(null);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        /* ---------- 1) 실명 훈련생 4명 + 수강 승인 ---------- */
        User t7 = trainee("trainee7", "김서연", 7);
        User t8 = trainee("trainee8", "박도윤", 8);
        User t9 = trainee("trainee9", "이하은", 9);
        User t10 = trainee("trainee10", "정민준", 10);
        enroll(t7, main, EnrollmentStatus.APPROVED, now.minusDays(12));
        enroll(t8, main, EnrollmentStatus.APPROVED, now.minusDays(11));
        enroll(t9, main, EnrollmentStatus.APPROVED, now.minusDays(10));
        enroll(t10, main, EnrollmentStatus.APPROVED, now.minusDays(9));
        if (recruit != null) {
            // 002 의 승인자가 6명이 되어 5초 패딩 시더(demo_t 계정)가 돌지 않는다.
            enroll(t7, recruit, EnrollmentStatus.APPROVED, now.minusDays(6));
        }

        /* ---------- 2) trainee1 의 002 승인 승격 + 승인대기 데모 계정 ---------- */
        if (recruit != null && trainee1 != null) {
            enrollmentRepository.findByTraineeIdAndCourseId(trainee1.getId(), recruit.getId())
                    .filter(e -> e.getStatus() == EnrollmentStatus.APPLIED)
                    .ifPresent(e -> e.approve(now.minusDays(2)));
        }
        if (recruit != null) {
            // 승인대기 목록이 비지 않도록 새 신청 1건을 남겨 둔다.
            User appliedTrainee = trainee("trainee_applied", "성수민", 11);
            enroll(appliedTrainee, recruit, EnrollmentStatus.APPLIED, now.minusHours(6));
        }

        /* ---------- 3) 학습기록(Progress) — 001 의 AI 학습 자료 6건 ---------- */
        int progressCount = seedProgress(main, trainee1, t7, t8);

        /* ---------- 4) 출결 → 이수 판정 → 확정 ---------- */
        int attendanceTouched = seedAttendanceAndCompletion(main);

        /* ---------- 5) 과제 제출물 + 일부 채점 완료 ---------- */
        int submissionCount = seedSubmissions(main, instructor, trainee1, t7, t8, now);

        /* ---------- 6) 설문 응답 ---------- */
        int surveyResponses = seedSurveyResponses(main, trainee1, t7, t8, t9);

        log.info("[local] 제안서 데모 시드 생성 완료 — 훈련생 5명(trainee7~10, trainee_applied), "
                        + "학습기록 {}건, 출결 {}건 산정, 과제 제출 {}건(일부 채점 완료), 설문 응답 {}건",
                progressCount, attendanceTouched, submissionCount, surveyResponses);
    }

    /* ===================== 내부 ===================== */

    /** 기본 시더(LocalDataInitializer)의 User 빌더 패턴을 그대로 따른다. 개인정보 필드는 CryptoConverter 로 암호화된다. */
    private User trainee(String loginId, String name, int seq) {
        return userRepository.findByLoginId(loginId).orElseGet(() ->
                userRepository.save(User.builder()
                        .loginId(loginId).password(passwordEncoder.encode("1234"))
                        .name(name).role(Role.TRAINEE).status(UserStatus.ACTIVE)
                        .email(loginId + "@ssa.local")
                        .phone(String.format("010-5000-%04d", seq))
                        .birthDate(String.format("2000-05-%02d", Math.min(seq + 10, 28)))
                        .profileImageUrl("/static/img/avatars/" + loginId + ".svg")
                        .privacyConsentAt(LocalDateTime.now())
                        .thirdPartyConsentAt(LocalDateTime.now())
                        .build()));
    }

    private void enroll(User trainee, Course course, EnrollmentStatus status, LocalDateTime appliedAt) {
        if (enrollmentRepository.findByTraineeIdAndCourseId(trainee.getId(), course.getId()).isPresent()) {
            return;
        }
        Enrollment enrollment = Enrollment.builder()
                .trainee(trainee).course(course).status(status).appliedAt(appliedAt)
                .build();
        if (status == EnrollmentStatus.APPROVED) {
            enrollment.approve(appliedAt.plusHours(20));   // 승인 처리 시각도 자연스럽게 남긴다
        }
        enrollmentRepository.save(enrollment);
    }

    /**
     * 001 의 AI 학습 자료(DOCUMENT 6건)에 학습기록을 심는다.
     * trainee1 = 4건 완료 + 1건 60% + 1건 미학습, trainee7 = 2건 완료 + 1건 부분,
     * trainee8 = 1건 완료 + 1건 부분.
     */
    private int seedProgress(Course main, User trainee1, User t7, User t8) {
        List<Content> contents = contentRepository.findByCourseIdOrderByOrderNoAscIdAsc(main.getId());
        if (contents.isEmpty()) {
            log.warn("[local] 과정 {} 에 콘텐츠가 없어 학습기록 시드를 건너뛴다", COURSE_MAIN);
            return 0;
        }
        int count = 0;
        if (trainee1 != null) {
            for (int i = 0; i < Math.min(4, contents.size()); i++) {
                progressOf(trainee1, contents.get(i)).markCompleted();
                count++;
            }
            if (contents.size() > 4) {
                partialDocumentProgress(trainee1, contents.get(4), 3, 5);   // 60%
                count++;
            }
            // 마지막 1건은 미학습(행 미생성)으로 남긴다 — 진도율 화면에 0% 항목이 보이게.
        }
        if (contents.size() >= 3) {
            progressOf(t7, contents.get(0)).markCompleted();
            progressOf(t7, contents.get(1)).markCompleted();
            partialDocumentProgress(t7, contents.get(2), 2, 5);             // 40%
            count += 3;
            progressOf(t8, contents.get(0)).markCompleted();
            partialDocumentProgress(t8, contents.get(1), 3, 10);            // 30%
            count += 2;
        }
        return count;
    }

    private Progress progressOf(User user, Content content) {
        return progressRepository.findByUserIdAndContentId(user.getId(), content.getId())
                .orElseGet(() -> progressRepository.save(
                        Progress.builder().user(user).content(content).build()));
    }

    /**
     * 부분 진도. AI 자료 시드의 pageCount 는 플레이스홀더 1이라 실제 페이지 수로는
     * 부분 진도율을 만들 수 없다 — 가상의 총 페이지 수로 도달 페이지를 계산시킨다.
     * (진도율 계산 로직은 실제 서비스와 같은 {@link Progress#updateDocumentProgress} 를 탄다.)
     */
    private void partialDocumentProgress(User user, Content content, int reachedPage, int totalPages) {
        progressOf(user, content).updateDocumentProgress(reachedPage, totalPages,
                ProgressService.COMPLETION_RATE);
    }

    /**
     * 001 의 승인 수강생 전원에 대해 차시별 접속 로그를 심고 → 출결 재산정 → 이수 판정 →
     * PASS 건 확정. {@code AttendanceCompletionDemoDataInitializer} 가 trainee1 몫을 이미
     * 만들어 뒀으므로 접속 로그는 존재 확인 후에만 넣고, 이미 확정된 이수는 다시 확정하지 않는다.
     */
    private int seedAttendanceAndCompletion(Course main) {
        List<Session> sessions = sessionRepository
                .findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(main.getId()).stream()
                .filter(s -> s.getLessonDate() != null)
                .toList();
        if (sessions.isEmpty()) {
            return 0;
        }
        List<User> attendees = enrollmentRepository.findByCourseIdOrderByAppliedAtDesc(main.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .map(Enrollment::getTrainee)
                .toList();
        for (User user : attendees) {
            for (Session session : sessions) {
                LocalDateTime at = session.getLessonDate().atTime(10, 0);
                if (accessLogRepository.existsByUserIdAndTypeAndOccurredAt(
                        user.getId(), AccessLog.Type.LOGIN, at)) {
                    continue;   // trainee1 몫은 @Order(40) 러너가 이미 심었다
                }
                accessLogRepository.save(AccessLog.builder()
                        .userId(user.getId()).loginId(user.getLoginId()).type(AccessLog.Type.LOGIN)
                        .ipAddress("127.0.0.1").userAgent("proposal-seed")
                        .occurredAt(at).build());
            }
        }
        int touched = attendanceService.recalculate(main.getId());
        completionService.evaluate(main.getId());
        for (Completion completion : completionService.findByCourse(main.getId())) {
            if (completion.getResult() == CompletionResult.PASS
                    && completion.getConfirmStatus() != ConfirmStatus.CONFIRMED) {
                completionService.confirm(completion.getId());
            }
        }
        return touched;
    }

    /**
     * 001 의 배정 과제 중 마감이 가장 이른 3건(마감 지남/임박)에 trainee1·trainee7·trainee8 의
     * 제출물을 만든다. 앞 2건 × trainee1·trainee7 은 instructor1 이 채점 완료(점수+피드백),
     * 나머지는 채점 대기로 남겨 강사 채점 화면에 "대기/완료"가 섞이게 한다.
     * 제출물 생성은 {@code SubmissionService.submit()} 의 빌더 사용법을 그대로 따른다.
     */
    private int seedSubmissions(Course main, User instructor, User trainee1, User t7, User t8,
                                LocalDateTime now) {
        List<CourseAssignment> targets = courseAssignmentRepository
                .search(main.getId(), null, null, null).stream()
                .filter(ca -> ca.getStatus() == CourseAssignment.CourseAssignmentStatus.OPEN)
                .sorted(Comparator.comparing(CourseAssignment::getEndAt))
                .limit(3)
                .toList();
        if (targets.isEmpty() || trainee1 == null) {
            log.warn("[local] 과정 {} 에 배정 과제가 없어 제출물 시드를 건너뛴다", COURSE_MAIN);
            return 0;
        }
        List<User> submitters = List.of(trainee1, t7, t8);
        String[][] texts = {
                {"게시판 CRUD API 를 구현하고 컨트롤러·서비스·리포지토리 계층을 분리했습니다. 예외 응답은 공통 핸들러로 통일했습니다.",
                 "요구사항의 등록/조회/수정/삭제를 모두 구현했고, 검증 실패 시 400 과 필드별 메시지를 내려주도록 처리했습니다.",
                 "CRUD 기능 구현을 완료했습니다. 삭제는 소프트 딜리트로 처리했고 목록 조회에 페이징을 적용했습니다."},
                {"주차별 실습 과제를 정리해 제출합니다. 실습 중 트랜잭션 경계 설정에서 배운 점을 함께 적었습니다.",
                 "실습 코드를 리팩터링하며 중복 로직을 공통 메서드로 추출했습니다. 테스트 3건을 추가했습니다.",
                 "이번 주차 실습 결과를 제출합니다. 미완성 부분은 주석으로 표시해 두었습니다."},
                {"과제 요구사항에 맞춰 구현을 마무리했습니다. 실행 방법은 본문 하단에 정리했습니다.",
                 "제출 기한 내 완료했습니다. 어려웠던 부분과 해결 과정을 함께 기록했습니다.",
                 "구현을 마치고 결과 화면 캡처와 설명을 정리해 제출합니다."}
        };
        List<Submission> saved = new ArrayList<>();
        for (int a = 0; a < targets.size(); a++) {
            CourseAssignment ca = targets.get(a);
            for (int u = 0; u < submitters.size(); u++) {
                User user = submitters.get(u);
                if (submissionRepository.countByCourseAssignmentIdAndUserId(ca.getId(), user.getId()) > 0) {
                    continue;
                }
                // 마감 전 과제는 지금 기준, 마감 지난 과제는 마감 기준으로 제출 시각을 과거에 둔다.
                LocalDateTime base = ca.getEndAt().isBefore(now) ? ca.getEndAt() : now;
                LocalDateTime submittedAt = base.minusHours(2L + u * 3L);
                if (a == 0 && u == 2 && ca.getEndAt().isBefore(now)) {
                    submittedAt = ca.getEndAt().plusHours(4);   // 지각 제출 1건 — 화면의 "지각" 뱃지용
                }
                boolean late = submittedAt.isAfter(ca.getEndAt());
                Submission submission = Submission.builder()
                        .courseAssignment(ca)
                        .user(user)
                        .attemptNo(1)
                        .submitType(ca.getSubmissionType())
                        .contentText(texts[a % texts.length][u])
                        .linkUrl(ca.getSubmissionType() == CourseAssignment.SubmissionType.LINK
                                ? "https://github.com/ssa-demo/" + user.getLoginId() + "-assignment" : null)
                        .submittedAt(submittedAt)
                        .late(late)
                        .status(Submission.SubmissionStatus.SUBMITTED)
                        .build();
                saved.add(submissionRepository.save(submission));
            }
        }

        // 채점 완료 4건 — 앞 2개 과제 × trainee1·trainee7. 실제 채점 경로(Grade upsert + 피드백)를 탄다.
        if (instructor != null) {
            int[] scores = {95, 88, 92, 85};
            String[] feedbacks = {
                    "요구사항을 충실히 반영했습니다. 예외 처리 계층 분리가 특히 좋았습니다.",
                    "핵심 기능은 잘 동작합니다. 변수 네이밍을 조금 더 일관되게 정리해 보세요.",
                    "구현 완성도가 높습니다. 테스트 코드까지 갖춰 모범적인 제출물입니다.",
                    "전반적으로 좋습니다. 마감 직전 제출이 잦으니 일정 관리에 신경 써 주세요."
            };
            int gradedIdx = 0;
            for (Submission submission : saved) {
                boolean firstTwoAssignments = indexOfAssignment(targets, submission) < 2;
                boolean gradedUser = submission.getUser().getId().equals(trainee1.getId())
                        || submission.getUser().getId().equals(t7.getId());
                if (!firstTwoAssignments || !gradedUser || gradedIdx >= scores.length) {
                    continue;
                }
                GradingForm form = new GradingForm();
                form.setScore(scores[gradedIdx]);
                form.setFeedback(feedbacks[gradedIdx]);
                assignmentGradingService.grade(submission.getId(), form, instructor.getId());
                gradedIdx++;
            }
        }
        return saved.size();
    }

    private int indexOfAssignment(List<CourseAssignment> targets, Submission submission) {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getId().equals(submission.getCourseAssignment().getId())) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 001 의 진행중(ONGOING) 설문에 훈련생 4명의 응답을 만든다.
     * 실제 제출 경로({@code SurveyService.submit()})를 그대로 태워 문항 유형별 검증을 통과한
     * 유효한 답만 저장된다. 이 설문은 익명이라 응답자는 저장되지 않고 건수만 통계에 잡힌다.
     */
    private int seedSurveyResponses(Course main, User trainee1, User t7, User t8, User t9) {
        Survey ongoing = surveyRepository.findAll().stream()
                .filter(s -> s.getStatus() == Survey.SurveyStatus.ONGOING
                        && s.getCourse() != null && s.getCourse().getId().equals(main.getId()))
                .findFirst().orElse(null);
        if (ongoing == null) {
            log.warn("[local] 과정 {} 에 진행중 설문이 없어 응답 시드를 건너뛴다", COURSE_MAIN);
            return 0;
        }
        Survey loaded = surveyRepository.findWithQuestions(ongoing.getId()).orElse(ongoing);

        String[] textAnswers = {
                "실습 위주 구성이라 이해가 빨랐습니다. 지금처럼 진행해 주세요.",
                "과제 피드백이 빨라서 좋았습니다. 심화 자료가 더 있으면 좋겠어요.",
                "튜터링 응답이 친절했습니다. 실습 환경 안내를 더 자세히 부탁드립니다.",
                "전반적으로 만족합니다. 프로젝트 기간을 조금 늘려 주시면 좋겠습니다."
        };
        List<User> respondents = new ArrayList<>();
        if (trainee1 != null) respondents.add(trainee1);
        respondents.add(t7);
        respondents.add(t8);
        respondents.add(t9);

        int count = 0;
        for (int i = 0; i < respondents.size(); i++) {
            surveyService.submit(loaded.getId(), respondents.get(i).getId(),
                    surveyForm(loaded, i, textAnswers[i % textAnswers.length]));
            count++;
        }
        return count;
    }

    /** 문항 유형별로 유효한 답을 채운 제출 폼 — SCALE 4~5, SINGLE/MULTI 보기 선택, TEXT 한 줄. */
    private SurveySubmitForm surveyForm(Survey survey, int variant, String textAnswer) {
        SurveySubmitForm form = new SurveySubmitForm();
        for (SurveyQuestion question : survey.getQuestions()) {
            SurveySubmitForm.AnswerInput input = new SurveySubmitForm.AnswerInput();
            input.setQuestionId(question.getId());
            switch (question.getQuestionType()) {
                case SCALE -> input.setScaleValue(4 + (variant % 2));
                case SINGLE -> input.setChoiceIds(List.of(
                        question.getChoices().get(variant % question.getChoices().size()).getId()));
                case MULTI -> {
                    List<Long> picks = new ArrayList<>();
                    int size = question.getChoices().size();
                    picks.add(question.getChoices().get(variant % size).getId());
                    if (size > 1 && variant % 2 == 0) {
                        picks.add(question.getChoices().get((variant + 1) % size).getId());
                    }
                    input.setChoiceIds(picks);
                }
                case TEXT -> input.setAnswerText(textAnswer);
            }
            form.getAnswers().add(input);
        }
        return form;
    }
}
