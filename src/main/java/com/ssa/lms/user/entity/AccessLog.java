package com.ssa.lms.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 접속 이력 — 내역서 보안 증빙 요건(접속 로그)이자 관리자 화면 "접속 이력" 데이터.
 * 이력 테이블이므로 수정/삭제하지 않고 계속 쌓는다 (3년 보존).
 */
@Entity
@Table(name = "access_log", indexes = {
        @Index(name = "idx_access_log_user", columnList = "user_id, occurred_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessLog {

    /**
     * {@code CREDENTIAL_CHANGE} 는 최고 관리자가 <b>남의</b> 계정 아이디/이메일/비밀번호를 바꾼 기록이다.
     * userId·loginId 는 대상 계정, ip/userAgent 는 작업한 최고 관리자의 것 — 대상자의 접속 이력 화면에서
     * "언제 계정 정보가 손대졌는지"가 바로 보이게 하려는 배치다.
     */
    public enum Type { LOGIN, LOGOUT, LOGIN_FAIL, IDENTITY_VERIFY, CREDENTIAL_CHANGE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 실패 등 사용자 특정 불가 시 null */
    @Column(name = "user_id")
    private Long userId;

    /** 시도된 로그인 ID 스냅샷 */
    @Column(name = "login_id", length = 50)
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    private AccessLog(Long userId, String loginId, Type type, String ipAddress, String userAgent,
                      LocalDateTime occurredAt) {
        this.userId = userId;
        this.loginId = loginId;
        this.type = type;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.occurredAt = occurredAt != null ? occurredAt : LocalDateTime.now();
    }
}
