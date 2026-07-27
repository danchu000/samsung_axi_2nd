package com.ssa.lms.completion.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssa.lms.completion.entity.Completion;
import com.ssa.lms.completion.repository.CompletionRepository;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 이수증(수료증) PDF 발급. 이수 확정(PASS+CONFIRMED)된 {@link Completion} 에 대해서만 발급한다.
 *
 * <p>HTML 템플릿 문자열을 openhtmltopdf 로 PDF 렌더링하며, 한글 표기를 위해 시스템 폰트(맑은 고딕)를
 * 임베드한다. 폰트 경로는 {@code lms.certificate.font-path}(기본 {@code C:/Windows/Fonts/malgun.ttf})
 * 로 재정의할 수 있다. (운영 리눅스 환경에서는 나눔고딕 등 배포 폰트 경로로 설정.)</p>
 */
@Slf4j
@Service
public class CertificateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    private final CompletionRepository completionRepository;
    private final String fontPath;

    public CertificateService(CompletionRepository completionRepository,
                              @Value("${lms.certificate.font-path:C:/Windows/Fonts/malgun.ttf}") String fontPath) {
        this.completionRepository = completionRepository;
        this.fontPath = fontPath;
    }

    /** 이수증 PDF 바이트 생성. 발급 불가(미이수/미확정) 시 {@link IllegalStateException}. */
    @Transactional(readOnly = true)
    public byte[] generate(Long completionId) {
        Completion c = completionRepository.findById(completionId)
                .orElseThrow(() -> new IllegalArgumentException("이수 정보를 찾을 수 없습니다: " + completionId));
        if (!c.isCertificateIssuable()) {
            throw new IllegalStateException("이수 확정된 수강생만 이수증을 발급할 수 있습니다.");
        }

        Course course = c.getCourse();
        User trainee = c.getTrainee();
        LocalDate issueDate = c.getConfirmedAt() != null ? c.getConfirmedAt().toLocalDate() : LocalDate.now();
        String certNo = "CERT-" + course.getCourseCode() + "-" + String.format("%06d", c.getId());

        String html = buildHtml(
                certNo,
                esc(trainee.getName()),
                trainee.getBirthDate() != null ? esc(trainee.getBirthDate()) : "-",
                esc(course.getCourseName()),
                course.getCohort() != null ? esc(course.getCohort()) : "-",
                course.getStartDate(), course.getEndDate(),
                c.getProgressRate(), c.getAttendanceRate(),
                issueDate);

        return render(html);
    }

    private byte[] render(String html) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            File font = new File(fontPath);
            if (font.exists()) {
                builder.useFont(font, "certFont");
            } else {
                log.warn("[certificate] 한글 폰트를 찾을 수 없습니다({}). 한글이 깨질 수 있습니다. lms.certificate.font-path 설정 필요.", fontPath);
            }
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("이수증 PDF 생성에 실패했습니다.", e);
        }
    }

    private String buildHtml(String certNo, String name, String birth, String courseName, String cohort,
                             LocalDate start, LocalDate end, int progressRate, int attendanceRate,
                             LocalDate issueDate) {
        // CSS 에 리터럴 '%' 가 있어 String.format/formatted 는 쓰지 않고 토큰 치환한다.
        String template = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"/>
            <style>
              @page { size: A4; margin: 30mm 25mm; }
              body { font-family: 'certFont', sans-serif; color: #222; }
              .frame { border: 3px solid #1c3d6e; padding: 24px 28px; }
              .cert-no { text-align: right; font-size: 11px; color: #666; }
              h1 { text-align: center; font-size: 40px; letter-spacing: 20px; color: #1c3d6e; margin: 40px 0 50px; }
              table.info { width: 100%; border-collapse: collapse; margin: 0 auto 40px; font-size: 15px; }
              table.info th { width: 30%; text-align: left; padding: 12px 10px; background: #f2f5fa; border: 1px solid #d8e0ec; color: #33466b; }
              table.info td { padding: 12px 14px; border: 1px solid #d8e0ec; }
              .statement { text-align: center; font-size: 16px; line-height: 2.1; margin: 30px 20px 50px; }
              .statement .hl { font-weight: bold; color: #1c3d6e; }
              .issue-date { text-align: center; font-size: 16px; margin-top: 60px; }
              .org { text-align: center; font-size: 22px; font-weight: bold; letter-spacing: 6px; margin-top: 14px; }
            </style></head>
            <body>
            <div class="frame">
              <div class="cert-no">발급번호: {{certNo}}</div>
              <h1>이 수 증</h1>
              <table class="info">
                <tr><th>성명</th><td>{{name}}</td></tr>
                <tr><th>생년월일</th><td>{{birth}}</td></tr>
                <tr><th>과정명 / 기수</th><td>{{courseName}} / {{cohort}}</td></tr>
                <tr><th>훈련기간</th><td>{{start}} ~ {{end}}</td></tr>
                <tr><th>진도율 / 출석률</th><td>{{progress}}% / {{attendance}}%</td></tr>
              </table>
              <div class="statement">
                위 사람은 <span class="hl">Samsung Academy LXP</span> 에서 시행한<br/>
                <span class="hl">{{courseName}}</span> 과정을 성실히 이수하였기에<br/>
                이 증서를 수여합니다.
              </div>
              <div class="issue-date">{{issueDate}}</div>
              <div class="org">Samsung Academy LXP</div>
            </div>
            </body></html>
            """;
        return template
                .replace("{{certNo}}", certNo)
                .replace("{{name}}", name)
                .replace("{{birth}}", birth)
                .replace("{{courseName}}", courseName)
                .replace("{{cohort}}", cohort)
                .replace("{{start}}", start.toString())
                .replace("{{end}}", end.toString())
                .replace("{{progress}}", String.valueOf(progressRate))
                .replace("{{attendance}}", String.valueOf(attendanceRate))
                .replace("{{issueDate}}", issueDate.format(DATE));
    }

    /** XHTML 안전을 위한 최소 이스케이프(성명/과정명 등 사용자 입력). */
    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
