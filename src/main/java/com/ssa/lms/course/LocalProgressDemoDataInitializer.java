package com.ssa.lms.course;

import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 로컬 시연용 진도율 시드.
 *
 * <p>대시보드의 "과정별 진행 현황"은 {@code Enrollment.progressRate} 평균을 쓴다.
 * 로컬 시드에는 학습 기록이 없어 전부 0% 로 나온다. 값 자체는 맞지만 화면으로는
 * 아무것도 안 보여 차트를 확인할 수가 없다. 시연에 쓸 수 있게 목표 평균을 채워 둔다.</p>
 *
 * <p><b>운영에서는 절대 돌지 않는다</b> — {@code @Profile("local")}.
 * 실제 진도는 {@code ProgressService} 가 콘텐츠 학습 기록으로 계산한다.</p>
 *
 * <p><b>왜 리스너가 아니라 지연 실행인가</b><br>
 * 수강 데이터를 만드는 시드가 {@code CommandLineRunner}(@Order 0, 20)와
 * {@code @EventListener}(@Order 없음 = 가장 늦게 실행)로 흩어져 있다.
 * CommandLineRunner 로 뒀을 때도, 리스너를 LOWEST_PRECEDENCE 로 뒀을 때도
 * 나중에 만들어지는 수강을 놓쳤다 — 클라우드 과정 2명 중 1명만 잡혔다.
 * 순서를 맞추려고 시드 여럿을 고치는 것보다 <b>모두 끝난 뒤 한 번</b> 도는 편이
 * 단순하고 덜 깨진다.</p>
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalProgressDemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(LocalProgressDemoDataInitializer.class);

    /** 과정명 일부 → 목표 평균 진도율(%). 부분 일치로 찾는다. */
    private static final Map<String, Integer> TARGET = Map.of(
            "데이터 분석 실무", 50,
            "클라우드 기반 풀스택", 70
    );

    /** 트랙이 그럴듯하게 보이려면 최소 이 인원은 있어야 한다. */
    private static final int MIN_TRAINEES = 6;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 기동 5초 뒤 한 번만. {@code fixedDelay} 를 크게 잡아 두 번 돌지 않게 한다. */
    @Scheduled(initialDelay = 5_000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void fill() {
        courseRepository.findAll().forEach(course -> {
            Integer target = targetOf(course.getCourseName());
            if (target == null) return;

            // 트랙에 주자가 2명뿐이면 "달리기"로 안 보인다. 시연용으로 채운다
            padTrainees(course.getId());

            List<Enrollment> rows = enrollmentRepository
                    .findByCourseIdOrderByAppliedAtDesc(course.getId()).stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                            || e.getStatus() == EnrollmentStatus.COMPLETED)
                    .toList();

            if (rows.isEmpty()) return;

            /*
             * 목표 평균이 정확히 나와야 하므로 "값이 있는 행은 건드리지 않기"를 하지 않는다.
             * 일부만 채우면 평균이 목표와 어긋난다 — 실제로 70% 를 넣었는데 화면엔 35% 로 나왔다
             * (2명 중 1명만 채워져서). 어차피 local 전용 시연 데이터다.
             *
             * 전원 같은 값이면 현실감이 없어 사람마다 흩어놓되,
             * 마지막 한 명이 남은 오차를 메워 평균이 정확히 target 이 되게 한다.
             */
            int n = rows.size();
            /*
             * 값이 목표 근처에 몰리면 트랙에서 주자들이 한 높이에 겹쳐 "달리기"로 안 보인다.
             * 목표 평균은 지키되 위아래로 넓게 벌린다(±30%p 안쪽).
             * 마지막 한 명이 남은 오차를 메워 평균이 정확히 target 이 되게 한다.
             */
            int spread = Math.min(30, Math.min(target - 5, 95 - target));
            int assigned = 0;

            for (int i = 0; i < n - 1; i++) {
                // -1 ~ +1 로 고르게 편 뒤 spread 만큼 벌린다
                double t = (n == 1) ? 0 : (2.0 * i / (n - 1) - 1.0);
                int v = clamp((int) Math.round(target - t * spread));
                rows.get(i).updateProgressRate(v);
                assigned += v;
            }
            rows.get(n - 1).updateProgressRate(clamp(target * n - assigned));

            log.info("[시드] 진도율 채움 — {} : 평균 {}% ({}명)", course.getCourseName(), target, n);
        });
    }

    /**
     * 수강생이 모자라면 시연용으로 채운다.
     * 계정은 로그인할 일이 없지만 형식은 실제와 같게 만든다 — 비밀번호도 인코딩해 둔다.
     */
    private void padTrainees(Long courseId) {
        long now = enrollmentRepository.findByCourseIdOrderByAppliedAtDesc(courseId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        if (now >= MIN_TRAINEES) return;

        String[] names = {"김서연", "박도윤", "이하은", "정민준", "최지우", "한소율"};
        for (int i = 0; (now + i) < MIN_TRAINEES; i++) {
            String loginId = "demo_t" + courseId + "_" + i;
            if (userRepository.findByLoginId(loginId).isPresent()) continue;

            User u = userRepository.save(User.builder()
                    .loginId(loginId)
                    .password(passwordEncoder.encode("1234"))
                    .name(names[i % names.length])
                    .role(Role.TRAINEE)
                    .status(UserStatus.ACTIVE)
                    .build());

            enrollmentRepository.save(Enrollment.builder()
                    .trainee(u).course(courseRepository.getReferenceById(courseId))
                    .status(EnrollmentStatus.APPROVED)
                    .appliedAt(LocalDateTime.now().minusDays(7))
                    .build());
        }
        enrollmentRepository.flush();
    }

    private Integer targetOf(String courseName) {
        if (courseName == null) return null;
        for (var e : TARGET.entrySet()) {
            if (courseName.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
