package com.ssa.lms.support;

import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.TutoringRoom;
import com.ssa.lms.support.repository.QnaRepository;
import com.ssa.lms.support.repository.TutoringRoomRepository;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 강사가 관리자·강사 공용 학습지원 화면({@code /admin/support/**})에 들어왔을 때의
 * <b>권한 경계</b>를 고정한다.
 *
 * <p><b>배경:</b> SecurityConfig 가 {@code /admin/support/**} 를 ADMIN·INSTRUCTOR 양쪽에
 * 열어 두는데, 화면·컨트롤러는 관리자만 온다는 전제로 짜여 있었다. 강사 사이드바에 진입로를
 * 붙이는 순간 세 가지가 드러난다 —
 * <ol>
 *   <li>목록이 전체를 내려줘 강사가 <b>남의 1:1 튜터링 방</b>까지 본다(개인정보 노출)</li>
 *   <li>배정·종료(운영 액션)에 역할 검사가 없어 강사도 POST 할 수 있다</li>
 *   <li>레이아웃이 {@code fragments/admin} 고정이라 강사에게 관리자 메뉴가 뜨고,
 *       누르면 {@code /admin/**} 이라 403 이 난다 (커밋 c459838 과 같은 부류)</li>
 * </ol>
 *
 * <p>렌더 검증은 status 200 으로 부족하다 — 잘린 응답도 200 이므로 {@code </html>} 까지 본다
 * (CLAUDE.md 규칙 3).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InstructorSupportScopeTest {

    /** 시드 강사(김강사) = 강사 A. 배정 대상이다. */
    private static final String INSTRUCTOR_A = "instructor1";
    /** 이 테스트에서 만드는 다른 강사 = 강사 B. 강사 A 에게 보이면 안 되는 쪽. */
    private static final String INSTRUCTOR_B = "instructor_scope_b";

    /** 관리자 사이드바에만 있는 링크 — 강사 화면에 있으면 안 된다. */
    private static final String ADMIN_ONLY_MENU = "/admin/users/trainees";
    /** 강사 사이드바에만 있는 링크. */
    private static final String INSTRUCTOR_ONLY_MENU = "/instructor/graduate";

    @Autowired MockMvc mvc;
    @Autowired TutoringRoomRepository roomRepository;
    @Autowired QnaRepository qnaRepository;
    @Autowired UserRepository userRepository;
    @Autowired QnaService qnaService;
    @Autowired PasswordEncoder passwordEncoder;

    private User user(String loginId) {
        return userRepository.findByLoginId(loginId).orElseThrow();
    }

    /** 강사 B — 시드에 ACTIVE 강사가 instructor1 하나뿐이라 여기서 만든다. */
    private User instructorB() {
        return userRepository.findByLoginId(INSTRUCTOR_B).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            return userRepository.save(User.builder()
                    .loginId(INSTRUCTOR_B).password(passwordEncoder.encode("1234"))
                    .name("정강사").role(Role.INSTRUCTOR).status(UserStatus.ACTIVE)
                    .email("instructor.scope.b@ssa.local").phone("010-7777-7777").birthDate("1987-07-07")
                    .privacyConsentAt(now).thirdPartyConsentAt(now)
                    .build());
        });
    }

    /** 제목으로 구분 가능한 튜터링 방을 만든다 (테스트끼리 데이터가 섞이지 않게 제목을 유일하게 쓴다). */
    private Long room(String title, User instructor) {
        return roomRepository.save(TutoringRoom.builder()
                .trainee(user("trainee1"))
                .instructor(instructor)
                .title(title)
                .status(TutoringRoom.RoomStatus.ACTIVE)
                .build()).getId();
    }

    private Long qna(String title, User assignee) {
        Long id = qnaRepository.save(Qna.builder()
                .user(user("trainee1"))
                .category(Qna.QnaCategory.ETC)
                .title(title)
                .content("내용")
                .status(Qna.QnaStatus.WAITING)
                .secret(false)
                .build()).getId();
        qnaService.assign(id, assignee.getId());   // 담당자는 builder 에 없다 — 실제 배정 경로를 쓴다
        return id;
    }

    /* ===== 1) 목록 스코프 — 강사는 본인 배정 건만 ===== */

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사 목록에는 본인 배정 튜터링 방만 보인다 (다른 강사 방은 안 보인다)")
    void 강사_튜터링목록_본인_배정만() throws Exception {
        room("스코프검증-강사A-방", user(INSTRUCTOR_A));
        room("스코프검증-강사B-방", instructorB());

        String html = mvc.perform(get("/admin/support/tutoring"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("스코프검증-강사A-방");
        assertThat(html).doesNotContain("스코프검증-강사B-방");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사 목록에는 본인 담당 Q&A 만 보인다 (다른 강사 담당 질문은 안 보인다)")
    void 강사_QNA목록_본인_담당만() throws Exception {
        qna("스코프검증-강사A-질문", user(INSTRUCTOR_A));
        qna("스코프검증-강사B-질문", instructorB());

        String html = mvc.perform(get("/admin/support/qna"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("스코프검증-강사A-질문");
        assertThat(html).doesNotContain("스코프검증-강사B-질문");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("담당자 필터 '미배정' 으로도 스코프를 우회할 수 없다")
    void 강사_QNA목록_미배정필터_우회불가() throws Exception {
        qna("스코프검증-우회-담당있음", instructorB());
        qnaRepository.save(Qna.builder()
                .user(user("trainee1")).category(Qna.QnaCategory.ETC)
                .title("스코프검증-우회-미배정").content("내용")
                .status(Qna.QnaStatus.WAITING).secret(false).build());

        String html = mvc.perform(get("/admin/support/qna").param("manager", "미배정"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("스코프검증-우회-담당있음");
        assertThat(html).doesNotContain("스코프검증-우회-미배정");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("관리자 목록은 전체를 본다 — 스코프가 관리자 모니터링까지 막지 않는다")
    void 관리자_목록_전체() throws Exception {
        room("스코프검증-관리자-A방", user(INSTRUCTOR_A));
        room("스코프검증-관리자-B방", instructorB());

        String html = mvc.perform(get("/admin/support/tutoring"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("스코프검증-관리자-A방");
        assertThat(html).contains("스코프검증-관리자-B방");
        assertThat(html).contains("</html>");
    }

    /* ===== 2) 배정·종료는 관리자 전용 ===== */

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사의 튜터 배정 POST 는 403")
    void 강사_튜터배정_403() throws Exception {
        Long roomId = room("권한검증-배정-방", user(INSTRUCTOR_A));
        mvc.perform(post("/admin/support/tutoring/" + roomId + "/assign")
                        .with(csrf()).param("instructorId", String.valueOf(user(INSTRUCTOR_A).getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사의 튜터링 종료 POST 는 403")
    void 강사_튜터링종료_403() throws Exception {
        Long roomId = room("권한검증-종료-방", user(INSTRUCTOR_A));
        mvc.perform(post("/admin/support/tutoring/" + roomId + "/close").with(csrf()))
                .andExpect(status().isForbidden());

        // 403 이 "화면만" 막은 게 아니라 실제로 상태를 바꾸지 않았는지까지 본다
        assertThat(roomRepository.findById(roomId).orElseThrow().getStatus())
                .isEqualTo(TutoringRoom.RoomStatus.ACTIVE);
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사의 Q&A 담당자 배정 POST 는 403")
    void 강사_QNA배정_403() throws Exception {
        Long qnaId = qna("권한검증-QNA-배정", user(INSTRUCTOR_A));
        mvc.perform(post("/admin/support/qna/" + qnaId + "/assign")
                        .with(csrf()).param("assigneeId", String.valueOf(instructorB().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사의 Q&A 종료 POST 는 403")
    void 강사_QNA종료_403() throws Exception {
        Long qnaId = qna("권한검증-QNA-종료", user(INSTRUCTOR_A));
        mvc.perform(post("/admin/support/qna/" + qnaId + "/close").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("관리자의 배정·종료는 그대로 동작한다 — 403 이 관리자까지 막지 않는다")
    void 관리자_배정종료_정상() throws Exception {
        Long roomId = room("권한검증-관리자-운영-방", null);
        mvc.perform(post("/admin/support/tutoring/" + roomId + "/assign")
                        .with(csrf()).param("instructorId", String.valueOf(user(INSTRUCTOR_A).getId())))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/support/tutoring/" + roomId + "/close").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    /* ===== 3) 상세 — 배정받은 강사는 답장까지, 남의 방은 못 연다 ===== */

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("배정받은 강사가 튜터링 상세를 열면 200 이고 입력창(canSend)이 있다")
    void 배정강사_상세_200_입력창() throws Exception {
        Long roomId = room("상세검증-내방", user(INSTRUCTOR_A));

        String html = mvc.perform(get("/admin/support/tutoring/" + roomId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/admin/support/tutoring/" + roomId + "/messages");
        assertThat(html).contains("답변을 입력하세요");   // 입력창(textarea)
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("배정받지 않은 강사가 튜터링 상세를 URL 로 직접 열면 403 — 목록 스코프만으로는 못 막는다")
    void 비배정강사_상세_403() throws Exception {
        Long roomId = room("상세검증-남의방", instructorB());
        mvc.perform(get("/admin/support/tutoring/" + roomId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사 상세에는 배정 select / 종료 버튼이 렌더되지 않는다")
    void 강사_상세_운영버튼_없음() throws Exception {
        Long roomId = room("상세검증-운영버튼", user(INSTRUCTOR_A));

        String html = mvc.perform(get("/admin/support/tutoring/" + roomId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 운영 액션 폼 자체가 없어야 한다 (버튼 문구가 아니라 form action 으로 본다)
        assertThat(html).doesNotContain("/admin/support/tutoring/" + roomId + "/assign");
        assertThat(html).doesNotContain("/admin/support/tutoring/" + roomId + "/close");
        assertThat(html).contains("</html>");
    }

    /* ===== 4) 레이아웃 — 강사에게는 강사 사이드바 ===== */

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사가 여는 목록 화면에는 관리자 사이드바가 아니라 강사 사이드바가 렌더된다")
    void 강사_목록_강사사이드바() throws Exception {
        String html = mvc.perform(get("/admin/support/tutoring"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(INSTRUCTOR_ONLY_MENU);
        assertThat(html).doesNotContain(ADMIN_ONLY_MENU);
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사가 여는 튜터링 상세에도 강사 사이드바가 렌더된다")
    void 강사_상세_강사사이드바() throws Exception {
        Long roomId = room("레이아웃검증-상세", user(INSTRUCTOR_A));

        String html = mvc.perform(get("/admin/support/tutoring/" + roomId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(INSTRUCTOR_ONLY_MENU);
        assertThat(html).doesNotContain(ADMIN_ONLY_MENU);
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사가 여는 Q&A 상세에도 강사 사이드바가 렌더된다")
    void 강사_QNA상세_강사사이드바() throws Exception {
        Long qnaId = qna("레이아웃검증-QNA상세", user(INSTRUCTOR_A));

        String html = mvc.perform(get("/admin/support/qna/" + qnaId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(INSTRUCTOR_ONLY_MENU);
        assertThat(html).doesNotContain(ADMIN_ONLY_MENU);
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("관리자가 여는 화면은 그대로 관리자 사이드바 — 레이아웃 분기가 관리자를 바꾸지 않는다")
    void 관리자_목록_관리자사이드바() throws Exception {
        String html = mvc.perform(get("/admin/support/tutoring"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ADMIN_ONLY_MENU);
        assertThat(html).doesNotContain(INSTRUCTOR_ONLY_MENU);
        assertThat(html).contains("</html>");
    }

    /* ===== 진입로 — 강사 사이드바 링크가 실제 URL 로 간다 ===== */

    @Test
    @WithUserDetails(INSTRUCTOR_A)
    @DisplayName("강사 사이드바의 튜터링/Q&A 가 '준비 중' alert 이 아니라 실제 화면으로 간다")
    void 강사_사이드바_진입로() throws Exception {
        String html = mvc.perform(get("/instructor"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/admin/support/tutoring");
        assertThat(html).contains("</html>");
    }
}
