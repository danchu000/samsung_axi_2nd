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
import java.time.format.DateTimeFormatter;
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
                .title("교육 과정 운영 일정 안내")
                .content(detailContent(base))
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
                .title("과제 제출 마감 일정 안내")
                .content("과제별 마감 시각은 과제 상세에서 확인할 수 있습니다. 마감 후 제출은 지각 처리됩니다.")
                .author(instructor).pinned(true).publishedAt(base.plusDays(2)).build());

        // notices.js 의 나머지 — 페이징·검색 검증용으로 충분한 건수(34건)를 만든다.
        // 예전에는 "공지사항 제목 N" 이었는데 화면·제출 서류에 그대로 실려 실제 공지 문구 풀로 바꿨다.
        // 건수는 페이지네이션·정렬 검증이 목적이므로 그대로 유지한다.
        for (int i = 1; i <= 34; i++) {
            NoticeCategory category = categories.get((i - 1) % categories.size());
            NoticeSeed seed = NOTICE_SEEDS.get((i - 1) % NOTICE_SEEDS.size());
            // 풀(20건)을 한 바퀴 돌면 제목이 겹치므로 정기 공지처럼 차수를 붙여 구분한다.
            int round = (i - 1) / NOTICE_SEEDS.size() + 1;
            noticeRepository.save(Notice.builder()
                    .category(category)
                    .course(i % 5 == 0 ? course : null)
                    .title(round == 1 ? seed.title() : seed.title() + " (" + round + "차)")
                    .content(seed.content())
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
        // 등장하는 이름·과정명은 실제로 시드된 것만 쓴다. 예전에는 "홍길동/김민아/이영희" 와
        // "Python 입문반/JavaScript 중급 과정" 처럼 어디에도 없는 값이라 화면에서 더미 티가 났다.
        List<Seed> seeds = List.of(
                new Seed("출석률 위험", "이훈련 학생의 출석률이 60% 미만입니다. 조치가 필요합니다.",
                        Notification.Priority.HIGH, 4),
                new Seed("수강률 저조", "박훈련 학생의 수강률이 70% 미만으로 떨어졌습니다.",
                        Notification.Priority.NORMAL, 3),
                new Seed("과정 출석률 위험", "클라우드 기반 풀스택 개발자 양성과정의 전체 출석률이 65%로 낮습니다.",
                        Notification.Priority.HIGH, 2),
                new Seed("수강률 경고", "데이터 분석 실무 (Python/Pandas) 과정의 전체 수강률이 68%입니다.",
                        Notification.Priority.LOW, 1),
                new Seed("출석률 경고", "클라우드 인프라 입문 (AWS) 과정에 출석률 50% 미만 교육생이 있습니다.",
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

    /** 공지 목록 34건이 순환해서 쓰는 문구 풀. 실제 훈련기관이 낼 법한 제목·본문으로 채운다. */
    private record NoticeSeed(String title, String content) {
    }

    private static final List<NoticeSeed> NOTICE_SEEDS = List.of(
            new NoticeSeed("출결 관리 시스템 이용 안내",
                    "출결은 입실·퇴실 각각 인증으로 처리됩니다. 인증이 누락된 경우 당일 출결 정정 요청을 넣어 주세요."),
            new NoticeSeed("훈련장려금 지급 일정 안내",
                    "전월 출결 마감 후 매월 25일에 지급됩니다. 계좌 정보가 변경된 경우 운영팀으로 알려 주세요."),
            new NoticeSeed("과제 제출 방법 및 평가 기준 안내",
                    "과제는 과제 메뉴에서 제출하며, 기한 이후 제출은 지각 처리됩니다. 평가 기준은 과제 상세에서 확인하세요."),
            new NoticeSeed("정기 시스템 점검에 따른 서비스 일시 중단 안내",
                    "점검 시간 동안 학습과 과제 제출이 제한됩니다. 마감이 임박한 과제는 미리 제출해 주세요."),
            new NoticeSeed("현직자 초청 특강 신청 안내",
                    "현업 개발자를 모시고 실무 사례를 공유하는 특강을 진행합니다. 신청은 선착순으로 마감됩니다."),
            new NoticeSeed("강의실 및 실습 장비 이용 수칙 안내",
                    "실습 장비는 사용 후 원위치해 주시고, 강의실 내 음식물 반입은 제한됩니다."),
            new NoticeSeed("취업 지원 프로그램 상담 신청 안내",
                    "이력서 첨삭과 모의 면접을 1:1 로 지원합니다. 상담 희망 일정을 신청해 주세요."),
            new NoticeSeed("교육 만족도 조사 참여 요청",
                    "과정 개선을 위한 설문입니다. 응답은 익명으로 처리되며 5분 정도 소요됩니다."),
            new NoticeSeed("수료 기준 및 이수 요건 재안내",
                    "출석률 80% 이상, 과제와 평가의 기준 점수를 모두 충족해야 수료 처리됩니다."),
            new NoticeSeed("출결 정정 요청 절차 안내",
                    "정정 요청은 사유와 증빙을 첨부해 발생일로부터 3일 이내에 제출해야 합니다."),
            new NoticeSeed("개인정보 처리방침 개정 안내",
                    "보관 기간과 위탁 항목이 일부 변경되었습니다. 자세한 내용은 본문을 확인해 주세요."),
            new NoticeSeed("학습 커뮤니티 이용 가이드",
                    "질문 게시판과 스터디 모집 게시판을 운영합니다. 상호 존중하는 표현을 사용해 주세요."),
            new NoticeSeed("포트폴리오 작성 특강 신청 안내",
                    "프로젝트 결과물을 포트폴리오로 정리하는 방법을 다룹니다. 노트북을 지참해 주세요."),
            new NoticeSeed("중간 평가 일정 및 응시 유의사항 안내",
                    "응시 전 본인인증을 완료해야 하며, 응시 중 화면 이탈은 부정행위로 기록될 수 있습니다."),
            new NoticeSeed("채용 연계 기업 설명회 참가 신청 안내",
                    "참여 기업의 채용 직무와 전형 절차를 안내합니다. 사전 신청자에 한해 입장할 수 있습니다."),
            new NoticeSeed("하계 휴가 기간 운영 일정 안내",
                    "휴가 기간에는 온라인 학습만 운영되며, 상담과 문의 응대는 순차적으로 처리됩니다."),
            new NoticeSeed("온라인 학습 플랫폼 문의 창구 안내",
                    "학습 중 오류가 발생하면 화면 캡처와 함께 문의 게시판에 남겨 주세요."),
            new NoticeSeed("4대보험 취득 확인 서류 제출 안내",
                    "제출 서류가 누락되면 훈련장려금 지급이 지연될 수 있습니다. 기한 내 제출해 주세요."),
            new NoticeSeed("프로젝트 팀 편성 결과 안내",
                    "팀별 주제와 멘토 배정 결과를 확인하고 첫 회의 일정을 팀원과 공유해 주세요."),
            new NoticeSeed("학습 진도 점검 및 보충 학습 안내",
                    "진도가 지연된 교육생을 대상으로 보충 학습을 운영합니다. 대상자에게는 개별 안내됩니다.")
    );

    /**
     * 고정 공지 1번의 본문. 개강일은 {@code base}(발행일) 기준 상대 계산이다.
     *
     * <p>예전에는 "2026년 1월 20일" 처럼 절대 날짜가 박혀 있었는데 {@code publishedAt} 은
     * {@code now.minusDays(40)} 기준 상대값이라, 촬영 시점이 1월이 아니면 본문 날짜와
     * 화면 날짜가 어긋났다. 그래서 발행일 기준으로 맞춰 렌더한다.</p>
     */
    private static String detailContent(LocalDateTime base) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
        return DETAIL_CONTENT_TEMPLATE
                .replace("{KDT_START}", base.plusDays(14).format(f))
                .replace("{HIGHSCHOOL_START}", base.plusDays(19).format(f))
                .replace("{ADVANCED_START}", base.plusDays(21).format(f));
    }

    private static final String DETAIL_CONTENT_TEMPLATE = """
            안녕하세요, 삼성 아카데미 LXP 교육생 여러분!

            교육 과정 일정 및 운영 방안을 안내드립니다. 올 한 해도 여러분의 성장과 발전을 위해 최선을 다하겠습니다.

            1. 교육 일정 안내
            - KDT 과정: {KDT_START} 개강 예정입니다. 사전 준비사항을 확인해주세요.
            - 고교위탁 과정: {HIGHSCHOOL_START} 개강 예정입니다. 고등학교와의 연계를 강화하겠습니다.
            - 심화 과정: {ADVANCED_START} 개강 예정입니다. 고급 기술 교육에 집중하겠습니다.

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
