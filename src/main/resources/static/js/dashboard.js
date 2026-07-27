
// ===== Dummy data (Thymeleaf 변환 대비) =====
const dashboardData = {
userName: "김민수",
today: "2026.01.25 (월)",
kpi: {
    pendingAssignments: 3,
    pendingExams: 1,
    pendingSupport: 5,
    proctoringExams: 1
},
courses: [
    {
    id: "COURSE-2024-001",
    name: "풀스택 웹 개발 (React & Node.js)",
    status: "진행중",
    progress: "12/20",
    todaySession: "오늘 차시: 8차시 (React Hooks)",
    missingCompletion: "미이수: 3명"
    },
    {
    id: "COURSE-2024-005",
    name: "데이터 사이언스 기초",
    status: "진행중",
    progress: "5/15",
    todaySession: "다음 차시: Pandas 실습",
    missingCompletion: "미이수: 1명"
    }
],
todayEvals: [
    {
    kind: "시험",
    title: "JavaScript 중간평가",
    meta: "미채점 4명 · 종료 18:00",
    actionText: "채점하기",
    href: "/templates/instructor/result.html"
    },
    {
    kind: "과제",
    title: "React 프로젝트 1차",
    meta: "미채점 3명 · 마감 지남",
    actionText: "채점하기",
    href: "/templates/instructor/result.html"
    }
],
supports: [
    {
    category: "튜터링",
    title: "useEffect 질문",
    meta: "미응답 · 02:13",
    href: "/templates/instructor/tutoring.html"
    },
    {
    category: "QnA",
    title: "과제 제출 오류",
    meta: "진행중 · 05:42",
    href: "/templates/instructor/tutoring.html"
    },
    {
    category: "튜터링",
    title: "출결 기준 문의",
    meta: "미응답 · 01:08",
    href: "/templates/instructor/tutoring.html"
    }
],
proctors: [
    {
    title: "풀스택 웹 개발 - 중간평가",
    meta: "현재 응시자 12명 · 이상행위 1건",
    monitorHref: "/templates/instructor/proctor/exams/12",
    recordingsHref: "/templates/instructor/proctor/exams/12/recordings"
    }
],
notices: [
    { title: "평가 운영 정책 변경 안내", href: "/templates/admin/admin-07-notice/admin-notice-detail.html" },
    { title: "출결 기준 업데이트", href: "/templates/admin/admin-07-notice/admin-notice-detail.html" }
]
};

// ===== Render helpers =====
function setText(id, text) {
const el = document.getElementById(id);
if (el) el.textContent = text;
}

function renderCourseList() {
const wrap = document.getElementById("courseList");
wrap.innerHTML = "";

dashboardData.courses.forEach(c => {
    const card = document.createElement("div");
    card.className = "course-card";
    card.innerHTML = `
    <div class="course-top">
        <div class="course-name" title="${c.name}">${c.name}</div>
        <div class="course-badge">${c.status} (${c.progress})</div>
    </div>
    <div class="course-info">
        <div>${c.todaySession}</div>
        <div>${c.missingCompletion}</div>
    </div>
    <div class="course-actions">
        <button class="btn btn-secondary" type="button" onclick="location.href='/admin/admin-05-attendance/admin-attendance'">출결</button>
        <button class="btn btn-secondary" type="button" onclick="location.href='/admin/admin-05-attendance/admin-attendance-graduate'">이수</button>
        <button class="btn btn-gray" type="button" onclick="location.href='/admin/admin-03-courses/admin-courses-edu'">과정</button>
    </div>
    `;
    wrap.appendChild(card);
});
}

function renderTodayEvalList() {
const wrap = document.getElementById("todayEvalList");
wrap.innerHTML = "";

dashboardData.todayEvals.forEach(e => {
    const item = document.createElement("div");
    item.className = "list-item";
    item.innerHTML = `
    <div class="item-left">
        <div class="item-title">[${e.kind}] ${e.title}</div>
        <div class="item-meta"><span>${e.meta}</span></div>
    </div>
    <div class="item-actions">
        <button class="btn btn-primary" type="button" onclick="location.href='${e.href}'">${e.actionText}</button>
    </div>
    `;
    wrap.appendChild(item);
});
}

function renderSupportList() {
const wrap = document.getElementById("supportList");
wrap.innerHTML = "";

dashboardData.supports.forEach(s => {
    const item = document.createElement("div");
    item.className = "list-item";
    item.innerHTML = `
    <div class="item-left">
        <div class="item-title">[${s.category}] ${s.title}</div>
        <div class="item-meta"><span>${s.meta}</span></div>
    </div>
    <div class="item-actions">
        <button class="btn btn-primary" type="button" onclick="location.href='${s.href}'">응답하기</button>
    </div>
    `;
    wrap.appendChild(item);
});
}

function renderProctorList() {
const wrap = document.getElementById("proctorList");
wrap.innerHTML = "";

dashboardData.proctors.forEach(p => {
    const item = document.createElement("div");
    item.className = "list-item";
    item.innerHTML = `
    <div class="item-left">
        <div class="item-title">${p.title}</div>
        <div class="item-meta"><span>${p.meta}</span></div>
    </div>
    <div class="item-actions">
        <button class="btn btn-primary" type="button" onclick="location.href='${p.monitorHref}'">실시간</button>
        <button class="btn btn-secondary" type="button" onclick="location.href='${p.recordingsHref}'">녹화</button>
    </div>
    `;
    wrap.appendChild(item);
});
}

function renderNoticeList() {
const wrap = document.getElementById("noticeList");
wrap.innerHTML = "";
dashboardData.notices.forEach(n => {
    const item = document.createElement("div");
    item.className = "note-item";
    item.innerHTML = `
    <div class="note-title" title="${n.title}">${n.title}</div>
    <button class="btn btn-gray" type="button" onclick="location.href='${n.href}'">보기</button>
    `;
    wrap.appendChild(item);
});
}

// ===== Init =====
document.addEventListener("DOMContentLoaded", () => {
setText("helloTitle", `👋 ${dashboardData.userName} 강사님, 좋은 하루입니다`);
setText("todayText", `${dashboardData.today}`);

setText("kpiAssignment", `${dashboardData.kpi.pendingAssignments}건`);
setText("kpiExam", `${dashboardData.kpi.pendingExams}건`);
setText("kpiSupport", `${dashboardData.kpi.pendingSupport}건`);
setText("kpiProctor", `${dashboardData.kpi.proctoringExams}건`);

renderCourseList();
renderTodayEvalList();
renderSupportList();
renderProctorList();
renderNoticeList();
});
