/**
 * ════════════════════════════════════════════════════════════════════════
 *  [기능 2] 학습 데이터 기반 맞춤 커리큘럼 추천     화면: /trainee/ai/curriculum
 * ════════════════════════════════════════════════════════════════════════
 *
 * ■ 무엇을 하는가
 *   진도·평가·출결 기록으로 약한 영역을 찾고, 그 영역을 메워 줄 원내 과정을
 *   추천 이유와 함께 제시한다.
 *
 * ■ 어떻게 구현했는가
 *   1) 학습 요약(stats) — 판단의 입력값을 먼저 보여준다. 추천만 던지면
 *      "왜 나한테 이걸?"이 되고 안 누른다.
 *   2) 영역별 이해도(weak) — 45% 미만은 빨강, 70% 미만은 주황. 색은 구분이
 *      아니라 경고에만 쓴다.
 *   3) 추천 과정(recommend) — 적합도와 함께 **이유를 문장으로** 붙인다.
 *      이유 없는 추천은 광고로 읽힌다.
 *   4) 나중에 볼 과정(later) — 선수 과정 미이수 등으로 아직 못 듣는 것.
 *      숨기지 않고 "왜 지금은 아닌지"를 알려준다.
 *
 * ■ 추천 범위
 *   원내 개설 과정만 — 외부 강의를 섞지 않는다. 훈련기관 LMS 가 외부 강의를
 *   권하면 훈련생이 어디서 신청하는지 알 수 없다.
 *
 * ■ 데이터
 *   ⚠ 시연용(아래 DUMMY). 추천 엔진이 아직 없다.
 *     추천 과정명은 **실제 모집중 과정**(COURSE-2026-002/003)과 맞춰 뒀다 —
 *     화면에서 누르면 실제로 신청할 수 있는 과정이어야 말이 된다.
 *   ✅ 실데이터로 바꾸려면: 진도(Enrollment.progressRate)·성적(Grade)·
 *     출결(Attendance) 집계 → 취약 영역 산출 → 모집중 과정과 매칭.
 *
 * ■ 서버 연동 지점
 *   window._serverCurriculum 을 이 스크립트보다 **앞에서** 대입한다.
 */
