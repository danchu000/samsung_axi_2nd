package com.ssa.lms.user;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.service.AdminAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 관리자 계정 서비스 — "마지막 활성 관리자 비활성화 거부" 불변식 검증.
 */
@SpringBootTest
@ActiveProfiles("local")
class AdminAccountServiceTest {

    @Autowired AdminAccountService adminAccountService;

    @Test
    @DisplayName("마지막 활성 관리자는 비활성화할 수 없다")
    void 마지막_활성관리자_보호() {
        List<User> actives = adminAccountService.findAdmins().stream()
                .filter(a -> a.getStatus() == UserStatus.ACTIVE)
                .toList();
        assertThat(actives).isNotEmpty();

        // 마지막 1명을 남기고 모두 비활성화 (여기까지는 성공)
        for (int i = 0; i < actives.size() - 1; i++) {
            adminAccountService.deactivate(actives.get(i).getId());
        }

        // 마지막 활성 관리자 비활성화 시도 → 거부
        Long lastId = actives.get(actives.size() - 1).getId();
        assertThatThrownBy(() -> adminAccountService.deactivate(lastId))
                .isInstanceOf(IllegalStateException.class);

        assertThat(adminAccountService.countActiveAdmins()).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자가 아닌 계정 조회는 예외")
    void 비관리자_조회_예외() {
        assertThatThrownBy(() -> adminAccountService.get(-999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
