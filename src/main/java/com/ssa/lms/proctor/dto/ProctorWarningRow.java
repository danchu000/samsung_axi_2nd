package com.ssa.lms.proctor.dto;

/** 감독관이 발송한 경고 한 건. 응시자 화면과 감독 화면이 같은 DTO 를 쓴다. */
public record ProctorWarningRow(
        Long id,
        Long attemptId,
        String message,
        String sentByName,
        String sentAt,
        String acknowledgedAt,
        boolean acknowledged
) {
}
