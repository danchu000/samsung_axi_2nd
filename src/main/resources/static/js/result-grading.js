    // 시험 정보 — Thymeleaf 전환 후에는 서버가 window._serverExamInfo 로 내려준다.
    // 값이 없으면 아래 더미가 쓰이므로 아직 전환되지 않은 화면도 그대로 동작한다.
    const examInfoData = window._serverExamInfo || {
        courseName: '풀스택 웹 개발 3기',
        examName: '최종 프로젝트 이론평가',
        instructor: '홍길동',
        startDate: '2026-01-05',
        endDate: '2026-01-10',
        status: '진행중', // 진행중, 완료, 대기, 마감 등
        statusClass: 'exam-status-ing', // exam-status-ing, exam-status-done, ...
        total: 23,
        graded: 15,
        ungraded: 8
    };

    function renderExamInfoCard(data) {
        const html = `
        <div class="exam-info-card">
            <div class="exam-info-row">
                <div class="exam-info-item">
                    <div class="exam-info-label">과정명</div>
                    <div class="exam-info-value">${data.courseName}</div>
                </div>
                <div class="exam-info-item">
                    <div class="exam-info-label">시험명</div>
                    <div class="exam-info-value">${data.examName}</div>
                </div>
                <div class="exam-info-item">
                    <div class="exam-info-label">강사명</div>
                    <div class="exam-info-value">${data.instructor}</div>
                </div>
                <div class="exam-info-item">
                    <div class="exam-info-label">제출 시작일</div>
                    <div class="exam-info-value">${data.startDate}</div>
                </div>
                <div class="exam-info-item">
                    <div class="exam-info-label">제출 마감일</div>
                    <div class="exam-info-value">${data.endDate}</div>
                </div>
                <div class="exam-info-item">
                    <div class="exam-info-label">상태</div>
                    <div class="exam-info-value"><span class="exam-status ${data.statusClass}">${data.status}</span></div>
                </div>
            </div>
            <div class="exam-info-row exam-info-row-bottom">
                <div class="exam-info-item" style="flex:1;">
                    <div class="exam-info-label">제출 현황</div>
                    <div class="exam-info-value">
                        <span style="color:#1976D2;font-weight:600;">${data.total}명</span> 응시 /
                        <span style="color:#388E3C;font-weight:600;">${data.graded}명</span> 채점완료 /
                        <span style="color:#D32F2F;font-weight:600;">${data.ungraded}명</span> 미채점
                    </div>
                </div>
            </div>
        </div>
        `;
        document.getElementById('examInfoCardContainer').innerHTML = html;
    }

        // 학생 목록 — 서버 전환 시 window._serverStudentRows 를 쓴다 (없으면 아래 더미).
        const studentTableData = window._serverStudentRows || [
            { no: 1, name: '홍길동', id: 'hong123', time: '2026-01-06 10:12', score: 85, status: '채점완료', gradeBtn: '수정' },
            { no: 2, name: '김민수', id: 'kimms', time: '2026-01-06 10:20', score: 92, status: '확정', gradeBtn: '수정'},
            { no: 3, name: '이영희', id: 'leeyh', time: '2026-01-06 10:25', score: '-', status: '미채점', gradeBtn: '채점' },
            { no: 4, name: '박철수', id: 'parkcs', time: '-', score: '-', status: '미채점', gradeBtn: '채점', disabled: true },
            { no: 5, name: '최지우', id: 'choijw', time: '2026-01-06 10:40', score: 60, status: '채점중', gradeBtn: '채점' },
        ];

        function renderStudentTable(data) {
            const tbody = document.getElementById('studentTableBody');
            if (!tbody) return;
            tbody.innerHTML = data.map(item => {
                let btnText = '';
                if (item.status === '확정') {
                    btnText = '상세보기';
                } else if (item.status === '채점완료') {
                    btnText = '수정하기';
                } else if (item.status === '미채점' || item.status === '채점중') {
                    btnText = '채점하기';
                } else {
                    btnText = '상세보기';
                }
                // 채점 팝업 URL 은 서버가 행마다 내려준다(gradingUrl). 없으면 기존 정적 파일로 떨어진다.
                // 미응시자는 서버가 disabled=true 로 내려준다 (채점할 답안 자체가 없다).
                const url = item.gradingUrl || 'grading-modal-result.html';
                const btnHtml = `<button class="btn-grade" data-url="${url}"${item.disabled ? ' disabled' : ''}>${btnText}</button>`;
                return `
                    <tr data-status="${item.status}" data-score-status="${item.scoreStatus || ''}" data-name="${item.name}">
                        <td><input type="checkbox" /></td>
                        <td>${item.no}</td>
                        <td>${item.name}</td>
                        <td>${item.id}</td>
                        <td>${item.time}</td>
                        <td>${item.score}</td>
                        <td>${item.status}</td>
                        <td>${btnHtml}</td>
                    </tr>
                `;
            }).join('');
        }

        /**
         * 채점상태(미채점/채점중/채점완료/확정) · 점수상태(합격/불합격/미정) · 이름 검색 · 정렬.
         * 서버가 status/scoreStatus 를 화면 표기 그대로 내려주므로 문자열 비교만 하면 된다.
         */
        function applyStudentFilters() {
            const name = (document.getElementById('filterNameInput')?.value || '').trim().toLowerCase();
            const gradingStatus = document.getElementById('filterGradingStatus')?.value || '';
            const scoreStatus = document.getElementById('filterScoreStatus')?.value || '';
            const tbody = document.getElementById('studentTableBody');
            if (!tbody) return;

            tbody.querySelectorAll('tr').forEach(function (row) {
                let show = true;
                if (gradingStatus && row.dataset.status !== gradingStatus) show = false;
                if (scoreStatus && row.dataset.scoreStatus !== scoreStatus) show = false;
                if (name && !(row.dataset.name || '').toLowerCase().includes(name)) show = false;
                row.style.display = show ? '' : 'none';
            });
        }

        function sortStudentRows(data, key) {
            const rows = data.slice();
            if (key === 'name') {
                rows.sort((a, b) => String(a.name).localeCompare(String(b.name), 'ko'));
            } else if (key === 'score') {
                const num = (v) => (v === '-' || v == null ? -1 : Number(v));
                rows.sort((a, b) => num(b.score) - num(a.score));
            } else {
                rows.sort((a, b) => String(a.time).localeCompare(String(b.time)));
            }
            return rows;
        }

    document.addEventListener('DOMContentLoaded', function() {
        renderExamInfoCard(examInfoData);
        renderStudentTable(studentTableData);

        ['filterNameInput', 'filterGradingStatus', 'filterScoreStatus'].forEach(function (id) {
            const el = document.getElementById(id);
            if (el) el.addEventListener(id === 'filterNameInput' ? 'input' : 'change', applyStudentFilters);
        });
        const sort = document.getElementById('filterSort');
        if (sort) {
            sort.addEventListener('change', function () {
                renderStudentTable(sortStudentRows(studentTableData, this.value));
                applyStudentFilters();
            });
        }

        const tbody = document.getElementById('studentTableBody');
        if (tbody) {
            tbody.addEventListener('click', function(e) {
                if (e.target.classList.contains('btn-grade') && !e.target.disabled) {
                    const w = window.screen.availWidth;
                    const h = window.screen.availHeight;
                    const url = e.target.getAttribute('data-url') || 'grading-modal-result.html';
                    window.open(url, 'gradingModal', `width=${w},height=${h},left=0,top=0,scrollbars=yes,resizable=yes`);
                }
            });
        }
    });
