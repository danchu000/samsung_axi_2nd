
        // 빠른 선택 과목 카드 데이터 예시 (DB에서 받아온다고 가정)
        const completionList = [
            {
                checked: false,
                no: 1,
                traineeName: "김민수",
                birthDate: "1996-03",
                courseName: "Java 웹 개발자 과정",
                courseBatch: "3기",
                attendanceRate: 92,
                attendanceStatus: "PASS", // PASS | WARNING | FAIL
                completionStatus: "CONFIRMED", // EXPECTED | PENDING | CONFIRMED
                finalDecision: "PASSED", // PASSED | FAILED | NONE
                confirmedDate: "2025-12-28"
            },
            {
                checked: false,
                no: 2,
                traineeName: "이지은",
                birthDate: "1994-11",
                courseName: "풀스택 웹 개발 (React & Node.js)",
                courseBatch: "3기",
                attendanceRate: 78,
                attendanceStatus: "WARNING",
                completionStatus: "CONFIRMED",
                finalDecision: "FAILED",
                confirmedDate: "2025-12-28"
            },
            {
                checked: false,
                no: 3,
                traineeName: "박준호",
                birthDate: "1998-07",
                courseName: "풀스택 웹 개발 (React & Node.js)",
                courseBatch: "1기",
                attendanceRate: 100,
                attendanceStatus: "PASS",
                completionStatus: "PENDING",
                finalDecision: "NONE",
                confirmedDate: null
            },
            {
                checked: false,
                no: 4,
                traineeName: "최유나",
                birthDate: "1995-01",
                courseName: "풀스택 웹 개발 (React & Node.js)",
                courseBatch: "2기",
                attendanceRate: 85,
                attendanceStatus: "PASS",
                completionStatus: "CONFIRMED",
                finalDecision: "PASSED",
                confirmedDate: "2025-11-15"
            },
            {
                checked: false,
                no: 5,
                traineeName: "정현우",
                birthDate: "1993-09",
                courseName: "AI 엔지니어링",
                courseBatch: "2기",
                attendanceRate: 60,
                attendanceStatus: "FAIL",
                completionStatus: "CONFIRMED",
                finalDecision: "FAILED",
                confirmedDate: "2025-11-15"
            },
            {
                checked: false,
                no: 6,
                traineeName: "한소영",
                birthDate: "1997-05",
                courseName: "AI 엔지니어링",
                courseBatch: "1기",
                attendanceRate: 88,
                attendanceStatus: "PASS",
                completionStatus: "EXPECTED",
                finalDecision: "NONE",
                confirmedDate: null
            }
        ];

        const completionStatusText = {
            EXPECTED: "이수예정",
            PENDING: "판정대기",
            CONFIRMED: "확정"
        };

            const finalDecisionText = {
            PASSED: "이수",
            FAILED: "미이수",
            NONE: "-"
        };

            const attendanceIcon = {
            PASS: "✅",
            WARNING: "⚠️",
            FAIL: "❌"
        };
        const courseCardData = [
            {
                courseId: 'course1',
                category: '개발',
                status: '운영중',
                title: '풀스택 웹 개발 (React & Node.js)',
                instructor: '홍길동',
                subjectCount: 12,
                totalHours: 96,
                studentCount: 45,
                progress: 75,
                period: '2024-01-10 ~ 2024-06-30',
                running: true
            },
            {
                courseId: 'course2',
                category: '인공지능',
                status: '종료',
                title: 'AI 엔지니어링',
                instructor: '김철수',
                subjectCount: 10,
                totalHours: 80,
                studentCount: 30,
                progress: 100,
                period: '2023-05-01 ~ 2023-10-31',
                running: false
            }
        ];
        function renderCourseCards() {
            const list = document.getElementById('quickCourseList');
            list.innerHTML = '';
            courseCardData.forEach(card => {
                const div = document.createElement('div');
                div.className = 'quick-course-card';
                div.innerHTML = `
                    <div class="course-top-section">
                        <span class="course-category badge-kdt">${card.category}</span>
                        <div class="toggle-status">
                            <span>${card.status}</span>
                        </div>
                    </div>
                    <div class="course-header">
                        <h3 class="course-title">${card.title}</h3>
                        <span class="instructor-name">${card.instructor}</span>
                    </div>
                    <div class="course-stats">
                        <div class="stat-box"><span class="stat-text">과목 <strong>${card.subjectCount}</strong>개</span></div>
                        <div class="divider-vertical"></div>
                        <div class="stat-box"><span class="stat-text">총 <strong>${card.totalHours}</strong>시간</span></div>
                        <div class="divider-vertical"></div>
                        <div class="stat-box"><span class="stat-text">수강생 <strong>${card.studentCount}</strong>명</span></div>
                    </div>
                    <div class="course-progress-info">
                        <div class="progress-left">
                            <span class="status-badge ${card.running ? 'status-completed' : 'status-disabled'}">${card.running ? '운영중' : '종료'}</span>
                            <span class="progress-period">📅 ${card.period}</span>
                        </div>
                        <span class="progress-percentage">${card.progress}%</span>
                    </div>
                    <div class="progress-bar-container">
                        <div class="progress-bar" style="width: ${card.progress}%;"></div>
                    </div>
                `;
                // 카드 클릭 시 과정 선택 select 변경
                div.addEventListener('click', () => {
                    const courseSelect = document.getElementById('courseSelect');
                    if (courseSelect) {
                        courseSelect.value = card.courseId;
                        // 트리거: 과정 변경 시 필요한 함수 호출
                            renderGraduateTable();
                    }
                });
                list.appendChild(div);
            });
        }
        // 정렬 상태 변수
        let sortKey = 'name';
        let sortAsc = true;

        // 정렬 기준/순서 UI 이벤트
        document.addEventListener('DOMContentLoaded', () => {
            const sortSelect = document.getElementById('sortSelect');
            const sortOrderBtn = document.getElementById('sortOrderBtn');
            const sortOrderIcon = document.getElementById('sortOrderIcon');
            if (sortSelect && sortOrderBtn && sortOrderIcon) {
                sortSelect.addEventListener('change', () => {
                    sortKey = sortSelect.value;
                    renderNameTable();
                    renderSessionTable();
                    renderStatusTable();
                });
                sortOrderBtn.addEventListener('click', () => {
                    sortAsc = !sortAsc;
                    sortOrderIcon.textContent = sortAsc ? '▲' : '▼';
                    renderNameTable();
                    renderSessionTable();
                    renderStatusTable();
                });
            }
        });

        // data-user-role="admin"이면서 button 또는 td, th만 숨김 처리 (body 등 전체 구조는 숨기지 않음)
        document.addEventListener('DOMContentLoaded', function() {
            document.querySelectorAll('[data-user-role="admin"]').forEach(function(el) {
                const tag = el.tagName.toLowerCase();
                if (tag === 'button' || tag === 'td' || tag === 'th') {
                    if (document.body.getAttribute('data-user-role') !== 'admin') {
                        el.style.display = 'none';
                    }
                }
            });
        });
        // 빠른 상태변경 모달 열기/닫기 함수
        function openStatusChangeModal() {
            const modal = document.getElementById('statusChangeModal');
            if (modal) {
                modal.classList.add('show');
            }
        }

        function closeStatusChangeModal() {
            const modal = document.getElementById('statusChangeModal');
            if (modal) {
                modal.classList.remove('show');
            }
        }

        // 출결 데이터 정렬 함수
        function getSortedAttendanceData(courseId) {
            if (typeof courseAttendanceData === 'undefined') return [];
            let data = (courseAttendanceData[courseId] || []).slice();
            if (sortKey === 'name') {
                data.sort((a, b) => sortAsc ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name));
            } else if (sortKey === 'rate') {
                data.sort((a, b) => {
                    const aRate = parseInt(a.rate);
                    const bRate = parseInt(b.rate);
                    return sortAsc ? aRate - bRate : bRate - aRate;
                });
            } else if (sortKey === 'confirmedDate') {
                data.sort((a, b) => {
                    // null/undefined/empty string goes last
                    if (!a.confirmedDate && !b.confirmedDate) return 0;
                    if (!a.confirmedDate) return 1;
                    if (!b.confirmedDate) return -1;
                    // Compare as date string (YYYY-MM-DD)
                    if (sortAsc) {
                        return a.confirmedDate.localeCompare(b.confirmedDate);
                    } else {
                        return b.confirmedDate.localeCompare(a.confirmedDate);
                    }
                });
            }
            return data;
        }

        function renderSessionTable() {
            const courseId = document.getElementById('courseSelect').value;
            const attendanceData = getSortedAttendanceData(courseId);
            // const sessionTable = document.querySelector('#session-table');
            // if (!sessionTable) return;
            // const sessionValue = document.getElementById('sessionSelect') ? document.getElementById('sessionSelect').value : 'all';
            const sessionValue = 'all';
            let startIdx = 0, endIdx = 0, isAll = false;
            let sessionCount = attendanceData.length > 0 ? attendanceData[0].sessions.length : 0;
            if (sessionValue === 'all') {
                startIdx = 0;
                endIdx = sessionCount - 1;
                isAll = true;
            } else {
                const sessionRange = sessionValue.split('-').map(Number);
                startIdx = sessionRange[0] - 1;
                endIdx = sessionRange[1] - 1;
            }
            const thead = sessionTable.querySelector('thead');
            thead.innerHTML = '';
            const thRow = document.createElement('tr');
            for (let i = startIdx; i <= endIdx; i++) {
                const th = document.createElement('th');
                th.textContent = `차시${i+1}`;
                thRow.appendChild(th);
            }
            thead.appendChild(thRow);
            let tbody = sessionTable.querySelector('tbody');
            if (!tbody) {
                tbody = document.createElement('tbody');
                sessionTable.appendChild(tbody);
            }
            tbody.innerHTML = '';
            attendanceData.forEach((row, idx) => {
                const tr = document.createElement('tr');
                row.sessions.slice(startIdx, endIdx+1).forEach(s => {
                    const td = document.createElement('td');
                    td.textContent = s ? 'O' : 'X';
                    tr.appendChild(td);
                });
                tr.addEventListener('mouseenter', () => handleRowHover(idx, true));
                tr.addEventListener('mouseleave', () => handleRowHover(idx, false));
                tr.addEventListener('click', () => {
                    window.location.href = 'admin-attendance-detail.html';
                });
                tbody.appendChild(tr);
            });
            // const sessionTable = document.getElementById('session-table');
            const wrapper = document.querySelector('.session-table-wrapper');
            if (isAll) {
                const minWidth = (sessionCount) * 100;
                sessionTable.style.minWidth = minWidth + 'px';
                wrapper.style.overflowX = 'auto';
            } else {
                sessionTable.style.minWidth = '';
                wrapper.style.overflowX = '';
            }
        }

        // 훈련생 상세정보 모달 닫기 함수 (전역)    
        function closeTraineeDetailModal() {
            const modal = document.getElementById('traineeDetailModal');
            if (modal) {
                modal.style.display = 'none';
            }
        }

        function renderGraduateTable() {
            const tbody = document.querySelector('#graduate_table tbody');
            if (!tbody) return;
            tbody.innerHTML = '';
            // 정렬 기준 가져오기
            const sortSelect = document.getElementById('sortSelect');
            const sortKey = sortSelect ? sortSelect.value : 'name';
            let sortAsc = true;
            const sortOrderIcon = document.getElementById('sortOrderIcon');
            if (sortOrderIcon && sortOrderIcon.textContent === '▼') sortAsc = false;

            // 과정 필터링
            const courseSelect = document.getElementById('courseSelect');
            const selectedCourse = courseSelect ? courseSelect.value : 'view-all';
            let filtered = completionList.filter(item => {
                if (selectedCourse === 'view-all') return true;
                if (selectedCourse === 'course1') return item.courseName === '풀스택 웹 개발 (React & Node.js)';
                if (selectedCourse === 'course2') return item.courseName === 'AI 엔지니어링';
                return false;
            });

            // 정렬 함수
            let sorted = filtered.slice();
            if (sortKey === 'name') {
                sorted.sort((a, b) => sortAsc ? a.traineeName.localeCompare(b.traineeName) : b.traineeName.localeCompare(a.traineeName));
            } else if (sortKey === 'rate') {
                sorted.sort((a, b) => sortAsc ? a.attendanceRate - b.attendanceRate : b.attendanceRate - a.attendanceRate);
            } else if (sortKey === 'confirmedDate') {
                sorted.sort((a, b) => {
                    if (!a.confirmedDate && !b.confirmedDate) return 0;
                    if (!a.confirmedDate) return 1;
                    if (!b.confirmedDate) return -1;
                    return sortAsc ? a.confirmedDate.localeCompare(b.confirmedDate) : b.confirmedDate.localeCompare(a.confirmedDate);
                });
            } else if (sortKey === 'finalDecision') {
                // PASSED > FAILED > NONE
                const order = { PASSED: 0, FAILED: 1, NONE: 2 };
                sorted.sort((a, b) => sortAsc ? order[a.finalDecision || 'NONE'] - order[b.finalDecision || 'NONE'] : order[b.finalDecision || 'NONE'] - order[a.finalDecision || 'NONE']);
            } else if (sortKey === 'completionStatus') {
                // CONFIRMED > PENDING > EXPECTED
                const order = { CONFIRMED: 0, PENDING: 1, EXPECTED: 2 };
                sorted.sort((a, b) => sortAsc ? order[a.completionStatus || 'EXPECTED'] - order[b.completionStatus || 'EXPECTED'] : order[b.completionStatus || 'EXPECTED'] - order[a.completionStatus || 'EXPECTED']);
            }

            sorted.forEach((item, idx) => {
                const tr = document.createElement('tr');
                const decision = item.finalDecision || 'NONE';
                const decisionClass = decision === 'PASSED' ? 'final-passed' : decision === 'FAILED' ? 'final-failed' : '';
                const decisionText = finalDecisionText[decision] || '-';
                tr.innerHTML = `
                    <td><input type="checkbox" class="graduate-checkbox" data-index="${idx}"></td>
                    <td>${item.no}</td>
                    <td>${item.traineeName}</td>
                    <td>${item.birthDate || '-'}</td>
                    <td>${item.courseName}${item.courseBatch ? ' (' + item.courseBatch + ')' : ''}</td>
                    <td>${item.attendanceRate}%</td>
                    <td>${attendanceIcon[item.attendanceStatus] || '-'}</td>
                    <td>${completionStatusText[item.completionStatus] || '-'}</td>
                    <td><span class="${decisionClass}">${decisionText}</span></td>
                    <td>${item.confirmedDate || '-'}</td>
                    <td data-user-role="admin"><button class="btn btn-gray btn-trainee-detail" data-idx="${idx}">수정하기</button></td>
                `;
                tbody.appendChild(tr);
            });

            // 상세보기 버튼 이벤트 연결
            tbody.querySelectorAll('.btn-trainee-detail').forEach(btn => {
                btn.addEventListener('click', function() {
                    const idx = this.getAttribute('data-idx');
                    showTraineeDetailModal(sorted[idx]);
                });
            });
                // 훈련생 상세정보 모달 표시 함수
            function showTraineeDetailModal(data) {
                const modal = document.getElementById('traineeDetailModal');
                const body = document.getElementById('traineeDetailBody');
                // 예시 변경로그 데이터 (실제론 서버에서 받아옴)
                const changeLog = data.changeLog || [
                        {date:'2025-12-01 10:22', user:'관리자A', reason:'출결률 수정'},
                        {date:'2025-12-15 14:10', user:'관리자B', reason:'이수상태 수동변경'}
                ];
                body.innerHTML = `
                        <table class="history-table" style="margin-bottom:18px;">
                            <tbody>
                                <tr><th>훈련생명</th><td>${data.traineeName}</td></tr>
                                <tr><th>생년월일</th><td>${data.birthDate || '-'}</td></tr>
                                <tr><th>과정명/기수</th><td>${data.courseName}${data.courseBatch ? ' ('+data.courseBatch+')' : ''}</td></tr>
                                <tr><th>출결률(%)</th><td>${data.attendanceRate}%</td></tr>
                                <tr><th>이수상태</th><td>${attendanceIcon[data.attendanceStatus] || '-'}</td></tr>
                                    <tr><th>이수확정상태</th><td>
                                        <select id="manualCompletionStatusSelect">
                                            <option value="EXPECTED" ${data.completionStatus==='EXPECTED'?'selected':''}>이수예정</option>
                                            <option value="PENDING" ${data.completionStatus==='PENDING'?'selected':''}>판정대기</option>
                                            <option value="CONFIRMED" ${data.completionStatus==='CONFIRMED'?'selected':''}>확정</option>
                                        </select>
                                    </td></tr>
                                <tr><th>최종판정</th><td>
                                    <select id="manualFinalDecisionSelect">
                                        <option value="NONE" ${data.finalDecision==='NONE'?'selected':''}>-</option>
                                        <option value="PASSED" ${data.finalDecision==='PASSED'?'selected':''}>이수</option>
                                        <option value="FAILED" ${data.finalDecision==='FAILED'?'selected':''}>미이수</option>
                                    </select>
                                </td></tr>
                                <tr><th>확정일자</th><td>${data.confirmedDate || '-'}</td></tr>
                                <tr><th>변경사유</th><td><input type="text" id="manualRemarkInput" value="${data.remark || ''}" style="width:100%; box-sizing:border-box; padding:4px 6px; border:1px solid #ccc; border-radius:4px;"></td></tr>
                            </tbody>
                        </table>
                        <div style="margin-bottom:8px; font-weight:600;">변경로그</div>
                        <table class="history-table">
                            <thead><tr><th>변경일시</th><th>변경인</th><th>변경사유</th></tr></thead>
                            <tbody>
                                ${changeLog.map(log=>`<tr><td>${log.date}</td><td>${log.user}</td><td>${log.reason}</td></tr>`).join('')}
                            </tbody>
                        </table>
                `;
                modal.style.display = 'flex';
                // 바깥 클릭 시 닫기
                modal.onclick = e => { if(e.target===modal) modal.style.display='none'; };
            }

        

            // graduate-checkbox 체크 여부에 따라 #statusChangeBtn(일괄 상태변경) 활성/비활성
            function updateStatusChangeBtn() {
                const checkboxes = tbody.querySelectorAll('.graduate-checkbox');
                const anyChecked = Array.from(checkboxes).some(cb => cb.checked);
                const statusChangeBtn = document.getElementById('statusChangeBtn');
                if (statusChangeBtn) {
                    statusChangeBtn.disabled = !anyChecked;
                    if (anyChecked) {
                        statusChangeBtn.classList.remove('btn-gray-line');
                        statusChangeBtn.classList.add('btn-gray');
                    } else {
                        statusChangeBtn.classList.remove('btn-gray');
                        statusChangeBtn.classList.add('btn-gray-line');
                    }
                }
            }
            // 체크박스 이벤트 연결
            tbody.querySelectorAll('.graduate-checkbox').forEach(cb => {
                cb.addEventListener('change', updateStatusChangeBtn);
            });
            // 전체선택 버튼 클릭 시 체크박스 전체 토글 및 상태버튼 갱신
            const selectAllBtn = document.getElementById('selectAllBtn');
            if (selectAllBtn) {
                selectAllBtn.onclick = () => {
                    const checkboxes = document.querySelectorAll('.graduate-checkbox');
                    const allChecked = Array.from(checkboxes).every(cb => cb.checked);
                    checkboxes.forEach(cb => cb.checked = !allChecked);
                    updateStatusChangeBtn();
                };
            }
            // 최초 렌더링 시 버튼 비활성화
            updateStatusChangeBtn();
        }
        document.getElementById('courseSelect').addEventListener('change', () => {
            renderGraduateTable();
        });

        document.addEventListener('DOMContentLoaded', () => {
            renderCourseCards();
            renderGraduateTable();
            const sortSelect = document.getElementById('sortSelect');
            const sortOrderBtn = document.getElementById('sortOrderBtn');
            if (sortSelect) {
                sortSelect.addEventListener('change', () => {
                    renderGraduateTable();
                });
            }
            if (sortOrderBtn) {
                sortOrderBtn.addEventListener('click', () => {
                    renderGraduateTable();
                });
            }
        });

