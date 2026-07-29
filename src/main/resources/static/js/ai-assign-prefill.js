/**
 * ════════════════════════════════════════════════════════════════════════
 *  [기능 4-연결] AI 추천 과제 → 배정 화면 값 미리 채우기
 * ════════════════════════════════════════════════════════════════════════
 *
 * ■ 무엇을 하는가
 *   강사 AI 학습진단(/instructor/ai/diagnosis)에서 [추천 과제 배부하기]를 누르면
 *   아래 쿼리를 달고 과제 배정 화면으로 온다. 이 스크립트가 그 값을 채운다.
 *
 *     ?aiTask=과제명&aiTrainees=이훈련,정민준&aiCourseId=1
 *
 * ■ 설계상 지킨 것 — AI 는 확정하지 않는다
 *   폼 바인딩(th:field)은 건드리지 않는다. **값만 넣고 나머지는 강사가 확인해 채운다.**
 *   AI 가 배정을 확정하는 게 아니라 입력을 줄여주는 역할이다.
 *   어디서 왜 왔는지 화면에 배너로 남긴다 — 값이 미리 채워진 이유를 알 수 있어야 한다.
 *
 * ■ 평소 배정 화면에서는 아무 일도 하지 않는다 (aiTask 가 없으면 즉시 반환)
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var q = new URLSearchParams(window.location.search);
        var task = q.get('aiTask');
        if (!task) return;   // 평소 배정 화면에서는 아무 일도 하지 않는다

        var trainees = (q.get('aiTrainees') || '').split(',').filter(Boolean);
        var courseId = q.get('aiCourseId');

        banner(task, trainees);
        prefill(task, courseId, q.get('aiDesc'), q.get('aiCriteria'));
    });

    /** 어디서 왜 왔는지 화면에 남긴다 — 값이 미리 채워진 이유를 알 수 있어야 한다. */
    function banner(task, trainees) {
        var anchor = document.querySelector('.main-content-inner')
                  || document.querySelector('main')
                  || document.body;

        var box = document.createElement('div');
        box.className = 'ai-disclaimer';
        box.style.margin = '0 0 16px';
        box.innerHTML =
            '<span>🤖</span>' +
            '<span><b>AI 학습진단에서 넘어왔어요.</b><br>' +
            '추천 과제: <b>' + esc(task) + '</b>' +
            (trainees.length
                ? '<br>대상 훈련생 ' + trainees.length + '명: ' + esc(trainees.join(', '))
                : '') +
            '<br>아래 내용을 확인하고 배정해 주세요. <b>확인 없이 배정되지 않아요.</b></span>';

        anchor.insertBefore(box, anchor.firstChild);
    }

    function prefill(task, courseId, desc, criteria) {
        // 과정 — 진단 대상 훈련생이 듣는 과정
        var course = document.getElementById('courseSelect');
        if (course && courseId) {
            setIfExists(course, courseId);
        }

        /*
         * 과제 제목. 이 화면은 "기존 과제 선택(라디오)" 과 "새 과제 만들기" 두 갈래인데,
         * AI 추천 과제는 아직 없는 과제이므로 새로 만드는 쪽에 넣는다.
         */
        var title = document.getElementById('newAssignmentTitle');
        if (title && !title.value) {
            title.value = task;
            title.focus();
        }

        /*
         * 설명·평가기준은 AI 초안에서 왔을 때만 넘어온다.
         *
         * 이 화면(AssignmentForm)에는 **평가기준 입력칸이 없다.** 그래서 설명 아래에
         * 붙여 넣는다 — 버리면 AI 가 만든 배점이 통째로 사라진다.
         * 폼에 필드가 생기면 그때 분리하면 된다.
         *
         * 이미 강사가 쓴 내용이 있으면 덮어쓰지 않는다 — 남의 입력을 지우면 안 된다.
         */
        var body = (desc || '').trim();
        if (criteria && criteria.trim()) {
            body += (body ? '\n\n' : '') + '[평가 기준]\n' + criteria.trim();
        }
        var descEl = document.getElementById('newAssignmentDesc');
        if (descEl && !descEl.value && body) descEl.value = body;
    }

    /** 없는 값을 강제로 넣으면 select 가 빈 상태가 되므로, 있는 경우에만 고른다. */
    function setIfExists(select, value) {
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].value === String(value)) {
                select.value = String(value);
                select.dispatchEvent(new Event('change', { bubbles: true }));
                return;
            }
        }
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
