// ===== 권한별 버튼 숨김 처리 (선택 삭제 버튼) =====
document.addEventListener('DOMContentLoaded', function() {
  var bodyRoleRaw = (document.body.dataset.userRole || '').trim();
  var btn = document.getElementById('bulkDeleteBtn');
  if (btn) {
    var elRoles = (btn.dataset.userRole || '').split(',').map(function(r) { return r.trim(); }).filter(Boolean);
    if (!elRoles.includes(bodyRoleRaw)) {
      btn.style.display = 'none';
    }
  }
});
/**
 * 공통 평가 목록(시험/과제) 렌더/필터/전체선택 스크립트
 * - RESULT(grading) / ASSIGNMENTS(assignment) 모두 동일하게 사용 가능
 *
 * ✅ HTML에서 필요한 기본 요소(없으면 자동으로 스킵함)
 * - 목록 컨테이너:  #gradingListItems 또는 #assignmentListItems
 * - 필터(있으면 적용): #courseFilter #evalTypeFilter #instructorFilter #statusFilter #searchInput
 * - 전체선택 체크박스(옵션): .grading-header input[type="checkbox"] 또는 #selectAllAssignments
 *
 * ✅ 상태 클래스는 현재 너가 쓰는 기준으로 통일
 * - completed => status-disabled
 * - waiting   => status-in-progress
 * - pending   => status-pending
 *
 * ⚠️ 참고: 너 CSS가 status-completed 등으로 되어 있으면,
 *         아래 STATUS_CLASS를 CSS에 맞춰 바꾸거나 CSS를 스크립트에 맞춰야 함.
 */
// RESULT 데이터는 evalType을 꼭 넣어줘야 evalTypeFilter가 정상 동작함
const gradingData = [
  {
    number: 1,
    courseName: "풀스택 웹 개발 (React & Node.js)",
    courseId: "COURSE-2024-001",
    title: "JavaScript 기초 중간 평가",
    instructor: "김민수",
    evalType: "test",
    startDate: "2026.01.13",
    startTime: "00:00부터",
    endDate: "2026.01.20",
    endTime: "23:59까지",
    submitted: 18,
    notSubmitted: 9,
    status: "completed",
  },
  {
    number: 2,
    courseName: "AI 엔지니어링 입문",
    courseId: "COURSE-2024-002",
    title: "딥러닝 기초 평가",
    instructor: "이영희",
    evalType: "test",
    startDate: "2026.01.15",
    startTime: "09:00부터",
    endDate: "2026.01.22",
    endTime: "18:00까지",
    submitted: 12,
    notSubmitted: 5,
    status: "pending",
  },
  {
    number: 3,
    courseName: "데이터 분석 실무",
    courseId: "COURSE-2024-003",
    title: "파이썬 데이터 분석 시험",
    instructor: "박철수",
    evalType: "test",
    startDate: "2026.01.18",
    startTime: "10:00부터",
    endDate: "2026.01.25",
    endTime: "17:00까지",
    submitted: 0,
    notSubmitted: 20,
    status: "waiting",
  },
  {
    number: 4,
    courseName: "웹 디자인 기초",
    courseId: "COURSE-2024-004",
    title: "HTML/CSS 중간 평가",
    instructor: "최지우",
    evalType: "test",
    startDate: "2026.01.10",
    startTime: "13:00부터",
    endDate: "2026.01.17",
    endTime: "15:00까지",
    submitted: 15,
    notSubmitted: 2,
    status: "completed",
  },
];

document.addEventListener("DOMContentLoaded", () => {
  initEvaluationList({
    containerId: "gradingListItems",
    data: gradingData,
    routes: {
      completed: "result-grading.html",
      pending: "result-grading.html",
      waiting: "result-grading.html",
    },
    itemClass: "grading-item",
    checkboxClass: "grading-checkbox",
    // RESULT는 보통 헤더 체크박스가 이렇게 생김
    selectAllSelector: ".grading-header input[type='checkbox']",
  });
});
// assignment 데이터
const assignmentData = [
  {
    number: 1,
    courseName: "풀스택 웹 개발 (React & Node.js)",
    courseId: "COURSE-2024-001",
    title: "React 프로젝트 개발 과제",
    instructor: "김민수",
    evalType: "assignment",
    startDate: "2026.01.13",
    startTime: "00:00부터",
    endDate: "2026.01.20",
    endTime: "23:59까지",
    submitted: 18,
    notSubmitted: 9,
    status: "completed",
  },
  {
    number: 2,
    courseName: "AI 엔지니어링 입문",
    courseId: "COURSE-2024-002",
    title: "머신러닝 실습 과제",
    instructor: "이영희",
    evalType: "assignment",
    startDate: "2026.01.15",
    startTime: "09:00부터",
    endDate: "2026.01.22",
    endTime: "18:00까지",
    submitted: 10,
    notSubmitted: 7,
    status: "pending",
  },
  {
    number: 3,
    courseName: "데이터 분석 실무",
    courseId: "COURSE-2024-003",
    title: "파이썬 데이터 분석 과제",
    instructor: "박철수",
    evalType: "assignment",
    startDate: "2026.01.18",
    startTime: "10:00부터",
    endDate: "2026.01.25",
    endTime: "17:00까지",
    submitted: 0,
    notSubmitted: 20,
    status: "waiting",
  },
  {
    number: 4,
    courseName: "웹 디자인 기초",
    courseId: "COURSE-2024-004",
    title: "HTML/CSS 실습 과제",
    instructor: "최지우",
    evalType: "assignment",
    startDate: "2026.01.10",
    startTime: "13:00부터",
    endDate: "2026.01.17",
    endTime: "15:00까지",
    submitted: 13,
    notSubmitted: 4,
    status: "completed",
  },
];

