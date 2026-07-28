package com.ssa.lms.support;

import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.TutoringRoom;
import com.ssa.lms.support.repository.QnaRepository;
import com.ssa.lms.support.repository.TutoringRoomRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q&A·튜터링 상세의 <b>권한 실패가 500 이 아니라 403</b> 임을 고정한다.
 *
 * <p><b>배경(실증):</b> 라이브 스윕에서 trainee2~6 이 남의 튜터링 방({@code /trainee/qna/tutoring/1})을
 * 열면 whitelabel 500 이 떴다. 컨트롤러/서비스가 권한 실패를 {@code IllegalStateException} 으로 던졌고,
 * 이 예외를 매핑하는 advice 가 없어 500 으로 샜기 때문이다. 권한 실패는
 * {@code AccessDeniedException} 으로 던져 {@link com.ssa.lms.web.AccessDeniedAdvice} 가 403 으로
 * 받아야 한다. 비밀글({@code secret=true}) 비작성자 열람도 같은 계열의 잠복 버그였다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SupportAccessDeniedMappingTest {

    @Autowired MockMvc mvc;
    @Autowired TutoringRoomRepository tutoringRoomRepository;
    @Autowired QnaRepository qnaRepository;
    @Autowired UserRepository userRepository;

    private User trainee(String loginId) {
        return userRepository.findByLoginId(loginId).orElseThrow();
    }

    private Long roomOwnedBy(String ownerLoginId) {
        return tutoringRoomRepository.save(TutoringRoom.builder()
                .trainee(trainee(ownerLoginId))
                .title("권한테스트 방")
                .status(TutoringRoom.RoomStatus.ACTIVE)
                .build()).getId();
    }

    private Long qnaOwnedBy(String ownerLoginId, boolean secret) {
        return qnaRepository.save(Qna.builder()
                .user(trainee(ownerLoginId))
                .category(Qna.QnaCategory.ETC)
                .title("권한테스트 질문")
                .content("내용")
                .status(Qna.QnaStatus.WAITING)
                .secret(secret)
                .build()).getId();
    }

    @Test
    @WithUserDetails("trainee2")
    @DisplayName("남의 튜터링 방 상세는 500 이 아니라 403")
    void 튜터링_비소유자_403() throws Exception {
        Long roomId = roomOwnedBy("trainee1"); // 소유자는 trainee1, 접근자는 trainee2
        mvc.perform(get("/trainee/qna/tutoring/" + roomId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("trainee1")
    @DisplayName("본인 튜터링 방 상세는 정상(200) — 403 이 소유자까지 막지 않는다")
    void 튜터링_소유자_200() throws Exception {
        Long roomId = roomOwnedBy("trainee1");
        mvc.perform(get("/trainee/qna/tutoring/" + roomId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("trainee2")
    @DisplayName("남의 비밀글 상세는 500 이 아니라 403")
    void 비밀글_비작성자_403() throws Exception {
        Long qnaId = qnaOwnedBy("trainee1", true);
        mvc.perform(get("/trainee/qna/" + qnaId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("trainee2")
    @DisplayName("남의 공개글은 그대로 열람 가능(200) — 비밀글만 통제된다")
    void 공개글_타인_200() throws Exception {
        Long qnaId = qnaOwnedBy("trainee1", false);
        mvc.perform(get("/trainee/qna/" + qnaId))
                .andExpect(status().isOk());
    }
}