(function () {
    'use strict';

    /*
     * 시연 데이터 — 실제 시드와 맞물리게 구성했다.
     *
     *  · 평균 진도율 55% : 대시보드 트랙에 찍히는 내 실제 진도와 같은 값.
     *    두 화면이 다른 숫자를 말하면 어느 쪽도 못 믿는다.
     *  · 취약 영역      : 이 과정에 실제로 등록된 학습 자료 6종(1~6주차)의 주제.
     *    자료에 없는 주제를 취약하다고 하면 공부할 곳이 없다.
     *  · 추천 과정      : 실제 모집중 과정 2개(COURSE-2026-002/003).
     *    누르면 실제로 신청할 수 있는 과정이어야 추천이 의미가 있다.
     */
    var DUMMY = {
        analyzedAt: '2026-07-29',
        stats: [
            { label: '평균 진도율', value: '55%', sub: '클라우드 기반 풀스택 과정' },
            { label: '평균 평가 점수', value: '68점', sub: '단원평가 3회 응시' },
            { label: '출석률', value: '92%', sub: '최근 8주' }
        ],
        weak: [
            { label: '데이터베이스·트랜잭션', value: 41 },   // 3주차 자료
            { label: 'JPA 영속성 컨텍스트', value: 55 },     // 4주차 자료
            { label: 'REST API 설계', value: 62 },           // 5주차 자료
            { label: '테스트 코드', value: 34 },             // 6주차 자료 — 가장 낮음
            { label: '의존성 주입', value: 78 }              // 2주차 자료
        ],
        recommend: [
            {
                name: '클라우드 인프라 입문 (AWS)',
                round: '1기',
                period: '2026-08-28 ~ 2026-12-29',
                seats: '정원 20명 · 모집중',
                fit: 88,
                reasons: [
                    '수강 중인 "클라우드 기반 풀스택" 과정의 다음 단계예요.',
                    '목표 직무(백엔드 개발자) 공고의 71%가 AWS 배포 경험을 요구해요.',
                    '지금 바로 신청할 수 있는 모집중 과정이에요.'
                ]
            },
            {
                name: '데이터 분석 실무 (Python/Pandas)',
                round: '2기',
                period: '2026-08-12 ~ 2026-11-29',
                seats: '정원 25명 · 신청 접수됨',
                fit: 64,
                reasons: [
                    '이미 신청하신 과정이에요. 승인되면 수강할 수 있어요.',
                    '데이터 직무로도 진로를 넓히고 싶다면 이 과정이 출발점이에요.'
                ]
            }
        ],
        later: [
            {
                name: '테스트 코드 작성 실무',
                meta: '개설 예정 · 일정 미정',
                reason: '테스트 코드 영역이 34%로 가장 낮지만, 아직 개설되지 않은 과정이에요. 개설되면 알림으로 알려드려요.'
            },
            {
                name: '데이터베이스 성능 튜닝',
                meta: '개설 예정 · 일정 미정',
                reason: '"데이터 분석 실무" 과정을 먼저 이수해야 해요. 현재 승인 대기 중이에요.'
            }
        ]
    };

    var data = window._serverCurriculum || DUMMY;

    document.addEventListener('DOMContentLoaded', function () {
        var stamp = document.querySelector('.js-analyzed-at');
        if (stamp) stamp.textContent = '분석 기준: ' + data.analyzedAt;

        renderStats();
        renderWeak();
        renderRecommend();
        renderLater();
    });

    function renderStats() {
        document.getElementById('statGrid').innerHTML = data.stats.map(function (s) {
            return '<div class="ai-card" style="margin:0;">' +
                '<p class="ai-card-sub" style="margin:0 0 6px;">' + esc(s.label) + '</p>' +
                '<p class="ai-card-title" style="font-size:26px;">' + esc(s.value) + '</p>' +
                '<p class="ai-card-sub">' + esc(s.sub) + '</p></div>';
        }).join('');
    }

    function renderWeak() {
        document.getElementById('weakMeters').innerHTML =
            '<p class="ai-item-title" style="margin-bottom:12px;">영역별 이해도</p>' +
            data.weak.map(function (m) {
                var cls = m.value >= 70 ? '' : (m.value >= 45 ? 'warn' : 'danger');
                return '<div class="ai-meter-row">' +
                    '<span class="label">' + esc(m.label) + '</span>' +
                    '<span class="ai-bar ' + cls + '"><span style="width:' + m.value + '%"></span></span>' +
                    '<span class="value">' + m.value + '%</span></div>';
            }).join('');
    }

    function renderRecommend() {
        var box = document.getElementById('recommendList');
        if (!data.recommend.length) {
            box.innerHTML = '<div class="ai-empty">추천할 과정이 아직 없어요.<br>학습을 더 진행하면 추천이 나와요.</div>';
            return;
        }
        box.innerHTML = data.recommend.map(function (c, i) {
            return '<div class="ai-card" style="margin-bottom:14px;">' +
                '<div class="ai-card-head" style="align-items:flex-start;">' +
                    '<div style="display:flex; gap:12px; align-items:flex-start;">' +
                        '<span class="ai-rank">' + (i + 1) + '</span>' +
                        '<div>' +
                            '<p class="ai-item-title" style="font-size:15px;">' + esc(c.name) + '</p>' +
                            '<p class="ai-item-meta">' + esc(c.round) + ' · ' + esc(c.period) + '<br>' + esc(c.seats) + '</p>' +
                        '</div>' +
                    '</div>' +
                    '<div style="text-align:right; flex:none;">' +
                        '<p class="ai-card-sub" style="margin:0 0 4px;">적합도</p>' +
                        '<p class="ai-card-title" style="font-size:22px;">' + c.fit + '%</p>' +
                    '</div>' +
                '</div>' +
                '<div class="ai-reason"><b>이 과정을 추천한 이유</b><br>' +
                    c.reasons.map(function (r) { return '· ' + esc(r); }).join('<br>') +
                '</div>' +
                '<div style="margin-top:14px; text-align:right;">' +
                    '<button type="button" class="btn btn-gray btn-sm js-detail">과정 상세</button> ' +
                    '<button type="button" class="btn btn-primary btn-sm js-apply">수강 신청</button>' +
                '</div>' +
            '</div>';
        }).join('');

        // 서버 연동 전이므로 이동 대신 안내만 — 조용히 아무 일도 안 일어나면 고장으로 보인다
        box.addEventListener('click', function (e) {
            if (e.target.classList.contains('js-apply')) {
                alert('수강 신청 화면으로 연결될 예정이에요. (서버 연동 준비 중)');
            } else if (e.target.classList.contains('js-detail')) {
                alert('과정 상세 화면으로 연결될 예정이에요. (서버 연동 준비 중)');
            }
        });
    }

    function renderLater() {
        var box = document.getElementById('laterList');
        if (!data.later.length) {
            box.innerHTML = '<li><div class="ai-empty">해당하는 과정이 없어요.</div></li>';
            return;
        }
        box.innerHTML = data.later.map(function (c) {
            return '<li>' +
                '<span class="ai-rank dim">–</span>' +
                '<div style="flex:1;">' +
                    '<p class="ai-item-title">' + esc(c.name) + '</p>' +
                    '<p class="ai-item-meta">' + esc(c.meta) + '</p>' +
                    '<div class="ai-reason">' + esc(c.reason) + '</div>' +
                '</div></li>';
        }).join('');
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
