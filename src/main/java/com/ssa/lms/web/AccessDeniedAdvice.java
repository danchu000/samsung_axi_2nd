package com.ssa.lms.web;

import com.ssa.lms.auth.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 권한 부족을 <b>안내 화면</b>으로 바꾼다.
 *
 * <p><b>왜 필요한가:</b> B 도메인 서비스들이 {@code AccessDeniedException} 을 던지는데
 * 이를 받는 곳이 없어 브라우저에 흰 403 페이지가 떴다. 사용자는 무엇이 잘못됐는지도,
 * 어떻게 권한을 받는지도 알 수 없었다. 특히 강사가 담당 아닌 과정을 열었을 때
 * "왜 안 되는지"를 알려줘야 운영 관리자에게 담당자 지정을 요청할 수 있다.</p>
 *
 * <p><b>범위를 B 컨트롤러 패키지로 한정했다.</b> 전역으로 열면 개발자 A 의 화면
 * (인증·사용자·과정·출결·이수)까지 이 안내로 가로채게 된다. 공통 {@code @ControllerAdvice}
 * 소유자가 정해지면 그쪽으로 이관하는 것이 맞다 — A 에게 요청해 둔 사항이다.</p>
 *
 * <p>JSON 응답이 필요한 경로(응시 중 답안 저장 등)는 각 컨트롤러가 자체적으로
 * 상태코드를 내리므로 여기까지 오지 않는다.</p>
 */
@Slf4j
@ControllerAdvice(basePackages = {
        "com.ssa.lms.web.admin.exam",
        "com.ssa.lms.web.admin.grading",
        "com.ssa.lms.web.admin.proctor",
        "com.ssa.lms.web.admin.assignment",
        "com.ssa.lms.web.admin.support",
        "com.ssa.lms.web.admin.notice",
        "com.ssa.lms.web.admin.survey",
        "com.ssa.lms.web.instructor",
        "com.ssa.lms.web.trainee"
})
public class AccessDeniedAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handle(AccessDeniedException e,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model) {
        // 사유는 서비스가 던진 메시지를 그대로 쓴다 — "담당하지 않는 과정의 시험입니다" 처럼
        // 구체적이어야 사용자가 다음 행동을 정할 수 있다.
        model.addAttribute("reason", e.getMessage());
        model.addAttribute("role",
                loginUser == null ? null : loginUser.getRole().name());
        model.addAttribute("homeUrl", homeUrlOf(loginUser));

        log.info("[접근거부] user={} role={} 사유={}",
                loginUser == null ? "-" : loginUser.getId(),
                loginUser == null ? "-" : loginUser.getRole(),
                e.getMessage());
        return "error/access-denied";
    }

    /** 역할별 홈으로 돌려보낸다. 루트(/)로 보내면 다시 권한 없는 화면으로 갈 수 있다. */
    private String homeUrlOf(LoginUser loginUser) {
        if (loginUser == null) {
            return "/login";
        }
        return switch (loginUser.getRole()) {
            case ADMIN -> "/admin";
            case INSTRUCTOR -> "/instructor";
            case TRAINEE -> "/trainee";
        };
    }
}
