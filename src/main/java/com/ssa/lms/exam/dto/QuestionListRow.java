package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Question;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 문제은행 목록 한 행.
 *
 * 화면(admin-evaluation-question-bank.html) 테이블 헤더와 1:1 대응:
 * 유형 / 제목 / 난이도 / 카테고리 / 사용중인 과정 수 / 평균 성취도 / 생성일 / 상태 / 수정하기
 *
 * static/js/contents.js 의 더미 행 shape 과 같은 모양이라, A의 콘텐츠(영상·문서·강의)
 * 목록도 같은 레코드로 만들어 한 목록에 병합할 수 있다.
 *
 * 엔티티를 그대로 화면에 넘기지 않는 이유: LAZY 프록시 문제 + 정답/해설이
 * 목록 HTML 에 실려 나가면 안 되기 때문.
 */
public record QuestionListRow(
        /** 화면 JS 가 data-id 문자열과 === 로 비교하므로 문자열로 내린다. */
        String id,
        String code,
        /** 화면 유형 라벨. 문제은행은 항상 "문제". */
        String type,
        String title,
        String difficulty,
        String category,
        long usedCourseCount,
        /** "85%" 형태. 채점 이력이 없으면 "-". */
        String avgAchievement,
        String createdAt,
        /** 화면 값 Active / Archived */
        String status
) {

    public static QuestionListRow of(Question q, long usedCourseCount, Double correctRatio) {
        return new QuestionListRow(
                String.valueOf(q.getId()),
                q.getQuestionCode(),
                "문제",
                titleOf(q),
                q.getDifficulty().name().toLowerCase(),
                q.getCategoryL(),
                usedCourseCount,
                formatAchievement(correctRatio),
                formatDate(q.getCreatedAt()),
                q.getStatus() == Question.QuestionStatus.ACTIVE ? "Active" : "Archived"
        );
    }

    /** 목록 제목은 지문 앞부분으로 만든다. 정답·해설은 절대 포함하지 않는다. */
    private static String titleOf(Question q) {
        String text = q.getQuestionText() == null ? "" : q.getQuestionText().strip();
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }

    private static String formatAchievement(Double ratio) {
        return ratio == null ? "-" : Math.round(ratio * 100) + "%";
    }

    private static String formatDate(LocalDateTime at) {
        return at == null ? "-" : at.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
