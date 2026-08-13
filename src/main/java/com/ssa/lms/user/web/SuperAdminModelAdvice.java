package com.ssa.lms.user.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.user.service.SuperAdminPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 사용자 관리 화면들이 "지금 보는 사람이 최고 관리자인가"를 알 수 있게 {@code superAdmin} 을 모델에 넣는다.
 *
 * <p>관리자 목록·사용자 수정 화면의 "계정정보 변경" 진입 버튼을 가리는 용도다. 화면에서 숨기는 것은
 * 어디까지나 안내이고, 실제 차단은 {@link AccountCredentialController} 의 진입부 검사가 한다.</p>
 */
@ControllerAdvice(basePackages = "com.ssa.lms.user.web")
@RequiredArgsConstructor
public class SuperAdminModelAdvice {

    private final SuperAdminPolicy superAdminPolicy;

    @ModelAttribute("superAdmin")
    public boolean superAdmin(@AuthenticationPrincipal LoginUser me) {
        return me != null && superAdminPolicy.isSuperAdmin(me.getUsername());
    }
}
