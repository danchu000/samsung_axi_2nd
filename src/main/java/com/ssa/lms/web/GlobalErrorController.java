package com.ssa.lms.web;

import com.ssa.lms.auth.LoginUser;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code /error} 매핑 — <b>Whitelabel Error Page 를 없애기 위한 것</b>.
 *
 * <p><b>왜 필요한가:</b> 스프링 부트는 {@code /error} 에 매핑이 없으면
 * "Whitelabel Error Page" 라는 흰 화면을 띄운다. 훈련생이 오래된 링크(예전 정적 화면의
 * {@code /templates/trainee/assignments.html})를 누르면 404 가 나면서 이 화면이 떴다.
 * 사용자는 무엇이 잘못됐는지도, 어디로 가야 하는지도 알 수 없었다.
 * 게다가 개발 모드에서는 스택트레이스까지 그대로 노출된다.</p>
 *
 * <p><b>필터 단계 403 도 여기로 온다.</b> {@link AccessDeniedAdvice} 는
 * {@code @ControllerAdvice} 라서 컨트롤러 안에서 던져진 {@code AccessDeniedException} 만 잡는다.
 * SecurityConfig 의 URL 규칙에서 걸린 403 은 컨트롤러에 도달조차 못 하므로 이 컨트롤러가 받는다.</p>
 *
 * <p>JSON 요청(AJAX)에는 기존 {@code BasicErrorController} 와 같은 모양의 본문을 돌려준다 —
 * 화면 JS 가 {@code status}/{@code message} 를 읽는 곳이 있어 형태를 바꾸면 안 된다.</p>
 */
@Controller
public class GlobalErrorController implements ErrorController {

    /** 브라우저 요청 — 안내 화면. */
    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String errorPage(HttpServletRequest request,
                            @AuthenticationPrincipal LoginUser loginUser,
                            Model model) {
        int status = statusOf(request);

        model.addAttribute("status", status);
        model.addAttribute("title", titleOf(status));
        model.addAttribute("guide", guideOf(status, loginUser));
        model.addAttribute("homeUrl", homeUrlOf(loginUser));
        model.addAttribute("loggedIn", loginUser != null);
        return "error/error";
    }

    /** AJAX/API 요청 — 기존 형식 유지. 본문에 내부 정보(스택트레이스)는 절대 담지 않는다. */
    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> errorJson(HttpServletRequest request) {
        int status = statusOf(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", HttpStatus.resolve(status) == null
                ? "Error" : HttpStatus.valueOf(status).getReasonPhrase());
        body.put("message", titleOf(status));
        return ResponseEntity.status(status).body(body);
    }

    /* ===== 내부 ===== */

    private int statusOf(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer i && HttpStatus.resolve(i) != null) {
            return i;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String titleOf(int status) {
        return switch (status) {
            case 400 -> "잘못된 요청입니다";
            case 401 -> "로그인이 필요합니다";
            case 403 -> "이 화면에 접근할 권한이 없습니다";
            case 404 -> "페이지를 찾을 수 없습니다";
            case 405 -> "허용되지 않은 요청 방식입니다";
            case 413 -> "파일 용량이 너무 큽니다";
            case 503 -> "잠시 후 다시 시도해 주세요";
            default -> status >= 500
                    ? "일시적인 오류가 발생했습니다"
                    : "요청을 처리할 수 없습니다";
        };
    }

    /**
     * 다음 행동을 알려주는 문구. 사유를 모른 채 "오류" 만 보여주면 사용자는 새로고침만 반복한다.
     */
    private String guideOf(int status, LoginUser loginUser) {
        return switch (status) {
            case 401 -> "세션이 만료되었을 수 있습니다. 다시 로그인해 주세요.";
            case 403 -> loginUser == null
                    ? "로그인 후 이용할 수 있는 화면입니다."
                    : "이 화면은 다른 권한의 계정에서만 열 수 있습니다. 상단 메뉴에서 다시 선택해 주세요.";
            case 404 -> "주소가 바뀌었거나 삭제된 화면입니다. 상단 메뉴에서 다시 선택해 주세요.";
            case 413 -> "첨부 파일 용량 제한을 초과했습니다. 파일을 줄여 다시 시도해 주세요.";
            default -> status >= 500
                    ? "잠시 후 다시 시도해 주세요. 계속되면 운영 담당자에게 알려 주세요."
                    : "요청 내용을 확인한 뒤 다시 시도해 주세요.";
        };
    }

    /** 역할별 홈. 루트(/)로 보내면 다시 권한 없는 화면으로 갈 수 있다 (AccessDeniedAdvice 와 같은 방침). */
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
