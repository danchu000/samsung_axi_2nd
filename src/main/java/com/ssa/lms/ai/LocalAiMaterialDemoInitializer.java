package com.ssa.lms.ai;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로컬 시연용 학습 자료 시드 — AI 학습 도우미가 답할 근거가 있어야 한다.
 *
 * <p>기존 콘텐츠 시드({@code ContentDemoDataInitializer}, @Order 30)는 <b>모집중 과정</b>에만
 * 자료를 넣는다. 그 과정에서 trainee1 은 승인 전(APPLIED) 상태라 질문할 수 없다.
 * 그래서 로컬에서 AI 도우미를 열면 어떤 과정을 골라도 "등록된 학습 자료가 없어요"만 나온다 —
 * 기능이 고장 난 것처럼 보인다.</p>
 *
 * <p>여기서는 <b>수강생이 실제로 듣고 있는(승인·수료) 과정</b> 중 자료가 하나도 없는 곳에만
 * 자료를 채운다. 이미 자료가 있으면 손대지 않는다.</p>
 *
 * <p><b>운영에서는 돌지 않는다</b> — {@code @Profile("local")}.
 * 자료 본문이 아니라 제목·설명만 프롬프트에 들어가므로, 설명을 실제 강의계획서처럼 적어 둔다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(40)   // 과정(20)·콘텐츠(30) 시드 뒤에 돈다
@RequiredArgsConstructor
public class LocalAiMaterialDemoInitializer implements CommandLineRunner {

    /** 제목 / 설명 — 설명이 곧 모델이 보는 근거다. 한 줄이라도 내용이 있어야 답이 나온다. */
    private static final String[][] MATERIALS = {
            {"1주차 - 개발환경 구축", "JDK·IDE 설치, Git 초기 설정, 프로젝트 생성 절차를 다룹니다."},
            {"2주차 - 스프링 컨테이너와 의존성 주입",
             "빈 등록 방식, 생성자 주입을 권장하는 이유, 순환 참조가 생기는 상황을 다룹니다."},
            {"3주차 - 데이터베이스와 트랜잭션",
             "트랜잭션의 ACID, 격리 수준별 차이, @Transactional 이 걸리지 않는 경우를 다룹니다."},
            {"4주차 - JPA 영속성 컨텍스트",
             "1차 캐시와 변경 감지, 지연 로딩과 N+1 문제, open-in-view 설정의 영향을 다룹니다."},
            {"5주차 - REST API 설계",
             "자원 중심 URL 설계, HTTP 메서드와 상태 코드 선택, 예외 응답 형식을 다룹니다."},
            {"6주차 - 테스트 코드 작성",
             "단위 테스트와 통합 테스트의 구분, 테스트 더블 사용 기준, 무엇을 검증해야 하는지를 다룹니다."}
    };

    private final CourseRepository courseRepository;
    private final ContentRepository contentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (Course course : courseRepository.findAll()) {
            if (!hasActiveTrainee(course.getId())) continue;
            if (contentRepository.countByCourseId(course.getId()) > 0) continue;

            int order = 1;
            for (String[] m : MATERIALS) {
                contentRepository.save(Content.builder()
                        .course(course)
                        .type(ContentType.DOCUMENT)
                        .title(m[0])
                        .description(m[1])
                        // 시연용 플레이스홀더 — 실제 파일은 강사 업로드로 채워진다
                        .fileUrl("/static/img/sample-class.png")
                        .originalFileName("sample-class.png")
                        .pageCount(1)
                        .orderNo(order++)
                        .required(true)
                        .status(ContentStatus.ACTIVE)
                        .build());
            }
            log.info("[시드] AI 도우미용 학습 자료 {}건 생성 — {}", MATERIALS.length, course.getCourseName());
        }
    }

    private boolean hasActiveTrainee(Long courseId) {
        List<EnrollmentStatus> counted = List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);
        return enrollmentRepository.findByCourseIdOrderByAppliedAtDesc(courseId).stream()
                .anyMatch(e -> counted.contains(e.getStatus()));
    }
}
