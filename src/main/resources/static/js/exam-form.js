/*
 * 시험 만들기 / 수정하기 화면 공통 스크립트 (개발자 B — 시험 생성 슬라이스)
 *
 * 원래 admin-evaluation-test-add.html / -update.html 안에 인라인으로 들어 있던 더미 로직을
 * 서버 데이터 기반으로 바꿔 분리한 것이다. 기존 id 속성(getElementById 대상)은 그대로 쓴다.
 *
 * 서버가 넘겨주는 값 (템플릿의 th:inline 블록):
 *   window._questionPool      : 사용중(ACTIVE) 문제 목록  [{id, code, title, difficulty, categoryL/M/S, tags, score}]
 *   window._selectedQuestions : 이미 편성된 문항 (수정 화면)  [{questionId, code, title, difficulty, seq, score, fromRule}]
 *   window._savedRules        : 저장된 출제 규칙 (수정 화면)
 *
 * 폼 전송 형태:
 *   questionIds        — 수동 편성 문항 id (여러 개)
 *   rules[i].categoryL / categoryM / categoryS / tags / totalCount
 *          / difficultyLevel1~3 / difficultyCount1~3
 */
(function () {
    'use strict';

    var pool = window._questionPool || [];
    var manualQuestions = [];   // {id, code, title}
    var rules = [];             // ExamRuleForm 모양
    var PER_PAGE = 5;

    /* ===== 초기 데이터 ===== */

    (window._selectedQuestions || []).forEach(function (q) {
        // 규칙으로 확정된 문항은 수동 목록에 넣지 않는다 (별도 영역에서 보여준다).
        if (!q.fromRule) {
            manualQuestions.push({ id: String(q.questionId), code: q.code, title: q.title });
        }
    });
    (window._savedRules || []).forEach(function (r) {
        rules.push(r);
    });

    /* ===== 카테고리 셀렉트 — 문제은행 데이터에서 실제 값만 뽑는다 ===== */

    function distinct(values) {
        var seen = {};
        var result = [];
        values.forEach(function (v) {
            if (v && !seen[v]) { seen[v] = true; result.push(v); }
        });
        result.sort();
        return result;
    }

    function fillOptions(select, values, placeholder) {
        if (!select) return;
        select.innerHTML = '<option value="">' + placeholder + '</option>';
        values.forEach(function (v) {
            var opt = document.createElement('option');
            opt.value = v;
            opt.textContent = v;
            select.appendChild(opt);
        });
    }

    function bindCategoryCascade(id1, id2, id3) {
        var s1 = document.getElementById(id1);
        var s2 = document.getElementById(id2);
        var s3 = document.getElementById(id3);
        if (!s1) return;

        fillOptions(s1, distinct(pool.map(function (q) { return q.categoryL; })), '대분류 선택');
        s1.addEventListener('change', function () {
            fillOptions(s2, distinct(pool.filter(function (q) { return q.categoryL === s1.value; })
                .map(function (q) { return q.categoryM; })), '중분류 선택');
            fillOptions(s3, [], '소분류 선택');
        });
        if (s2) {
            s2.addEventListener('change', function () {
                fillOptions(s3, distinct(pool.filter(function (q) {
                    return q.categoryL === s1.value && q.categoryM === s2.value;
                }).map(function (q) { return q.categoryS; })), '소분류 선택');
            });
        }
    }

    /* ===== 문제 검색 모달 ===== */

    function filterQuestions(page) {
        page = page || 1;
        var cat1 = valueOf('category1');
        var cat2 = valueOf('category2');
        var cat3 = valueOf('category3');
        var title = valueOf('searchTitle').trim();

        var filtered = pool.filter(function (q) {
            if (cat1 && q.categoryL !== cat1) return false;
            if (cat2 && q.categoryM !== cat2) return false;
            if (cat3 && q.categoryS !== cat3) return false;
            if (title && q.title.indexOf(title) === -1) return false;
            return true;
        });

        var list = document.getElementById('questionList');
        var start = (page - 1) * PER_PAGE;
        var pageData = filtered.slice(start, start + PER_PAGE);
        list.innerHTML = '';
        if (pageData.length === 0) {
            list.innerHTML = '<div style="color:#888;font-size:14px;">검색 결과가 없습니다.</div>';
        } else {
            pageData.forEach(function (q) {
                var row = document.createElement('div');
                row.style.cssText = 'background:#f8f9fa;border-radius:6px;padding:10px 16px;margin-bottom:8px;display:flex;align-items:center;justify-content:space-between;';
                var label = document.createElement('span');
                label.style.fontSize = '15px';
                label.textContent = '[' + (q.categoryL || '-') + ' > ' + (q.categoryM || '-') + ' > ' + (q.categoryS || '-') + '] '
                    + q.title + ' / ' + q.difficulty + ' / ' + q.score + '점';
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-gray';
                btn.style.cssText = 'font-size:13px;padding:2px 10px;';
                btn.textContent = '추가';
                btn.addEventListener('click', function () { addManualQuestion(q); });
                row.appendChild(label);
                row.appendChild(btn);
                list.appendChild(row);
            });
        }

        var pagination = document.getElementById('questionPagination');
        pagination.innerHTML = '';
        var totalPages = Math.ceil(filtered.length / PER_PAGE);
        for (var i = 1; i <= totalPages; i++) {
            (function (n) {
                var b = document.createElement('button');
                b.type = 'button';
                b.className = 'btn';
                b.style.cssText = 'padding:2px 8px;' + (n === page ? 'background:#e0e0e0;' : '');
                b.textContent = n;
                b.addEventListener('click', function () { filterQuestions(n); });
                pagination.appendChild(b);
            })(i);
        }
    }

    function addManualQuestion(q) {
        var already = manualQuestions.some(function (m) { return m.id === String(q.id); });
        if (already) {
            alert('이미 추가된 문제입니다.');
            return;
        }
        manualQuestions.push({ id: String(q.id), code: q.code, title: q.title });
        renderManualQuestions();
        syncHiddenInputs();
        document.getElementById('questionModal').style.display = 'none';
    }

    function renderManualQuestions() {
        var list = document.getElementById('manualQuestionList');
        if (!list) return;
        list.innerHTML = '';
        if (manualQuestions.length === 0) {
            list.innerHTML = '<div style="color:#888;font-size:14px;">추가된 문제가 없습니다.</div>';
            return;
        }
        manualQuestions.forEach(function (q, idx) {
            var row = document.createElement('div');
            row.style.cssText = 'background:#f8f9fa;border-radius:6px;padding:10px 16px;margin-bottom:8px;display:flex;align-items:center;justify-content:space-between;';
            var label = document.createElement('span');
            label.style.fontSize = '15px';
            label.textContent = (idx + 1) + '. [' + q.code + '] ' + q.title;
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-gray';
            btn.textContent = '삭제';
            btn.addEventListener('click', function () {
                manualQuestions.splice(idx, 1);
                renderManualQuestions();
                syncHiddenInputs();
            });
            row.appendChild(label);
            row.appendChild(btn);
            list.appendChild(row);
        });
    }

    /* ===== 출제 규칙 ===== */

    function ruleDifficultyText(rule) {
        var parts = [];
        for (var i = 1; i <= 3; i++) {
            var level = rule['difficultyLevel' + i];
            var count = rule['difficultyCount' + i];
            if (level && count) parts.push(level + ' ' + count);
        }
        return parts.length ? parts.join(' / ') : '지정 없음';
    }

    function renderRules() {
        var ruleList = document.getElementById('ruleList');
        if (!ruleList) return;
        ruleList.innerHTML = '';
        if (rules.length === 0) {
            ruleList.innerHTML = '<div style="color:#888;font-size:14px;">등록된 출제 규칙이 없습니다.</div>';
            return;
        }
        rules.forEach(function (rule, idx) {
            var card = document.createElement('div');
            card.className = 'rule-card';
            card.style.cssText = 'background:#f8f9fa;border-radius:8px;padding:18px 20px 14px 20px;margin-bottom:12px;position:relative;';
            var info = document.createElement('div');
            info.style.cssText = 'font-size:15px;color:#333;line-height:1.7;';
            info.innerHTML =
                '<div><strong>대분류 &gt; 중분류 &gt; 소분류</strong></div>'
                + '<div>' + esc(rule.categoryL || '-') + ' &gt; ' + esc(rule.categoryM || '-') + ' &gt; ' + esc(rule.categoryS || '-') + '</div>'
                + '<div style="margin-top:8px;"><strong>난이도</strong></div>'
                + '<div>' + esc(ruleDifficultyText(rule)) + '</div>'
                + '<div style="margin-top:8px;"><strong>태그</strong></div>'
                + '<div>' + esc(rule.tags || '-') + '</div>'
                + '<div style="margin-top:8px;">총 ' + (rule.totalCount || 0) + '문항</div>';
            var actions = document.createElement('div');
            actions.style.cssText = 'position:absolute;right:18px;top:18px;display:flex;gap:8px;';
            var del = document.createElement('button');
            del.type = 'button';
            del.className = 'btn btn-gray';
            del.textContent = '삭제';
            del.addEventListener('click', function () {
                if (confirm('이 규칙을 삭제하시겠습니까?')) {
                    rules.splice(idx, 1);
                    renderRules();
                    syncHiddenInputs();
                }
            });
            actions.appendChild(del);
            card.appendChild(info);
            card.appendChild(actions);
            ruleList.appendChild(card);
        });
    }

    function updateRuleCount() {
        var total = 0;
        for (var i = 1; i <= 3; i++) {
            total += parseInt(valueOf('addRuleDifficultyCount' + i), 10) || 0;
        }
        var target = document.getElementById('addRuleCount');
        if (target) target.value = total;
    }

    function submitRule() {
        var rule = {
            categoryL: valueOf('addRuleSubject'),
            categoryM: valueOf('addRuleTopic'),
            categoryS: valueOf('addRuleSubtopic'),
            tags: valueOf('addRuleTags').trim(),
            totalCount: parseInt(valueOf('addRuleCount'), 10) || 0
        };
        for (var i = 1; i <= 3; i++) {
            rule['difficultyLevel' + i] = valueOf('addRuleDifficultyLevel' + i);
            rule['difficultyCount' + i] = parseInt(valueOf('addRuleDifficultyCount' + i), 10) || 0;
        }
        if (!rule.categoryL && !rule.tags) {
            alert('대분류 또는 태그 중 하나는 지정해야 합니다.');
            return;
        }
        if (rule.totalCount <= 0) {
            alert('난이도별 문항 수를 입력하세요.');
            return;
        }
        rules.push(rule);
        renderRules();
        syncHiddenInputs();
        document.getElementById('addRuleModal').style.display = 'none';
        resetRuleModal();
    }

    function resetRuleModal() {
        ['addRuleSubject', 'addRuleTopic', 'addRuleSubtopic', 'addRuleTags', 'addRuleCount'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) el.value = '';
        });
        for (var i = 1; i <= 3; i++) {
            setValue('addRuleDifficultyLevel' + i, '');
            setValue('addRuleDifficultyCount' + i, '');
        }
    }

    /* ===== 폼 전송용 hidden input ===== */

    function syncHiddenInputs() {
        var box = document.getElementById('hiddenInputs');
        if (!box) return;
        box.innerHTML = '';

        manualQuestions.forEach(function (q) {
            box.appendChild(hidden('questionIds', q.id));
        });

        rules.forEach(function (rule, i) {
            var prefix = 'rules[' + i + '].';
            box.appendChild(hidden(prefix + 'categoryL', rule.categoryL || ''));
            box.appendChild(hidden(prefix + 'categoryM', rule.categoryM || ''));
            box.appendChild(hidden(prefix + 'categoryS', rule.categoryS || ''));
            box.appendChild(hidden(prefix + 'tags', rule.tags || ''));
            box.appendChild(hidden(prefix + 'totalCount', rule.totalCount || 0));
            for (var n = 1; n <= 3; n++) {
                box.appendChild(hidden(prefix + 'difficultyLevel' + n, rule['difficultyLevel' + n] || ''));
                box.appendChild(hidden(prefix + 'difficultyCount' + n, rule['difficultyCount' + n] || 0));
            }
        });

        updateCounters();
    }

    function updateCounters() {
        var ruleTotal = rules.reduce(function (sum, r) { return sum + (parseInt(r.totalCount, 10) || 0); }, 0);
        var current = manualQuestions.length;
        var confirmedFromRule = (window._selectedQuestions || []).filter(function (q) { return q.fromRule; }).length;

        setText('currentCount', current + confirmedFromRule);
        setText('maxCount', current + ruleTotal);

        var warn = document.getElementById('countWarning');
        if (warn) {
            if (ruleTotal > 0 && confirmedFromRule === 0) {
                warn.textContent = '⚠ 출제 규칙 ' + ruleTotal + '문항이 아직 확정되지 않았습니다.';
            } else if (ruleTotal > 0 && confirmedFromRule < ruleTotal) {
                warn.textContent = '⚠ 규칙 목표 ' + ruleTotal + '문항 중 ' + confirmedFromRule + '문항만 확정됨 (문제은행 부족)';
            } else {
                warn.textContent = '';
            }
        }
    }

    /* ===== 유틸 ===== */

    function hidden(name, value) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        return input;
    }

    function valueOf(id) {
        var el = document.getElementById(id);
        return el ? el.value : '';
    }

    function setValue(id, v) {
        var el = document.getElementById(id);
        if (el) el.value = v;
    }

    function setText(id, v) {
        var el = document.getElementById(id);
        if (el) el.textContent = v;
    }

    function esc(s) {
        return String(s).replace(/[&<>"]/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
        });
    }

    function onClick(id, handler) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', handler);
    }

    /* ===== 초기화 ===== */

    document.addEventListener('DOMContentLoaded', function () {
        bindCategoryCascade('category1', 'category2', 'category3');
        bindCategoryCascade('addRuleSubject', 'addRuleTopic', 'addRuleSubtopic');

        renderManualQuestions();
        renderRules();
        syncHiddenInputs();

        // 문제 검색 모달
        onClick('addManualQuestionBtn', function () {
            document.getElementById('questionModal').style.display = 'flex';
            filterQuestions(1);
        });
        onClick('closeQuestionModal', function () {
            document.getElementById('questionModal').style.display = 'none';
        });
        var searchForm = document.getElementById('questionSearchForm');
        if (searchForm) {
            searchForm.addEventListener('submit', function (e) {
                e.preventDefault();
                filterQuestions(1);
            });
        }

        // 규칙 모달
        onClick('addRuleBtn', function () {
            document.getElementById('addRuleModal').style.display = 'flex';
        });
        onClick('closeAddRuleModal', function () {
            document.getElementById('addRuleModal').style.display = 'none';
        });
        onClick('cancelAddRule', function () {
            document.getElementById('addRuleModal').style.display = 'none';
        });
        onClick('submitAddRule', submitRule);
        for (var i = 1; i <= 3; i++) {
            var el = document.getElementById('addRuleDifficultyCount' + i);
            if (el) el.addEventListener('input', updateRuleCount);
        }

        window.addEventListener('click', function (e) {
            ['questionModal', 'addRuleModal'].forEach(function (id) {
                var modal = document.getElementById(id);
                if (modal && e.target === modal) modal.style.display = 'none';
            });
        });

        // 재응시 체크 해제 시 응시 횟수는 1회로 고정 (서버도 같은 규칙으로 보정한다)
        var retake = document.getElementById('retakeAllowed');
        var maxAttempts = document.getElementById('maxAttempts');
        if (retake && maxAttempts) {
            var syncRetake = function () {
                // disabled 로 두면 값이 전송되지 않아 서버의 @NotNull 에 걸린다 → readOnly 를 쓴다
                maxAttempts.readOnly = !retake.checked;
                if (!retake.checked) maxAttempts.value = 1;
                else if (parseInt(maxAttempts.value, 10) < 2) maxAttempts.value = 2;
            };
            retake.addEventListener('change', syncRetake);
            syncRetake();
        }

        // 감독 미사용이면 웹캠 필수도 의미가 없다
        var proctor = document.getElementById('proctorEnabled');
        var webcam = document.getElementById('requireWebcam');
        if (proctor && webcam) {
            var syncProctor = function () {
                webcam.disabled = !proctor.checked;
                if (!proctor.checked) webcam.checked = false;
            };
            proctor.addEventListener('change', syncProctor);
            syncProctor();
        }
    });
})();
