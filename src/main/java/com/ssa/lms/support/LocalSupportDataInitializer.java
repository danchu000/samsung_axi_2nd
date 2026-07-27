package com.ssa.lms.support;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.QnaAnswer;
import com.ssa.lms.support.entity.TutoringMessage;
import com.ssa.lms.support.entity.TutoringRoom;
import com.ssa.lms.support.repository.QnaRepository;
import com.ssa.lms.support.repository.TutoringMessageRepository;
import com.ssa.lms.support.repository.TutoringRoomRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * local 프로필에서만 Q&A·튜터링 흐름을 확인하기 위한 B 도메인 시드.
 *
 * <p><b>CommandLineRunner 를 쓰면 안 된다.</b> A 의 {@code config.LocalDataInitializer}
 * (사용자·과정 시드)에는 {@code @Order} 가 없어 {@code LOWEST_PRECEDENCE} 로 잡히는데,
 * 여기에 {@code @Order} 를 달면 오히려 <b>A 보다 먼저</b> 돌아 trainee1/과정이 없는 상태에서
 * 시드가 조용히 건너뛰어진다 (시험·과제 슬라이스가 실제로 밟은 함정).
 * {@code ApplicationReadyEvent} 는 모든 CommandLineRunner 가 끝난 뒤 발행되므로
 * A 의 시드 완료를 보장받는다.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalSupportDataInitializer {
    private final QnaRepository qnaRepository;
    private final TutoringRoomRepository roomRepository;
    private final TutoringMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (qnaRepository.count() > 0 || roomRepository.count() > 0) return;
        var trainee = userRepository.findByLoginId("trainee1").orElse(null);
        var instructor = userRepository.findByLoginId("instructor1").orElse(null);
        Course course = courseRepository.findAll().stream().findFirst().orElse(null);
        if (trainee == null) {
            log.warn("[local] Q&A·튜터링 시드 건너뜀 — trainee1 계정이 없다 (A 시드 확인 필요)");
            return;
        }

        Qna qna = Qna.builder().user(trainee).course(course)
                .category(Qna.QnaCategory.LECTURE).title("Spring Bean 주입 방식이 궁금합니다")
                .content("생성자 주입과 필드 주입의 차이를 알려주세요.")
                .status(Qna.QnaStatus.WAITING).secret(false).build();
        qnaRepository.save(qna);
        if (instructor != null) {
            QnaAnswer answer = QnaAnswer.builder().responder(instructor)
                    .content("테스트하기 쉽고 불변성을 지킬 수 있는 생성자 주입을 권장합니다.").build();
            qna.addAnswer(answer, LocalDateTime.now().minusHours(1));
        }

        TutoringRoom room = roomRepository.save(TutoringRoom.builder().course(course).trainee(trainee)
                .instructor(instructor).title("2차시 Java 문법 질문")
                .status(instructor == null ? TutoringRoom.RoomStatus.WAITING : TutoringRoom.RoomStatus.ACTIVE).build());
        messageRepository.save(TutoringMessage.builder().room(room).sender(trainee)
                .content("컬렉션과 배열의 차이가 궁금해요.").sentAt(LocalDateTime.now().minusMinutes(20)).build());
        room.touch(LocalDateTime.now().minusMinutes(20));
        log.info("[local] Q&A·튜터링 시드 생성 완료");
    }
}
