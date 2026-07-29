/**
 * ════════════════════════════════════════════════════════════════════════
 *  [기능 4] AI 학습진단 → 추가 과제 배부        화면: /instructor/ai/diagnosis
 * ════════════════════════════════════════════════════════════════════════
 *
 * ■ 무엇을 하는가
 *   훈련생이 [기능 3] AI 도우미에 남긴 질문 + 평가·과제 결과를 묶어 취약 영역을
 *   진단하고, 보완할 과제를 강사에게 **제안**한다.
 *
 * ■ 어떻게 구현했는가
 *   1) 요약(summary) — 분석 대상·보완 필요 인원·질문 건수. 먼저 규모를 보여준다.
 *   2) 질문이 몰린 주제(topics) — 여러 명이 같은 곳에서 막히면 개인 문제가 아니라
 *      **수업에서 덜 다뤄진 것**이다. 그래서 개인 목록보다 위에 둔다.
 *   3) 훈련생 목록(rows) — 시급도 높음/보통/낮음. 근거(evidence)를 반드시 같이
 *      보여준다. 근거 없이 "보완 필요"라고만 하면 강사가 판단할 수 없다.
 *   4) 체크 → [추천 과제 배부하기] → 평가관리 > 과제채점 > 과제 배정 화면으로
 *      대상자를 실어 보낸다.
 *
 * ■ 설계상 지킨 것 — AI 는 배부하지 않는다
 *   추천은 제안일 뿐이고 **배부 확정은 강사**가 한다. 훈련생에게 과제가 늘어나는
 *   일을 모델이 자동으로 결정하면, 잘못 붙어도 되돌릴 사람이 없다.
 *   여러 명을 골랐을 때 추천 과제가 서로 다르면 같은 과제끼리만 묶어 넘긴다
 *   (배정 화면이 과제 하나를 다루기 때문).
 *
 * ■ 데이터
 *   ⚠ 시연용(아래 DUMMY). 진단 엔진이 아직 없다.
 *     훈련생 이름은 **실제 시드 계정**과 같게 맞췄고(김서연·박도윤·이하은·정민준·
 *     최지우·한소율·이훈련), 취약 주제도 실제 등록된 학습 자료 6종(1~6주차)의
 *     범위 안에서만 쓴다. 자료에 없는 주제를 진단하면 보완할 방법이 없다.
 *   ✅ 실데이터로 바꾸려면: AiUsageLog·질문 본문에서 주제 분류(AI) →
 *     Grade·Submission 과 대조 → 취약 영역 산출.
 *
 * ■ 서버 연동 지점
 *   window._serverDiagnosis 를 이 스크립트보다 **앞에서** 대입한다.
 */
