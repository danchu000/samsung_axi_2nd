package com.ssa.lms.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 약관·방침 정적 문서 화면 — 이용약관 / 개인정보처리방침.
 *
 * <p>푸터(fragments/trainee.html)와 로그인·가입 화면에서 링크된다. 가입 전 이용자도 읽을 수 있어야
 * 하므로 {@code SecurityConfig} 에서 {@code permitAll} 로 열어 둔다 (동의 대상 문서를 로그인해야만
 * 볼 수 있으면 동의 자체가 성립하지 않는다).</p>
 *
 * <p>문서 내용은 실제 구현(수집 항목·보관 기간·암호화 방식·승인 절차)에서 뽑아낸 것이다.
 * 관련 구현이 바뀌면 템플릿의 주석에 적힌 근거 클래스와 함께 문서도 고쳐야 한다.
 * 기관이 채워야 하는 값은 템플릿에서 {@code .legal-fill} 로 표시돼 있다.</p>
 */
@Controller
public class LegalController {

    @GetMapping("/terms")
    public String terms() {
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "legal/privacy";
    }
}
