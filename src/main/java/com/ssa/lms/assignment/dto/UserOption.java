package com.ssa.lms.assignment.dto;

/**
 * 사용자 셀렉트/표시 옵션.
 *
 * 이름 외의 개인정보(생년월일·연락처·이메일)는 담지 않는다 — AES 암호문이기도 하고
 * 채점 화면에 실려 나갈 이유도 없다. 화면이 이름 옆에 식별자를 요구하면 loginId 를 쓴다.
 */
public record UserOption(Long id, String loginId, String name) {
}
