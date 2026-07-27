// 바로 학습하기 버튼 클릭 시 문서 학습 페이지로 이동
document.addEventListener('DOMContentLoaded', function() {
    const learnBtn = document.querySelector('.btn.btn-primary');
    if (learnBtn) {
        learnBtn.addEventListener('click', function() {
            window.location.href = '/templates/trainee/play-document.html';
        });
    }
});
// 컨텐츠 타입별 라벨과 URL 매핑 함수
function getContentUrlByType(content) {
    // typeLabel 기준: 동영상, 문서, 시험, 실습, 과제
    switch (content.typeLabel) {
        case '동영상':
            return `/templates/trainee/play-video.html`;
        case '문서':
            return `/templates/trainee/play-document.html`;
        case '시험':
            return `/templates/trainee/play-grading.html`;
        case '실습':
            return `/templates/trainee/play-practice.html`;
        case '수업':
            return `/templates/trainee/play-class.html`;
    }
}
// 차시 > 과목(컨텐츠) 구조 더미데이터
    // 더미데이터 (폴백용)
    const courseData =
    {
        id: 1,
        category: '개발',
        status: '운영중',
        title: '풀스택 웹 개발 (React & Node.js)',
        instructor: '홍길동',
        subjects: 12,
        totalHours: 96,
        students: 45,
        progress: 75,
        period: '2024-01-10 ~ 2024-06-30',
        buttonText: '학습하기'
    };
    const dummyLessonList = [
        {
            lessonId: "LESSON-01",
            lessonTitle: "1차시: JavaScript 기본 문법",
            period: { startDate: "2026-01-06", endDate: "2026-01-10" },
            contents: [
                { contentId: "CNT-001", subjectId: "SUB-01", subjectTitle: "기초 문법", order: 1, title: "변수와 데이터 타입", type: "VIDEO", typeLabel: "동영상", durationMinutes: 45, progressRate: 100, isCompleted: true },
                { contentId: "CNT-002", subjectId: "SUB-01", subjectTitle: "기초 문법", order: 2, title: "연산자와 표현식", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 30, progressRate: 60, isCompleted: false },
                { contentId: "CNT-003", subjectId: "SUB-02", subjectTitle: "제어문", order: 3, title: "조건문과 반복문", type: "TEST", typeLabel: "실습", durationMinutes: 20, progressRate: 0, isCompleted: false },
                { contentId: "CNT-004", subjectId: "SUB-02", subjectTitle: "제어문", order: 3, title: "조건문과 반복문", type: "TEST", typeLabel: "수업", durationMinutes: 20, progressRate: 0, isCompleted: false },
                { contentId: "CNT-005", subjectId: "SUB-02", subjectTitle: "제어문", order: 3, title: "조건문과 반복문", type: "TEST", typeLabel: "시험", durationMinutes: 20, progressRate: 0, isCompleted: false }
            ]
        },
        {
            lessonId: "LESSON-02",
            lessonTitle: "2차시: 함수와 객체",
            period: { startDate: "2026-01-11", endDate: "2026-01-15" },
            contents: [
                { contentId: "CNT-004", subjectId: "SUB-03", subjectTitle: "함수", order: 1, title: "함수와 스코프", type: "VIDEO", typeLabel: "동영상", durationMinutes: 50, progressRate: 80, isCompleted: false },
                { contentId: "CNT-005", subjectId: "SUB-04", subjectTitle: "객체", order: 2, title: "객체와 배열", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 35, progressRate: 100, isCompleted: true },
                { contentId: "CNT-006", subjectId: "SUB-04", subjectTitle: "객체", order: 3, title: "내장 객체 활용", type: "VIDEO", typeLabel: "동영상", durationMinutes: 40, progressRate: 20, isCompleted: false }
            ]
        },
        {
            lessonId: "LESSON-03",
            lessonTitle: "3차시: 이벤트와 비동기",
            period: { startDate: "2026-01-16", endDate: "2026-01-20" },
            contents: [
                { contentId: "CNT-007", subjectId: "SUB-05", subjectTitle: "이벤트", order: 1, title: "이벤트와 DOM", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 25, progressRate: 0, isCompleted: false },
                { contentId: "CNT-008", subjectId: "SUB-06", subjectTitle: "비동기", order: 2, title: "비동기 프로그래밍", type: "VIDEO", typeLabel: "동영상", durationMinutes: 60, progressRate: 50, isCompleted: false },
                { contentId: "CNT-009", subjectId: "SUB-06", subjectTitle: "비동기", order: 3, title: "콜백과 프로미스", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 30, progressRate: 100, isCompleted: true }
            ]
        },
        {
            lessonId: "LESSON-04",
            lessonTitle: "4차시: ES6와 모듈",
            period: { startDate: "2026-01-21", endDate: "2026-01-25" },
            contents: [
                { contentId: "CNT-010", subjectId: "SUB-07", subjectTitle: "ES6", order: 1, title: "ES6 문법", type: "VIDEO", typeLabel: "동영상", durationMinutes: 55, progressRate: 10, isCompleted: false },
                { contentId: "CNT-011", subjectId: "SUB-08", subjectTitle: "모듈", order: 2, title: "모듈과 패키지", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 20, progressRate: 0, isCompleted: false },
                { contentId: "CNT-012", subjectId: "SUB-09", subjectTitle: "시험", order: 3, title: "디버깅과 시험", type: "TEST", typeLabel: "시험", durationMinutes: 25, progressRate: 0, isCompleted: false }
            ]
        },
        {
            lessonId: "LESSON-05",
            lessonTitle: "5차시: 에러 처리와 실전",
            period: { startDate: "2026-01-26", endDate: "2026-01-31" },
            contents: [
                { contentId: "CNT-013", subjectId: "SUB-10", subjectTitle: "에러 처리", order: 1, title: "에러 처리", type: "DOCUMENT", typeLabel: "문서", durationMinutes: 15, progressRate: 100, isCompleted: true },
                { contentId: "CNT-014", subjectId: "SUB-11", subjectTitle: "실전 프로젝트", order: 2, title: "실전 프로젝트", type: "VIDEO", typeLabel: "동영상", durationMinutes: 120, progressRate: 0, isCompleted: false },
                { contentId: "CNT-015", subjectId: "SUB-12", subjectTitle: "최종 시험", order: 3, title: "최종 시험", type: "TEST", typeLabel: "시험", durationMinutes: 30, progressRate: 0, isCompleted: false }
            ]
        }
    ];

    // 아코디언 카드 생성 함수
    function renderProgressCircle(percent) {
        const radius = 28;
        const stroke = 4;
        const normalizedRadius = radius - stroke / 2;
        const circumference = 2 * Math.PI * normalizedRadius;
        const progress = Math.max(0, Math.min(percent, 100));
        const offset = circumference - (progress / 100) * circumference;
        return `
            <svg class="progress-circle" width="${radius * 2}" height="${radius * 2}">
                <circle
                    stroke="#e0e0e0"
                    fill="none"
                    stroke-width="${stroke}"
                    cx="${radius}"
                    cy="${radius}"
                    r="${normalizedRadius}"/>
                <circle
                    stroke="#007bff"
                    fill="none"
                    stroke-width="${stroke}"
                    stroke-linecap="round"
                    cx="${radius}"
                    cy="${radius}"
                    r="${normalizedRadius}"
                    stroke-dasharray="${circumference}"
                    stroke-dashoffset="${offset}"
                    style="transition:stroke-dashoffset 0.4s;"/>
                <text x="50%" y="50%" class="progress-circle-text" dy="1">${progress}%</text>
            </svg>
        `;
    }

    // 차시별 아코디언 렌더링
    function renderLessonAccordion(lessons) {
            return lessons.map(lesson => {
                // 차시별 평균 진도율
                const avgProgress = lesson.contents.length > 0 ? Math.round(
                    lesson.contents.reduce((sum, c) => sum + c.progressRate, 0) / lesson.contents.length
                ) : 0;
                const periodText = `${lesson.period.startDate} ~ ${lesson.period.endDate}`;
                // subject별로 그룹핑
                const subjectMap = {};
                lesson.contents.forEach(content => {
                    if (!subjectMap[content.subjectId]) {
                        subjectMap[content.subjectId] = {
                            subjectTitle: content.subjectTitle,
                            items: []
                        };
                    }
                    subjectMap[content.subjectId].items.push(content);
                });
                return `
                    <div class="accordion" data-id="${lesson.lessonId}">
                        <div class="accordion-header">
                            <div class="accordion-header-content">
                                <span>${lesson.lessonTitle}</span>
                                <div style="font-size:13px; margin-top:4px; color:#444; display:flex; gap:16px; flex-wrap:wrap;">
                                    <span><strong>진도율</strong> <span style='color:#007bff;'>${avgProgress}%</span></span>
                                    <span><strong>컨텐츠</strong> ${lesson.contents.length}개</span>
                                    <span><strong>수강 가능 기간</strong> ${periodText}</span>
                                </div>
                            </div>
                            <div style="display:flex;align-items:center;">
                                ${renderProgressCircle(avgProgress)}
                            </div>
                        </div>
                        <div class="accordion-content">
                            ${Object.values(subjectMap).map(subject => `
                                <div style="margin-bottom:12px;">
                                    <div style="font-weight:600; color:#007bff; margin-bottom:4px;">${subject.subjectTitle}</div>
                                    <ul style="list-style:none; padding:0; margin:0;">
                                        ${subject.items.map(content => {
                                            // type에 따라 아이콘 파일명 매핑
                                            let iconFile = '';
                                            switch(content.type) {
                                                case 'DOCUMENT': iconFile = 'content-document.png'; break;
                                                case 'PRACTICE': iconFile = 'content-practice.png'; break;
                                                case 'TEST': iconFile = 'content-test.png'; break;
                                                case 'VIDEO': iconFile = 'content-video.png'; break;
                                                default: iconFile = 'content-document.png';
                                            }
                                            const url = getContentUrlByType(content);
                                            return `
                                            <li style="display:flex;align-items:center;gap:12px;padding:10px;border-bottom:1px solid #eee;">
                                            <span style="color:#888;">${content.order}.</span>
                                                <img src="/static/icons/${iconFile}" alt="${content.typeLabel}" style="width:42px;height:42px;object-fit:contain;"/>
                                                <span style="flex:1;">${content.title}<span style="font-size:14px;color:#999;margin-left:8px;"> ${Math.floor(content.durationMinutes/60) > 0 ? Math.floor(content.durationMinutes/60)+"시간 " : ''}${content.durationMinutes%60}분</span></span>
                                                <span style="font-size:14px;color:#007bff;">${content.progressRate}%</span>
                                                <span class="status-badge ${content.isCompleted ? 'status-completed' : 'status-in-progress'}">${content.isCompleted ? '완료' : '진행중'}</span>
                                                <button class="${content.isCompleted ? 'btn btn-gray' : 'btn-secondary btn'}" onclick="window.location.href='${url}'">${content.isCompleted ? '다시보기' : '학습하기'}</button>
                                            </li>
                                            `;
                                        }).join('')}
                                    </ul>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                `;
            }).join('');
        }
	// myCourse.js의 courseData 중 첫 번째 데이터 사용 (id: 1)
	// -> continue-learning.js에서는 이미 myCourse.js에서 courseData가 선언되어 있으므로 중복 선언하지 않고, 필요시 window.courseData로 접근하거나 import 방식으로 사용해야 함.
	// 아래 courseData 선언부는 삭제 또는 주석 처리 필요.
	// const courseData = [ ... ]; // <-- 이 부분 삭제 또는 주석 처리

    // 남은 기간 계산 함수
    function getDaysLeft(period) {
        // period: '2024-01-10 ~ 2024-06-30' 형식
        const endDateStr = period.split('~')[1]?.trim();
        if (!endDateStr) return '-';
        const endDate = new Date(endDateStr);
        const today = new Date();
        const diffMs = endDate - today;
        const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
        if (diffDays < 0) return '종료';
        return `${diffDays}일 남음`;
    }
	
    function createCourseCard(course) {
		const statusClassMap = {
			'운영중': 'status-completed',
			'준비중': 'status-pending',
			'종료': 'status-disabled'
		};
		const statusClass = statusClassMap[course.status] || 'status-disabled';
		return `
		<div class="course-card">
			<div class="course-card-header">
				<span class="course-category badge-kdt">${course.category}</span>
			</div>
			<h3 class="course-title">${course.title}</h3>
			<p class="course-instructor">${course.instructor} 강사</p>
			<div class="course-stats">
				<div class="stat-item">
					<div class="stat-label">차시</div>
					<div class="stat-value">${course.sessions}개</div>
				</div>
				<div class="stat-item">
					<div class="stat-label">총 시간</div>
					<div class="stat-value">${course.totalHours}시간</div>
				</div>
                <div class="stat-item">
                    <div class="stat-label">남은 기간</div>
                    <div class="stat-value">${getDaysLeft(course.period)}</div>
				</div>
			</div>
			<div class="course-progress">
				<div class="progress-info">
					<span class="course-period">
						<span class="status-badge ${statusClass}">${course.status}</span>
						📆${course.period}
					</span>
					<span class="progress-percentage">${course.progress}%</span>
				</div>
				<div class="progress-bar">
					<div class="progress-fill" style="width: ${course.progress}%;"></div>
				</div>
			</div>
			<div class="course-footer">
			</div>
		</div>
		`;
	}
    
    async function loadLessons() {
        let lessonList = [];
        try {
            // 실제 API 엔드포인트로 변경 필요
            const response = await fetch('/api/lessons');
            if (!response.ok) throw new Error('데이터를 불러오는데 실패했습니다.');
            lessonList = await response.json();
        } catch (error) {
            console.error('차시 데이터를 불러오는 중 오류 발생:', error);
            lessonList = dummyLessonList;
        }
        const subjectContainer = document.querySelector('.subject-container');
        if (!subjectContainer) return;

        subjectContainer.innerHTML = renderLessonAccordion(lessonList);

        // 아코디언 동작 이벤트
        document.querySelectorAll('.accordion-header').forEach(header => {
        header.onclick = function () {
            const accordion = this.parentElement;
            accordion.classList.toggle('open');
        };
});
        
    }

    function renderMainCourseCard() {
        const cardEl = document.getElementById('continue-course-card');
        if (!cardEl) return; // 요소가 없으면 아무것도 하지 않음
        cardEl.innerHTML = createCourseCard(courseData);
    }

    document.addEventListener('DOMContentLoaded', function() {
        renderMainCourseCard();
        loadLessons();
    });

function initContinueLearning() {
  const courseCardEl = document.getElementById('continue-course-card');
  const subjectContainer = document.querySelector('.subject-container');

  // SPA 가드 (아직 DOM 안 붙었으면 실행 안 함)
  if (!courseCardEl || !subjectContainer) return;

  // 기존 렌더링 제거 (재진입 대비)
  courseCardEl.innerHTML = '';
  subjectContainer.innerHTML = '';

  renderMainCourseCard();
  loadLessons();
}