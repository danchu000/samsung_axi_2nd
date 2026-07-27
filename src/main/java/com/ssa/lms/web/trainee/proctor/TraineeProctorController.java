package com.ssa.lms.web.trainee.proctor;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.proctor.dto.ProctorWarningRow;
import com.ssa.lms.proctor.service.ProctorWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 응시자가 받은 감독관 경고 확인.
 *
 * <p>접근 권한: SecurityConfig 의 {@code /trainee/exam/**} → TRAINEE 전용.
 * 그 위에 서비스가 "본인 회차인지"를 다시 본다 — attemptId 만 바꿔 남의 경고를 볼 수 없어야 한다.</p>
 *
 * <p>응시 화면({@code trainee/do-test.html})의 컨트롤러({@code TraineeExamController})를 건드리지 않으려고
 * 별도 컨트롤러로 뺐다. 응시/제출 슬라이스와 동시 작업 중이라 그 파일은 손대지 않는다.</p>
 */
@RestController
@RequestMapping("/trainee/exam/proctor")
@RequiredArgsConstructor
public class TraineeProctorController {

    private final ProctorWarningService proctorWarningService;

    /** 아직 확인하지 않은 경고. 응시 화면이 주기적으로 폴링한다. */
    @GetMapping("/attempt/{attemptId}/warnings")
    public List<ProctorWarningRow> pending(@PathVariable Long attemptId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        return proctorWarningService.findPendingForTrainee(attemptId, loginUser.getId());
    }

    /** 경고 확인 처리. */
    @PostMapping("/warnings/{warningId}/ack")
    public Map<String, Object> acknowledge(@PathVariable Long warningId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        proctorWarningService.acknowledge(warningId, loginUser.getId());
        return Map.of("ok", true);
    }
}
