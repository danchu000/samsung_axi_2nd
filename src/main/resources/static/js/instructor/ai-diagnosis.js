/**
 * AI 학습진단 (강사).
 *
 * 훈련생이 AI 도우미에 남긴 질문 + 평가·과제 결과 → 취약 영역 진단 → 추천 과제.
 * 추천은 **제안일 뿐** 이고 배부는 강사가 확정한다 (화면에서 체크 → 배부).
 *
 * 서버 연동 시 window._serverDiagnosis 를 이 스크립트보다 앞에서 대입한다.
 */
(function () {
    'use strict';

    var DUMMY = {
        analyzedAt: '2026-07-29',
        summary: [
            { label: '분석 대상 훈련생', value: '24명', sub: '담당 과정 2개' },
            { label: '보완 필요', value: '7명', sub: '시급도 높음 3명' },
            { label: 'AI 질문 건수', value: '186건', sub: '최근 2주' }
        ],
        topics: [
            { label: '트랜잭션·동시성', value: 82, count: 38 },
            { label: 'Docker·배포', value: 64, count: 29 },
            { label: 'JPA 연관관계', value: 51, count: 24 },
            { label: 'REST API 설계', value: 33, count: 15 },
            { label: '테스트 코드', value: 22, count: 10 }
        ],
        rows: [
            {
                id: 1,
                name: '김훈련',
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'high',
                levelLabel: '높음',
                weak: ['트랜잭션·동시성', 'JPA 연관관계'],
                evidence: 'AI 도우미에 트랜잭션 관련 질문 9회 · 단원평가 3회차 해당 단원 4문항 중 1문항 정답 · 3주차 과제 미제출',
                task: '트랜잭션 격리수준 실습 과제'
            },
            {
                id: 2,
                name: '이수강',
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'high',
                levelLabel: '높음',
                weak: ['Docker·배포'],
                evidence: 'AI 도우미에 컨테이너 관련 질문 11회 · 5주차 실습 과제 재제출 2회 · 관련 문항 정답률 25%',
                task: 'Dockerfile 작성 실습 과제'
            },
            {
                id: 3,
                name: '박학생',
                course: '데이터 분석 실무 과정',
                level: 'high',
                levelLabel: '높음',
                weak: ['데이터 전처리'],
                evidence: 'AI 도우미에 Pandas 관련 질문 8회 · 과제 점수 평균 52점 (반 평균 74점)',
                task: 'Pandas 데이터 정제 실습 과제'
            },
            {
                id: 4,
                name: '최교육',
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'mid',
                levelLabel: '보통',
                weak: ['REST API 설계'],
                evidence: 'AI 도우미 질문 4회 · 중간평가 해당 단원 정답률 60%',
                task: 'REST API 설계 보완 과제'
            },
            {
                id: 5,
                name: '정연수',
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'mid',
                levelLabel: '보통',
                weak: ['테스트 코드'],
                evidence: '과제 제출물에 테스트 코드 누락 3회 · 관련 질문 2회',
                task: '단위 테스트 작성 과제'
            },
            {
                id: 6,
                name: '한지원',
                course: '데이터 분석 실무 과정',
                level: 'low',
                levelLabel: '낮음',
                weak: ['시각화'],
                evidence: 'AI 도우미 질문 2회 · 과제 점수 71점 (반 평균 74점)',
                task: '시각화 리포트 작성 과제'
            },
            {
                id: 7,
                name: '오민서',
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'low',
                levelLabel: '낮음',
                weak: ['JPA 연관관계'],
                evidence: 'AI 도우미 질문 3회 · 관련 문항 정답률 70%',
                task: 'JPA 연관관계 매핑 과제'
            }
        ]
    };

    var data = window._serverDiagnosis || DUMMY;

    document.addEventListener('DOMContentLoaded', function () {
        var stamp = document.querySelector('.js-analyzed-at');
        if (stamp) stamp.textContent = '분석 기준: ' + data.analyzedAt;

        renderSummary();
        renderTopics();
        renderRows(data.rows);

        document.getElementById('btnFilter').addEventListener('click', applyFilter);

        document.getElementById('checkAll').addEventListener('change', function (e) {
            document.querySelectorAll('#diagBody .js-pick').forEach(function (c) {
                c.checked = e.target.checked;
            });
        });

        document.getElementById('btnAssign').addEventListener('click', function () {
            var picked = Array.prototype.slice.call(document.querySelectorAll('#diagBody .js-pick:checked'));
            if (!picked.length) {
                alert('과제를 배부할 훈련생을 선택해 주세요.');
                return;
            }
            var names = picked.map(function (c) { return c.dataset.name; }).join(', ');
            alert('선택한 훈련생에게 추천 과제를 배부합니다.\n\n대상 ' + picked.length + '명: ' + names +
                  '\n\n과제 등록 화면으로 연결될 예정입니다. (서버 연동 준비 중)');
        });
    });

    function applyFilter() {
        var course = document.getElementById('courseFilter').value;
        var level = document.getElementById('levelFilter').value;
        renderRows(data.rows.filter(function (r) {
            return (!course || r.course === course) && (!level || r.level === level);
        }));
        document.getElementById('checkAll').checked = false;
    }

    function renderSummary() {
        document.getElementById('summaryGrid').innerHTML = data.summary.map(function (s) {
            return '<div class="ai-card" style="margin:0;">' +
                '<p class="ai-card-sub" style="margin:0 0 6px;">' + esc(s.label) + '</p>' +
                '<p class="ai-card-title" style="font-size:26px;">' + esc(s.value) + '</p>' +
                '<p class="ai-card-sub">' + esc(s.sub) + '</p></div>';
        }).join('');
    }

    function renderTopics() {
        document.getElementById('topicMeters').innerHTML = data.topics.map(function (t) {
            var cls = t.value >= 70 ? 'danger' : (t.value >= 45 ? 'warn' : '');
            return '<div class="ai-meter-row">' +
                '<span class="label">' + esc(t.label) + '</span>' +
                '<span class="ai-bar ' + cls + '"><span style="width:' + t.value + '%"></span></span>' +
                '<span class="value">' + t.count + '건</span></div>';
        }).join('');
    }

    function renderRows(rows) {
        var body = document.getElementById('diagBody');
        if (!rows.length) {
            body.innerHTML = '<tr><td colspan="6" class="ai-empty">조건에 맞는 훈련생이 없습니다.</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (r) {
            return '<tr>' +
                '<td><input type="checkbox" class="js-pick" data-id="' + r.id + '" data-name="' + esc(r.name) + '" aria-label="' + esc(r.name) + ' 선택"></td>' +
                '<td><b>' + esc(r.name) + '</b></td>' +
                '<td><span class="ai-level ' + r.level + '">' + esc(r.levelLabel) + '</span></td>' +
                '<td>' + r.weak.map(function (w) { return '<span class="ai-tag lack" style="margin:2px 3px 2px 0; display:inline-block;">' + esc(w) + '</span>'; }).join('') + '</td>' +
                '<td style="font-size:12.5px; color:#475569; line-height:1.7;">' + esc(r.evidence) + '</td>' +
                '<td>' + esc(r.task) +
                    '<div style="margin-top:7px;"><button type="button" class="btn btn-gray btn-sm js-one" data-name="' + esc(r.name) + '">이 훈련생만 배부</button></div>' +
                '</td></tr>';
        }).join('');

        body.querySelectorAll('.js-one').forEach(function (btn) {
            btn.addEventListener('click', function () {
                alert(btn.dataset.name + ' 훈련생에게 추천 과제를 배부합니다.\n\n과제 등록 화면으로 연결될 예정입니다. (서버 연동 준비 중)');
            });
        });
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
