
// 강사 출결 샘플 데이터 (타임리프 연동 전 임시)
const instructorAttendanceData = [
    {
        no: 1,
        name: '김민수',
        category: 'KDT',
        subject: 'JavaScript 기초',
        course: '풀스택 웹 개발 / 1기',
        sessionCount: 15,
        avgRate: '98%',
        risk: 'low'
    },
    {
        no: 2,
        name: '이수진',
        category: 'AI',
        subject: 'AI 기초',
        course: 'AI 엔지니어링 / 2기',
        sessionCount: 12,
        avgRate: '92%',
        risk: 'medium'
    },
    {
        no: 3,
        name: '박철수',
        category: 'DB',
        subject: 'SQL 실습',
        course: '데이터베이스 / 1기',
        sessionCount: 10,
        avgRate: '85%',
        risk: 'high'
    }
];

function renderInstructorTable() {
    const tbody = document.querySelector('#instructor-table tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    instructorAttendanceData.forEach(row => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${row.no}</td>
            <td>${row.name}</td>
            <td>${row.category}</td>
            <td>${row.subject}</td>
            <td>${row.course}</td>
            <td>${row.sessionCount}</td>
            <td>${row.avgRate}</td>
            <td><span style="color:${row.risk==='low'?'#28a745':row.risk==='high'?'#dc3545':'#ffc107'};font-weight:600;">● ${row.risk==='low'?'낮음':row.risk==='high'?'높음':'중간'}</span></td>
        `;
        // tr.addEventListener('click', function() {
        //     window.location.href = 'admin-attendance-instructor-detail.html?instructor=' + encodeURIComponent(row.name);
        // });
        tbody.appendChild(tr);
    });
}

document.addEventListener('DOMContentLoaded', function() {
    renderInstructorTable();
});
        // 탭 버튼 클릭 시 영역 전환
        document.addEventListener('DOMContentLoaded', function() {
            const tabTrainee = document.getElementById('tabTrainee');
            const tabInstructor = document.getElementById('tabInstructor');
            const traineeArea = document.getElementById('traineeArea');
            const instructorArea = document.getElementById('instructorArea');
            if (tabTrainee && tabInstructor && traineeArea && instructorArea) {
                tabTrainee.addEventListener('click', function() {
                    tabTrainee.classList.add('active');
                    tabInstructor.classList.remove('active');
                    traineeArea.style.display = '';
                    instructorArea.style.display = 'none';
                });
                tabInstructor.addEventListener('click', function() {
                    tabInstructor.classList.add('active');
                    tabTrainee.classList.remove('active');
                    traineeArea.style.display = 'none';
                    instructorArea.style.display = '';
                });
            }
        });
// 빠른 선택 과목 카드 데이터 예시 (DB에서 받아온다고 가정)
const courseCardData = [
    {
        courseId: 'course1',
        category: 'KDT',
        status: '활성중',
        title: '풀스택 웹 개발 (React & Node.js)',
        instructor: '김민수 강사',
        subjectCount: 12,
        totalHours: 120,
        studentCount: 45,
        running: true,
        period: '2026.01.02 ~ 2026.06.30',
        progress: 42
    },
    {
        courseId: 'course2',
        category: 'AI',
        status: '비활성',
        title: 'AI 엔지니어링',
        instructor: '이수진 강사',
        subjectCount: 10,
        totalHours: 100,
        studentCount: 30,
        running: false,
        period: '2026.02.01 ~ 2026.07.15',
        progress: 65
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
                updateSessionSelect();
                renderNameTable();
                renderSessionTable();
                renderStatusTable();
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
// 과정별 출결정보 예시 데이터 (DB에서 15개 차시)
const courseAttendanceData = {
    course1: [
        {
            no: 1,
            name: '홍길동',
            sessions: [true,true,true,true,true,true,true,true,true,false,true,true,true,true,true,true],
            rate: '93%',
            risk: 'low'
        },
        {
            no: 2,
            name: '김민아',
            sessions: [true,true,false,true,false,true,true,true,true,true,true,false,true,true,false,true],
            rate: '80%',
            risk: 'high'
        }
    ],
    course2: [
        {
            no: 1,
            name: '이수진',
            sessions: [true,true,true,true,true,true,true,true,true,true,true,true,true,true,true],
            rate: '100%',
            risk: 'low'
        },
        {
            no: 2,
            name: '박철수',
            sessions: [true,false,true,true,true,true,true,true,true,true,true,true,true,true,true],
            rate: '93%',
            risk: 'medium'
        }
    ]
};

function updateSessionSelect() {
    const courseId = document.getElementById('courseSelect').value;
    const attendanceData = courseAttendanceData[courseId] || [];
    const sessionCount = attendanceData.length > 0 ? attendanceData[0].sessions.length : 0;
    const sessionSelect = document.getElementById('sessionSelect');
    sessionSelect.innerHTML = '';
    // 전체보기 옵션 추가
    const allOption = document.createElement('option');
    allOption.value = 'all';
    allOption.textContent = '전체보기';
    sessionSelect.appendChild(allOption);
    for (let i = 1; i <= sessionCount; i += 10) {
        const end = Math.min(i + 9, sessionCount);
        const option = document.createElement('option');
        option.value = `${i}-${end}`;
        option.textContent = `${i}~${end}차시`;
        sessionSelect.appendChild(option);
    }
}

function getSortedAttendanceData(courseId) {
    let data = (courseAttendanceData[courseId] || []).slice();
    if (sortKey === 'name') {
        data.sort((a, b) => sortAsc ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name));
    } else if (sortKey === 'rate') {
        data.sort((a, b) => {
            const aRate = parseInt(a.rate);
            const bRate = parseInt(b.rate);
            return sortAsc ? aRate - bRate : bRate - aRate;
        });
    } else if (sortKey === 'risk') {
        // 위험도: low < medium < high
        const riskOrder = { low: 0, medium: 1, high: 2 };
        data.sort((a, b) => sortAsc ? riskOrder[a.risk] - riskOrder[b.risk] : riskOrder[b.risk] - riskOrder[a.risk]);
    }
    return data;
}

function renderNameTable() {
    const courseId = document.getElementById('courseSelect').value;
    const attendanceData = getSortedAttendanceData(courseId);
    const tbody = document.querySelector('#name-table tbody');
    tbody.innerHTML = '';
    attendanceData.forEach((row, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${row.no}</td><td>${row.name}</td>`;
        tr.addEventListener('mouseenter', () => handleRowHover(idx, true));
        tr.addEventListener('mouseleave', () => handleRowHover(idx, false));
        tr.addEventListener('click', () => {
            if(document.body.getAttribute('data-user-role') === 'admin') {
                window.location.href = './attendance-detail.html';
            }
        });
        tbody.appendChild(tr);
    });
}
function renderStatusTable() {
    const courseId = document.getElementById('courseSelect').value;
    const attendanceData = getSortedAttendanceData(courseId);
    let tbody = document.querySelector('#status-table tbody');
    if (!tbody) {
        tbody = document.createElement('tbody');
        document.querySelector('#status-table').appendChild(tbody);
    }
    tbody.innerHTML = '';
    attendanceData.forEach((row, idx) => {
        const tr = document.createElement('tr');
        // 이수현황: 이수한 차시 개수/전체 차시 개수
        const completed = row.sessions.filter(s => s).length;
        const total = row.sessions.length;
        tr.innerHTML = `
            <td>${completed}/${total}</td>
            <td>${row.rate}</td>
            <td><span style="color:${row.risk==='low'?'#28a745':row.risk==='high'?'#dc3545':'#ffc107'};font-weight:600;">● ${row.risk==='low'?'낮음':row.risk==='high'?'높음':'중간'}</span></td>
        `;
        tr.addEventListener('mouseenter', () => handleRowHover(idx, true));
        tr.addEventListener('mouseleave', () => handleRowHover(idx, false));
        tr.addEventListener('click', () => {
            window.location.href = './attendance-detail.html';
        });
        tbody.appendChild(tr);
    });
}
function renderSessionTable() {
    const courseId = document.getElementById('courseSelect').value;
    const attendanceData = getSortedAttendanceData(courseId);
    const sessionValue = document.getElementById('sessionSelect').value;
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
    // 동적 thead 생성 (선택된 구간만)
    const thead = document.querySelector('#session-table thead');
    thead.innerHTML = '';
    const thRow = document.createElement('tr');
    for (let i = startIdx; i <= endIdx; i++) {
        const th = document.createElement('th');
        th.textContent = `차시${i+1}`;
        thRow.appendChild(th);
    }
    thead.appendChild(thRow);
    // 동적 tbody 생성 (선택된 구간만)
    let tbody = document.querySelector('#session-table tbody');
    if (!tbody) {
        tbody = document.createElement('tbody');
        document.querySelector('#session-table').appendChild(tbody);
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
            window.location.href = './attendance-detail.html';
        });
        tbody.appendChild(tr);
    });
    // 전체보기일 때만 min-width 동적 설정
    const sessionTable = document.getElementById('session-table');
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
// row hover 동기화 함수
function handleRowHover(idx, isHover) {
    const nameRows = document.querySelectorAll('#name-table tbody tr');
    const sessionRows = document.querySelectorAll('#session-table tbody tr');
    const statusRows = document.querySelectorAll('#status-table tbody tr');
    [nameRows[idx], sessionRows[idx], statusRows[idx]].forEach(tr => {
        if (tr) {
            if (isHover) {
                tr.classList.add('row-hover');
            } else {
                tr.classList.remove('row-hover');
            }
        }
    });
}
document.getElementById('courseSelect').addEventListener('change', () => {
    updateSessionSelect();
    renderNameTable();
    renderSessionTable();
    renderStatusTable();
    // renderAttendanceTable();
});
document.getElementById('sessionSelect').addEventListener('change', () => {
    renderSessionTable();
    renderStatusTable();
    // renderAttendanceTable();
});
document.addEventListener('DOMContentLoaded', () => {
    renderCourseCards();
    updateSessionSelect();
    renderNameTable();
    renderSessionTable();
    renderStatusTable();
});

window.addEventListener('DOMContentLoaded', function() {
    var bodyRole = document.body.getAttribute('data-user-role');
    document.querySelectorAll('[data-user-role]').forEach(function(el) {
        if (el.getAttribute('data-user-role') !== bodyRole) {
            el.style.display = 'none';
        }
    });
});

