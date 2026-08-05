package com.ssa.lms.demo;

import com.ssa.lms.assignment.dto.TraineeAssignmentCard;
import com.ssa.lms.attendance.web.TraineeAttendanceView;
import com.ssa.lms.completion.web.CompletionView;
import com.ssa.lms.exam.dto.ExamTakeRow;
import com.ssa.lms.survey.dto.TraineeSurveyRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 훈련생 화면 <b>예시(샘플) 데이터</b> — 화면정의서 캡처용.
 *
 * <p><b>왜 필요한가:</b> 새로 만든 계정이나 아직 과제·시험이 배정되지 않은 과정에서는
 * 과제/시험/출결/이수/설문 화면이 전부 "데이터가 없습니다" 만 나온다. 화면정의서에는
 * 훈련생이 실제로 무엇을 할 수 있는지가 보여야 하므로, <b>본인 데이터가 0건일 때만</b>
 * 예시 행을 대신 보여준다.</p>
 *
 * <p><b>DB 에는 아무것도 쓰지 않는다.</b> 출결·이수는 3년 보존 대상 증빙 데이터라
 * 시연용 레코드를 섞으면 안 된다. 여기서 만드는 값은 요청마다 메모리에서 조립되고
 * 사라진다. 실제 데이터가 1건이라도 생기면 그 순간부터 예시는 나오지 않는다.</p>
 *
 * <p><b>끄는 방법:</b> {@code LMS_DEMO_SAMPLE_DATA=false} (서버 {@code .env})
 * 또는 {@code --lms.demo.sample-data=false}. 재배포 없이 값만 바꾸면 된다.</p>
 *
 * <p><b>id 규약:</b> 예시 행의 id 는 {@link #SAMPLE_ID_MIN}~{@link #SAMPLE_ID_MAX} 구간을 쓴다.
 * 실제 데이터와 겹치지 않게 하려는 것이고, 컨트롤러는 {@link #isSampleId}로 판별해
 * "예시 데이터에는 이 동작을 할 수 없다"고 안내한다 — 예시 id 를 그대로 조회하면
 * 500 이나 빈 화면이 나기 때문이다.</p>
 */
@Component
public class SampleScreenData {

    /** 예시 행 id 구간 (양끝 포함). 실제 PK 가 이 구간에 닿을 일은 없다. */
    public static final long SAMPLE_ID_MIN = 900_001L;
    public static final long SAMPLE_ID_MAX = 909_999L;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String COURSE = "K-디지털 트레이닝 풀스택 개발자 양성과정";
    private static final String COHORT = "3기";
    private static final String COURSE_LABEL = COURSE + " / " + COHORT;

    private final boolean enabled;

    public SampleScreenData(@Value("${lms.demo.sample-data:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static boolean isSampleId(Long id) {
        return id != null && id >= SAMPLE_ID_MIN && id <= SAMPLE_ID_MAX;
    }

    /**
     * 실제 데이터가 있으면 그대로, 0건이고 기능이 켜져 있으면 예시로 채운다.
     * 예시를 썼는지 여부는 화면 상단 안내 문구를 띄우는 데 쓴다.
     */
    public <T> Filled<T> fill(List<T> real, Supplier<List<T>> sample) {
        if (real != null && !real.isEmpty()) {
            return new Filled<>(real, false);
        }
        if (!enabled) {
            return new Filled<>(real == null ? List.of() : real, false);
        }
        return new Filled<>(sample.get(), true);
    }

    /** @param sample 예시 데이터로 채워졌는지 — true 면 화면이 "예시" 배지를 보여준다. */
    public record Filled<T>(List<T> rows, boolean sample) {
    }

    /* ===================== 과제 ===================== */

    public List<TraineeAssignmentCard> assignments() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                // 진행중 — 제출 모달까지 열어볼 수 있게 canSubmit=true 로 둔다
                new TraineeAssignmentCard(
                        "900001",
                        "REST API 게시판 구현",
                        COURSE, COHORT,
                        "ONGOING",
                        now.plusDays(5).withHour(23).withMinute(59).format(DATETIME),
                        "FILE_TEXT", true,
                        "Spring Boot 로 게시글 CRUD REST API 를 구현하고, 소스 압축본과 구현 설명을 함께 제출하세요. "
                                + "평가 기준: 요구사항 충족 50점 / 코드 품질 30점 / 문서화 20점.",
                        false, null, null, null,
                        true, 0, false),
                // 제출완료 + 채점완료 — 결과 모달(점수·피드백·내 답안)을 보여준다
                new TraineeAssignmentCard(
                        "900002",
                        "React 재사용 컴포넌트 3종 만들기",
                        COURSE, COHORT,
                        "SUBMITTED",
                        now.minusDays(4).withHour(23).withMinute(59).format(DATETIME),
                        "LINK", false,
                        "버튼·모달·테이블 컴포넌트를 props 기반으로 재사용 가능하게 만들고 배포 링크를 제출하세요.",
                        true,
                        "컴포넌트 분리 기준이 명확하고 props 설계가 좋았습니다. 모달의 접근성(포커스 트랩)만 보완하면 실무에서 그대로 쓸 수 있는 수준입니다.",
                        "https://sample-components.vercel.app\n첨부: 컴포넌트_설명서.pdf",
                        "92 / 100",
                        false, 1, false),
                // 제출완료 + 채점대기
                new TraineeAssignmentCard(
                        "900003",
                        "데이터 전처리 리포트",
                        COURSE, COHORT,
                        "SUBMITTED",
                        now.minusDays(1).withHour(23).withMinute(59).format(DATETIME),
                        "TEXT", true,
                        "결측치 처리와 정규화 과정을 실제 데이터셋 기준으로 정리해 제출하세요.",
                        false, null,
                        "결측치는 평균 대치 대신 중앙값으로 처리했고, 이상치는 IQR 기준으로 제거했습니다. "
                                + "정규화는 StandardScaler 를 사용했습니다.",
                        null,
                        false, 1, false),
                // 미제출(마감) — 지각 제출 불가 상태
                new TraineeAssignmentCard(
                        "900004",
                        "SQL 실행계획 분석 과제",
                        COURSE, COHORT,
                        "LATE",
                        now.minusDays(7).withHour(23).withMinute(59).format(DATETIME),
                        "FILE", false,
                        "느린 쿼리 3건의 실행계획을 캡처하고 인덱스 개선 전후를 비교해 제출하세요.",
                        false, null, null, null,
                        false, 0, false)
        );
    }

    /* ===================== 시험 ===================== */

    /**
     * 응시 목록 예시. 전부 {@code startable=false} 다 —
     * 예시 시험을 실제로 시작시키면 존재하지 않는 시험 id 로 응시 회차를 만들려다 실패한다.
     */
    public List<ExamTakeRow> exams() {
        LocalDateTime now = LocalDateTime.now();
        String block = "화면 예시용 샘플 시험입니다. 실제 시험이 배정되면 응시할 수 있어요.";
        return List.of(
                new ExamTakeRow(
                        "900101", "중간평가 — Java/Spring 기초",
                        "900001", COURSE, "8차시",
                        now.minusDays(1).format(DATETIME), now.plusDays(2).format(DATETIME),
                        60, 25, "객관식+주관식", "1회 응시", 0, 1,
                        "available", "응시 전 본인인증이 필요합니다.",
                        true, null, null,
                        true, false, block, false, 1, "채점 완료 후 공개"),
                new ExamTakeRow(
                        "900102", "쪽지시험 — HTML/CSS 레이아웃",
                        "900001", COURSE, "5차시",
                        now.minusDays(9).format(DATETIME), now.minusDays(8).format(DATETIME),
                        20, 10, "객관식", "재응시 불가", 1, 1,
                        "completed", "제출 완료 — 18/20 (90점)",
                        false, null, null,
                        true, false, block, false, 1, "즉시 공개"),
                new ExamTakeRow(
                        "900103", "최종평가 — 프로젝트 이해도",
                        "900001", COURSE, "20차시",
                        now.plusDays(10).format(DATETIME), now.plusDays(10).plusHours(2).format(DATETIME),
                        90, 40, "객관식+서술형", "1회 응시", 0, 1,
                        "scheduled", "응시 기간이 시작되면 열립니다.",
                        true, null, null,
                        true, false, block, false, 2, "채점 완료 후 공개")
        );
    }

    /* ===================== 출결현황 ===================== */

    public List<TraineeAttendanceView> attendance() {
        // 12차시: 출석 9 / 지각 1 / 공결 1 / 결석 1 → 인정 11건 = 92%
        String[][] plan = {
                {"오리엔테이션 및 개발환경 구축", "PRESENT"},
                {"HTML/CSS 기본 구조", "PRESENT"},
                {"JavaScript 기초 문법", "PRESENT"},
                {"JavaScript DOM 제어", "LATE"},
                {"Java 객체지향 기초", "PRESENT"},
                {"Java 컬렉션과 스트림", "PRESENT"},
                {"데이터베이스와 SQL", "PRESENT"},
                {"JDBC 와 커넥션 풀", "EXCUSED"},
                {"Spring Boot 시작하기", "PRESENT"},
                {"Spring Data JPA", "ABSENT"},
                {"REST API 설계", "PRESENT"},
                {"React 컴포넌트 기초", "PRESENT"},
        };

        LocalDate start = LocalDate.now().minusDays(plan.length * 2L);
        List<TraineeAttendanceView.Row> rows = new ArrayList<>();
        int credited = 0;
        for (int i = 0; i < plan.length; i++) {
            String status = plan[i][1];
            boolean isCredited = !"ABSENT".equals(status);
            if (isCredited) {
                credited++;
            }
            rows.add(new TraineeAttendanceView.Row(
                    i + 1, plan[i][0], start.plusDays(i * 2L), status, labelOf(status), isCredited));
        }
        int rate = (int) Math.round(credited * 100.0 / rows.size());
        return List.of(new TraineeAttendanceView(SAMPLE_ID_MIN, COURSE_LABEL, rate, rows));
    }

    private String labelOf(String status) {
        return switch (status) {
            case "PRESENT" -> "출석";
            case "LATE" -> "지각";
            case "EXCUSED" -> "공결";
            default -> "결석";
        };
    }

    /* ===================== 이수관리 ===================== */

    public List<CompletionView> completions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                // 확정된 과정 — 이수증 다운로드 버튼이 보이는 상태
                new CompletionView(
                        900_201L, "홍길동", "1998-04-12",
                        "K-디지털 트레이닝 백엔드 개발자 양성과정 / 2기",
                        100, 96, true,
                        "PASS", "이수", "CONFIRMED", "확정",
                        now.minusDays(20), true),
                // 진행 중인 과정 — 이수예정
                new CompletionView(
                        900_202L, "홍길동", "1998-04-12",
                        COURSE_LABEL,
                        68, 92, false,
                        "PENDING", "판정대기", "EXPECTED", "이수예정",
                        null, false)
        );
    }

    /* ===================== 설문조사 ===================== */

    public List<TraineeSurveyRow> surveys() {
        LocalDate today = LocalDate.now();
        return List.of(
                new TraineeSurveyRow("900301", "중간 만족도 조사", COURSE, COHORT,
                        today.minusDays(2).format(DATE), today.plusDays(5).format(DATE),
                        10, "ONGOING", today.minusDays(3).format(DATE)),
                new TraineeSurveyRow("900302", "강사 강의 평가 (8차시)", COURSE, COHORT,
                        today.minusDays(14).format(DATE), today.minusDays(7).format(DATE),
                        8, "SUBMITTED", today.minusDays(15).format(DATE)),
                new TraineeSurveyRow("900303", "수료 후 취업 지원 수요조사", COURSE, COHORT,
                        today.plusDays(10).format(DATE), today.plusDays(20).format(DATE),
                        12, "SCHEDULED", today.minusDays(1).format(DATE))
        );
    }
}
