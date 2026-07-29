/**
 * 강사 대시보드 차트 + 처리할 일 체크리스트.
 *
 * 관리자 화면과 **같은 그림**을 담당 과정 범위로만 그린다 — 보는 범위만 다르고
 * 판단 기준은 같아야 한다. 그리는 코드는 dashboard-charts-common.js 한 벌을 쓴다.
 *
 * 데이터 출처
 *  · 과정 이름       window._serverInstructorDashboard.courses[].name   ← 서버 실제 값
 *  · 처리할 일 건수  window._serverInstructorDashboard.kpi              ← 서버 실제 값
 *  · 진도·기간·이수  아직 서버가 내려주지 않는다 → 화면에 "표본" 이라고 밝힌다
 *
 * 예전에 여기 있던 차트는 고정 시드 난수로 만든 가짜였고, 코딩시간·이탈률처럼
 * 이 시스템이 수집하지도 않는 지표였다. 지금은 최소한 **과정 이름은 진짜**이고,
 * 아직 없는 수치는 없다고 표시한다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var d = window._serverInstructorDashboard;
        if (!d || !window.DashCharts) return;

        renderWorklist(d.kpi);
        draw(d);

        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { draw(d); }, 200);
        });
    });

    function draw(d) {
        var m = window._serverDashboardMetrics;

        // 서버 지표가 있으면 그것만 쓴다 (담당 과정 범위로 계산돼 내려온다)
        if (m) {
            window.DashCharts.courseProgress('courseProgressChart', m.courses,
                '수강생이 배정된 담당 과정이 없습니다');
            window.DashCharts.completion('completionChart', m.completion,
                '담당 과정에 이수 판정 이력이 없습니다');
            return;   // 표본 배지도 달지 않는다
        }

        // 서버 지표가 없을 때만 표본 (과정 이름은 실제 값)
        var courses = (d.courses || []).filter(function (c) { return c.name; });
        var rows = courses.map(function (c, i) {
            var p = SAMPLE_PROGRESS[i % SAMPLE_PROGRESS.length];
            return { name: c.name, progress: p.progress, elapsed: p.elapsed };
        });
        window.DashCharts.courseProgress('courseProgressChart', rows, '담당 과정이 없습니다');
        mark('courseProgressChart');

        var total = countTrainees(courses);
        window.DashCharts.completion('completionChart',
            total ? sampleCompletion(total) : [], '이수 판정 이력이 없습니다');
        if (total) mark('completionChart');
    }

    /** 담당 과정 수강 인원 합계 — courses[].progress 가 "수강생 N명" 문자열이다. */
    function countTrainees(courses) {
        return courses.reduce(function (a, c) {
            var m = String(c.progress || '').match(/\d+/);
            return a + (m ? parseInt(m[0], 10) : 0);
        }, 0);
    }

    var SAMPLE_PROGRESS = [
        { progress: 62, elapsed: 58 },
        { progress: 41, elapsed: 70 },
        { progress: 88, elapsed: 84 },
        { progress: 25, elapsed: 46 }
    ];

    function sampleCompletion(total) {
        return [
            { label: '진도 충족', done: Math.round(total * 0.75), total: total },
            { label: '출석 충족', done: Math.round(total * 0.88), total: total },
            { label: '성적 충족', done: Math.round(total * 0.58), total: total },
            { label: '전체 충족', done: Math.round(total * 0.50), total: total }
        ];
    }

    /**
     * 아직 서버 값이 아닌 차트에는 표시를 남긴다.
     * 표본을 실제 수치로 착각하면 잘못된 판단을 하게 된다.
     */
    function mark(canvasId) {
        var cv = document.getElementById(canvasId);
        if (!cv || cv.parentNode.querySelector('.sample-mark')) return;
        var tag = document.createElement('span');
        tag.className = 'sample-mark';
        tag.textContent = '표본';
        tag.title = '서버 연동 전 표본 값입니다. 과정 이름만 실제 데이터입니다.';
        cv.parentNode.insertBefore(tag, cv);
    }

    /**
     * 처리할 일 — 막대가 아니라 체크리스트.
     * 건수가 0~2건이라 막대로는 "0 아니면 100"으로만 보여 아무 정보가 안 된다.
     * 무엇을 눌러서 처리하는지가 훨씬 중요하다.
     */
    function renderWorklist(kpi) {
        var box = document.getElementById('myWorklist');
        if (!box) return;
        kpi = kpi || {};

        var items = [
            { label: '과제 채점', n: num(kpi.pendingAssignments), href: '/instructor/assignments', cta: '채점' },
            { label: '시험 채점', n: num(kpi.pendingExams), href: '/instructor/grading', cta: '채점' },
            { label: '문의 응답', n: num(kpi.pendingSupport), href: '/instructor/notice', cta: '응답' },
            { label: '시험 감독', n: num(kpi.proctoringExams), href: '/instructor/proctor/exams', cta: '모니터링' }
        ];

        var left = items.filter(function (i) { return i.n > 0; }).length;
        var sum = document.getElementById('workloadSummary');
        if (sum) sum.textContent = left ? left + '개 항목 남음' : '모두 처리됨';

        // 남은 것을 위로 — 다 끝난 항목이 위에 있으면 할 일이 안 보인다
        items.sort(function (a, b) { return (b.n > 0) - (a.n > 0); });

        box.innerHTML = items.map(function (i) {
            var done = i.n === 0;
            return '<li class="worklist-item' + (done ? ' done' : '') + '">' +
                '<span class="worklist-check">' + (done ? '✓' : '') + '</span>' +
                '<span class="worklist-label">' + esc(i.label) + '</span>' +
                '<span class="worklist-count">' + (done ? '완료' : i.n + '건') + '</span>' +
                (done ? '' : '<a class="btn btn-gray btn-sm" href="' + esc(i.href) + '">' + esc(i.cta) + '</a>') +
            '</li>';
        }).join('');
    }

    function num(v) {
        var m = String(v == null ? '' : v).match(/\d+/);
        return m ? parseInt(m[0], 10) : 0;
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
