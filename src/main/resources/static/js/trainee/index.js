 const encouragementMessages = {
    normal: [
      // 기본형
      `조금만 더 학습하면 출석 기준을 충족할 수 있어요.`,
      `학습을 완료하면 출결 상태가 자동으로 반영됩니다.`,
      `남은 학습을 이어서 진행해 보세요.`,
      `학습을 이어가면
      오늘 출석으로 인정돼요.`,

      // 긍정 강화형
      `잘하고 있어요! 지금 페이스를 유지해 보세요.`,
      `학습 기록이 차곡차곡 쌓이고 있어요.`,
      `지금처럼만 해도 충분히 잘하고 있어요.`,
      `조금씩 꾸준히 학습하면 충분히 달성할 수 있어요.`
    ],

    absent: [
      // 결석 전용
      `아직 학습 기록이 없어요. 지금 학습을 시작해 보세요.`,
      `오늘의 학습을 놓치지 마세요.`,
      `지금 시작해도 늦지 않아요.`,
      `지금 학습을 시작하면 다음 차시는 출석으로 기록돼요.`,
      `이번 학습부터 다시 시작해 보세요.` 
    ]
  };

    // ===== Sparkle Effect =====
    function randomInt(min, max) {
      return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    function createSparkle(target) {
      const sparkle = document.createElement('img');
      sparkle.className = 'sparkle';
      sparkle.style.opacity = 0;
      target.appendChild(sparkle);
      return sparkle;
    }

    function setRandomStarImage(sparkle) {
      // star (1).png ~ star (13).png
      const n = Math.floor(Math.random() * 13) + 1;
      sparkle.src = `/static/img/star/star (${n}).png`;
    }

    function animateSparkle(sparkle, areaW, areaH) {
      setRandomStarImage(sparkle);
      const x = randomInt(0, areaW - 24);
      const y = randomInt(0, areaH - 24);
      sparkle.style.left = x + 'px';
      sparkle.style.top = y + 'px';
      sparkle.style.opacity = 1;
      sparkle.style.transform = 'scale(' + (Math.random() * 0.5 + 0.8) + ')';
      setTimeout(() => {
        sparkle.style.opacity = 0;
      }, randomInt(900, 1600)); // 더 오래 보이게
    }

    function startRandomSparkles() {
      const heartArea = document.getElementById('heart-area');
      if (!heartArea) return;
      const getAreaSize = () => ({
        w: heartArea.offsetWidth || 300,
        h: heartArea.offsetHeight || 120
      });
      const sparkleCount = 5;
      const sparkles = [];
      for (let i = 0; i < sparkleCount; i++) {
        sparkles.push(createSparkle(heartArea));
      }
      function sparkleLoop(sparkle) {
        const { w, h } = getAreaSize();
        animateSparkle(sparkle, w, h);
        setTimeout(() => sparkleLoop(sparkle), randomInt(900, 1700)); // 빈도도 느리게
      }
      sparkles.forEach((sparkle, idx) => {
        setTimeout(() => sparkleLoop(sparkle), idx * 200);
      });
    }

    // 출석 도장 표시 함수 (월~금)
    function renderAttendanceStamps() {
      // 예시: DB에서 받아온 출석 정보 (월~금, 출석한 요일 true)
      // 실제로는 API에서 받아온 데이터로 대체
      // 월~금: [월, 화, 수, 목, 금]
      const attendanceWeek = [true, false, true, true, false];
      for (let i = 0; i < 5; i++) {
        const stemp = document.querySelector('.stemp-outline.stemp' + (i + 1) + ' .heart-in');
        if (stemp) {
          stemp.style.display = attendanceWeek[i] ? 'block' : 'none';
        }
      }
    }

    document.addEventListener('DOMContentLoaded', function() {
      // Set #message text to a random encouragement message
      const msgBox = document.getElementById('message');
      if (msgBox && encouragementMessages.normal && encouragementMessages.normal.length > 0) {
        const randomIdx = Math.floor(Math.random() * encouragementMessages.normal.length);
        msgBox.innerText = encouragementMessages.normal[randomIdx];
      }
      renderAttendanceStamps();
      startRandomSparkles();
    });
  /**
   * 학생 메인(홈) - 더미 데이터 기반
   * - TODO: API 연동 시 DATA 영역만 교체하면 됨
   * - 라우팅: goTo() 내부 경로를 프로젝트에 맞게 바꿔서 사용
   */

  // ===== DATA (서버) =====
  // templates/trainee/index.html 의 인라인 <script th:inline="javascript"> 가
  // window._serverTraineeDashboard 를 먼저 채운다 (이 파일은 defer 라 그 뒤에 실행된다).
  //
  // 폴백은 "빈 대시보드"다. 예전 더미(김민아 / 풀스택 과정 …)를 남겨두면 서버 조회가
  // 실패했을 때 남의 이름과 가짜 과정이 본인 것처럼 보이므로 그대로 두면 안 된다.
  const TRAINEE_DASHBOARD = window._serverTraineeDashboard || {
    userName: "훈련생", today: "-", stats: [], todos: [], courses: [], notices: []
  };

  const USER = { name: TRAINEE_DASHBOARD.userName };
  const STATS = TRAINEE_DASHBOARD.stats || [];
  const COURSES = TRAINEE_DASHBOARD.courses || [];
  const TODOS = TRAINEE_DASHBOARD.todos || [];

  // ===== DOM =====
  const hpUserName = document.getElementById("hpUserName");
  const hpTodayText = document.getElementById("hpTodayText");
  const hpStats = document.getElementById("hpStats");

  const hpTodoList = document.getElementById("hpTodoList");
  const hpTodoEmpty = document.getElementById("hpTodoEmpty");

  const hpCourseGrid = document.getElementById("hpCourseGrid");
  const hpCourseEmpty = document.getElementById("hpCourseEmpty");

  const hpNoticeList = document.getElementById("hpNoticeList");
  const hpGoNotice = document.getElementById("hpGoNotice");
  const hpRefresh = document.getElementById("hpRefresh");

  // ===== Utils =====
  function escapeHtml(str){
    return String(str ?? "")
      .replaceAll("&","&amp;")
      .replaceAll("<","&lt;")
      .replaceAll(">","&gt;")
      .replaceAll('"',"&quot;")
      .replaceAll("'","&#039;");
  }

  function nowKSTText(){
    const now = new Date();
    // 사용자 환경이 KST라 가정(프로젝트 기준)
    const days = ["일","월","화","수","목","금","토"];
    const y = now.getFullYear();
    const m = String(now.getMonth()+1).padStart(2,"0");
    const d = String(now.getDate()).padStart(2,"0");
    const day = days[now.getDay()];
    return `${y}-${m}-${d} (${day})`;
  }

  function pill(type){
    if (type==="TASK") return `<span class="pill task">과제</span>`;
    if (type==="SURVEY") return `<span class="pill survey">설문</span>`;
    if (type==="EXAM") return `<span class="pill exam">시험</span>`;
    return `<span class="pill">기타</span>`;
  }

  function ddayPill(dday){
    if (dday === 0) return `<span class="pill dday">D-day</span>`;
    if (dday > 0 && dday <= 3) return `<span class="pill dday">D-${dday}</span>`;
    return `<span class="pill">D-${dday}</span>`;
  }

  // ===== Routing =====
  // 실제 컨트롤러 URL. 기존 값은 상대경로 *.html 이라 전부 404 였고 "surveys.html.html" 오타도 있었다.
  function goTo(key, params){
    if (key==="learning") location.href = "/trainee/learning";
    if (key==="attendance") location.href = "/trainee/my-course";
    if (key==="examTask") location.href = "/trainee/exam";
    if (key==="assignment") location.href = "/trainee/assignment";
    if (key==="survey") location.href = "/trainee/survey";
    if (key==="notice") location.href = "/trainee/notice";

    if (key==="surveyDetail") location.href = `/trainee/survey/${encodeURIComponent(params.surveyId)}`;
    if (key==="noticeDetail") location.href = `/trainee/notice/${encodeURIComponent(params.noticeId)}`;
  }

  // ===== Render: Stats =====
  // 타일 구성(수강중 과정 / 남은 과제·시험 / 최근 성적 / 안 읽은 알림)은 서버가 정한다.
  // 화면 JS 가 다시 계산하면 서버 집계와 값이 갈린다.
  function renderStats(){
    if (!hpStats) return;
    if (!STATS.length){
      hpStats.innerHTML = `<div class="stat"><div class="stat-k">요약</div><div class="stat-v">-</div><div class="stat-s">표시할 데이터가 없어요</div></div>`;
      return;
    }
    hpStats.innerHTML = STATS.map(b => `
      <div class="stat">
        <div class="stat-k">${escapeHtml(b.k)}</div>
        <div class="stat-v">${escapeHtml(b.v)}</div>
        <div class="stat-s">${escapeHtml(b.s)}</div>
      </div>
    `).join("");
  }

  // ===== Render: Todos =====
  function renderTodos(){
    if (!hpTodoList || !hpTodoEmpty) return;
    // 서버가 이미 마감 임박순으로 내려주지만, 화면에서도 같은 기준으로 한 번 더 정렬한다.
    const sorted = [...TODOS].sort((a,b)=>{
      const ad = (a.dday === null || a.dday === undefined) ? 9999 : a.dday;
      const bd = (b.dday === null || b.dday === undefined) ? 9999 : b.dday;
      return ad - bd;
    });

    if (!sorted.length){
      hpTodoList.innerHTML = "";
      hpTodoEmpty.style.display = "block";
      return;
    }
    hpTodoEmpty.style.display = "none";

    hpTodoList.innerHTML = sorted.map(item => `
      <div class="todo-item">
        <div class="todo-left">
          <div class="todo-top">
            ${pill(item.type)}
            ${typeof item.dday === "number" ? ddayPill(item.dday) : ""}
          </div>
          <div class="todo-title">${escapeHtml(item.title)}</div>
          <div class="todo-meta">${escapeHtml(item.meta)} · 마감 ${escapeHtml(item.due)}</div>
        </div>
        <div class="todo-actions">
          <button type="button" style="width:85px;" class="mini-btn primary" data-href="${escapeHtml(item.href)}">
            ${item.type==="TASK" ? "제출하기" : item.type==="SURVEY" ? "응답하기" : "자세히"}
          </button>
        </div>
      </div>
    `).join("");
  }

  // ===== Render: Courses =====
  function renderCourses(){
    if (!hpCourseGrid || !hpCourseEmpty) return;
    if (!COURSES.length){
      hpCourseGrid.innerHTML = "";
      hpCourseEmpty.style.display = "block";
      return;
    }
    hpCourseEmpty.style.display = "none";

    hpCourseGrid.innerHTML = COURSES.map(c => {
      // 진도는 숫자만 있으면 눈에 안 들어온다 — 막대로 함께 보여준다
      const pct = pctOf(c.progressRate);
      const att = pctOf(c.attendanceRate);
      // 출결이 이수 기준(보통 80%)에 가까워지면 알려준다. 훈련생이 가장 늦게 아는 값이다
      const attWarn = att !== null && att < 80;

      return `
      <div class="course-card" data-course="${escapeHtml(c.id)}">
        <div>
          <div class="course-name">${escapeHtml(c.name)}</div>
          <div class="course-sub">${escapeHtml(c.cohort)} · ${escapeHtml(c.startAt)} ~ ${escapeHtml(c.endAt)}</div>
        </div>

        <div class="course-progress">
          <div class="course-progress-head">
            <span>학습 진도</span>
            <b>${escapeHtml(c.progressRate)}</b>
          </div>
          <div class="ai-bar"><span style="width:${pct === null ? 0 : pct}%"></span></div>
        </div>

        <div class="course-info">
          <div class="info-box">
            <div class="info-k">출결률</div>
            <div class="info-v${attWarn ? " warn" : ""}">${escapeHtml(c.attendanceRate)}</div>
          </div>
          <div class="info-box">
            <div class="info-k">종료까지</div>
            <div class="info-v">${escapeHtml(c.dday)}</div>
          </div>
        </div>

        ${attWarn ? '<p class="course-hint">출결률이 이수 기준(80%)보다 낮아요. 출결 현황을 확인해 주세요.</p>' : ""}

        <div class="course-actions">
          <button type="button" class="mini-btn primary" data-href="${escapeHtml(c.continueHref)}">이어서 학습하기</button>
          <button type="button" class="mini-btn" data-course-action="attendance">출결/이수</button>
          <button type="button" class="mini-btn" data-href="${escapeHtml(c.noticeHref)}">공지</button>
        </div>
      </div>
    `;}).join("");
  }

  /** "62%" 같은 표시용 문자열에서 숫자만 뽑는다. 값이 없으면("-") null. */
  function pctOf(v){
    const m = String(v ?? "").match(/\d+(\.\d+)?/);
    return m ? Math.max(0, Math.min(100, Math.round(parseFloat(m[0])))) : null;
  }

  // ===== Render: Notices =====
// function renderNotices(){
//   const top5 = NOTICES.slice(0,5);

//   hpNoticeList.innerHTML = top5.map(n => `
//     <li class="notice-mini-item" data-notice="${escapeHtml(n.id)}">
//       <span class="notice-mini-title">${escapeHtml(n.title)}</span>
//       <span class="notice-mini-date">${escapeHtml(n.date)}</span>
//     </li>
//   `).join("");
// }


  // ===== Events =====
  // 서버가 내려준 data-href 로 이동한다. 화면 JS 가 경로를 다시 조립하면 컨트롤러 URL 이 바뀔 때마다 깨진다.
  function bindHrefClicks(root){
    if (!root) return;
    root.addEventListener("click",(e)=>{
      const btn = e.target.closest("[data-href]");
      if (btn) { location.href = btn.getAttribute("data-href"); return; }
      const action = e.target.closest("[data-course-action]");
      if (action) goTo(action.getAttribute("data-course-action"));
    });
  }

  const quickActions = document.querySelector(".quick-actions");
  if (quickActions){
    quickActions.addEventListener("click",(e)=>{
      const btn = e.target.closest("[data-go]");
      if(!btn) return;
      goTo(btn.getAttribute("data-go"));
    });
  }

  bindHrefClicks(hpTodoList);
  bindHrefClicks(hpCourseGrid);

  if (hpRefresh) hpRefresh.addEventListener("click", ()=> location.reload());

  // hpNoticeList.addEventListener("click",(e)=>{
  //   const btn = e.target.closest("[data-notice]");
  //   if(!btn) return;
  //   const noticeId = btn.getAttribute("data-notice");
  //   // 공지 상세 페이지가 있으면 그쪽으로
  //   // goTo("noticeDetail", { noticeId });
  //   goTo("notice");
  // });

  // hpGoNotice.addEventListener("click",()=>goTo("notice"));
  // hpRefresh.addEventListener("click",()=>{
  //   // 실제 API면 재호출, 더미면 재렌더
  //   renderAll();
  // });

  // ===== init =====
  function renderAll(){
    if (hpUserName) hpUserName.textContent = USER.name || "훈련생";
    // 오늘 날짜도 서버 값(KST 서버 기준)을 쓴다. 브라우저 시계로 계산하면 마감 D-day 와 어긋난다.
    if (hpTodayText) hpTodayText.textContent = TRAINEE_DASHBOARD.today || nowKSTText();
    renderStats();
    renderTodos();
    renderCourses();
    // renderNotices();
  }

  renderAll();