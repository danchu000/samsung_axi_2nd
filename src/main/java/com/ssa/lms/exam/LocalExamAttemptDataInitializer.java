package com.ssa.lms.exam;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
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
import java.util.List;
import java.util.Optional;

/**
 * local 프로필 전용 <b>응시/제출 슬라이스</b> 시드 (개발자 B).
 *
 * 다른 슬라이스의 시드 파일({@code LocalDataInitializer} / {@code LocalExamDataInitializer} /
 * {@code LocalQuestionDataInitializer})은 건드리지 않는다. 필요한 데이터는 전부 여기서 따로 만든다.
 *
 * <p><b>CommandLineRunner 를 쓰지 않는 이유</b> — A의 {@code LocalDataInitializer} 에 {@code @Order} 가 없어
 * (미지정 = LOWEST_PRECEDENCE) B 시드가 먼저 돌아 과정·사용자가 없는 상태로 실패한다.
 * 러너가 전부 끝난 뒤 발행되는 {@code ApplicationReadyEvent} 를 쓴다. (시험·과제·공지·Q&A 네 슬라이스가 연속으로 밟은 함정)</p>
 *
 * <p><b>@EventListener 끼리는 순서가 보장되지 않는다.</b> {@code LocalExamDataInitializer} 의 가드가
 * "exam 이 0건일 때만 시드"라서, 내가 먼저 시험을 만들면 그쪽 시드가 통째로 건너뛰어진다.
 * 그래서 맨 앞에서 그쪽 시드를 명시적으로 한 번 호출한다 — 자체 멱등 가드가 있어 두 번 불려도 안전하다.</p>
 *
 * <p>여기서 만드는 것</p>
 * <ul>
 *   <li>훈련생 2번 계정(trainee2) + 수강신청 — "남의 응시 회차 접근 403" 검증에 필요</li>
 *   <li>주관식 대소문자 구분 문제(Q-2001) — 자동 채점 규칙 검증용</li>
 *   <li>응시 시나리오별 시험 6종 (진행중 / 재응시 / 만료 / 문항 미확정 / 종료 / 예정)</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalExamAttemptDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커 시험명. */
    private static final String MARKER = "[응시검증] 진행중 시험";

    private final ExamRepository examRepository;
    private final ExamRefRepository examRefRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<LocalExamDataInitializer> examSeedProvider;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // 시험 생성 슬라이스 시드를 먼저 확정시킨다 (위 클래스 주석 참고).
        examSeedProvider.ifAvailable(LocalExamDataInitializer::seed);

        if (examRepository.search(null, List.of(Exam.ExamStatus.values()), null, "[응시검증]").size() > 0) {
            return;
        }
        List<Course> courses = examRefRepository.findAllCourses();
        if (courses.isEmpty()) {
            log.warn("[local] 과정이 없어 응시 시드를 건너뛴다");
            return;
        }
        Course course = courses.get(0);
        User instructor = examRefRepository.findUsersByRole(Role.INSTRUCTOR).stream()
                .findFirst().orElse(null);

        seedSecondTrainee(course);

        Question mc = requireQuestion("Q-0001");        // 객관식 5점 (정답 보기 3번 "int")
        Question shortAnswer = requireQuestion("Q-0002"); // 주관식 10점, caseSensitive=false, allowPartial=true
        Question caseSensitive = seedCaseSensitiveQuestion();

        List<Exam> exams = new ArrayList<>();

        // 1) 지금 응시 가능한 시험 — 본인인증 필수 + 부정행위 차단 플래그 on
        exams.add(withQuestions(exam(MARKER, Exam.ExamType.UNIT, course, instructor,
                        60, 25, 0, 25, 15,
                        false, false, 1,
                        now().minusHours(1), now().plusDays(7),
                        true, true, true,
                        "본인인증 후 60분간 응시합니다. 탭 전환과 복사/붙여넣기는 기록됩니다.",
                        Exam.ExamStatus.OPEN),
                List.of(mc, shortAnswer, caseSensitive)));

        // 2) 재응시 허용 (최대 2회)
        exams.add(withQuestions(exam("[응시검증] 재응시 허용 시험", Exam.ExamType.UNIT, course, instructor,
                        30, 15, 0, 15, 8,
                        true, true, 2,
                        now().minusHours(1), now().plusDays(7),
                        true, false, false,
                        "재응시는 최대 2회까지 가능합니다.",
                        Exam.ExamStatus.OPEN),
                List.of(mc, shortAnswer)));

        // 3) 제한시간 1분 — 서버 타이머 만료(AUTO_SUBMITTED) 검증용.
        //    만료 판정만 보려는 시험이라 본인인증은 끈다.
        exams.add(withQuestions(exam("[응시검증] 제한시간 1분 시험", Exam.ExamType.UNIT, course, instructor,
                        1, 5, 0, 5, 3,
                        false, true, 3,
                        now().minusHours(1), now().plusDays(7),
                        false, false, false,
                        "제한시간이 1분입니다. 만료 후 제출은 자동 제출로 기록됩니다.",
                        Exam.ExamStatus.OPEN),
                List.of(mc)));

        // 4) 규칙만 있고 확정 문항 0건 — 응시 진입이 막혀야 한다
        Exam notReady = exam("[응시검증] 문항 미확정 시험", Exam.ExamType.UNIT, course, instructor,
                30, 10, 0, 10, 5,
                false, false, 1,
                now().minusHours(1), now().plusDays(7),
                false, false, false,
                "출제 규칙만 등록된 상태입니다.",
                Exam.ExamStatus.OPEN);
        ExamQuestionRule rule = ExamQuestionRule.builder().categoryL("KDT").totalCount(2).build();
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(Difficulty.MEDIUM).questionCount(2).build());
        notReady.addQuestionRule(rule);
        exams.add(notReady);

        // 5) 응시 기간 종료
        exams.add(withQuestions(exam("[응시검증] 기간 종료 시험", Exam.ExamType.UNIT, course, instructor,
                        30, 5, 0, 5, 3,
                        false, false, 1,
                        now().minusDays(10), now().minusDays(3),
                        false, false, false,
                        "응시 기간이 종료된 시험입니다.",
                        Exam.ExamStatus.CLOSED),
                List.of(mc)));

        // 6) 응시 기간 전(예정)
        exams.add(withQuestions(exam("[응시검증] 응시 예정 시험", Exam.ExamType.UNIT, course, instructor,
                        30, 5, 0, 5, 3,
                        false, false, 1,
                        now().plusDays(3), now().plusDays(4),
                        true, false, false,
                        "아직 응시 기간이 시작되지 않았습니다.",
                        Exam.ExamStatus.SCHEDULED),
                List.of(mc)));

        examRepository.saveAll(exams);
        log.info("[local] 응시 검증용 시험 {}건 생성", exams.size());
    }

    /* ===== 내부 ===== */

    /**
     * 두 번째 훈련생. "다른 훈련생의 응시 회차 접근 → 403" 을 실제로 확인하려면 계정이 둘 있어야 한다.
     * A 소유 리포지토리를 쓰는 유일한 지점이고, local 프로필 시드에 한정한다 (운영 로직에서는 쓰지 않는다).
     */
    private void seedSecondTrainee(Course course) {
        Optional<User> existing = userRepository.findByLoginId("trainee2");
        User trainee2 = existing.orElseGet(() -> userRepository.save(User.builder()
                .loginId("trainee2").password(passwordEncoder.encode("1234"))
                .name("박훈련").role(Role.TRAINEE).status(UserStatus.ACTIVE)
                .email("trainee2@ssa.local").phone("010-3333-3333").birthDate("2000-05-05")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build()));

        if (enrollmentRepository.findByTraineeIdAndCourseId(trainee2.getId(), course.getId()).isEmpty()) {
            enrollmentRepository.save(Enrollment.builder()
                    .trainee(trainee2).course(course)
                    .status(EnrollmentStatus.APPROVED)
                    .appliedAt(LocalDateTime.now().minusDays(8))
                    .build());
        }
    }

    /** 주관식 대소문자 구분 채점 검증용. 기존 시드 문제는 전부 caseSensitive=false 라 확인이 안 된다. */
    private Question seedCaseSensitiveQuestion() {
        return questionRepository.findByQuestionCode("Q-2001").orElseGet(() ->
                questionRepository.save(Question.builder()
                        .questionCode("Q-2001")
                        .questionType(Question.QuestionType.SHORT_ANSWER)
                        .questionText("서블릿에서 HTTP 요청 정보를 담는 인터페이스 이름을 정확히 쓰시오.")
                        .correctAnswer("HttpServletRequest")
                        .explanation("대소문자까지 정확히 일치해야 정답으로 인정한다.")
                        .difficulty(Difficulty.MEDIUM)
                        .score(10)
                        .categoryL("KDT").categoryM("웹").categoryS("서블릿")
                        .tags("서블릿,기초")
                        .caseSensitive(true)
                        .allowPartial(false)
                        .status(Question.QuestionStatus.ACTIVE)
                        .build()));
    }

    private Question requireQuestion(String code) {
        return questionRepository.findByQuestionCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "문제은행 시드가 없다: " + code + " — LocalQuestionDataInitializer 확인"));
    }

    private Exam withQuestions(Exam exam, List<Question> questions) {
        int seq = 0;
        for (Question q : questions) {
            exam.addExamQuestion(ExamQuestion.builder()
                    .question(q).seq(++seq).fromRule(false).build());
        }
        return exam;
    }

    private Exam exam(String name, Exam.ExamType type, Course course, User instructor,
                      int timeLimitMin, int autoScore, int manualScore, int totalScore, int passScore,
                      boolean requireIdentity, boolean retakeAllowed, int maxAttempts,
                      LocalDateTime windowStart, LocalDateTime windowEnd,
                      boolean proctorEnabled, boolean blockTabSwitch, boolean blockCopyPaste,
                      String note, Exam.ExamStatus status) {
        return Exam.builder()
                .examName(name)
                .examType(type)
                .course(course)
                .instructor(instructor)
                .timeLimitMin(timeLimitMin)
                .autoScore(autoScore).manualScore(manualScore)
                .totalScore(totalScore).passScore(passScore)
                .randomOrder(false)
                .retakeAllowed(retakeAllowed).maxAttempts(maxAttempts)
                .windowStart(windowStart).windowEnd(windowEnd)
                .requireIdentityVerification(requireIdentity)
                .proctorEnabled(proctorEnabled).requireWebcam(false)
                .blockTabSwitch(blockTabSwitch).blockCopyPaste(blockCopyPaste)
                .note(note)
                .status(status)
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
