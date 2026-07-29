package com.ssa.lms.dashboard.service;

import com.ssa.lms.dashboard.dto.DropoutPredictionView;
import org.springframework.stereotype.Service;

/**
 * 학습자 이탈(중도탈락) 예측.
 *
 * <p><b>현재는 표본 단계다.</b> 운영 데이터가 아직 없어 모델을 학습시킬 수 없으므로
 * {@link DropoutPredictionView#sampleView()} 고정 표본을 돌려주고, 화면은 표본임을 명시한다.
 * 실데이터가 없는데 그럴듯한 계산을 흉내 내면 표본보다 위험하다 — 진짜로 오해된다.</p>
 *
 * <p><b>실모델 연동 지점(이 메서드 하나만 바꾸면 된다):</b> 훈련 데이터가 쌓이면
 * 아래 소스에서 피처를 뽑아 학습한 모델(XGBoost 등)의 예측값으로 DTO 를 채운다.
 * 컨트롤러·화면·JS 는 그대로다.</p>
 * <ul>
 *   <li>접속: {@code user.entity.AccessLog} — 최근 접속 간격, 접속 빈도 추세</li>
 *   <li>진도: {@code content.entity.Progress}, {@code course.entity.Enrollment.progressRate} — 기간 경과 대비 지연폭</li>
 *   <li>출결: {@code attendance.entity.Attendance} — 출석률, 최근 결석 연속일</li>
 *   <li>제출/응시: {@code assignment.entity.Submission}, {@code exam.entity.ExamAttempt} — 미제출·미응시 건수</li>
 * </ul>
 */
@Service
public class DropoutPredictionService {

    public DropoutPredictionView forAdmin() {
        return DropoutPredictionView.sampleView();
    }
}
