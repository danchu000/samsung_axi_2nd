package com.ssa.lms.web.admin.grading;

import com.ssa.lms.grading.service.GradingException;
import com.ssa.lms.web.instructor.grading.InstructorGradingController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

/**
 * 채점 <b>화면(HTML) 경로</b>에서 새어 나온 {@link GradingException} 을 알맞은 상태코드로 바꾼다.
 *
 * <p>JSON API 경로는 {@code AdminGradingController.handle()} 안에서 이미 잡아 400/403 으로 내리므로
 * 여기까지 오지 않는다. 여기서 처리하는 건 화면 라우트다 — 없는 examId/attemptId 로 들어왔을 때
 * 그냥 두면 500 이 떠서 "서버 장애"와 "잘못된 링크"를 구분할 수 없다.</p>
 *
 * <p>범위를 채점 컨트롤러 둘로 못 박아 둔다. 전역 {@code @ControllerAdvice} 로 열면
 * 다른 슬라이스의 예외 처리까지 조용히 가로채게 된다.</p>
 */
@ControllerAdvice(assignableTypes = {AdminGradingController.class, InstructorGradingController.class})
public class GradingControllerAdvice {

    /**
     * {@code sendError} 로 컨테이너 에러 페이지(/error)에 넘긴다.
     * 여기서 {@code ResponseStatusException} 을 다시 던지면 예외 해석이 이미 끝난 뒤라
     * 그대로 500 이 된다 (실제로 그렇게 나왔다).
     */
    @ExceptionHandler(GradingException.class)
    public void handle(GradingException e, HttpServletResponse response) throws IOException {
        HttpStatus status = "NOT_FOUND".equals(e.getCode())
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        response.sendError(status.value(), e.getMessage());
    }
}
