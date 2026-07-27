package com.ssa.lms.notice;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.notice.entity.Notice;
import com.ssa.lms.notice.entity.NoticeAttachment;
import com.ssa.lms.notice.entity.NoticeCategory;
import com.ssa.lms.notice.entity.Notification;
import com.ssa.lms.notice.entity.NotificationRecipient;
import com.ssa.lms.notice.repository.NoticeCategoryRepository;
import com.ssa.lms.notice.repository.NoticeRepository;
import com.ssa.lms.notice.repository.NotificationRecipientRepository;
import com.ssa.lms.notice.repository.NotificationRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * local 프로필 전용 공지/알림 시드 (개발자 B).
 *
 * 기존 정적 화면의 더미 배열을 옮긴 것이다.
 *  - static/js/notices.js         noticeData (37건, 카테고리 kdt/고교위탁/심화 순환)
 *  - static/js/notices-detail.js  noticeDetail (첨부 2건 + 본문)
 *  - static/js/trainee/notices.js dummyNotices (고정 공지 2건 포함)
 *  - admin-alarm.html 인라인      alarmHistory (5건)
 *
 * A의 {@code config.LocalDataInitializer} 와 파일을 나눠 두어 서로 충돌하지 않는다.
 *
 * <p><b>주의</b>: 예전에는 {@code CommandLineRunner + @Order(200)} 이었는데, A의
 * {@code LocalDataInitializer} 에 {@code @Order} 가 없어 LOWEST_PRECEDENCE 로 취급된다.
 * 그래서 B 시드가 <b>먼저</b> 돌아 admin 계정을 못 찾고 통째로 건너뛰었다(실측 확인).
 * {@code ApplicationReadyEvent} 는 모든 {@code CommandLineRunner} 가 끝난 뒤에 발행되므로
 * 순서 의존을 없애려고 이쪽으로 바꿨다. 되돌리지 말 것.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalNoticeDataInitializer {

    private final NoticeRepository noticeRepository;
    private final NoticeCategoryRepository noticeCategoryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (noticeRepository.count() > 0) {
            return;
        }
        User admin = userRepository.findByLoginId("admin").orElse(null);
        if (admin == null) {
            log.warn("[local] 공지 시드 건너뜀 — admin 계정이 없다 (A의 LocalDataInitializer 확인)");
            return;
        }
        User instructor = userRepository.findByLoginId("instructor1").orElse(admin);
        Course course = courseRepository.findAll().stream().findFirst().orElse(null);

        List<NoticeCategory> categories = seedCategories();
        seedNotices(categories, admin, instructor, course);
        seedNotifications(admin, course);

        log.info("[local] 공지 시드 {}건 / 알림 시드 {}건 생성",
                noticeRepository.count(), notificationRepository.count());
    }

    /** notices.js 의 카테고리 문자열(kdt/고교위탁/심화)을 코드 테이블로 (b-entity-mapping.md 결정 #3). */
    private List<NoticeCategory> seedCategories() {
        if (noticeCategoryRepository.count() > 0) {
            return noticeCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        }
        return noticeCategoryRepository.saveAll(List.of(
                NoticeCategory.builder().code("KDT").name("kdt").sortOrder(1).active(true).build(),
                NoticeCategory.builder().code("HIGHSCHOOL").name("고교위탁").sortOrder(2).active(true).build(),
                NoticeCategory.builder().code("ADVANCED").name("심화").sortOrder(3).active(true).build()
        ));
    }

    /**
     * notices.js 의 noticeData 37건을 그대로 옮긴다.
     * 원본은 id 37 → 1 역순이었으므로, 오름차순으로 넣어 PK 와 화면 번호가 맞게 한다.
     */
    private void seedNotices(List<NoticeCategory> categories, User admin, User instructor, Course course) {
        LocalDateTime base = LocalDateTime.now().minusDays(40);

        // 1번 글은 notices-detail.js 의 상세 더미(첨부 2건 + 긴 본문)를 그대로 쓴다.
        Notice detail = Notice.builder()
                .category(categories.get(0))
                .course(null)
                .title("2026년 1월 교육 일정 안내")
                .content(DETAIL_CONTENT)
                .author(admin)
                .pinned(true)
                .publishedAt(base)
                .build();
        detail.addAttachment(NoticeAttachment.builder()
                .originalName("교육일정표.pdf").storedPath("/files/교육일정표.pdf")
                .sizeBytes(482_133L).contentType("application/pdf").build());
        detail.addAttachment(NoticeAttachment.builder()
                .originalName("운영안내.docx").storedPath("/files/운영안내.docx")
                .sizeBytes(120_940L).contentType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document").build());
        noticeRepository.save(detail);

        // trainee/notices.js 의 dummyNotices — 훈련생 화면에 보이는 고정 공지 2건.
        noticeRepository.save(Notice.builder()
                .category(categories.get(0)).course(course)
                .title("[필독] 수강생 학습 유의사항 안내")
                .content("출석률 80% 이상 유지, 과제 제출 기한 엄수, 커뮤니티 활동 참여를 부탁드립니다.")
                .author(admin).pinned(true).publishedAt(base.plusDays(1)).build());

        noticeRepository.save(Notice.builder()
                .category(categories.get(0)).course(course)
                .title("1월 과제 제출 마감 일정 안내")
                .content("1월 과제는 1월 31일 23:59 까지 제출해야 합니다. 마감 후 제출은 지각 처리됩니다.")
                .author(instructor).pinned(true).publishedAt(base.plusDays(2)).build());

        // notices.js 의 나머지 — 페이징·검색 검증용으로 충분한 건수를 만든다.
        for (int i = 1; i <= 34; i++) {
            NoticeCategory category = categories.get((i - 1) % categories.size());
            noticeRepository.save(Notice.builder()
                    .category(category)
                    .course(i % 5 == 0 ? course : null)
                    .title("공지사항 제목 " + i)
                    .content("공지사항 " + i + " 본문입니다. 분류: " + category.getName())
                    .author(i % 2 == 0 ? admin : instructor)
                    .pinned(false)
                    .publishedAt(base.plusDays(2 + i))
                    .build());
        }
    }

    /** admin-alarm.html 의 alarmHistory 5건. 발송 완료 건은 수신자까지 펼친다. */
    private void seedNotifications(User admin, Course course) {
        if (notificationRepository.count() > 0) {
            return;
        }
        LocalDateTime base = LocalDateTime.now().minusDays(5);
        List<User> allUsers = userRepository.findAll();

        record Seed(String title, String content, Notification.Priority priority, int dayOffset) {
        }
        List<Seed> seeds = List.of(
                new Seed("출석률 위험", "홍길동 학생의 출석률이 60% 미만입니다. 조치가 필요합니다.",
                        Notification.Priority.HIGH, 4),
                new Seed("수강률 저조", "김민아 학생의 수강률이 70% 미만으로 떨어졌습니다.",
                        Notification.Priority.NORMAL, 3),
                new Seed("과정 출석률 위험", "Python 입문반 전체 출석률이 65%로 낮습니다.",
                        Notification.Priority.HIGH, 2),
                new Seed("수강률 경고", "JavaScript 중급 과정의 전체 수강률이 68%입니다.",
                        Notification.Priority.LOW, 1),
                new Seed("출석률 경고", "이영희 학생의 출석률이 50% 미만입니다.",
                        Notification.Priority.HIGH, 0)
        );

        for (Seed s : seeds) {
            Notification n = notificationRepository.save(Notification.builder()
                    .title(s.title()).content(s.content())
                    .priority(s.priority())
                    .targetType(course == null ? Notification.TargetType.ALL : Notification.TargetType.COURSE)
                    .targetRefId(course == null ? null : course.getId())
                    .sendAt(base.plusDays(s.dayOffset()))
                    .dueDate(base.plusDays(s.dayOffset() + 7))
                    .sender(admin)
                    .status(Notification.NotificationStatus.SENT)
                    .build());

            // 시드에서는 관리자·강사도 수신자로 넣어 화면의 읽음/읽지않음 배지를 확인할 수 있게 한다.
            for (User u : allUsers) {
                NotificationRecipient r = recipientRepository.save(
                        NotificationRecipient.builder().notification(n).user(u).build());
                if (u.getRole() == Role.ADMIN && s.dayOffset() % 2 == 1) {
                    r.markRead(LocalDateTime.now());
                }
            }
        }
    }

    private static final String DETAIL_CONTENT = """
            안녕하세요, 삼성 아카데미 LXP 교육생 여러분!

            2026년 1월 교육 일정 및 운영 방안을 안내드립니다. 올 한 해도 여러분의 성장과 발전을 위해 최선을 다하겠습니다.

            1. 교육 일정 안내
            - KDT 과정: 2026년 1월 20일 개강 예정입니다. 사전 준비사항을 확인해주세요.
            - 고교위탁 과정: 2026년 1월 25일 개강 예정입니다. 고등학교와의 연계를 강화하겠습니다.
            - 심화 과정: 2026년 1월 27일 개강 예정입니다. 고급 기술 교육에 집중하겠습니다.

            2. 운영 방안
            - 온라인 플랫폼을 통한 실시간 강의 제공
            - 프로젝트 기반 학습 강화
            - 멘토링 프로그램 확대 운영
            - 취업 지원 프로그램 강화

            3. 유의사항
            - 출석률 80% 이상 유지 필수
            - 과제 제출 기한 엄수
            - 커뮤니티 활동 적극 참여
            - 건강 관리 및 스트레스 관리 중요

            자세한 내용은 첨부파일을 참고해주시기 바랍니다. 궁금한 사항은 언제든지 문의해주세요.

            삼성 아카데미 LXP 운영팀 드림""";
}
