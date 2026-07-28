package com.ssa.lms.exam.service;

import com.ssa.lms.exam.dto.QuestionForm;
import com.ssa.lms.exam.dto.QuestionListRow;
import com.ssa.lms.exam.dto.QuestionSearchCond;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import com.ssa.lms.exam.repository.AnswerRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.export.ExcelWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 문제은행 서비스.
 *
 * 권한정의서(1) 19~20행: 콘텐츠(문제) 등록·수정은 관리자 O / 강사 O.
 * 접근 통제는 SecurityConfig 의 /admin/evaluation/** (ADMIN, INSTRUCTOR) 로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private static final String CODE_PREFIX = "Q-";

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    /**
     * 목록 조회. 사용중인 과정 수와 평균 성취도는 페이지에 담긴 id 묶음으로
     * 한 번씩만 집계한다 (행마다 조회하면 N+1).
     */
    public Page<QuestionListRow> search(QuestionSearchCond cond, Pageable pageable) {
        if (!cond.includesQuestionType()) {
            // "영상"/"문서" 등 A 소유 콘텐츠 유형만 선택된 경우 — 문제은행은 결과 없음
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Page<Question> page = questionRepository.search(
                cond.keywordOrNull(), cond.difficultyOrNull(),
                cond.categoryOrNull(), cond.statusOrNull(), pageable);

        List<Long> ids = page.getContent().stream().map(Question::getId).toList();
        Map<Long, Long> usedCourses = toLongMap(
                ids.isEmpty() ? Collections.emptyList() : questionRepository.countUsedCourses(ids));
        Map<Long, Double> achievement = toDoubleMap(
                ids.isEmpty() ? Collections.emptyList() : questionRepository.averageAchievement(ids));

        return page.map(q -> QuestionListRow.of(
                q,
                usedCourses.getOrDefault(q.getId(), 0L),
                achievement.get(q.getId())));
    }

    /**
     * 조건에 맞는 전체 목록.
     *
     * 문제은행 화면은 탭 전환·페이징을 contents.js 가 클라이언트에서 처리하므로
     * 서버는 필터링된 전체 행을 한 번에 내려준다. 데이터가 커지거나 A의 콘텐츠 목록과
     * 병합돼 행 수가 늘어나면 {@link #search(QuestionSearchCond, Pageable)} 로 전환한다.
     */
    public List<QuestionListRow> searchAll(QuestionSearchCond cond) {
        return search(cond, Pageable.unpaged()).getContent();
    }

    /** 문제은행 엑셀 시트 헤더 — 화면 표(admin-evaluation-question-bank.html) 컬럼과 같은 순서. */
    private static final String[] EXPORT_HEADERS = {
            "번호", "문제코드", "유형", "제목", "난이도", "카테고리",
            "사용중인 과정 수", "평균 성취도", "생성일", "상태"};

    /**
     * 화면의 "엑셀로 다운로드" — 현재 검색 조건에 걸린 <b>전체</b> 문항.
     *
     * <p>목록 화면은 서버 페이징(10건)이지만 다운로드는 페이지가 아니라 조건 전체를 내린다.
     * 페이지에 보이는 10건만 받는 건 실무에서 쓸모가 없다.</p>
     *
     * <p><b>정답·해설은 넣지 않는다.</b> {@link QuestionListRow} 가 이미 그 둘을 뺀 DTO 이고,
     * 다운로드 파일은 메일·메신저로 흘러다니기 쉬워 목록 화면보다 유출 위험이 크다.</p>
     */
    public byte[] exportExcel(QuestionSearchCond cond) {
        List<QuestionListRow> rows = searchAll(cond);
        try (ExcelWriter writer = ExcelWriter.create()) {
            writer.sheet("문제은행", EXPORT_HEADERS);
            int no = 0;
            for (QuestionListRow r : rows) {
                writer.row(++no, r.code(), r.type(), r.title(), r.difficulty(), r.category(),
                        r.usedCourseCount(), r.avgAchievement(), r.createdAt(),
                        "Active".equals(r.status()) ? "활성화" : "비활성화");
            }
            if (rows.isEmpty()) {
                writer.emptyNote("검색 조건에 맞는 문제가 없습니다.");
            }
            return writer.toBytes();
        }
    }

    public Question getOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + id));
    }

    public QuestionForm loadForm(Long id) {
        return QuestionForm.from(getOrThrow(id));
    }

    @Transactional
    public Long create(QuestionForm form) {
        Question question = Question.builder()
                .questionCode(resolveCode(form.getQuestionCode()))
                .questionType(form.toQuestionType())
                .questionText(form.getQuestionText())
                .correctAnswer(form.getCorrectAnswer())
                .explanation(form.getExplanation())
                .difficulty(form.toDifficulty())
                .score(form.getScore())
                .categoryL(form.getCategory())
                .categoryM(form.getCategoryM())
                .categoryS(form.getCategoryS())
                .tags(form.getTags())
                .timeLimit(form.getTimeLimit())
                .caseSensitive(form.isCaseSensitive())
                .allowPartial(form.isAllowPartial())
                .status(form.toStatus())
                .build();

        applyChoices(question, form);
        return questionRepository.save(question).getId();
    }

    @Transactional
    public void update(Long id, QuestionForm form) {
        Question question = getOrThrow(id);
        question.update(
                form.toQuestionType(), form.getQuestionText(), form.getCorrectAnswer(),
                form.getExplanation(), form.toDifficulty(), form.getScore(),
                form.getCategory(), form.getCategoryM(), form.getCategoryS(), form.getTags(),
                form.getTimeLimit(), form.isCaseSensitive(), form.isAllowPartial(), form.toStatus());

        syncChoices(question, form);
    }

    /** 선택 비활성화 — 화면의 "선택한 문제 비활성화". */
    @Transactional
    public void deactivate(List<Long> ids) {
        questionRepository.findAllById(ids)
                .forEach(q -> q.changeStatus(Question.QuestionStatus.INACTIVE));
    }

    /**
     * 선택 삭제. Question 에 @SQLDelete 가 걸려 있어 물리 삭제가 아니라
     * is_deleted 마킹만 된다 (내역서 3년 보존 요건).
     */
    @Transactional
    public void delete(List<Long> ids) {
        questionRepository.deleteAllById(ids);
    }

    /* ===== 내부 ===== */

    /**
     * 보기 반영 — 삭제·재생성이 아니라 <b>제자리 갱신</b>이다.
     *
     * <p>예전에는 보기를 통째로 지우고 새로 넣었는데, {@code answer.choice_id} 가
     * question_choice 를 참조하고 있어서 <b>이미 응시된 문제를 수정하면 FK 위반으로
     * 500 이 났다.</b> 관리자가 오탈자 하나 고치려다 막히는 상황이었다.</p>
     *
     * <p>같은 seq 는 내용만 바꿔 행을 유지하고, 사라진 seq 는 참조가 없을 때만 지운다.
     * 참조가 있으면 지우지 않고 그대로 둔다 — 응시자가 "3번을 골랐다"는 기록이
     * 남아 있어야 채점·이의제기 대응이 되기 때문이다(3년 보존 요건).</p>
     */
    private void syncChoices(Question question, QuestionForm form) {
        if (question.getQuestionType() != Question.QuestionType.MULTIPLE_CHOICE) {
            // 객관식이 아니게 바뀐 경우 — 남은 보기는 참조가 없을 때만 정리한다
            removeUnreferenced(question, List.copyOf(question.getChoices()));
            return;
        }
        List<QuestionChoice> removed = question.syncChoices(form.toChoices());
        removeUnreferenced(question, removed);
    }

    /** 답안이 참조하지 않는 보기만 실제로 지운다. */
    private void removeUnreferenced(Question question, List<QuestionChoice> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        List<Long> ids = candidates.stream()
                .map(QuestionChoice::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Set<Long> referenced = ids.isEmpty()
                ? Set.of()
                : Set.copyOf(answerRepository.findReferencedChoiceIds(ids));

        List<QuestionChoice> deletable = candidates.stream()
                .filter(c -> c.getId() == null || !referenced.contains(c.getId()))
                .toList();
        if (!deletable.isEmpty()) {
            question.removeChoices(deletable);
        }
    }

    private void applyChoices(Question question, QuestionForm form) {
        if (question.getQuestionType() != Question.QuestionType.MULTIPLE_CHOICE) {
            return;
        }
        for (QuestionChoice choice : form.toChoices()) {
            question.addChoice(choice);
        }
    }

    /** 비어 있으면 Q-0001 형식으로 채번. 입력됐으면 중복만 검사. */
    private String resolveCode(String input) {
        if (input != null && !input.isBlank()) {
            String code = input.strip();
            // 삭제된 문제의 코드도 DB 에 남아 있어 유니크 제약이 걸린다
            if (questionRepository.existsByQuestionCodeIncludingDeleted(code)) {
                throw new IllegalArgumentException("이미 사용 중인 문제 코드입니다: " + code);
            }
            return code;
        }
        int next = questionRepository.findMaxQuestionCode()
                .filter(c -> c.startsWith(CODE_PREFIX))
                .map(c -> parseSeq(c) + 1)
                .orElse(1);
        return CODE_PREFIX + String.format("%04d", next);
    }

    private int parseSeq(String code) {
        try {
            return Integer.parseInt(code.substring(CODE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<Long, Long> toLongMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<Long, Double> toDoubleMap(List<Object[]> rows) {
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[1] != null) {
                map.put((Long) row[0], ((Number) row[1]).doubleValue());
            }
        }
        return map;
    }
}
