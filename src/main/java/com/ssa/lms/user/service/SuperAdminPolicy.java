package com.ssa.lms.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 최고 관리자 판별 — <b>부트스트랩 관리자 계정 하나만</b> 최고 관리자로 본다(기본 loginId {@code admin}).
 *
 * <p>권한 enum({@link com.ssa.lms.user.entity.Role})은 3종으로 고정돼 있고 SecurityConfig 의
 * {@code /admin/**}=ADMIN 경계가 전 화면에 걸려 있다. 여기에 4번째 역할을 끼워 넣으면 경계 규칙을
 * 전부 다시 검토해야 하므로, 역할 체계는 건드리지 않고 <b>계정 하나를 지목하는 방식</b>으로 갔다
 * ({@code AdminAccountInitializer} 가 만드는 그 계정).</p>
 *
 * <p>운영 환경에서 아이디가 다르면 {@code LMS_SUPER_ADMIN_LOGIN_ID} 로 바꾼다. 값이 없어도
 * 기본값 {@code admin} 으로 뜨므로 앱 기동에는 영향이 없다.</p>
 *
 * <p><b>주의</b>: 판별 기준이 loginId 자체라서, 최고 관리자 계정의 아이디는 변경할 수 없다
 * ({@link AccountCredentialService} 에서 거부). 다른 계정이 이 아이디를 가져가는 것도 막는다.</p>
 */
@Component
public class SuperAdminPolicy {

    private final String superLoginId;

    public SuperAdminPolicy(@Value("${lms.admin.super-login-id:admin}") String superLoginId) {
        this.superLoginId = superLoginId;
    }

    /** 최고 관리자로 지정된 로그인 아이디. */
    public String superLoginId() {
        return superLoginId;
    }

    public boolean isSuperAdmin(String loginId) {
        return loginId != null && superLoginId.equals(loginId);
    }
}
