package com.ssa.lms.common.web;

import com.ssa.lms.content.service.ContentNotFoundException;
import com.ssa.lms.course.service.CourseNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * 전 컨트롤러 공통 예외 → HTTP 상태 매핑 (b-audit-reply.md §3-3(3) 합의로 신설, 소유: 공동).
 *
 * <p>서비스 계층이 "없는/삭제된 리소스"를 {@code IllegalArgumentException}(orElseThrow 관례)으로
 * 던지는데, 처리기가 없어 삭제된 리소스 URL 직접 접근 시 whitelabel 500 이 떴다. 여기서 404 로
 * 통일한다 — "서버 장애"와 "잘못된 링크"를 구분하기 위함.</p>
 *
 * <p>규칙:</p>
 * <ul>
 *   <li>{@code @Order(LOWEST_PRECEDENCE)} — 도메인별 advice(예: GradingControllerAdvice)와
 *       컨트롤러 내부 {@code @ExceptionHandler} 가 항상 우선한다. 여기는 마지막 안전망.</li>
 *   <li>{@code response.sendError} 로 컨테이너 에러 페이지(/error)에 넘긴다. 여기서
 *       {@code ResponseStatusException} 을 다시 던지면 예외 해석이 끝난 뒤라 500 이 된다
 *       (GradingControllerAdvice 에서 실증된 함정).</li>
 *   <li>업무 규칙 위반(400 계열)은 각 도메인이 자기 예외 타입으로 직접 처리할 것 —
 *       여기에 400 매핑을 추가하지 말 것.</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            NoSuchElementException.class,
            EntityNotFoundException.class,
            CourseNotFoundException.class,
            ContentNotFoundException.class
    })
    public void handleNotFound(RuntimeException e, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("[404] {} {} — {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        if (!response.isCommitted()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
        }
    }
}
