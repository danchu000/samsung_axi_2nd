/**
 * 직무 로드맵 화면 (훈련생).
 *
 * 지금은 화면만 있는 단계라 아래 더미로 그린다.
 * 서버가 붙으면 화면에서 window._serverRoadmap 을 내려주면 되고, 이 파일은
 * 그대로 둔다 — 다른 화면들과 같은 규칙이다.
 *
 *   window._serverRoadmap = { collectedAt: '2026-07-27', jobs: [...] }
 *
 * 주의: 대입 스크립트는 이 파일보다 **앞에** 와야 한다. 뒤에 두면 서버 데이터가
 * 영원히 더미에 가려진다 (CLAUDE.md 규칙 4).
 */
(function () {
    'use strict';

    var DUMMY = {
        collectedAt: '2026-07-27',
        jobs: [
            {
                id: 'backend',
                name: '백엔드 개발자',
                postingCount: 128,
                matchRate: 62,
                avgCareer: '신입~3년',
                have: ['Java', 'Spring Boot', 'JPA', 'MySQL', 'Git', 'REST API'],
                lack: ['Docker', 'AWS', 'Redis', 'CI/CD', '테스트 코드'],
                meters: [
                    { label: '언어·프레임워크', value: 80 },
                    { label: '데이터베이스', value: 65 },
                    { label: '인프라·배포', value: 25 },
                    { label: '협업·형상관리', value: 70 },
                    { label: '테스트·품질', value: 30 }
                ],
                steps: [
                    {
                        title: 'Java · Spring 기본기',
                        meta: '이수 완료 · 2026-05 ~ 2026-07',
                        reason: '"클라우드 기반 풀스택" 과정에서 이미 학습했어요. 공고 92%가 요구하는 항목이라 잘 갖추셨어요.',
                        status: 'done', icon: '☕'
                    },
                    {
                        title: '테스트 코드 작성 습관 들이기',
                        meta: '예상 2주 · 공고 68%가 요구',
                        reason: '지금 수강 중인 과정에서 단위 테스트를 다루지 않았어요. 백엔드 공고 대부분이 요구하는 항목이라 가장 먼저 채우면 좋아요.',
                        status: 'current', icon: '🧪'
                    },
                    {
                        title: 'Docker 로 개발 환경 구성하기',
                        meta: '예상 3주 · 공고 74%가 요구',
                        reason: '보완이 필요한 역량 중 요구 빈도가 가장 높아요. 원내 "컨테이너 기반 배포" 과정과 연결돼요.',
                        status: 'locked', icon: '🐳'
                    },
                    {
                        title: 'AWS 배포 경험 만들기',
                        meta: '예상 4주 · 공고 71%가 요구',
                        reason: 'Docker 를 익힌 뒤에 이어서 하면 학습 부담이 줄어요.',
                        status: 'locked', icon: '☁️'
                    },
                    {
                        title: '포트폴리오 프로젝트 정리',
                        meta: '예상 3주',
                        reason: '이수한 과정의 과제 결과물을 묶으면 별도 준비 없이 포트폴리오가 돼요.',
                        status: 'locked', icon: '📦'
                    }
                ],
                postings: [
                    { company: '(주)에이치디테크', title: '백엔드 개발자 (신입/경력)', skills: 'Java, Spring Boot, MySQL, Docker', date: '2026-07-26' },
                    { company: '세종소프트', title: 'Java 백엔드 서버 개발', skills: 'Java, JPA, AWS, Redis', date: '2026-07-25' },
                    { company: '넥스트클라우드', title: '플랫폼 백엔드 엔지니어', skills: 'Spring, Kubernetes, CI/CD', date: '2026-07-24' },
                    { company: '(주)데이터브릿지', title: '주니어 백엔드 개발자', skills: 'Java, Spring Boot, 테스트 코드', date: '2026-07-22' }
                ]
            },
            {
                id: 'data',
                name: '데이터 분석가',
                postingCount: 74,
                matchRate: 41,
                avgCareer: '신입~2년',
                have: ['Python', 'SQL', '통계 기초'],
                lack: ['Pandas 심화', '데이터 시각화', '머신러닝', 'BI 도구'],
                meters: [
                    { label: '프로그래밍', value: 60 },
                    { label: '데이터 처리', value: 45 },
                    { label: '통계·분석', value: 40 },
                    { label: '시각화·리포팅', value: 20 },
                    { label: '머신러닝', value: 15 }
                ],
                steps: [
                    {
                        title: 'Python · SQL 기초',
                        meta: '이수 완료 · 2026-04 ~ 2026-06',
                        reason: '데이터 직무의 공통 기반이에요. 이미 갖추셨어요.',
                        status: 'done', icon: '🐍'
                    },
                    {
                        title: 'Pandas 로 데이터 다루기',
                        meta: '예상 3주 · 공고 82%가 요구',
                        reason: '데이터 직무 공고에서 가장 많이 등장하는 도구예요.',
                        status: 'current', icon: '🧮'
                    },
                    {
                        title: '시각화 리포트 만들어보기',
                        meta: '예상 2주 · 공고 63%가 요구',
                        reason: '분석 결과를 전달하는 능력을 함께 봐요.',
                        status: 'locked', icon: '📊'
                    },
                    {
                        title: '머신러닝 기초 과정 수강',
                        meta: '예상 5주 · 공고 55%가 요구',
                        reason: '원내 "AI 실무" 과정이 이 단계에 해당해요.',
                        status: 'locked', icon: '🤖'
                    }
                ],
                postings: [
                    { company: '(주)인사이트랩', title: '데이터 분석가 (신입)', skills: 'Python, Pandas, SQL', date: '2026-07-26' },
                    { company: '메트릭스코리아', title: 'BI 분석 담당자', skills: 'SQL, Tableau, 통계', date: '2026-07-23' }
                ]
            },
            {
                id: 'frontend',
                name: '프론트엔드 개발자',
                postingCount: 96,
                matchRate: 55,
                avgCareer: '신입~3년',
                have: ['HTML/CSS', 'JavaScript', 'Git'],
                lack: ['React', 'TypeScript', '상태관리', '반응형 설계'],
                meters: [
                    { label: '마크업·스타일', value: 85 },
                    { label: 'JavaScript', value: 60 },
                    { label: '프레임워크', value: 20 },
                    { label: '타입 시스템', value: 10 },
                    { label: '협업·형상관리', value: 70 }
                ],
                steps: [
                    {
                        title: 'HTML · CSS · JavaScript',
                        meta: '이수 완료 · 2026-03 ~ 2026-05',
                        reason: '웹 개발의 기본기예요. 이미 갖추셨어요.',
                        status: 'done', icon: '🎨'
                    },
                    {
                        title: 'React 기초 익히기',
                        meta: '예상 4주 · 공고 88%가 요구',
                        reason: '프론트엔드 공고에서 사실상 필수로 요구돼요.',
                        status: 'current', icon: '⚛️'
                    },
                    {
                        title: 'TypeScript 적용하기',
                        meta: '예상 3주 · 공고 67%가 요구',
                        reason: 'React 를 익힌 다음에 붙이면 이해가 빨라요.',
                        status: 'locked', icon: '🔷'
                    }
                ],
                postings: [
                    { company: '위드유컴퍼니', title: '프론트엔드 개발자', skills: 'React, TypeScript', date: '2026-07-25' },
                    { company: '(주)스튜디오랩', title: '웹 UI 개발', skills: 'JavaScript, React, CSS', date: '2026-07-21' }
                ]
            }
        ]
    };

    var data = window._serverRoadmap || DUMMY;
    var current = null;

    document.addEventListener('DOMContentLoaded', function () {
        var sel = document.getElementById('jobSelect');
        data.jobs.forEach(function (job) {
            var opt = document.createElement('option');
            opt.value = job.id;
            opt.textContent = job.name;
            sel.appendChild(opt);
        });

        var stamp = document.querySelector('.js-collected-at');
        if (stamp) stamp.textContent = '마지막 수집: ' + data.collectedAt;

        sel.addEventListener('change', function () { render(sel.value); });
        document.getElementById('btnAnalyze').addEventListener('click', function () { render(sel.value); });

        render(data.jobs[0].id);

        // 폭이 바뀌면 정거장 좌표가 달라지므로 다시 그린다
        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { if (current) renderRoadmap(); }, 200);
        });
    });

    function render(jobId) {
        current = data.jobs.filter(function (j) { return j.id === jobId; })[0];
        if (!current) return;
        renderSummary();
        renderSkills();
        renderMeters();
        renderRoadmap();
        renderPostings();
    }

    function renderSummary() {
        var barClass = current.matchRate >= 70 ? '' : (current.matchRate >= 45 ? 'warn' : 'danger');
        document.getElementById('summaryGrid').innerHTML =
            card('수집된 채용공고', current.postingCount + '건', '최근 4주 기준') +
            '<div class="ai-card"><p class="ai-card-sub" style="margin:0 0 6px;">내 역량 충족도</p>' +
            '<p class="ai-card-title" style="font-size:26px; margin-bottom:10px;">' + current.matchRate + '%</p>' +
            '<div class="ai-bar ' + barClass + '"><span style="width:' + current.matchRate + '%"></span></div></div>' +
            card('공고 요구 경력', current.avgCareer, '평균값');
    }

    function card(label, value, sub) {
        return '<div class="ai-card">' +
            '<p class="ai-card-sub" style="margin:0 0 6px;">' + label + '</p>' +
            '<p class="ai-card-title" style="font-size:26px;">' + value + '</p>' +
            '<p class="ai-card-sub">' + sub + '</p></div>';
    }

    function renderSkills() {
        // 색만으로 구분하지 않도록 기호를 함께 붙인다
        document.getElementById('skillsHave').innerHTML = current.have.map(function (s) {
            return '<span class="ai-tag have">✓ ' + esc(s) + '</span>';
        }).join('');
        document.getElementById('skillsLack').innerHTML = current.lack.map(function (s) {
            return '<span class="ai-tag lack">! ' + esc(s) + '</span>';
        }).join('');
    }

    function renderMeters() {
        document.getElementById('skillMeters').innerHTML = current.meters.map(function (m) {
            var cls = m.value >= 70 ? '' : (m.value >= 40 ? 'warn' : 'danger');
            return '<div class="ai-meter-row">' +
                '<span class="label">' + esc(m.label) + '</span>' +
                '<span class="ai-bar ' + cls + '"><span style="width:' + m.value + '%"></span></span>' +
                '<span class="value">' + m.value + '%</span></div>';
        }).join('');
    }

    function renderRoadmap() {
        // 지도 — 길 위의 정거장으로 그린다
        if (typeof window.renderRoadmapMap === 'function') {
            window.renderRoadmapMap(current.steps, showDetail);
        }

        // 좁은 화면용 목록 (지도가 겹쳐 안 보일 때 CSS 가 이쪽을 보여준다)
        document.getElementById('roadmapList').innerHTML = current.steps.map(function (s, i) {
            return '<li>' +
                '<span class="ai-rank' + (s.status === 'done' ? ' dim' : '') + '">' + (i + 1) + '</span>' +
                '<div style="flex:1;">' +
                '<p class="ai-item-title">' + (s.icon || '') + ' ' + esc(s.title) + '</p>' +
                '<p class="ai-item-meta">' + esc(s.meta) + '</p>' +
                '<div class="ai-reason">' + esc(s.reason) + '</div>' +
                '</div></li>';
        }).join('');

        // 처음에는 "지금 할 차례"를 펼쳐 둔다 — 사용자가 가장 궁금한 단계다
        var idx = 0;
        for (var i = 0; i < current.steps.length; i++) {
            if (current.steps[i].status === 'current') { idx = i; break; }
        }
        showDetail(idx);
    }

    /** 정거장 상세. 지도에서 정거장을 누르면 바뀐다. */
    function showDetail(idx) {
        var box = document.getElementById('mapDetail');
        if (!box) return;
        var s = current.steps[idx];
        if (!s) { box.innerHTML = ''; return; }

        var stateText = s.status === 'done' ? '완료한 단계'
                      : (s.status === 'current' ? '지금 할 차례' : '앞으로 할 단계');
        var stateTag = s.status === 'done' ? 'have' : (s.status === 'current' ? 'partial' : 'lack');

        box.innerHTML =
            '<div class="map-detail-head">' +
                '<span class="map-detail-icon">' + (s.icon || '📘') + '</span>' +
                '<div>' +
                    '<p class="ai-item-title" style="font-size:15px;">' + esc(s.title) + '</p>' +
                    '<p class="ai-item-meta">' + esc(s.meta) + '</p>' +
                '</div>' +
                '<span class="ai-tag ' + stateTag + '" style="margin-left:auto;">' + stateText + '</span>' +
            '</div>' +
            '<div class="ai-reason"><b>왜 이 단계인가요?</b><br>' + esc(s.reason) + '</div>' +
            (s.status === 'locked'
                ? '<p class="ai-card-sub" style="margin-top:10px;">앞 단계를 먼저 마치면 더 수월해요.</p>'
                : '');

        // 지도에서도 선택한 정거장을 표시
        document.querySelectorAll('.map-node').forEach(function (el) {
            el.setAttribute('aria-current', Number(el.dataset.idx) === idx ? 'true' : 'false');
        });
    }

    function renderPostings() {
        var body = document.getElementById('postingBody');
        if (!current.postings.length) {
            body.innerHTML = '<tr><td colspan="4" class="ai-empty">수집된 공고가 없어요.</td></tr>';
            return;
        }
        body.innerHTML = current.postings.map(function (p) {
            return '<tr>' +
                '<td>' + esc(p.company) + '</td>' +
                '<td>' + esc(p.title) + '</td>' +
                '<td>' + esc(p.skills) + '</td>' +
                '<td>' + esc(p.date) + '</td></tr>';
        }).join('');
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
