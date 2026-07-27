package com.ssa.lms.proctor.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 감독 기능 권한 위반 → 403.
 *
 * <p>두 가지 경우가 있다.</p>
 * <ul>
 *   <li>강사가 담당하지 않는 과정을 보려 할 때 (권한정의서 △)</li>
 *   <li>강사가 응시 무효 처리를 하려 할 때 (권한정의서(1) 16행 — 제재는 관리자 O / 강사 △경고만)</li>
 * </ul>
 *
 * <p>URL 레벨 차단(SecurityConfig)만으로는 두 경우 다 막을 수 없다. 강사도 같은 URL 을
 * 정당하게 쓰기 때문이다. 그래서 서비스에서 한 번 더 본다.</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ProctorAccessDeniedException extends RuntimeException {

    public ProctorAccessDeniedException(String message) {
        super(message);
    }
}
