// ====== 더미 데이터 ======
  // status: ONGOING / SUBMITTED / CLOSED_UNANSWERED / SCHEDULED
  const SURVEYS = [
    {
      id: "SV-001",
      title: "과정 만족도 조사",
      course: "풀스택 웹 개발 (React & Node.js)",
      cohort: "1기",
      startAt: "2026-01-10",
      endAt: "2026-01-30",
      questionCount: 10,
      status: "ONGOING",
      createdAt: "2026-01-09"
    },
    {
      id: "SV-002",
      title: "강의 피드백 (중간점검)",
      course: "AI 활용 실무 (Python)",
      cohort: "2기",
      startAt: "2026-01-01",
      endAt: "2026-01-07",
      questionCount: 5,
      status: "SUBMITTED",
      createdAt: "2025-12-29"
    },
    {
      id: "SV-003",
      title: "시설 만족도 조사",
      course: "전체",
      cohort: "-",
      startAt: "2026-01-20",
      endAt: "2026-01-25",
      questionCount: 8,
      status: "CLOSED_UNANSWERED",
      createdAt: "2026-01-18"
    },
    {
      id: "SV-004",
      title: "수료 전 최종 설문",
      course: "풀스택 웹 개발 (React & Node.js)",
      cohort: "1기",
      startAt: "2026-02-01",
      endAt: "2026-02-05",
      questionCount: 12,
      status: "SCHEDULED",
      createdAt: "2026-01-25"
    }
  ];

  // ====== 상태 ======
  const sl = {
    status: "ALL",
    course: "ALL",
    start: "",
    end: "",
    keyword: "",
    sort: "DUE_SOON",
    rows: []
  };

  // ====== DOM ======
  const slStatus = document.getElementById("slStatus");
  const slCourse = document.getElementById("slCourse");
  const slStart = document.getElementById("slStart");
  const slEnd = document.getElementById("slEnd");
  const slKeyword = document.getElementById("slKeyword");
  const slSort = document.getElementById("slSort");
  const slReset = document.getElementById("slReset");

  const slTbody = document.getElementById("slTbody");
  const slEmpty = document.getElementById("slEmpty");
  const slHint = document.getElementById("slHint");

  // ====== 유틸 ======
  function badgeClass(status){
    if (status === "ONGOING") return "ongoing";
    if (status === "SUBMITTED") return "submitted";
    if (status === "CLOSED_UNANSWERED") return "closed";
    if (status === "SCHEDULED") return "scheduled";
    return "";
  }
  function statusLabel(status){
    if (status === "ONGOING") return "진행중";
    if (status === "SUBMITTED") return "제출완료";
    if (status === "CLOSED_UNANSWERED") return "종료(미응답)";
    if (status === "SCHEDULED") return "예정";
    return "-";
  }
  function inRange(dateStr, start, end){
    const d = new Date(dateStr + "T00:00:00");
    if (start){
      const s = new Date(start + "T00:00:00");
      if (d < s) return false;
    }
    if (end){
      const e = new Date(end + "T23:59:59");
      if (d > e) return false;
    }
    return true;
  }
  function escapeHtml(str){
    return String(str ?? "")
      .replaceAll("&","&amp;")
      .replaceAll("<","&lt;")
      .replaceAll(">","&gt;")
      .replaceAll('"',"&quot;")
      .replaceAll("'","&#039;");
  }

  function initCourseOptions(){
    const courses = Array.from(new Set(SURVEYS.map(s => s.course))).filter(Boolean);
    slCourse.innerHTML = `<option value="ALL">전체</option>` + courses.map(c =>
      `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`
    ).join("");
  }

  function applyFilters(){
    let rows = [...SURVEYS];

    if (sl.status !== "ALL") rows = rows.filter(r => r.status === sl.status);
    if (sl.course !== "ALL") rows = rows.filter(r => r.course === sl.course);

    // 기간은 응답기간(endAt 기준으로 필터링하는 느낌이 자연스러움)
    rows = rows.filter(r => inRange(r.endAt, sl.start || "", sl.end || ""));

    if (sl.keyword.trim()){
      const k = sl.keyword.trim().toLowerCase();
      rows = rows.filter(r => r.title.toLowerCase().includes(k));
    }

    // 정렬
    if (sl.sort === "DUE_SOON") rows.sort((a,b) => a.endAt.localeCompare(b.endAt));
    if (sl.sort === "DUE_LATE") rows.sort((a,b) => b.endAt.localeCompare(a.endAt));
    if (sl.sort === "NEWEST") rows.sort((a,b) => b.createdAt.localeCompare(a.createdAt));

    sl.rows = rows;
  }

  function render(){
    applyFilters();

    if (!sl.rows.length){
      slTbody.innerHTML = "";
      slEmpty.style.display = "block";
      slHint.textContent = "전체 0건";
      return;
    }
    slEmpty.style.display = "none";

    slTbody.innerHTML = sl.rows.map((r, idx) => {
      const period = `${r.startAt} ~ ${r.endAt}`;
      const courseText = r.course === "전체" ? "전체" : `${r.course} / ${r.cohort}`;
      const disabled = (r.status === "SCHEDULED" || r.status === "CLOSED_UNANSWERED");
      const actionLabel = (r.status === "ONGOING") ? "응답하기"
                        : (r.status === "SUBMITTED") ? "보기"
                        : "불가";

      return `
        <tr>
          <td>${idx + 1}</td>
          <td style="color:#111;">${escapeHtml(r.title)}</td>
          <td>${escapeHtml(courseText)}</td>
          <td>${escapeHtml(period)}</td>
          <td>${r.questionCount}</td>
          <td><span class="badge ${badgeClass(r.status)}">${statusLabel(r.status)}</span></td>
          <td>
            <button class="link-btn" data-survey-open="${r.id}" ${disabled ? "disabled" : ""}>
              ${actionLabel}
            </button>
          </td>
        </tr>
      `;
    }).join("");

    slHint.textContent = `전체 ${sl.rows.length}건`;
  }

  // ====== 이벤트 ======
  slStatus.addEventListener("change", () => { sl.status = slStatus.value; render(); });
  slCourse.addEventListener("change", () => { sl.course = slCourse.value; render(); });
  slStart.addEventListener("change", () => { sl.start = slStart.value; render(); });
  slEnd.addEventListener("change", () => { sl.end = slEnd.value; render(); });
  slKeyword.addEventListener("input", () => { sl.keyword = slKeyword.value || ""; render(); });
  slSort.addEventListener("change", () => { sl.sort = slSort.value; render(); });

  slReset.addEventListener("click", () => {
    slStatus.value = "ALL";
    slCourse.value = "ALL";
    slStart.value = "";
    slEnd.value = "";
    slKeyword.value = "";
    slSort.value = "DUE_SOON";
    sl.status = "ALL"; sl.course = "ALL"; sl.start = ""; sl.end = ""; sl.keyword = ""; sl.sort = "DUE_SOON";
    render();
  });

  // ✅ 동적 버튼 클릭(이벤트 위임)
  slTbody.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-survey-open]");
    if (!btn) return;
    const surveyId = btn.getAttribute("data-survey-open");
    // 별도 페이지로 새 창에서 이동
    window.open(`/templates/trainee/survey-detail-page.html`, '_blank');
  });

  initCourseOptions();
  render();