package com.ssa.lms.grading;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.exam.LocalExamAttemptDataInitializer;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.entity.ExamAttempt.AttemptStatus;
import com.ssa.lms.exam.repository.AnswerRepository;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.exam.service.AutoGrader;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * local 프로필 전용 <b>시험 채점 슬라이스</b> 시드 (개발자 B).
 *
 * <p><b>CommandLineRunner 를 쓰지 않는 이유</b> — A의 {@code LocalDataInitializer} 에 {@code @Order} 가 없어
 * (미지정 = LOWEST_PRECEDENCE) B 시드가 먼저 돌면 과정·사용자가 없는 상태로 실패한다.
 * 러너가 전부 끝난 뒤 발행되는 {@code ApplicationReadyEvent} 를 쓴다. (다섯 슬라이스가 연속으로 밟은 함정)</p>
 *
 * <p><b>@EventListener 끼리는 순서가 보장되지 않는다.</b> 그래서 앞 슬라이스의 시드를 맨 앞에서
 * 명시적으로 한 번 호출한다 — 각자 멱등 가드가 있어 두 번 불려도 안전하다.
 * ({@code LocalExamAttemptDataInitializer} 가 {@code LocalExamDataInitializer} 를 다시 부른다.)</p>
 *
 * <p>여기서 만드는 것</p>
 * <ul>
 *   <li>서술형(CODING) 문제 2개 — 부분점수 허용 / 불허 각각. 부분점수 규칙 검증용</li>
 *   <li>[채점검증] 서술형 포함 시험 — 자동 채점 문항 + 수동 채점 문항이 섞여 있다</li>
 *   <li>[채점검증] 자동채점 전용 시험 — 수동 문항이 0건일 때 바로 "채점완료"가 되는지 확인용</li>
 *   <li>trainee1 / trainee2 의 제출 완료 회차 + 자동 채점된 객관식·주관식 답안</li>
 *   <li>강사1 담당이 <b>아닌</b> 과정 + 그 과정의 시험 — "담당 아닌 과정 채점 차단" 검증용</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalGradingDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커. */
    private static final String MARKER = "[채점검증] 서술형 포함 시험";
    private static final String OTHER_COURSE_CODE = "COURSE-2026-909";

    private final ExamRepository examRepository;
    private final ExamRefRepository examRefRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AutoGrader autoGrader;
    private final ObjectProvider<LocalExamAttemptDataInitializer> attemptSeedProvider;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // 앞 슬라이스 시드를 먼저 확정시킨다 (위 클래스 주석 참고).
        attemptSeedProvider.ifAvailable(LocalExamAttemptDataInitializer::seed);

        if (!examRepository.search(null, List.of(Exam.ExamStatus.values()), null, "[채점검증]").isEmpty()) {
            return;
        }
        List<Course> courses = examRefRepository.findAllCourses();
        if (courses.isEmpty()) {
            log.warn("[local] 과정이 없어 채점 시드를 건너뛴다");
            return;
        }
        Course course = courses.stream()
                .filter(c -> !OTHER_COURSE_CODE.equals(c.getCourseCode()))
                .findFirst().orElse(courses.get(0));
        User instructor = examRefRepository.findUsersByRole(Role.INSTRUCTOR).stream()
                .findFirst().orElse(null);

        Question mc = requireQuestion("Q-0001");          // 객관식 5점 (정답 보기 3번 "int")
        Question shortAnswer = requireQuestion("Q-0002"); // 주관식 10점, caseSensitive=false, allowPartial=true
        Question partial = codingQuestion("Q-3001",
                "REST API 설계 시 멱등성(idempotency)이 무엇인지, GET/PUT/POST 를 예로 들어 설명하시오.",
                "같은 요청을 여러 번 보내도 서버 상태가 달라지지 않는 성질. GET/PUT 은 멱등, POST 는 비멱등.",
                20, true);
        Question strict = codingQuestion("Q-3002",
                "트랜잭션의 ACID 중 격리성(Isolation)을 한 문장으로 정의하시오.",
                "동시에 실행되는 트랜잭션이 서로의 중간 상태를 보지 못하게 하는 성질.",
                10, false);

        // 1) 자동 + 수동이 섞인 시험 — 이 슬라이스의 주 검증 대상
        Exam mixed = withQuestions(exam(MARKER, course, instructor, 45, 25), List.of(
                new Entry(mc, 5), new Entry(shortAnswer, 10),
                new Entry(partial, 20), new Entry(strict, 10)));

        // 2) 수동 문항 0건 — 제출과 동시에 "채점완료"여야 한다
        Exam autoOnly = withQuestions(exam("[채점검증] 자동채점 전용 시험", course, instructor, 15, 8),
                List.of(new Entry(mc, 5), new Entry(shortAnswer, 10)));

        examRepository.saveAll(List.of(mixed, autoOnly));

        // 3) 강사1 담당이 아닌 과정 + 시험 — 권한 차단 검증용
        Course other = otherCourse();
        Exam foreign = withQuestions(
                exam("[채점검증] 타 과정 시험(강사1 담당 아님)", other, null, 25, 13),
                List.of(new Entry(mc, 5), new Entry(partial, 20)));
        examRepository.save(foreign);

        // 4) 제출 완료 회차 + 자동 채점
        List<User> trainees = new ArrayList<>();
        userRepository.findByLoginId("trainee1").ifPresent(trainees::add);
        userRepository.findByLoginId("trainee2").ifPresent(trainees::add);
        if (trainees.isEmpty()) {
            log.warn("[local] 훈련생 계정이 없어 채점용 응시 회차를 만들지 않는다");
            return;
        }
        for (User trainee : trainees) {
            submitAttempt(mixed, trainee, answersOf(trainee));
            submitAttempt(autoOnly, trainee, answersOf(trainee));
        }
        log.info("[local] 채점 검증용 시험 3건 / 응시 회차 {}건 생성", trainees.size() * 2);
    }

    /* ===== 내부 ===== */

    /** 편성 문항 한 건 (문제 + 이 시험에서의 배점). */
    private record Entry(Question question, int score) {
    }

    /**
     * 훈련생별 답안 전략.
     * trainee1 은 객관식·주관식 모두 정답, trainee2 는 객관식만 정답 —
     * "자동 채점 점수가 채점 화면에서 그대로 유지되는가"를 서로 다른 값으로 확인할 수 있어야 한다.
     */
    private boolean answersOf(User trainee) {
        return "trainee1".equals(trainee.getLoginId());
    }

    /**
     * 제출 완료 회차 생성 + 자동 채점.
     *
     * <p>{@code ExamAttemptService.finish()} 와 같은 순서로 처리한다 — 자동 채점 문항만
     * {@code Answer.gradeAuto()} 로 점수를 박고, 수동 문항은 <b>답안만 저장하고 점수는 비워둔다</b>.
     * 수동 채점 대기가 있으므로 passScore 를 넘기지 않아 {@code passed} 는 null(미정)로 남는다.</p>
     */
    private void submitAttempt(Exam exam, User trainee, boolean allCorrect) {
        LocalDateTime started = LocalDateTime.now().withNano(0).minusHours(2);
        LocalDateTime submitted = started.plusMinutes(35);

        ExamAttempt attempt = examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam).user(trainee).attemptNo(1)
                .startedAt(started)
                .expiresAt(started.plusMinutes(exam.getTimeLimitMin()))
                .status(AttemptStatus.SUBMITTED)
                .identityVerifiedAt(started)
                .identityVerifyMethod("PASSWORD")
                .ip("127.0.0.1").userAgent("local-seed")
                .build());

        int autoScore = 0;
        boolean manualPending = false;

        for (ExamQuestion eq : exam.getExamQuestions()) {
            Question q = eq.getQuestion();
            int max = eq.resolveScore();

            QuestionChoice choice = null;
            String text = null;
            switch (q.getQuestionType()) {
                case MULTIPLE_CHOICE -> choice = pickChoice(q, true);
                case SHORT_ANSWER -> text = allCorrect
                        ? q.getCorrectAnswer()
                        : "잘 모르겠습니다";
                case CODING -> text = allCorrect
                        ? "같은 요청을 반복해도 결과가 같아야 한다는 뜻입니다. GET 과 PUT 은 멱등이고 POST 는 아닙니다."
                        : "여러 번 호출해도 괜찮다는 의미인 것 같습니다.";
            }

            Answer answer = answerRepository.save(Answer.builder()
                    .attempt(attempt).question(q).choice(choice).answerText(text)
                    .savedAt(submitted.minusMinutes(1))
                    .build());

            if (!q.isAutoGradable()) {
                manualPending = true;   // 수동 채점 대기 — 점수를 비워둔다
                continue;
            }
            AutoGrader.Result result = autoGrader.grade(q, answer, max);
            answer.gradeAuto(result.correct(), result.score(), submitted);
            autoScore += result.score();
        }

        attempt.submit(submitted, AttemptStatus.SUBMITTED);
        attempt.applyScore(autoScore, null, manualPending ? null : exam.getPassScore());
    }

    private QuestionChoice pickChoice(Question q, boolean correct) {
        return q.getChoices().stream()
                .filter(c -> c.isCorrect() == correct)
                .findFirst()
                .orElseGet(() -> q.getChoices().isEmpty() ? null : q.getChoices().get(0));
    }

    /** 강사1 담당이 아닌 과정. "담당 과정만 채점 가능"을 실제로 확인하려면 과정이 둘 있어야 한다. */
    private Course otherCourse() {
        Optional<Course> existing = examRefRepository.findAllCourses().stream()
                .filter(c -> OTHER_COURSE_CODE.equals(c.getCourseCode()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        Course course = courseRepository.save(Course.builder()
                .courseCode(OTHER_COURSE_CODE)
                .courseName("데이터 분석 실무 과정 (강사1 미담당)")
                .cohort("1기").category("데이터")
                .description("채점 권한 경계 검증용 과정 — 담당 강사를 지정하지 않는다")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusMonths(3))
                .capacity(20)
                .status(CourseStatus.IN_PROGRESS)
                .completionProgressRate(80)
                .build());
        // 훈련생은 넣어둔다 (응시자 목록이 비어 있으면 화면 확인이 애매해진다).
        userRepository.findByLoginId("trainee1").ifPresent(t -> {
            if (enrollmentRepository.findByTraineeIdAndCourseId(t.getId(), course.getId()).isEmpty()) {
                enrollmentRepository.save(Enrollment.builder()
                        .trainee(t).course(course)
                        .status(EnrollmentStatus.APPROVED)
                        .appliedAt(LocalDateTime.now().minusDays(2))
                        .build());
            }
        });
        return course;
    }

    private Question codingQuestion(String code, String text, String answer, int score, boolean allowPartial) {
        return questionRepository.findByQuestionCode(code).orElseGet(() ->
                questionRepository.save(Question.builder()
                        .questionCode(code)
                        .questionType(Question.QuestionType.CODING)
                        .questionText(text)
                        .correctAnswer(answer)
                        .explanation(allowPartial
                                ? "부분점수 허용 문항 — 0 ~ 배점 사이의 임의 점수를 줄 수 있다."
                                : "부분점수 불가 문항 — 0 또는 만점만 줄 수 있다.")
                        .difficulty(Difficulty.HARD)
                        .score(score)
                        .categoryL("KDT").categoryM("백엔드").categoryS("설계")
                        .tags("서술형,채점")
                        .caseSensitive(false)
                        .allowPartial(allowPartial)
                        .status(Question.QuestionStatus.ACTIVE)
                        .build()));
    }

    private Question requireQuestion(String code) {
        return questionRepository.findByQuestionCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "문제은행 시드가 없다: " + code + " — LocalQuestionDataInitializer 확인"));
    }

    private Exam withQuestions(Exam exam, List<Entry> entries) {
        int seq = 0;
        for (Entry e : entries) {
            exam.addExamQuestion(ExamQuestion.builder()
                    .question(e.question()).seq(++seq)
                    .scoreOverride(e.score()).fromRule(false).build());
        }
        return exam;
    }

    private Exam exam(String name, Course course, User instructor, int totalScore, int passScore) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return Exam.builder()
                .examName(name)
                .examType(Exam.ExamType.MIDTERM)
                .course(course)
                .instructor(instructor)
                .timeLimitMin(60)
                .autoScore(totalScore).manualScore(0)
                .totalScore(totalScore).passScore(passScore)
                .randomOrder(false)
                .retakeAllowed(false).maxAttempts(1)
                .windowStart(now.minusDays(3)).windowEnd(now.minusHours(1))
                .requireIdentityVerification(true)
                .proctorEnabled(false).requireWebcam(false)
                .blockTabSwitch(true).blockCopyPaste(true)
                .note("채점 검증용 시험입니다.")
                .status(Exam.ExamStatus.CLOSED)
                .build();
    }
}
