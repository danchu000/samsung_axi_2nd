package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.ExamQuestionRule;
import com.ssa.lms.exam.entity.ExamQuestionRuleDifficulty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자동 출제 규칙 한 건.
 *
 * 화면 admin-evaluation-test-add.html 의 addRuleModal 한 벌에 대응한다.
 *  addRuleSubject / addRuleTopic / addRuleSubtopic / addRuleTags / addRuleCount
 *  + addRuleDifficultyLevel1~3, addRuleDifficultyCount1~3
 *
 * 모달은 "추가" 시 JS 로 hidden input(rules[i].xxx)을 만들어 시험 폼과 함께 전송한다.
 * 모달 안의 id 속성은 기존 JS 가 getElementById 로 쓰므로 손대지 않았다.
 */
@Getter
@Setter
public class ExamRuleForm {

    private Long id;

    /** 대분류 (addRuleSubject). */
    private String categoryL;
    /** 중분류 (addRuleTopic). */
    private String categoryM;
    /** 소분류 (addRuleSubtopic). */
    private String categoryS;

    private String tags;

    /** 총 문항수 (addRuleCount). 화면에서 난이도 개수 합으로 자동 계산된다. */
    private Integer totalCount;

    private String difficultyLevel1;
    private Integer difficultyCount1;
    private String difficultyLevel2;
    private Integer difficultyCount2;
    private String difficultyLevel3;
    private Integer difficultyCount3;

    /**
     * 난이도 3줄을 자식 엔티티로 정규화한다.
     *
     * 같은 난이도를 두 줄에 적으면 uk_rule_difficulty(rule_id, difficulty) 유니크 제약에
     * 걸리므로 여기서 개수를 합쳐 한 줄로 만든다.
     */
    public List<ExamQuestionRuleDifficulty> toDifficulties() {
        Map<Difficulty, Integer> merged = new LinkedHashMap<>();
        put(merged, difficultyLevel1, difficultyCount1);
        put(merged, difficultyLevel2, difficultyCount2);
        put(merged, difficultyLevel3, difficultyCount3);

        List<ExamQuestionRuleDifficulty> result = new ArrayList<>();
        merged.forEach((difficulty, count) -> result.add(ExamQuestionRuleDifficulty.builder()
                .difficulty(difficulty)
                .questionCount(count)
                .build()));
        return result;
    }

    /** 난이도 줄이 하나도 없으면 총 문항수만으로 뽑는다. */
    public int resolveTotalCount() {
        if (totalCount != null && totalCount > 0) {
            return totalCount;
        }
        return sum(difficultyCount1) + sum(difficultyCount2) + sum(difficultyCount3);
    }

    public boolean isEmptyRow() {
        return blank(categoryL) && blank(categoryM) && blank(categoryS)
                && blank(tags) && resolveTotalCount() == 0;
    }

    public ExamQuestionRule toEntity() {
        ExamQuestionRule rule = ExamQuestionRule.builder()
                .categoryL(nullIfBlank(categoryL))
                .categoryM(nullIfBlank(categoryM))
                .categoryS(nullIfBlank(categoryS))
                .tags(nullIfBlank(tags))
                .totalCount(resolveTotalCount())
                .build();
        toDifficulties().forEach(rule::addDifficulty);
        return rule;
    }

    public static ExamRuleForm from(ExamQuestionRule rule) {
        ExamRuleForm form = new ExamRuleForm();
        form.id = rule.getId();
        form.categoryL = rule.getCategoryL();
        form.categoryM = rule.getCategoryM();
        form.categoryS = rule.getCategoryS();
        form.tags = rule.getTags();
        form.totalCount = rule.getTotalCount();

        List<ExamQuestionRuleDifficulty> rows = rule.getDifficulties();
        for (int i = 0; i < rows.size() && i < 3; i++) {
            String level = rows.get(i).getDifficulty().name();
            Integer count = rows.get(i).getQuestionCount();
            switch (i) {
                case 0 -> { form.difficultyLevel1 = level; form.difficultyCount1 = count; }
                case 1 -> { form.difficultyLevel2 = level; form.difficultyCount2 = count; }
                default -> { form.difficultyLevel3 = level; form.difficultyCount3 = count; }
            }
        }
        return form;
    }

    /* ===== 내부 ===== */

    private static void put(Map<Difficulty, Integer> map, String level, Integer count) {
        if (blank(level) || count == null || count <= 0) {
            return;
        }
        Difficulty difficulty = Difficulty.valueOf(level.trim().toUpperCase());
        map.merge(difficulty, count, Integer::sum);
    }

    private static int sum(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String nullIfBlank(String v) {
        return blank(v) ? null : v.trim();
    }
}
