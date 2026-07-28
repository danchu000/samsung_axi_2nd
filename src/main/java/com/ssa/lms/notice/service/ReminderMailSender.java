package com.ssa.lms.notice.service;

import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 독려·리마인드 <b>메일</b> 발송기.
 *
 * <p>인앱 알림({@code Notification})은 훈련생이 LMS 에 들어와야 본다. 마감 임박 독려는
 * 안 들어온 사람에게 보내는 것이라 인앱만으로는 도달하지 않는다. 그래서 같은 문구를
 * 메일로도 한 번 더 보낸다. <b>인앱을 대체하는 게 아니라 추가</b>다.</p>
 *
 * <h3>설정</h3>
 * <table>
 *   <tr><th>키</th><th>기본값</th><th>설명</th></tr>
 *   <tr><td>{@code lms.mail.enabled}</td><td>{@code false}</td>
 *       <td>꺼져 있으면 <b>실제로 보내지 않고 로그만</b> 남긴다. local/dev 에서 실수로
 *           훈련생에게 메일이 나가는 사고를 막기 위한 기본값이다.</td></tr>
 *   <tr><td>{@code lms.mail.from}</td><td>{@code no-reply@ssa.local}</td><td>발신 주소</td></tr>
 *   <tr><td>{@code lms.mail.subject-prefix}</td><td>{@code [삼성 청년 SW·AI 아카데미]}</td>
 *       <td>메일 제목 앞에 붙는 기관명. 본문은 인앱 알림과 같은 문구를 쓴다.</td></tr>
 * </table>
 *
 * <p>SMTP 접속 정보({@code spring.mail.host/port/username/password})는 <b>application.yml 에 두지 않는다</b>
 * — 공동 소유 파일이고 자격증명이 저장소에 들어가면 안 된다. 환경변수나 실행 인자로 주입한다:</p>
 * <pre>
 *   SPRING_MAIL_HOST=smtp.example.com SPRING_MAIL_PORT=587 \
 *   SPRING_MAIL_USERNAME=... SPRING_MAIL_PASSWORD=... \
 *   LMS_MAIL_ENABLED=true ./gradlew bootRun
 *   # 또는
 *   ./gradlew bootRun --args='--lms.mail.enabled=true --spring.mail.host=smtp.example.com'
 * </pre>
 *
 * <p><b>{@link ObjectProvider} 로 받는 이유:</b> {@code spring.mail.host} 가 없는 프로필에서는
 * 스프링 부트가 {@link JavaMailSender} 빈을 아예 만들지 않는다. 생성자로 직접 받으면
 * local/H2 부팅이 통째로 깨진다 (build.gradle 의 A 주석과 같은 이유).</p>
 *
 * <p><b>실패는 절대 위로 던지지 않는다.</b> 한 사람 메일이 실패했다고 그 사람의 인앱 알림이나
 * 뒤이은 다른 사람의 발송이 멈추면 안 된다. 모든 예외를 여기서 삼키고 결과만 돌려준다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderMailSender {

    /** 발송 결과 — 호출부가 집계·로그에 쓴다. 예외 대신 이 값으로 실패를 알린다. */
    public enum Result {
        /** 실제로 SMTP 로 넘겼다. */
        SENT,
        /** {@code lms.mail.enabled=false} — 로그만 남기고 보내지 않았다. */
        DRY_RUN,
        /** 수신자에게 메일 주소가 없다 (또는 형식이 아니다) — 인앱만 나간다. */
        NO_ADDRESS,
        /** 발송을 시도했으나 실패했다. 인앱 알림은 이미 나갔다. */
        FAILED
    }

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${lms.mail.enabled:false}")
    private boolean enabled;

    @Value("${lms.mail.from:no-reply@ssa.local}")
    private String from;

    @Value("${lms.mail.subject-prefix:[삼성 청년 SW·AI 아카데미]}")
    private String subjectPrefix;

    /**
     * 메일 1건 발송.
     *
     * @param user    수신자. {@code User.email} 은 AES-256 암호문이라 엔티티로 읽어야 평문이 나온다.
     *                (DB 를 직접 조회하면 암호문이 그대로 나오므로 여기서 엔티티를 받는다)
     * @param title   인앱 알림 제목 — 기관명을 앞에 붙여 메일 제목으로 쓴다
     * @param content 인앱 알림 본문 — 그대로 메일 본문으로 쓴다
     * @return 발송 결과. <b>절대 예외를 던지지 않는다.</b>
     */
    public Result send(User user, String title, String content) {
        String to = addressOf(user);
        if (to == null) {
            // 메일 주소가 없는 계정은 정상 상황이다 (관리자가 수기 등록한 훈련생 등). 인앱만 나간다.
            // INFO 로 남기는 이유: "메일이 왜 안 갔나"를 운영자가 로그만 보고 답할 수 있어야 한다.
            log.info("[메일-주소없음] userId={} — 메일 주소가 없어 인앱 알림만 발송",
                    user == null ? null : user.getId());
            return Result.NO_ADDRESS;
        }

        String subject = subjectPrefix + " " + title;

        if (!enabled) {
            // local/dev 기본 경로. 실제 훈련생 주소로 메일이 나가는 사고를 막는다.
            log.info("[메일-미발송] lms.mail.enabled=false — to={}, subject={}", to, subject);
            return Result.DRY_RUN;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            // enabled 는 켰는데 spring.mail.host 가 없는 상태. 설정 실수라 눈에 띄어야 한다.
            log.warn("[메일-발송기없음] lms.mail.enabled=true 인데 spring.mail.host 가 없다 — to={}", to);
            return Result.FAILED;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            sender.send(message);
            log.info("[메일-발송] to={}, subject={}", to, subject);
            return Result.SENT;
        } catch (Exception e) {
            // RuntimeException(MailException) 뿐 아니라 드라이버가 던지는 Error 성 예외까지 막는다.
            // 여기서 새어 나가면 이 사람 이후의 발송이 통째로 멈춘다.
            log.error("[메일-실패] to={}, subject={} — 인앱 알림은 정상 발송됨", to, subject, e);
            return Result.FAILED;
        }
    }

    /**
     * 수신 주소 추출. 없거나 형식이 아니면 null.
     *
     * <p>{@code @} 를 확인하는 이유: 주소가 아닌 값이 들어 있으면 {@code SimpleMailMessage} 가
     * 발송 시점에 파싱 예외를 던진다. 미리 걸러 "주소 없음"으로 처리하는 편이 로그가 읽힌다.</p>
     */
    private String addressOf(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        String email = user.getEmail().trim();
        if (email.isEmpty() || !email.contains("@")) {
            return null;
        }
        return email;
    }
}