(function () {
    'use strict';

    /*
     * 시연 데이터 — 실제 시드와 맞물리게 구성했다.
     *
     *  · 훈련생 이름 : local 시드가 실제로 만드는 계정과 같다. 화면에 없는 사람이
     *    나오면 과제를 배부하려 할 때 대상자를 못 찾는다.
     *  · 취약 주제   : 이 과정에 등록된 학습 자료 6종(1~6주차) 범위 안에서만 쓴다.
     *    자료에 없는 주제(예: Docker)를 진단해 봐야 보완할 자료가 없다.
     *  · 인원 수     : 담당 과정 2개 · 실제 수강 인원과 맞춘 규모로 적는다.
     */
    var DUMMY = {
        analyzedAt: '2026-07-29',
        summary: [
            { label: '분석 대상 훈련생', value: '7명', sub: '담당 과정 2개' },
            { label: '보완 필요', value: '5명', sub: '시급도 높음 2명' },
            { label: 'AI 질문 건수', value: '63건', sub: '최근 2주' }
        ],
        /*
         * 여러 명이 같은 곳에서 막혔다는 것은 개인 문제가 아니라 수업에서 덜 다뤄진
         * 것이다. value 는 막대 길이(가장 많은 주제를 100 기준으로 환산).
         */
        topics: [
            { label: '데이터베이스·트랜잭션', value: 100, count: 21, students: 5, correctRate: 41,
              askedBy: ['이훈련', '김서연', '박도윤', '정민준', '최지우'] },
            { label: '테스트 코드 작성', value: 76, count: 16, students: 4, correctRate: 38,
              askedBy: ['김서연', '이하은', '정민준', '한소율'] },
            { label: 'JPA 영속성 컨텍스트', value: 57, count: 12, students: 3, correctRate: 55,
              askedBy: ['이훈련', '박도윤', '최지우'] },
            { label: 'REST API 설계', value: 43, count: 9, students: 3, correctRate: 60,
              askedBy: ['이하은', '한소율', '박도윤'] },
            { label: '스프링 의존성 주입', value: 24, count: 5, students: 2, correctRate: 72,
              askedBy: ['정민준', '최지우'] }
        ],
        rows: [
            {
                id: 1,
                name: '이훈련',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'high',
                levelLabel: '높음',
                weak: ['데이터베이스·트랜잭션', 'JPA 영속성 컨텍스트'],
                evidence: 'AI 도우미에 트랜잭션 관련 질문 7회 · 3주차 자료 학습 후에도 관련 문항 4개 중 1개 정답 · 진도율 55%(반 평균 70%)',
                task: '트랜잭션 격리수준 실습 과제'
            },
            {
                id: 2,
                name: '김서연',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'high',
                levelLabel: '높음',
                weak: ['테스트 코드 작성'],
                evidence: 'AI 도우미에 테스트 관련 질문 6회 · 과제 제출물에 테스트 코드 누락 3회 · 6주차 자료 미열람',
                task: '단위 테스트 작성 과제'
            },
            {
                id: 3,
                name: '박도윤',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'mid',
                levelLabel: '보통',
                weak: ['JPA 영속성 컨텍스트'],
                evidence: 'AI 도우미 질문 5회 · 지연 로딩 관련 문항 정답률 55% · 4주차 자료는 완료',
                task: 'JPA 연관관계 매핑 과제'
            },
            {
                id: 4,
                name: '이하은',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'mid',
                levelLabel: '보통',
                weak: ['REST API 설계'],
                evidence: 'AI 도우미 질문 4회 · 상태 코드 선택 문항 오답 3회 · 진도율 65%',
                task: 'REST API 설계 보완 과제'
            },
            {
                id: 5,
                name: '정민준',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'mid',
                levelLabel: '보통',
                weak: ['데이터베이스·트랜잭션', '테스트 코드 작성'],
                evidence: 'AI 도우미 질문 5회 · 두 영역 모두 평균 이하지만 최근 2주 진도가 빠르게 올라옴',
                task: '트랜잭션 격리수준 실습 과제'
            },
            {
                id: 6,
                name: '최지우',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'low',
                levelLabel: '낮음',
                weak: ['스프링 의존성 주입'],
                evidence: 'AI 도우미 질문 2회 · 관련 문항 정답률 72% · 진도율 85%로 상위권',
                task: '의존성 주입 심화 과제'
            },
            {
                id: 7,
                name: '한소율',
                courseId: 1,
                course: '클라우드 기반 풀스택 개발자 양성과정',
                level: 'low',
                levelLabel: '낮음',
                weak: ['테스트 코드 작성'],
                evidence: 'AI 도우미 질문 2회 · 과제는 모두 제출했으나 테스트 코드가 형식적임',
                task: '단위 테스트 작성 과제'
            }
        ]
    };

    var data = window._serverDiagnosis || DUMMY;

    document.addEventListener('DOMContentLoaded', function () {
        var stamp = document.querySelector('.js-analyzed-at');
        if (stamp) stamp.textContent = '분석 기준: ' + data.analyzedAt;

        renderSummary();
        renderTopics();
        renderTopicTable();
        renderTaskGroups(data.rows);
        renderRows(data.rows);

        // 대시보드 카드에서 #secTopics / #secTasks / #secTrainees 로 넘어온 경우
        // 해당 목록으로 스크롤한다 (긴 화면이라 어디를 보라는지 알려줘야 한다)
        if (window.location.hash) {
            var target = document.querySelector(window.location.hash);
            if (target) {
                setTimeout(function () {
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    target.style.outline = '2px solid #c7d2fe';
                    target.style.outlineOffset = '3px';
                    setTimeout(function () { target.style.outline = ''; }, 2000);
                }, 120);
            }
        }

        document.getElementById('btnFilter').addEventListener('click', applyFilter);
        initDraft();

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

            // 여러 명을 한 번에 고르면 추천 과제가 서로 다를 수 있다.
            // 배정 화면은 과제 하나를 다루므로, 같은 과제인 경우에만 묶어서 넘긴다.
            var tasks = {};
            picked.forEach(function (c) { tasks[c.dataset.task] = true; });
            var kinds = Object.keys(tasks);

            if (kinds.length > 1) {
                alert('선택한 훈련생들의 추천 과제가 서로 달라요.\n\n' +
                      kinds.map(function (t) { return '· ' + t; }).join('\n') +
                      '\n\n같은 과제를 추천받은 훈련생끼리 나눠서 배부해 주세요.');
                return;
            }

            goAssign(kinds[0], picked.map(function (c) { return c.dataset.name; }),
                     picked[0].dataset.courseId);
        });
    });

    function applyFilter() {
        var course = document.getElementById('courseFilter').value;
        var level = document.getElementById('levelFilter').value;
        var filtered = data.rows.filter(function (r) {
            return (!course || r.course === course) && (!level || r.level === level);
        });
        renderRows(filtered);
        renderTaskGroups(filtered);
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

    /** 주제 목록 — 막대만으로는 "누가 무엇을 물었는지" 를 알 수 없다. */
    function renderTopicTable() {
        var body = document.getElementById('topicBody');
        if (!body) return;
        body.innerHTML = data.topics.map(function (t) {
            // 정답률이 낮을수록 시급하다 — 질문이 많아도 정답률이 높으면 관심일 뿐이다
            var cls = t.correctRate < 45 ? 'high' : (t.correctRate < 60 ? 'mid' : 'low');
            return '<tr>' +
                '<td><b>' + esc(t.label) + '</b></td>' +
                '<td>' + t.count + '건</td>' +
                '<td>' + t.students + '명</td>' +
                '<td><span class="ai-level ' + cls + '">' + t.correctRate + '%</span></td>' +
                '<td>' + (t.askedBy || []).map(function (n) {
                    return '<span class="ai-tag partial" style="margin:2px 3px 2px 0; display:inline-block;">' + esc(n) + '</span>';
                }).join('') + '</td>' +
            '</tr>';
        }).join('');
    }

    /**
     * 추천 과제별로 훈련생을 묶는다.
     * 배정 화면은 과제 하나를 다루므로, 같은 과제끼리 묶어야 한 번에 배부할 수 있다.
     */
    function renderTaskGroups(rows) {
        var body = document.getElementById('taskBody');
        if (!body) return;

        var order = { high: 3, mid: 2, low: 1 };
        var groups = {};
        rows.forEach(function (r) {
            var g = groups[r.task] || (groups[r.task] = {
                task: r.task, names: [], courseId: r.courseId, level: 'low', levelLabel: '낮음'
            });
            g.names.push(r.name);
            if (order[r.level] > order[g.level]) { g.level = r.level; g.levelLabel = r.levelLabel; }
        });

        var list = Object.keys(groups).map(function (k) { return groups[k]; })
            .sort(function (a, b) { return order[b.level] - order[a.level] || b.names.length - a.names.length; });

        if (!list.length) {
            body.innerHTML = '<tr><td colspan="5" class="ai-empty">배부할 추천 과제가 없습니다.</td></tr>';
            return;
        }

        body.innerHTML = list.map(function (g) {
            return '<tr>' +
                '<td><b>' + esc(g.task) + '</b></td>' +
                '<td>' + g.names.length + '명</td>' +
                '<td><span class="ai-level ' + g.level + '">' + esc(g.levelLabel) + '</span></td>' +
                '<td>' + g.names.map(function (n) {
                    return '<span class="ai-tag partial" style="margin:2px 3px 2px 0; display:inline-block;">' + esc(n) + '</span>';
                }).join('') + '</td>' +
                '<td><button type="button" class="btn btn-primary btn-sm js-group"' +
                    ' data-task="' + esc(g.task) + '" data-names="' + esc(g.names.join(',')) + '"' +
                    ' data-course-id="' + g.courseId + '">배부</button></td>' +
            '</tr>';
        }).join('');

        body.querySelectorAll('.js-group').forEach(function (btn) {
            btn.addEventListener('click', function () {
                goAssign(btn.dataset.task, btn.dataset.names.split(','), btn.dataset.courseId);
            });
        });
    }

    function renderRows(rows) {
        var body = document.getElementById('diagBody');
        if (!rows.length) {
            body.innerHTML = '<tr><td colspan="6" class="ai-empty">조건에 맞는 훈련생이 없습니다.</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (r) {
            return '<tr>' +
                '<td><input type="checkbox" class="js-pick" data-id="' + r.id + '" data-name="' + esc(r.name) + '"' +
                    ' data-task="' + esc(r.task) + '" data-course-id="' + r.courseId + '"' +
                    ' aria-label="' + esc(r.name) + ' 선택"></td>' +
                '<td><b>' + esc(r.name) + '</b></td>' +
                '<td><span class="ai-level ' + r.level + '">' + esc(r.levelLabel) + '</span></td>' +
                '<td>' + r.weak.map(function (w) { return '<span class="ai-tag lack" style="margin:2px 3px 2px 0; display:inline-block;">' + esc(w) + '</span>'; }).join('') + '</td>' +
                '<td style="font-size:12.5px; color:#475569; line-height:1.7;">' + esc(r.evidence) + '</td>' +
                '<td>' + esc(r.task) +
                    '<div style="margin-top:7px;"><button type="button" class="btn btn-gray btn-sm js-one"' +
                        ' data-name="' + esc(r.name) + '" data-task="' + esc(r.task) + '" data-course-id="' + r.courseId + '">이 훈련생만 배부</button></div>' +
                '</td></tr>';
        }).join('');

        body.querySelectorAll('.js-one').forEach(function (btn) {
            btn.addEventListener('click', function () {
                goAssign(btn.dataset.task, [btn.dataset.name], btn.dataset.courseId);
            });
        });
    }

    /**
     * 과제 배정 화면으로 넘긴다 — 평가 관리 > 과제 채점 > 과제 배정.
     *
     * 배정 화면이 스스로 "AI 추천으로 왔다"를 알 수 있도록 값을 쿼리로 넘긴다.
     * 그 화면의 폼 바인딩(th:field)은 건드리지 않고, 배정 화면 쪽 스크립트가
     * 이 값을 읽어 제목을 채우고 안내 배너를 띄운다.
     */
    function goAssign(task, names, courseId, desc, criteria) {
        var q = '?aiTask=' + encodeURIComponent(task) +
                '&aiTrainees=' + encodeURIComponent(names.join(',')) +
                (courseId ? '&aiCourseId=' + encodeURIComponent(courseId) : '') +
                (desc ? '&aiDesc=' + encodeURIComponent(desc) : '') +
                (criteria ? '&aiCriteria=' + encodeURIComponent(criteria) : '');
        window.location.href = '/admin/evaluation/assignments/new' + q;
    }

    /* ══════════════════════════════════════════════════════════════════
     *  AI 과제 초안 생성 — 실제 모델 호출
     * ══════════════════════════════════════════════════════════════════
     *
     * 예전에는 과제 제목이 이 파일의 DUMMY 에 하드코딩돼 있었다.
     * 취약 영역이 뭐로 나오든 같은 제목이 붙었다. 이제 서버가 모델로 만든다.
     *
     * 만든 결과를 **바로 배부하지 않는다.** 모달에 띄워 강사가 읽고 고칠 수 있게 한 뒤,
     * [이 내용으로 배부하기]를 눌러야 배정 화면으로 넘어간다.
     */
    var draftCtx = null;   // 어떤 훈련생/과정으로 만들었는지 — 배부 때 그대로 써야 한다

    function initDraft() {
        var btn = document.getElementById('btnDraft');
        if (!btn) return;

        btn.addEventListener('click', function () {
            var picked = pickedRows();
            if (!picked.length) {
                alert('과제를 만들 훈련생을 먼저 선택해 주세요.');
                return;
            }

            // 취약 영역이 서로 다르면 한 과제로 묶을 수 없다 — 가장 많이 겹치는 영역으로 만든다
            var area = topWeakArea(picked);
            draftCtx = {
                names: picked.map(function (r) { return r.name; }),
                courseId: picked[0].courseId,
                area: area
            };

            openDraft();
            requestDraft(picked[0].courseId, area, picked.length);
        });

        document.getElementById('draftClose').addEventListener('click', closeDraft);
        document.getElementById('draftCancel').addEventListener('click', closeDraft);

        document.getElementById('draftApply').addEventListener('click', function () {
            if (!draftCtx) return;
            goAssign(
                document.getElementById('draftTitleInput').value.trim(),
                draftCtx.names,
                draftCtx.courseId,
                document.getElementById('draftDescInput').value.trim(),
                document.getElementById('draftCriteriaInput').value.trim()
            );
        });
    }

    /** 체크된 행을 진단 데이터에서 되찾는다. */
    function pickedRows() {
        var ids = Array.prototype.slice
            .call(document.querySelectorAll('#diagBody .js-pick:checked'))
            // 체크박스는 value 가 아니라 data-id 를 쓴다. value 로 읽으면 전부 'on' 이 나와
            // 매칭이 하나도 안 되고 "선택한 훈련생이 없다"로 조용히 끝난다
            .map(function (c) { return String(c.dataset.id); });
        return data.rows.filter(function (r) { return ids.indexOf(String(r.id)) >= 0; });
    }

    /** 고른 훈련생들에게 가장 많이 겹치는 취약 영역. 동점이면 먼저 나온 것. */
    function topWeakArea(rows) {
        var count = {};
        rows.forEach(function (r) {
            (r.weak || []).forEach(function (w) { count[w] = (count[w] || 0) + 1; });
        });
        var best = null;
        Object.keys(count).forEach(function (k) {
            if (best === null || count[k] > count[best]) best = k;
        });
        return best || '학습 보완';
    }

    function requestDraft(courseId, weakArea, traineeCount) {
        var headers = { 'Content-Type': 'application/json' };
        var t = (document.querySelector('meta[name="_csrf"]') || {}).content;
        var h = (document.querySelector('meta[name="_csrf_header"]') || {}).content;
        if (t && h) headers[h] = t;

        fetch('/instructor/ai/assignment-draft', {
            method: 'POST', headers: headers, credentials: 'same-origin',
            body: JSON.stringify({ courseId: courseId, weakArea: weakArea, traineeCount: traineeCount })
        })
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (d) {
                if (!d.ok) { showDraftError(d.message || '과제를 만들지 못했어요.'); return; }
                showDraft(d);
            })
            .catch(function () {
                showDraftError('과제를 만들지 못했어요. 잠시 후 다시 시도해 주세요.');
            });
    }

    function openDraft() {
        document.getElementById('draftBack').hidden = false;
        document.getElementById('draftLoading').hidden = false;
        document.getElementById('draftResult').hidden = true;
        document.getElementById('draftError').hidden = true;
        document.getElementById('draftApply').disabled = true;
    }

    function closeDraft() {
        document.getElementById('draftBack').hidden = true;
    }

    function showDraft(d) {
        document.getElementById('draftLoading').hidden = true;
        document.getElementById('draftResult').hidden = false;
        document.getElementById('draftTitleInput').value = d.title || '';
        document.getElementById('draftDescInput').value = d.description || '';
        document.getElementById('draftCriteriaInput').value = d.criteria || '';
        document.getElementById('draftApply').disabled = false;
    }

    function showDraftError(msg) {
        document.getElementById('draftLoading').hidden = true;
        var box = document.getElementById('draftError');
        box.hidden = false;
        box.textContent = msg;
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
