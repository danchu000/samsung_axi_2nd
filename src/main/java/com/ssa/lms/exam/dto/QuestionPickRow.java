package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Question;

/**
 * 시험 출제용 문제 선택 모달(questionModal)의 한 행.
 *
 * 문제은행 화면용 {@link QuestionListRow} 와 달리 카테고리 3단계와 난이도를 그대로 내려준다.
 * 모달 JS 가 대/중/소분류로 클라이언트 필터링을 하기 때문이다.
 * 정답·해설은 절대 포함하지 않는다 (출제 화면에 실려 나가면 안 된다).
 */
public record QuestionPickRow(
        String id,
        String code,
        String title,
        String difficulty,
        String categoryL,
        String categoryM,
        String categoryS,
        String tags,
        int score
) {

    public static QuestionPickRow of(Question q) {
        return new QuestionPickRow(
                String.valueOf(q.getId()),
                q.getQuestionCode(),
                titleOf(q),
                q.getDifficulty().name().toLowerCase(),
                nvl(q.getCategoryL()),
                nvl(q.getCategoryM()),
                nvl(q.getCategoryS()),
                nvl(q.getTags()),
                q.getScore() == null ? 0 : q.getScore()
        );
    }

    private static String titleOf(Question q) {
        String text = q.getQuestionText() == null ? "" : q.getQuestionText().strip();
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