// 과제 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", () => {
  initEvaluationList({
    containerId: "assignmentListItems",
    data: assignmentData,
    routes: {
      completed: "assignment-grading.html",
      pending: "assignment-grading.html",
      waiting: "assignment-grading.html",
    },
    itemClass: "assignment-item",
    checkboxClass: "assignment-checkbox",
    // ASSIGNMENTS는 너가 id로 쓰는 경우가 많아서 이렇게
    selectAllSelector: "#selectAllAssignments",
  });
});

const STATUS_CLASS = {
  completed: "status-disabled",
  waiting: "status-in-progress",
  pending: "status-pending",
  "not-submitted": "status-not-submitted", // 필요 시 사용
};

const STATUS_TEXT = {
  completed: "채점완료",
  waiting: "평가예정",
  pending: "채점예정",
  "not-submitted": "미제출",
};

const BUTTON_CONFIG = {
  completed: { text: "결과보기", className: "btn-gray", disabled: false },
  waiting:   { text: "채점하기", className: "btn-gray", disabled: true  },
  pending:   { text: "채점하기", className: "btn-primary", disabled: false },
  "not-submitted": { text: "확인", className: "btn-gray", disabled: false },
};

function safeGet(id) {
  return document.getElementById(id);
}

function safeQuery(sel) {
  return document.querySelector(sel);
}

function normalize(str) {
  return (str || "").toString().trim().toLowerCase();
}

/**
 * @param {Object} opt
 * @param {string} opt.containerId  - 목록이 들어갈 컨테이너 id (필수)
 * @param {Array}  opt.data         - 평가 데이터 배열 (필수)
 * @param {Object} opt.routes       - 상태별 이동 주소 매핑(선택)
 * @param {string} opt.itemClass    - 렌더된 item 클래스명 (예: 'grading-item' / 'assignment-item')
 * @param {string} opt.checkboxClass- 체크박스 클래스명 (예: 'grading-checkbox' / 'assignment-checkbox')
 * @param {string} opt.selectAllSelector - 전체선택 체크박스 selector (없으면 스킵)
 */
