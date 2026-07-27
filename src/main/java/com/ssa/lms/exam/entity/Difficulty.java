package com.ssa.lms.exam.entity;

/**
 * 난이도. 문제은행(question)과 과제(assignment)가 공유한다.
 * 화면 값: easy / medium / hard (question-bank-add), Easy / Medium / Hard (test-add 출제규칙)
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
