-- =====================================================================
-- PostgreSQL 전용: soft delete 와 공존하는 부분 유니크 인덱스
-- =====================================================================
--
-- 왜 별도 스크립트인가
--   이 프로젝트는 삭제를 물리 DELETE 가 아니라 is_deleted = true 마킹으로 한다
--   (내역서 3년 보존 요건). 그런데 일반 UNIQUE 제약은 is_deleted 를 보지 않아서,
--   행을 삭제한 뒤 같은 값을 다시 넣으면 반드시 충돌한다.
--   실제로 course_assignment / survey_question / question 세 곳에서 500 이 났다.
--
--   PostgreSQL 은 WHERE 절을 가진 부분 유니크 인덱스를 지원하므로
--   "살아 있는 행끼리만 유일" 이라는 정확한 제약을 걸 수 있다.
--   JPA 의 @UniqueConstraint / @Index 로는 WHERE 를 표현할 수 없어 SQL 로 둔다.
--
-- 왜 dev(PostgreSQL) 프로필에서만 실행되는가
--   local 프로필의 H2 는 부분 인덱스를 지원하지 않는다 (실측 확인:
--   "Syntax error ... [*]where is_deleted = false"). H2 에서 일반 UNIQUE 로
--   대체하면 위의 삭제-재등록 버그가 로컬 개발에서 되살아난다.
--   그래서 local 은 서비스 계층 중복 검사에만 의존하고, 실제 보호는 운영 DB 에서 건다.
--
-- 멱등성
--   ddl-auto: update 와 함께 매 기동마다 실행되므로 IF NOT EXISTS 를 붙였다.
-- =====================================================================

-- 과정에 배정된 과제: 같은 과정에 같은 과제를 두 번 배정할 수 없다.
-- (배정 삭제 후 같은 과제를 다시 배정하는 것은 허용되어야 한다)
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_assignment_alive
    ON course_assignment (course_id, assignment_id)
    WHERE is_deleted = false;

-- 성적: 학습자 × 평가 1건당 한 행.
-- Grade 에는 아직 soft delete 를 걸지 않았지만, 나중에 붙더라도 이 인덱스가
-- 그대로 유효하도록 처음부터 부분 인덱스로 만든다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_grade_target_alive
    ON grade (user_id, eval_type, eval_ref_id)
    WHERE is_deleted = false;

-- 시험 응시 회차: (시험, 응시자, 회차) 유일.
CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_attempt_alive
    ON exam_attempt (exam_id, user_id, attempt_no)
    WHERE is_deleted = false;

-- 과제 제출: (배정, 제출자, 회차) 유일. 재제출은 attempt_no 를 올린 새 행이다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_attempt_alive
    ON submission (course_assignment_id, user_id, attempt_no)
    WHERE is_deleted = false;

-- 설문 응답: 한 사람이 같은 설문에 두 번 응답할 수 없다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_survey_response_user_alive
    ON survey_response (survey_id, user_id)
    WHERE is_deleted = false;

-- 알림 수신자: 같은 알림을 같은 사람에게 두 번 만들지 않는다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_recipient_alive
    ON notification_recipient (notification_id, user_id)
    WHERE is_deleted = false;

-- =====================================================================
-- 여기 넣지 않은 것과 그 이유
--
--  question(question_code)
--    일반 UNIQUE 를 그대로 둔다. 문제 코드는 "한 번 쓰면 영원히 그 문제의 것"
--    이어야 감사 추적이 된다. 삭제된 코드를 재사용하면 과거 시험 기록의 Q-0004 가
--    어느 문제였는지 모호해진다. 대신 채번/중복검사를 네이티브 쿼리로 바꿔
--    삭제된 행까지 보도록 했다 (QuestionRepository.findMaxQuestionCode).
--
--  survey_question(survey_id, seq), survey_choice(question_id, seq)
--    이 둘은 soft delete 를 걸지 않고 물리 삭제한다. 부모(Survey)가 soft delete 라
--    보존 요건은 충족되고, 설문 수정 시 문항을 통째로 교체하는 동작과 맞다.
--    soft delete 를 다시 걸면 H2(local)에서 삭제-재삽입 버그가 되살아난다.
-- =====================================================================
