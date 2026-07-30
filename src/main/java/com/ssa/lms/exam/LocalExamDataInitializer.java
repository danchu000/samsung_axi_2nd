package com.ssa.lms.exam;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * local 프로필 전용 시험 시드 (개발자 B — 시험 생성/설정 슬라이스).
 *
 * 기존 정적 화면의 더미 배열(static/js/exams.js 의 testList)을 옮긴 것이다.
 * 과정(A의 LocalDataInitializer)과 문제은행(LocalQuestionDataInitializer)이 모두 심어진 뒤에
 * 돌아야 한다. A의 러너에는 @Order 가 없어 CommandLineRunner 로는 순서를 뒤로 뺄 수 없으므로
 * (@Order 미지정 = LOWEST_PRECEDENCE), 러너가 전부 끝난 뒤 발행되는 ApplicationReadyEvent 를 쓴다.
 * 두 파일 모두 다른 슬라이스 소유라 손대지 않는다.
 *
 * 규칙 기반 출제를 실제로 검증하려면 카테고리·난이도가 흩어진 문제가 몇 개 더 필요해서
 * 여기서 시험용 문제 몇 건을 추가로 만든다 (코드 채번은 Q-0004 부터 이어 붙인다).
 *
 * <p><b>가드는 마커(시험명) 기반이다.</b> {@code @EventListener} 는 {@code @Order} 가 지정된
 * 리스너가 무순서(LOWEST_PRECEDENCE) 리스너보다 <b>먼저</b> 실행된다. 그래서
 * {@code LocalExamVolumeDataInitializer}(@Order(100))가 이 시더보다 먼저 돌아 시험을 만들면,
 * 과거의 {@code examRepository.count() > 0} 가드는 이 시드를 통째로 건너뛰게 했다
 * (Q-0004~Q-0009 와 기본 시험 2건이 사라지는 버그). 지금은 "내가 만드는 시험"의 존재
 * 여부로만 판정한다.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalExamDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커 시험명. */
    private static final String MARKER = "JavaScript 기초 중간 평가";

    private final ExamRepository examRepository;
    private final ExamRefRepository examRefRepository;
    private final QuestionRepository questionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // 다른 시더(볼륨/채점/감독)가 먼저 시험을 만들어도 이 시드는 죽으면 안 된다 — count 가드 금지.
        if (!examRepository.search(null, List.of(Exam.ExamStatus.values()), null, MARKER).isEmpty()) {
            return;
        }
        List<Course> courses = examRefRepository.findAllCourses();
        if (courses.isEmpty()) {
            log.warn("[local] 과정이 없어 시험 시드를 건너뛴다");
            return;
        }
        Course course = courses.get(0);
        User instructor = examRefRepository.findUsersByRole(Role.INSTRUCTOR).stream()
                .findFirst().orElse(null);

        seedExtraQuestions();

        // 1) 수동 출제 시험 — 문제은행에서 골라 편성
        Exam manual = Exam.builder()
                .examName(MARKER)
                .examType(Exam.ExamType.MIDTERM)
                .course(course)
                .instructor(instructor)
                .timeLimitMin(60)
                .autoScore(80).manualScore(20).totalScore(100).passScore(70)
                .randomOrder(false)
                .retakeAllowed(false).maxAttempts(1)
                .windowStart(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .windowEnd(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .requireIdentityVerification(true)
                .proctorEnabled(true).requireWebcam(true)
                .blockTabSwitch(true).blockCopyPaste(true)
                .note("시험 시작 10분 전까지 본인인증을 완료하세요.")
                .status(Exam.ExamStatus.SCHEDULED)
                .build();

        int seq = 0;
        for (Question q : activeQuestions(3)) {
            manual.addExamQuestion(ExamQuestion.builder()
                    .question(q).seq(++seq).fromRule(false).build());
        }

        // 2) 자동 출제 규칙 시험 — 규칙만 등록된 상태. 화면의 "규칙으로 문항 확정"으로 문항이 내려간다.
        Exam byRule = Exam.builder()
                .examName("Python 기본 문법 테스트")
                .examType(Exam.ExamType.UNIT)
                .course(course)
                .instructor(instructor)
                .timeLimitMin(45)
                .autoScore(100).manualScore(0).totalScore(100).passScore(60)
                .randomOrder(true)
                .retakeAllowed(true).maxAttempts(2)
                .windowStart(LocalDateTime.now().plusDays(3).withHour(13).withMinute(0).withSecond(0).withNano(0))
                .windowEnd(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .requireIdentityVerification(true)
                .proctorEnabled(true).requireWebcam(false)
                .blockTabSwitch(true).blockCopyPaste(true)
                .note("자동 출제 규칙으로 문항이 편성됩니다.")
                .status(Exam.ExamStatus.DRAFT)
                .build();

        ExamQuestionRule rule = ExamQuestionRule.builder()
                .categoryL("KDT")
                .totalCount(3)
                .build();
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(Difficulty.EASY).questionCount(1).build());
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(Difficulty.MEDIUM).questionCount(2).build());
        byRule.addQuestionRule(rule);

        examRepository.saveAll(List.of(manual, byRule));
        log.info("[local] 시험 시드 {}건 생성", examRepository.count());
    }

    /* ===== 내부 ===== */

    /**
     * 자동 출제 규칙 검증용 추가 문제.
     * 문제은행 시드가 3건(그중 1건 미사용)뿐이라 난이도별로 뽑히는지 확인할 수 없다.
     */
    private void seedExtraQuestions() {
        if (questionRepository.existsByQuestionCode("Q-0004")) {
            return;
        }
        questionRepository.saveAll(List.of(
                question("Q-0004", "리스트와 튜플의 가장 큰 차이는?", Difficulty.EASY, 5,
                        "KDT", "프로그래밍", "파이썬", "자료구조,기초문법"),
                question("Q-0005", "파이썬 for 문으로 1~10 의 합을 구하는 코드를 쓰시오.", Difficulty.MEDIUM, 10,
                        "KDT", "프로그래밍", "파이썬", "반복문,구현"),
                question("Q-0006", "딕셔너리에서 키가 없을 때 기본값을 돌려주는 메서드는?", Difficulty.MEDIUM, 10,
                        "KDT", "프로그래밍", "파이썬", "자료구조"),
                question("Q-0007", "제너레이터와 이터레이터의 차이를 설명하시오.", Difficulty.HARD, 15,
                        "KDT", "프로그래밍", "파이썬", "고급문법"),
                question("Q-0008", "CSS 박스 모델의 구성 요소를 나열하시오.", Difficulty.EASY, 5,
                        "고교위탁", "웹", "CSS", "레이아웃"),
                question("Q-0009", "flex-grow 와 flex-shrink 의 차이는?", Difficulty.MEDIUM, 10,
                        "고교위탁", "웹", "CSS", "레이아웃,flex")
        ));
    }

    private Question question(String code, String text, Difficulty difficulty, int score,
                              String categoryL, String categoryM, String categoryS, String tags) {
        return Question.builder()
                .questionCode(code)
                .questionType(Question.QuestionType.SHORT_ANSWER)
                .questionText(text)
                .correctAnswer("-")
                .explanation("시험 출제 검증용 시드 문제")
                .difficulty(difficulty)
                .score(score)
                .categoryL(categoryL)
                .categoryM(categoryM)
                .categoryS(categoryS)
                .tags(tags)
                .caseSensitive(false)
                .allowPartial(true)
                .status(Question.QuestionStatus.ACTIVE)
                .build();
    }

    private List<Question> activeQuestions(int limit) {
        return questionRepository
                .search(null, null, null, Question.QuestionStatus.ACTIVE, Pageable.ofSize(limit))
                .getContent();
    }
}
