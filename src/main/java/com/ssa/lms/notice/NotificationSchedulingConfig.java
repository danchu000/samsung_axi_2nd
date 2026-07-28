package com.ssa.lms.notice;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화.
 *
 * <p><b>왜 여기 있나:</b> {@code @EnableScheduling} 은 보통 공용 설정({@code com.ssa.lms.config})
 * 이나 메인 클래스에 붙이는데, 그쪽은 개발자 A 소유라 B 가 수정하지 않기로 한 영역이다
 * (CLAUDE.md). {@code @EnableScheduling} 은 어느 {@code @Configuration} 에 붙어도
 * 애플리케이션 전역에 적용되므로 B 패키지에 두었다.</p>
 *
 * <p><b>A 확인 필요:</b> 공용 설정으로 옮기는 게 맞다고 판단되면 이 클래스를 지우고
 * {@code com.ssa.lms.config} 로 이관하면 된다. 중복으로 붙어도 동작에는 문제가 없다.</p>
 *
 * <p>끄려면 {@code lms.scheduler.notification.enabled=false} — 개별 스케줄러 단위로 막는다.
 * 스케줄링 자체를 끄면 이후 다른 도메인의 스케줄러도 같이 멈추므로 여기서 끄지 마라.</p>
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfig {
}
