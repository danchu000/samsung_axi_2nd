/**
 * 강사 대시보드 차트 + 처리할 일 체크리스트.
 *
 * <b>관리자 화면과 완전히 같은 코드 경로를 쓴다.</b> 그리는 함수(DashCharts)도,
 * 데이터 모양(window._serverDashboardMetrics)도 같고, 범위만 담당 과정으로 좁혀
 * 서버에서 계산돼 내려온다.
 *
 * 예전엔 강사 쪽에만 표본 폴백이 남아 있어 같은 과정을 두고 관리자와 강사가
 * 다른 그림을 봤다. 판단 기준이 갈리면 안 된다.
 *
 * 데이터 출처
 *  · 진도·기간·이수  window._serverDashboardMetrics   (DashboardMetricsService)
 *  · 처리할 일 건수  window._serverInstructorDashboard.kpi
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        if (!window.DashCharts) return;

        var d = window._serverInstructorDashboard;
        if (d) renderWorklist(d.kpi);

        draw();

        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(draw, 200);
        });
    });

    function draw() {
        var m = window._serverDashboardMetrics;
        if (!m) return;   // 서버 값이 없으면 아무것도 그리지 않는다 — 가짜로 채우지 않는다

        window.DashCharts.courseProgress('courseProgressChart', m.courses,
            '수강생이 배정된 담당 과정이 없습니다');
        window.DashCharts.completion('completionChart', m.completion,
            '담당 과정에 이수 판정 이력이 없습니다');
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
