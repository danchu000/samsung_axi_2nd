package com.ssa.lms.exam.dto;

/**
 * 화면 셀렉트 박스 한 칸.
 *
 * 과정·과목·차시·강사는 A 소유 엔티티라 화면으로 그대로 넘기지 않고
 * (id, 라벨) 두 값만 뽑아 이 레코드로 내린다.
 * value 는 화면 JS 가 문자열 비교를 하므로 String 이다.
 */
public record OptionItem(String value, String label) {
}
