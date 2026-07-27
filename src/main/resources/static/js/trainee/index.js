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

  // ===== DATA (더미) =====
  const USER = { name: "김민아" };

  const COURSES = [
    {
      id: "C-01",
      name: "풀스택 웹 개발 (React & Node.js)",
      cohort: "1기",
      startAt: "2025-12-01",
      endAt: "2026-01-31",
      attendanceRate: 92,
      lastStudy: "오늘 08:12",
      dday: 5
    },
    {
      id: "C-02",
      name: "AI 활용 실무 (Python)",
      cohort: "2기",
      startAt: "2026-01-10",
      endAt: "2026-03-10",
      attendanceRate: 78,
      lastStudy: "어제 21:40",
      dday: 44
    }
  ];

  // type: TASK / SURVEY / EXAM
  const TODOS = [
    { id:"A1", type:"TASK", title:"과제 1 - 연산자 요약", meta:"풀스택 웹 개발 / 3차시", due:"2026-01-26 23:59", dday:0, action:"submitTask" },
    { id:"SV-001", type:"SURVEY", title:"과정 만족도 조사", meta:"풀스택 웹 개발 / 1기", due:"2026-01-30 23:59", dday:4, action:"answerSurvey" },
    { id:"EX-01", type:"EXAM", title:"모의시험 1", meta:"AI 활용 실무 / 2기", due:"2026-01-28 10:00", dday:2, action:"goExamTask" }
  ];

  const NOTICES = [
    { id:"N-01", title:"[안내] 학습 일정 변경 공지", date:"2026-01-25" },
    { id:"N-02", title:"[필독] 시험 응시 유의사항", date:"2026-01-24" },
    { id:"N-03", title:"[공지] 설문 참여 안내", date:"2026-01-23" },
    { id:"N-04", title:"[안내] 출결 처리 기준", date:"2026-01-22" },
    { id:"N-05", title:"[공지] 과제 제출 형식 안내", date:"2026-01-21" }
  ];

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

  // ===== Routing (경로만 너 프로젝트에 맞게 수정) =====
  function goTo(key, params){
    if (key==="learning") location.href = "continue-learning.html";
    if (key==="attendance") location.href = "attendance.html";
    if (key==="examTask") location.href = "online-test.html";
    if (key==="survey") location.href = "surveys.html.html";
    if (key==="notice") location.href = "notices.html";

    // 세부 이동
    if (key==="surveyDetail") location.href = `trainee-survey-detail.html?surveyId=${encodeURIComponent(params.surveyId)}`;
    if (key==="noticeDetail") location.href = `notices-detail.html?noticeId=${encodeURIComponent(params.noticeId)}`;
  }

  // ===== Render: Stats =====
  function renderStats(){
    const ongoingCourses = COURSES.length;
    const dueTodayTasks = TODOS.filter(x => x.type==="TASK" && x.dday === 0).length;
    const ongoingSurveys = TODOS.filter(x => x.type==="SURVEY").length;

    // 최근 학습 요약 (가장 최근 학습시간을 단순히 첫 과정으로)
    const lastStudy = COURSES[0]?.lastStudy || "-";

    const blocks = [
      { k:"진행중 과정", v:`${ongoingCourses}개`, s:"현재 수강 중" },
      { k:"오늘 마감 과제", v:`${dueTodayTasks}개`, s:"오늘까지 제출" },
      { k:"진행중 설문", v:`${ongoingSurveys}개`, s:"응답 필요" },
      { k:"최근 학습", v:lastStudy, s:"마지막 접속" }
    ];

    hpStats.innerHTML = blocks.map(b => `
      <div class="stat">
        <div class="stat-k">${escapeHtml(b.k)}</div>
        <div class="stat-v">${escapeHtml(b.v)}</div>
        <div class="stat-s">${escapeHtml(b.s)}</div>
      </div>
    `).join("");
  }

  // ===== Render: Todos =====
  function renderTodos(){
    // 우선순위: D-day → D-1.. → 그 외
    const sorted = [...TODOS].sort((a,b)=>{
      const ad = a.dday ?? 9999;
      const bd = b.dday ?? 9999;
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
          <button type="button" style="width:85px;" class="mini-btn primary" data-todo="${escapeHtml(item.id)}" data-action="${escapeHtml(item.action)}">
            ${item.type==="TASK" ? "제출하기" : item.type==="SURVEY" ? "응답하기" : "자세히"}
          </button>
        </div>
      </div>
    `).join("");
  }

  // ===== Render: Courses =====
  function renderCourses(){
    if (!COURSES.length){
      hpCourseGrid.innerHTML = "";
      hpCourseEmpty.style.display = "block";
      return;
    }
    hpCourseEmpty.style.display = "none";

    hpCourseGrid.innerHTML = COURSES.map(c => `
      <div class="course-card" data-course="${escapeHtml(c.id)}">
        <div>
          <div class="course-name">${escapeHtml(c.name)}</div>
          <div class="course-sub">${escapeHtml(c.cohort)} · ${escapeHtml(c.startAt)} ~ ${escapeHtml(c.endAt)}</div>
        </div>

        <div class="course-info">
          <div class="info-box">
            <div class="info-k">출결률</div>
            <div class="info-v">${escapeHtml(c.attendanceRate)}%</div>
          </div>
          <div class="info-box">
            <div class="info-k">종료 D-day</div>
            <div class="info-v">D-${escapeHtml(c.dday)}</div>
          </div>
        </div>

        <div class="course-actions">
          <button type="button" class="mini-btn" data-course-action="continue" data-course="${escapeHtml(c.id)}">학습 계속하기</button>
          <button type="button" class="mini-btn" data-course-action="attendance" data-course="${escapeHtml(c.id)}">출결/이수</button>
          <button type="button" class="mini-btn" data-course-action="notice" data-course="${escapeHtml(c.id)}">공지</button>
        </div>
      </div>
    `).join("");
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
  document.querySelector(".quick-actions").addEventListener("click",(e)=>{
    const btn = e.target.closest("[data-go]");
    if(!btn) return;
    goTo(btn.getAttribute("data-go"));
  });

  hpTodoList.addEventListener("click",(e)=>{
    const btn = e.target.closest("[data-action][data-todo]");
    if(!btn) return;
    const action = btn.getAttribute("data-action");
    const id = btn.getAttribute("data-todo");

    if(action==="submitTask"){
      // 과제 페이지로 이동(또는 과제 제출 모달 직접 열고 싶으면 연결 가능)
      goTo("examTask");
    }
    if(action==="answerSurvey"){
      goTo("surveyDetail", { surveyId: id });
    }
    if(action==="goExamTask"){
      goTo("examTask");
    }
  });

  hpCourseGrid.addEventListener("click",(e)=>{
    const btn = e.target.closest("[data-course-action]");
    if(!btn) return;
    const action = btn.getAttribute("data-course-action");
    // const courseId = btn.getAttribute("data-course"); // 필요하면 사용
    if(action==="continue") goTo("learning");
    if(action==="attendance") goTo("attendance");
    if(action==="notice") goTo("notice");
  });

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
    hpUserName.textContent = USER.name || "훈련생";
    hpTodayText.textContent = `${nowKSTText()}`;
    renderStats();
    renderTodos();
    renderCourses();
    // renderNotices();
  }

  renderAll();