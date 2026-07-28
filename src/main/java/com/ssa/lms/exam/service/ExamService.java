package com.ssa.lms.exam.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.entity.Subject;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.dto.*;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 시험 생성/설정 서비스.
 *
 * 권한정의서(1): 시험 등록·수정은 관리자 O / 강사 O(담당 과정).
 * 접근 통제는 SecurityConfig 의 /admin/evaluation/** (ADMIN, INSTRUCTOR) 로 처리한다.
 *
 * 출제 방식은 두 가지다.
 *  1) 수동 — 문제은행에서 골라 ExamQuestion 으로 편성
 *  2) 자동 규칙 — ExamQuestionRule(+난이도별 개수)로 조건만 저장했다가
 *     {@link #materializeRules(Long)} 시점에 실제 문항을 뽑아 ExamQuestion(fromRule=true)으로 내린다.
 *     응시자에게 실제로 나간 문항을 3년간 재현하려면 규칙만 남겨선 안 되고 문항이 확정돼야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamService {


    private final ExamRepository examRepository;
    private final ExamRefRepository examRefRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CourseQueryService courseQueryService;

    /* ===== 조회 ===== */

    public List<ExamListRow> search(ExamSearchCond cond) {
        List<Exam.ExamStatus> statuses = cond.statusesOrNull();
        if (statuses == null) {
            statuses = List.of(Exam.ExamStatus.values());
        }
        List<Exam> exams = examRepository.search(
                cond.getCourseId(), statuses, cond.instructorOrNull(), cond.keywordOrNull());

        List<Long> ids = exams.stream().map(Exam::getId).toList();
        Map<Long, Long> counts = ids.isEmpty()
                ? Map.of()
                : toCountMap(examRepository.countQuestions(ids));

        return exams.stream()
                .map(e -> ExamListRow.of(e, counts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public Exam getOrThrow(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public ExamForm loadForm(Long id) {
        Exam exam = examRepository.findWithQuestions(id)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + id));
        // 규칙은 별도 컬렉션이라 fetch join 을 겹치면 카테시안 곱이 된다. 트랜잭션 안에서 초기화만 한다.
        exam.getQuestionRules().forEach(r -> r.getDifficulties().size());
        return ExamForm.from(exam);
    }

    /** 수정 화면에 보여줄 "현재 편성된 문항" 목록 (수동/규칙 구분 포함). */
    public List<ExamQuestionRow> loadExamQuestions(Long examId) {
        Exam exam = examRepository.findWithQuestions(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));
        return exam.getExamQuestions().stream().map(ExamQuestionRow::of).toList();
    }

    /** 응시 대상자 수 — 수강생 명단은 A 의 계약(CourseQueryService)으로만 얻는다. */
    public int countTargetTrainees(Long courseId) {
        if (courseId == null) {
            return 0;
        }
        return courseQueryService.findUserIdsByCourseId(courseId).size();
    }

    /* ===== 화면 옵션 ===== */

    public List<OptionItem> courseOptions() {
        return examRefRepository.findAllCourses().stream()
                .map(c -> new OptionItem(String.valueOf(c.getId()),
                        c.getCourseName() + " (" + c.getCourseCode() + ")"))
                .toList();
    }

    public List<OptionItem> subjectOptions(Long courseId) {
        if (courseId == null) {
            return List.of();
        }
        return examRefRepository.findSubjectsByCourse(courseId).stream()
                .map(s -> new OptionItem(String.valueOf(s.getId()), s.getName()))
                .toList();
    }

    public List<OptionItem> sessionOptions(Long courseId) {
        if (courseId == null) {
            return List.of();
        }
        return examRefRepository.findSessionsByCourse(courseId).stream()
                .map(s -> new OptionItem(String.valueOf(s.getId()), s.getSeq() + ". " + s.getName()))
                .toList();
    }

    public List<OptionItem> instructorOptions() {
        return examRefRepository.findUsersByRole(Role.INSTRUCTOR).stream()
                .map(u -> new OptionItem(String.valueOf(u.getId()), u.getName()))
                .toList();
    }

    /** 문제 선택 모달에 내려줄 사용중(ACTIVE) 문제 전체. */
    public List<QuestionPickRow> questionPool() {
        return questionRepository
                .search(null, null, null, Question.QuestionStatus.ACTIVE, Pageable.unpaged())
                .getContent().stream()
                .map(QuestionPickRow::of)
                .toList();
    }

    /* ===== 등록/수정 ===== */

    @Transactional
    public Long create(ExamForm form) {
        validate(form);

        Exam exam = Exam.builder()
                .examName(form.getExamName().strip())
                .examType(form.toExamType())
                .course(course(form.getCourseId()))
                .subject(subject(form.getSubjectId()))
                .session(session(form.getSessionId()))
                .instructor(user(form.getInstructorId()))
                .timeLimitMin(form.getTimeLimitMin())
                .autoScore(form.getAutoScore())
                .manualScore(form.getManualScore())
                .totalScore(form.getTotalScore())
                .passScore(form.getPassScore())
                .randomOrder(form.isRandomOrder())
                .retakeAllowed(form.isRetakeAllowed())
                .maxAttempts(resolveMaxAttempts(form))
                .windowStart(form.getWindowStart())
                .windowEnd(form.getWindowEnd())
                .requireIdentityVerification(form.isRequireIdentityVerification())
                .proctorEnabled(form.isProctorEnabled())
                .requireWebcam(form.isRequireWebcam())
                .blockTabSwitch(form.isBlockTabSwitch())
                .blockCopyPaste(form.isBlockCopyPaste())
                .note(form.getNote())
                .status(form.toStatus())
                .build();

        applyManualQuestions(exam, form.effectiveQuestionIds(), 0);
        applyRules(exam, form.effectiveRules());

        return examRepository.save(exam).getId();
    }

    @Transactional
    public void update(Long id, ExamForm form) {
        validate(form);
        ensureNotAttempted(id);

        Exam exam = getOrThrow(id);
        exam.update(
                form.getExamName().strip(), form.toExamType(),
                course(form.getCourseId()), subject(form.getSubjectId()), session(form.getSessionId()),
                user(form.getInstructorId()), form.getTimeLimitMin(), form.getAutoScore(),
                form.getManualScore(), form.getTotalScore(), form.getPassScore(), form.isRandomOrder(),
                form.isRetakeAllowed(), resolveMaxAttempts(form), form.getWindowStart(), form.getWindowEnd(),
                form.isRequireIdentityVerification(), form.isProctorEnabled(), form.isRequireWebcam(),
                form.isBlockTabSwitch(), form.isBlockCopyPaste(), form.getNote(), form.toStatus());

        // 문항·규칙은 통째로 교체한다. clear() 하고 바로 add() 하면 Hibernate 가 orphan DELETE 보다
        // INSERT 를 먼저 내보내 uk_exam_question(exam_id, question_id) /
        // uk_rule_difficulty(rule_id, difficulty) 유니크 제약에 걸린다 (QuestionService.update() 와 같은 지뢰).
        // 그래서 삭제를 먼저 flush 한 뒤에 새로 넣는다.
        exam.clearExamQuestions();
        exam.clearQuestionRules();
        examRepository.flush();

        applyManualQuestions(exam, form.effectiveQuestionIds(), 0);
        applyRules(exam, form.effectiveRules());
        // 규칙으로 확정됐던 문항은 여기서 함께 지워진다. 수정 후에는 "규칙으로 문항 확정"을 다시 눌러야
        // 실제 출제 문항이 생긴다 — 규칙이 바뀌었는데 옛 문항이 남는 쪽이 더 위험하다.
    }

    /**
     * 자동 출제 규칙으로 실제 문항을 확정한다.
     *
     * 규칙만 저장돼 있으면 응시 시점마다 문항이 달라져 재현이 불가능하다.
     * 확정된 문항은 ExamQuestion(fromRule=true)으로 내려간다.
     *
     * @return 새로 확정된 문항 수
     */
    @Transactional
    public int materializeRules(Long examId) {
        ensureNotAttempted(examId);

        Exam exam = examRepository.findWithQuestions(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));

        if (exam.getQuestionRules().isEmpty()) {
            throw new IllegalArgumentException("등록된 자동 출제 규칙이 없습니다.");
        }

        // 재실행 가능하게 기존 규칙 문항을 먼저 걷어낸다. 여기서도 DELETE 를 먼저 flush 해야
        // 같은 문제를 다시 뽑았을 때 유니크 제약에 걸리지 않는다.
        exam.removeRuleQuestions();
        examRepository.flush();

        Set<Long> already = new LinkedHashSet<>();
        int seq = 0;
        for (ExamQuestion eq : exam.getExamQuestions()) {
            already.add(eq.getQuestion().getId());
            seq = Math.max(seq, eq.getSeq());
        }

        int added = 0;
        for (ExamQuestionRule rule : exam.getQuestionRules()) {
            List<ExamQuestionRuleDifficulty> difficulties = rule.getDifficulties();
            if (difficulties.isEmpty()) {
                // 난이도 지정 없이 총 문항수만 있는 규칙
                added += pick(exam, rule, null, rule.getTotalCount(), already, seq + added);
            } else {
                for (ExamQuestionRuleDifficulty d : difficulties) {
                    added += pick(exam, rule, d.getDifficulty(), d.getQuestionCount(),
                            already, seq + added);
                }
            }
        }
        return added;
    }

    /** 선택 비활성화 — 화면의 "선택 비활성화". 상태를 종료(CLOSED)로 내린다. */
    @Transactional
    public void deactivate(List<Long> ids) {
        examRepository.findAllById(ids).forEach(e -> e.changeStatus(Exam.ExamStatus.CLOSED));
    }

    /** 선택 삭제. Exam 에 @SQLDelete 가 걸려 있어 is_deleted 마킹만 된다 (3년 보존 요건). */
    @Transactional
    public void delete(List<Long> ids) {
        examRepository.deleteAllById(ids);
    }

    /* ===== 검증 ===== */

    /**
     * 시험 수정 잠금 — 응시 기록(ExamAttempt)이 하나라도 있으면 문항 구성을 못 바꾸게 막는다.
     *
     * update()/materializeRules() 는 편성 문항을 통째로 지우고 다시 만든다. 이미 응시한 사람이 있으면
     * Answer 가 가리키던 문항이 시험에서 사라져, "응시자에게 실제로 나간 문항"을 3년간 재현하라는
     * 내역서 요건이 그 자리에서 깨진다. (문항이 지워지는 게 아니라 Exam 과의 연결이 끊겨서
     * 채점·통계·이의신청 화면이 전부 어긋난다)
     *
     * 응시 후에도 정말 바꿔야 하는 경우는 시험을 새로 만들어 대체하는 것이 맞다.
     */
    private void ensureNotAttempted(Long examId) {
        if (examAttemptRepository.existsByExamId(examId)) {
            throw new IllegalArgumentException(
                    "이미 응시 기록이 있는 시험은 수정할 수 없습니다. 문항을 바꾸려면 시험을 새로 등록하세요.");
        }
    }

    /**
     * 화면 입력만으로는 막을 수 없는 규칙을 여기서 본다.
     * 내역서상 배점·합격 기준은 시험 정의의 핵심이라 저장 전에 반드시 걸러야 한다.
     */
    private void validate(ExamForm form) {
        int auto = nvl(form.getAutoScore());
        int manual = nvl(form.getManualScore());
        int total = nvl(form.getTotalScore());
        int pass = nvl(form.getPassScore());

        if (auto + manual != total) {
            throw new IllegalArgumentException(
                    "자동채점 배점(" + auto + ")과 수동채점 배점(" + manual + ")의 합이 총점("
                            + total + ")과 일치해야 합니다.");
        }
        if (pass > total) {
            throw new IllegalArgumentException(
                    "합격점수(" + pass + ")는 총점(" + total + ")을 넘을 수 없습니다.");
        }
        if (form.getWindowStart() != null && form.getWindowEnd() != null
                && !form.getWindowEnd().isAfter(form.getWindowStart())) {
            throw new IllegalArgumentException("응시 종료일시는 시작일시보다 뒤여야 합니다.");
        }
        if (form.isRetakeAllowed() && nvl(form.getMaxAttempts()) < 2) {
            throw new IllegalArgumentException("재응시를 허용하면 최대 응시 횟수는 2회 이상이어야 합니다.");
        }
    }

    /* ===== 내부 ===== */

    private int pick(Exam exam, ExamQuestionRule rule, Difficulty difficulty, Integer count,
                     Set<Long> already, int startSeq) {
        int need = count == null ? 0 : count;
        if (need <= 0) {
            return 0;
        }
        // 이미 편성된 문제를 걸러야 하므로 넉넉히 받아 온 뒤 잘라 쓴다.
        Pageable limit = PageRequest.of(0, need + already.size() + 10);
        List<Question> candidates = examRepository.findRuleCandidates(
                Question.QuestionStatus.ACTIVE, difficulty,
                rule.getCategoryL(), rule.getCategoryM(), rule.getCategoryS(), rule.getTags(), limit);

        int seq = startSeq;
        int added = 0;
        for (Question q : candidates) {
            if (added >= need) {
                break;
            }
            if (!already.add(q.getId())) {
                continue;
            }
            exam.addExamQuestion(ExamQuestion.builder()
                    .question(q)
                    .seq(++seq)
                    .fromRule(true)
                    .build());
            added++;
        }
        // 후보가 모자라면 조용히 덜 뽑는다. 화면이 "현재 총 문항 수 / 목표"로 부족분을 보여준다.
        return added;
    }

    private void applyManualQuestions(Exam exam, List<Long> questionIds, int startSeq) {
        int seq = startSeq;
        for (Long questionId : questionIds) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + questionId));
            exam.addExamQuestion(ExamQuestion.builder()
                    .question(question)
                    .seq(++seq)
                    .fromRule(false)
                    .build());
        }
    }

    private void applyRules(Exam exam, List<ExamRuleForm> rules) {
        for (ExamRuleForm ruleForm : rules) {
            exam.addQuestionRule(ruleForm.toEntity());
        }
    }

    private int resolveMaxAttempts(ExamForm form) {
        return form.isRetakeAllowed() ? nvl(form.getMaxAttempts()) : 1;
    }

    private Course course(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("과정을 선택하세요.");
        }
        return examRefRepository.findCourse(id)
                .orElseThrow(() -> new IllegalArgumentException("과정을 찾을 수 없습니다. id=" + id));
    }

    private Subject subject(Long id) {
        return id == null ? null : examRefRepository.findSubject(id).orElse(null);
    }

    private Session session(Long id) {
        return id == null ? null : examRefRepository.findSession(id).orElse(null);
    }

    private User user(Long id) {
        return id == null ? null : examRefRepository.findUser(id).orElse(null);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
