package com.ssa.lms.export;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.exam.dto.QuestionSearchCond;
import com.ssa.lms.exam.service.QuestionService;
import com.ssa.lms.survey.dto.SurveyForm;
import com.ssa.lms.survey.dto.SurveyQuestionForm;
import com.ssa.lms.survey.dto.SurveySubmitForm;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.service.SurveyReportService;
import com.ssa.lms.survey.service.SurveyService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 다운로드가 <b>정말 열리는 xlsx 인지</b>를 POI 로 되읽어 고정한다.
 *
 * <p>HTTP 200 과 Content-Type 만 보면 "0바이트지만 200"이나 "BOM 이 붙어 zip 이 깨진 파일"을
 * 잡지 못한다. 그래서 시그니처(PK) → 워크북 파싱 → 시트명·헤더·행 수·한글 순으로 확인한다.</p>
 */
@SpringBootTest
@Transactional
class ExcelExportTest {

    @Autowired SurveyService surveyService;
    @Autowired SurveyReportService surveyReportService;
    @Autowired SurveyRepository surveyRepository;
    @Autowired QuestionService questionService;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired UserRepository userRepository;

    private Course course;
    private Long traineeId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        course = courseRepository.findAll().stream().findFirst().orElseThrow();
        traineeId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TRAINEE)
                .findFirst().map(User::getId).orElseThrow();
        adminId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst().map(User::getId).orElseThrow();
    }

    /* ===================== ExcelWriter 자체 ===================== */

    @Test
    @DisplayName("ExcelWriter 가 만든 바이트는 PK 시그니처로 시작하고 POI 로 다시 열린다 — 한글 시트명·헤더 유지")
    void 엑셀_시그니처와_한글() throws IOException {
        byte[] bytes;
        try (ExcelWriter writer = ExcelWriter.create()) {
            writer.sheet("성적표", "이름", "총점");
            writer.row("김훈련", 95);
            writer.row("이수강", null);
            bytes = writer.toBytes();
        }

        // xlsx 는 zip 이다. BOM 을 붙이면 여기서 깨진다.
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("성적표");
            assertThat(sheet).as("한글 시트명이 그대로 살아야 한다").isNotNull();
            assertThat(text(sheet, 0, 0)).isEqualTo("이름");
            assertThat(text(sheet, 1, 0)).isEqualTo("김훈련");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(95.0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("같은 이름의 시트를 두 번 열어도 예외 없이 번호가 붙는다 — 시험명이 곧 시트명이라 실제로 겹친다")
    void 시트명_중복() throws IOException {
        byte[] bytes;
        try (ExcelWriter writer = ExcelWriter.create()) {
            writer.sheet("결과", "A");
            writer.sheet("결과", "A");
            bytes = writer.toBytes();
        }
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("결과(2)");
        }
    }

    @Test
    @DisplayName("31자를 넘는 시트명과 엑셀 금지문자를 넣어도 깨지지 않는다")
    void 시트명_보정() throws IOException {
        byte[] bytes;
        try (ExcelWriter writer = ExcelWriter.create()) {
            writer.sheet("2026년 1차 정보처리 산업기사 대비 모의고사 [최종]/1회", "A");
            bytes = writer.toBytes();
        }
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheetAt(0).getSheetName()).hasSizeLessThanOrEqualTo(31);
        }
    }

    /* ===================== 설문 결과 리포트 ===================== */

    @Test
    @DisplayName("설문 결과 리포트 — 시트 3장, 문항별 집계와 주관식 원문이 담긴다")
    void 설문리포트_집계() throws IOException {
        Long surveyId = createSurveyWithResponse();

        byte[] bytes = surveyReportService.reportExcel(surveyId, adminId, true);
        assertThat(bytes[0]).isEqualTo((byte) 'P');

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(wb.getSheetName(0)).isEqualTo("설문요약");
            assertThat(wb.getSheetName(1)).isEqualTo("문항별집계");
            assertThat(wb.getSheetName(2)).isEqualTo("주관식응답");

            Sheet summary = wb.getSheetAt(0);
            assertThat(text(summary, 1, 0)).isEqualTo("설문명");
            assertThat(text(summary, 1, 1)).isEqualTo("엑셀검증 설문");
            assertThat(numberByLabel(summary, "응답 건수")).isEqualTo(1.0);

            Sheet questions = wb.getSheetAt(1);
            assertThat(text(questions, 0, 5)).isEqualTo("항목");
            // 단일선택 보기 2개 + 척도 5단계 + 주관식 1행 = 8행
            assertThat(questions.getLastRowNum()).isEqualTo(8);
            assertThat(text(questions, 1, 2)).isEqualTo("가장 도움이 된 활동은?");
            assertThat(text(questions, 1, 5)).isEqualTo("1. 실습");
            assertThat(questions.getRow(1).getCell(6).getNumericCellValue())
                    .as("선택한 보기의 응답수").isEqualTo(1.0);
            assertThat(text(questions, 1, 7)).isEqualTo("100%");

            Sheet texts = wb.getSheetAt(2);
            assertThat(text(texts, 1, 3)).isEqualTo("실습 시간이 더 필요합니다");
        }
    }

    @Test
    @DisplayName("응답이 0건이어도 시트 3장이 정상 생성되고 안내 행이 남는다")
    void 설문리포트_응답0건() throws IOException {
        Long surveyId = surveyService.create(surveyForm("응답없는 설문"));
        surveyRepository.flush();

        byte[] bytes = surveyReportService.reportExcel(surveyId, adminId, true);
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(numberByLabel(wb.getSheetAt(0), "응답 건수")).isEqualTo(0.0);
            // 문항은 있으므로 집계 시트에는 보기 행이 남고, 주관식만 안내 행이 된다
            assertThat(text(wb.getSheetAt(2), 1, 0)).isEqualTo("주관식 응답이 없습니다.");
        }
    }

    @Test
    @DisplayName("담당하지 않는 과정의 설문 결과는 강사에게 403 — URL 의 id 만 바꿔 받아갈 수 없어야 한다")
    void 설문리포트_강사_권한() {
        Long surveyId = createSurveyWithResponse();
        Long instructorId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INSTRUCTOR)
                .filter(u -> !courseInstructorRepository.existsByCourseIdAndInstructorId(
                        course.getId(), u.getId()))
                .findFirst().map(User::getId).orElse(null);

        // 담당이 아닌 강사를 찾지 못하면 "담당 여부를 못 세운 것"이므로 테스트 의미가 없다.
        assertThat(instructorId).as("담당하지 않는 강사 시드가 있어야 한다").isNotNull();

        assertThatThrownBy(() -> surveyReportService.reportExcel(surveyId, instructorId, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* ===================== 문제은행 ===================== */

    @Test
    @DisplayName("문제은행 다운로드 — 조건에 맞는 전체 행이 담기고 정답·해설 컬럼은 없다")
    void 문제은행_다운로드() throws IOException {
        byte[] bytes = questionService.exportExcel(
                new QuestionSearchCond("문제", null, null, null, null));

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("문제은행");
            assertThat(sheet).isNotNull();
            assertThat(text(sheet, 0, 0)).isEqualTo("번호");
            assertThat(text(sheet, 0, 3)).isEqualTo("제목");

            List<String> headers = new ArrayList<>();
            for (Cell cell : sheet.getRow(0)) {
                headers.add(cell.getStringCellValue());
            }
            assertThat(headers).doesNotContain("정답", "해설");

            int expected = questionService.searchAll(
                    new QuestionSearchCond("문제", null, null, null, null)).size();
            assertThat(sheet.getLastRowNum())
                    .as("헤더 1행 + 데이터 %d행", expected)
                    .isEqualTo(expected);
        }
    }

    /* ===================== 도우미 ===================== */

    private Long createSurveyWithResponse() {
        Long id = surveyService.create(surveyForm("엑셀검증 설문"));
        surveyRepository.flush();

        Survey survey = surveyRepository.findById(id).orElseThrow();
        SurveySubmitForm submit = new SurveySubmitForm();
        List<SurveySubmitForm.AnswerInput> inputs = new ArrayList<>();
        for (SurveyQuestion q : survey.getQuestions()) {
            SurveySubmitForm.AnswerInput input = new SurveySubmitForm.AnswerInput();
            input.setQuestionId(q.getId());
            switch (q.getQuestionType()) {
                case SINGLE, MULTI ->
                        input.setChoiceIds(new ArrayList<>(List.of(q.getChoices().get(0).getId())));
                case SCALE -> input.setScaleValue(4);
                case TEXT -> input.setAnswerText("실습 시간이 더 필요합니다");
            }
            inputs.add(input);
        }
        submit.setAnswers(inputs);
        surveyService.submit(id, traineeId, submit);
        surveyRepository.flush();
        return id;
    }

    /** 단일선택 1 + 척도(5단계) 1 + 주관식 1 문항짜리 설문. */
    private SurveyForm surveyForm(String title) {
        SurveyForm f = new SurveyForm();
        f.setTitle(title);
        f.setSurveyType("만족도");
        f.setCourseId(course.getId());
        f.setStartDate(LocalDate.now().minusDays(1));
        f.setDueDate(LocalDate.now().plusMonths(6));
        f.setRequired(true);
        f.setReflectCompletion(false);
        f.setAnonymous(false);
        f.setStatus("active");

        SurveyQuestionForm single = new SurveyQuestionForm();
        single.setQuestionType("single");
        single.setContent("가장 도움이 된 활동은?");
        single.setRequired(true);
        single.setChoices(new ArrayList<>(List.of("실습", "이론")));

        SurveyQuestionForm scale = new SurveyQuestionForm();
        scale.setQuestionType("scale");
        scale.setContent("전반적인 만족도를 평가해 주세요.");
        scale.setRequired(true);
        scale.setScaleMax(5);

        SurveyQuestionForm text = new SurveyQuestionForm();
        text.setQuestionType("text");
        text.setContent("바라는 점을 적어 주세요.");
        text.setRequired(false);

        f.setQuestions(new ArrayList<>(List.of(single, scale, text)));
        return f;
    }

    private static String text(Sheet sheet, int row, int column) {
        Row r = sheet.getRow(row);
        if (r == null) {
            return null;
        }
        Cell c = r.getCell(column);
        return c == null ? null : c.getStringCellValue();
    }

    /** "항목 / 값" 두 칸짜리 요약 시트에서 라벨로 숫자 값을 찾는다. */
    private static double numberByLabel(Sheet sheet, String label) {
        for (Row row : sheet) {
            Cell key = row.getCell(0);
            if (key != null && label.equals(key.getStringCellValue())) {
                return row.getCell(1).getNumericCellValue();
            }
        }
        throw new AssertionError("요약 시트에 '" + label + "' 행이 없다");
    }
}