function initEvaluationList(opt) {
  const {
    containerId,
    data,
    routes = {},
    itemClass = "grading-item",
    checkboxClass = "grading-checkbox",
    selectAllSelector = null,
  } = opt;

  const container = safeGet(containerId);
  if (!container) return;

  // 내부 상태: 현재 필터링된 DOM을 다루기 때문에, 렌더 후 필터를 다시 적용하는 형태
  function renderList(list) {
    container.innerHTML = "";

    list.forEach((item) => {
      const status = item.status;
      const statusText = STATUS_TEXT[status] || status;
      const statusClass = STATUS_CLASS[status] || "";
      const btn = BUTTON_CONFIG[status] || { text: "보기", className: "btn-gray", disabled: false };

      // 주소 매핑
      // routes 예: { completed: '...', pending: '...', waiting: '...' }
      const address = routes[status] || item.address || "#";

      const el = document.createElement("div");
      el.className = itemClass;

      // evalType 컬럼/필터를 위해 DOM에 반드시 심어둠 (중요!)
      // evalType 값: 'test' | 'assignment' (추천)
      const evalType = item.evalType || ""; // 'test' or 'assignment'
      const evalTypeLabel = evalType === "test" ? "시험" : (evalType === "assignment" ? "과제" : "-");
      const evalTypeClass = evalType ? `eval-type ${evalType}` : "eval-type";

      // title 필드 공통화: 시험명/과제명
      const title = item.title || item.testName || item.assignmentName || "-";

      el.innerHTML = `
        <div><input type="checkbox" class="${checkboxClass}"></div>
        <div class="${itemClass.includes('grading') ? 'grading-number' : 'assignment-number'}">${item.number ?? ""}</div>

        <div class="${itemClass.includes('grading') ? 'grading-course' : 'assignment-course'}">
          <span class="course-name">${item.courseName || "-"}</span>
          <span class="course-id">${item.courseId || "-"}</span>
        </div>

        <div class="${itemClass.includes('grading') ? 'test-name' : 'assignment-name'}">${title}</div>

        <div class="instructor-name">${item.instructor || "-"}</div>

        <!-- evalType: 필터가 이것을 바라봄 -->
        <div class="${evalTypeClass}">${evalTypeLabel}</div>

        <div class="deadline-info">
          <span class="deadline-date">${item.startDate || "-"}</span>
          <span class="deadline-time">${item.startTime || ""}</span>
        </div>

        <div class="deadline-info">
          <span class="deadline-date">${item.endDate || "-"}</span>
          <span class="deadline-time">${item.endTime || ""}</span>
        </div>

        <div class="submission-info">
          <span class="submitted">제출: ${item.submitted ?? 0}명</span>
          <span class="not-submitted">미제출: ${item.notSubmitted ?? 0}명</span>
        </div>

        <div class="grading-status">
          <span class="status-badge ${statusClass}">${statusText}</span>
        </div>

        <div>
          <button class="btn ${btn.className}" type="button" ${btn.disabled ? "disabled" : ""} data-action="go" data-url="${address}">
            ${btn.text}
          </button>
        </div>
      `;

      container.appendChild(el);
    });

    // 버튼 이동 이벤트 (inline onclick 제거)
    container.querySelectorAll('button[data-action="go"]').forEach((b) => {
      b.addEventListener("click", (e) => {
        const url = e.currentTarget.getAttribute("data-url");
        if (!url || url === "#") return;
        location.href = url;
      });
    });

    // 렌더가 끝난 뒤: 전체선택 바인딩을 "최신 DOM 기준"으로 재적용
    bindSelectAll();
  }

  function filterList() {
    const courseFilter = safeGet("courseFilter")?.value || "";
    const evalTypeFilter = safeGet("evalTypeFilter")?.value || "";
    const instructorFilter = safeGet("instructorFilter")?.value || "";
    const statusFilter = safeGet("statusFilter")?.value || "";
    const searchText = normalize(safeGet("searchInput")?.value);

    const items = container.querySelectorAll(`.${itemClass}`);
    items.forEach((row) => {
      let show = true;

      // 과정명
      if (courseFilter) {
        const courseName = row.querySelector(".course-name")?.textContent || "";
        if (!courseName.includes(courseFilter)) show = false;
      }

      // 평가유형
      if (evalTypeFilter) {
        const evalEl = row.querySelector(".eval-type");
        if (!evalEl || !evalEl.classList.contains(evalTypeFilter)) show = false;
      }

      // 강사명
      if (instructorFilter) {
        const ins = row.querySelector(".instructor-name")?.textContent || "";
        if (ins !== instructorFilter) show = false;
      }

      // 상태
      if (statusFilter) {
        const badge = row.querySelector(".status-badge");
        const expected = STATUS_CLASS[statusFilter] || "";
        if (!badge || (expected && !badge.classList.contains(expected))) show = false;
      }

      // 검색어(과정명/제목/강사)
      if (searchText) {
        const courseName = normalize(row.querySelector(".course-name")?.textContent);
        const title = normalize(
          row.querySelector(".test-name")?.textContent ||
          row.querySelector(".assignment-name")?.textContent
        );
        const ins = normalize(row.querySelector(".instructor-name")?.textContent);

        if (!courseName.includes(searchText) && !title.includes(searchText) && !ins.includes(searchText)) {
          show = false;
        }
      }

      row.style.display = show ? "grid" : "none";
    });
  }

  function bindFilterEvents() {
    // 필터 요소가 있으면 자동 바인딩
    const ids = ["courseFilter", "evalTypeFilter", "instructorFilter", "statusFilter"];
    ids.forEach((id) => {
      const el = safeGet(id);
      if (el) el.addEventListener("change", filterList);
    });

    const search = safeGet("searchInput");
    if (search) search.addEventListener("input", filterList);
  }

  function bindSelectAll() {
    if (!selectAllSelector) return;

    const selectAll = safeQuery(selectAllSelector);
    if (!selectAll) return;

    const checkboxes = container.querySelectorAll(`.${itemClass} .${checkboxClass}`);

    // 중복 바인딩 방지: 기존 이벤트 제거가 어려우니, 간단히 flag 사용
    if (selectAll.dataset.bound === "1") return;
    selectAll.dataset.bound = "1";

    selectAll.addEventListener("change", () => {
      checkboxes.forEach((cb) => (cb.checked = selectAll.checked));
    });

    checkboxes.forEach((cb) => {
      cb.addEventListener("change", () => {
        selectAll.checked = Array.from(checkboxes).every((c) => c.checked);
      });
    });
  }

  // 초기 렌더
  renderList(data);
  bindFilterEvents();

  // 외부에서도 호출할 수 있게 반환
  return {
    render: renderList,
    filter: filterList,
  };
}


