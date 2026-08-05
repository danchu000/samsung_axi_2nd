package com.ssa.lms.completion.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.completion.service.CertificateService;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.demo.SampleScreenData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * 훈련생 이수관리 — <b>본인</b> 이수 현황 조회 + 이수증(PDF) 다운로드.
 *
 * <p>권한 경계: 목록은 인증 주체 것만 조회한다. 이수증 다운로드는 completionId 로 접근하므로
 * 본인 소유인지 반드시 검증하고({@link CompletionService#isOwnedByTrainee}), 아니면 404 로
 * 존재 여부 자체를 노출하지 않는다. PDF 생성은 관리자와 동일한 {@link CertificateService} 를 재사용한다.</p>
 */
@Controller
@RequestMapping("/trainee/completion-management")
@RequiredArgsConstructor
public class TraineeCompletionController {

    private final CompletionService completionService;
    private final CertificateService certificateService;
    private final SampleScreenData sampleScreenData;

    @GetMapping
    public String completion(@AuthenticationPrincipal LoginUser user, Model model) {
        var filled = sampleScreenData.fill(
                completionService.viewsByTrainee(user.getId()),
                sampleScreenData::completions);
        model.addAttribute("completions", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        return "trainee/completion-management";
    }

    /** 이수증 PDF — 본인 소유 & 이수 확정(PASS+CONFIRMED) 건만. 새 탭 인라인 표시. */
    @GetMapping("/{completionId}/certificate")
    public ResponseEntity<byte[]> certificate(@PathVariable Long completionId,
                                              @AuthenticationPrincipal LoginUser user) {
        // 예시 행에는 발급할 이수증이 없다. 새 탭으로 열리는 링크라 플래시 메시지가 보이지 않으므로
        // 그 탭에 바로 이유를 적어 보낸다 (빈 PDF 나 404 보다 낫다).
        if (SampleScreenData.isSampleId(completionId)) {
            return ResponseEntity.ok()
                    .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                    .body(("<!doctype html><html lang=\"ko\"><meta charset=\"utf-8\">"
                            + "<title>이수증 안내</title>"
                            + "<body style=\"font-family:sans-serif;padding:48px;text-align:center;color:#374151;\">"
                            + "<h1 style=\"font-size:20px;\">화면 예시용 데이터입니다</h1>"
                            + "<p>실제 과정이 이수 확정되면 이 버튼에서 이수증 PDF 를 내려받을 수 있어요.</p>"
                            + "</body></html>").getBytes(StandardCharsets.UTF_8));
        }
        if (!completionService.isOwnedByTrainee(completionId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이수 정보를 찾을 수 없습니다.");
        }
        byte[] pdf = certificateService.generate(completionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("certificate-" + completionId + ".pdf").build().toString())
                .body(pdf);
    }
}
