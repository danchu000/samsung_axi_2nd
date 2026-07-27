package com.ssa.lms.exam.service;

import com.ssa.lms.exam.dto.QuestionForm;
import com.ssa.lms.exam.dto.QuestionListRow;
import com.ssa.lms.exam.dto.QuestionSearchCond;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import com.ssa.lms.exam.repository.QuestionRepository;
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

        // 보기를 통째로 교체한다. clear() 만 하고 바로 add() 하면 Hibernate 가
        // orphan DELETE 보다 INSERT 를 먼저 내보내 uk_question_choice_seq(question_id, seq)
        // 유니크 제약에 걸린다. 그래서 삭제를 먼저 flush 한 뒤에 새 보기를 넣는다.
        question.clearChoices();
        questionRepository.flush();
        applyChoices(question, form);
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
