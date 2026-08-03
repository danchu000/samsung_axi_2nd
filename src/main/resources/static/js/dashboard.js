
// ===== 서버 데이터 (com.ssa.lms.dashboard.service.InstructorDashboardService) =====
// templates/instructor/index.html 의 인라인 <script th:inline="javascript"> 가
// window._serverInstructorDashboard 를 먼저 채운다 (이 파일은 defer 라 그 뒤에 실행된다).
//
// 폴백은 "빈 대시보드"다. 예전 더미 숫자를 남겨두면 서버 조회가 실패했을 때
// 가짜 담당 과정/채점 건수가 사실처럼 보이므로, 값이 없으면 없다고 보여준다.
const EMPTY_DASHBOARD = {
userName: "-",
today: "-",
kpi: {
    pendingAssignments: "-",
    pendingExams: "-",
    pendingSupport: "-",
    proctoringExams: "-"
},
courses: [],
todayEvals: [],
supports: [],
proctors: [],
notices: []
};

const dashboardData = window._serverInstructorDashboard || EMPTY_DASHBOARD;

// ===== Render helpers =====
function setText(id, text) {
const el = document.getElementById(id);
if (el) el.textContent = text;
}

// 목록이 0건이면(신규 강사 계정 등) 빈 패널 대신 안내 문구를 남긴다.
// 그냥 비워두면 렌더 실패와 구분이 안 된다.
function renderEmpty(wrap, message) {
wrap.innerHTML = `<div class="list-item"><div class="item-left"><div class="item-meta">${message}</div></div></div>`;
}

function renderCourseList() {
const wrap = document.getElementById("courseList");
if (!wrap) return;
wrap.innerHTML = "";
if (!dashboardData.courses.length) { renderEmpty(wrap, "담당하는 과정이 없습니다."); return; }

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
        <button class="btn btn-secondary" type="button" onclick="location.href='${c.attendanceHref}'">출결</button>
        <button class="btn btn-secondary" type="button" onclick="location.href='${c.completionHref}'">이수</button>
        <button class="btn btn-gray" type="button" onclick="location.href='${c.detailHref}'">과정</button>
    </div>
    `;
    wrap.appendChild(card);
});
}

function renderTodayEvalList() {
const wrap = document.getElementById("todayEvalList");
if (!wrap) return;
wrap.innerHTML = "";
if (!dashboardData.todayEvals.length) { renderEmpty(wrap, "채점 대기 중인 시험/과제가 없습니다."); return; }

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
if (!wrap) return;
wrap.innerHTML = "";
if (!dashboardData.supports.length) { renderEmpty(wrap, "응답이 필요한 문의가 없습니다."); return; }

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
if (!wrap) return;
wrap.innerHTML = "";
if (!dashboardData.proctors.length) { renderEmpty(wrap, "현재 응시가 진행 중인 시험이 없습니다."); return; }

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
if (!wrap) return;
wrap.innerHTML = "";
if (!dashboardData.notices.length) {
    wrap.innerHTML = `<div class="note-item"><div class="note-title">등록된 공지가 없습니다.</div></div>`;
    return;
}
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
