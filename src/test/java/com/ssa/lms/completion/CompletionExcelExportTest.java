package com.ssa.lms.completion;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 이수 현황 xlsx 다운로드 검증 — 데모 과정(COURSE-2026-001)에는 시더가 이수 정보를 심어 둔다.
 *
 * <p>바이트 자체를 파싱하진 않고 (1) 200 + xlsx MIME/attachment 헤더, (2) 실제 zip(xlsx) 시그니처 {@code PK},
 * (3) 판정 이수가 0건인 과정도 200(빈 시트가 아니라 안내 행) 세 가지를 본다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CompletionExcelExportTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;

    @Test
    @DisplayName("관리자 이수 현황 엑셀 다운로드는 200 + xlsx 첨부이고 본문이 실제 xlsx(PK) 다")
    @WithMockUser(roles = "ADMIN")
    void adminDownloadsCompletionExcel() throws Exception {
        Course course = courseRepository.findByCourseCode("COURSE-2026-001").orElseThrow();

        MvcResult result = mvc.perform(get("/admin/completion/export.xlsx")
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_MIME))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // xlsx 는 zip 컨테이너 — 시그니처가 'P','K' 여야 엑셀이 연다(BOM 이 앞에 붙으면 안 됨).
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("판정된 이수가 없는 과정도 엑셀 다운로드는 200(안내 행 포함 빈 표)")
    @WithMockUser(roles = "ADMIN")
    void emptyCourseStillDownloads() throws Exception {
        Course empty = courseRepository.save(Course.builder()
                .courseCode("COURSE-EXCEL-EMPTY")
                .courseName("이수판정 전 과정")
                .startDate(java.time.LocalDate.of(2026, 3, 1))
                .endDate(java.time.LocalDate.of(2026, 8, 1))
                .capacity(20)
                .status(com.ssa.lms.course.entity.CourseStatus.DRAFT)
                .completionProgressRate(80)
                .build());

        mvc.perform(get("/admin/completion/export.xlsx")
                        .param("courseId", empty.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_MIME));
    }

    @Test
    @DisplayName("훈련생은 관리자 이수 현황 엑셀을 내려받을 수 없다(403)")
    @WithMockUser(roles = "TRAINEE")
    void traineeCannotDownload() throws Exception {
        Course course = courseRepository.findByCourseCode("COURSE-2026-001").orElseThrow();
        mvc.perform(get("/admin/completion/export.xlsx")
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isForbidden());
    }
}
