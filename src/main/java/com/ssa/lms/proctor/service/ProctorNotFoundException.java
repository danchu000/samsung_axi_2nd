package com.ssa.lms.proctor.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 감독 대상(시험/응시/녹화)이 없을 때 → 404.
 *
 * <p>{@code IllegalArgumentException} 을 그대로 던지면 500 이 나가고 서버 로그에 스택이 쌓인다.
 * 존재하지 않는 id 는 클라이언트 잘못이지 서버 장애가 아니다.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProctorNotFoundException extends RuntimeException {

    public ProctorNotFoundException(String message) {
        super(message);
    }
}
