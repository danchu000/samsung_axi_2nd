package com.ssa.lms.proctor.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 녹화가 아직/더는 재생 가능한 상태가 아닐 때 → 409.
 *
 * <p>녹화중(RECORDING)·처리중(PROCESSING)·실패(FAILED)·보존기간 경과(PURGED) 가 여기에 해당한다.
 * 404 로 내리지 않는 이유는 <b>녹화 자체는 존재</b>하기 때문이다 — 보존기간이 지나 지워진 건과
 * 애초에 없던 건을 구분할 수 있어야 감사 대응이 된다.</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RecordingUnavailableException extends RuntimeException {

    public RecordingUnavailableException(String message) {
        super(message);
    }
}
