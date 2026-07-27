// myCourse.js - 나의 과정 페이지용 스크립트

// 서버에서 내려주는 과정 데이터 (타임리프 변수로 치환 예정)
// 예: <script>window.courseData = /*[[${courseList}]]*/ [];</script>
const courseData = window.courseData || [
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
    },
    {
        id: 2,
        category: '인공지능',
        status: '종료',
        title: 'AI 엔지니어링',
        instructor: '김철수',
        subjects: 10,
        totalHours: 80,
        students: 30,
        progress: 100,
        period: '2023-05-01 ~ 2023-10-31',
        buttonText: '리뷰보기'
    },
    {
        id: 3,
        category: '데이터',
        status: '준비중',
        title: '데이터 사이언스 기초',
        instructor: '박영희',
        subjects: 8,
        totalHours: 64,
        students: 0,
        progress: 0,
        period: '2024-03-01 ~ 2024-08-31',
        buttonText: '대기중'
    },
    {
        id: 4,
        category: '보안',
        status: '운영중',
        title: '사이버 보안 전문가',
        instructor: '이철수',
        subjects: 15,
        totalHours: 120,
        students: 28,
        progress: 45,
        period: '2024-02-15 ~ 2024-07-15',
        buttonText: '학습하기'
    }
];

// 카드 생성 함수
function createCourseCard(course) {
    // status에 따른 badge 클래스 매핑
    const statusClassMap = {
        '운영중': 'status-completed',
        '준비중': 'status-pending',
        '종료': 'status-disabled'
    };
    
    const statusClass = statusClassMap[course.status] || 'status-disabled';
    
    const card = document.createElement('div');
    card.className = 'course-card';
    card.innerHTML = `
        <div class="course-card-header">
            <span class="course-category badge-kdt">${course.category}</span>
        </div>
        <h3 class="course-title">${course.title}</h3>
        <p class="course-instructor">${course.instructor} 강사</p>
        <div class="course-stats">
        <div class="stat-item">
        <div class="stat-label">과목</div>
        <div class="stat-value">${course.subjects}개</div>
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
            <button class="course-button">${course.buttonText}</button>
        </div>
    `;
    // 버튼 클릭 시 이동
    const btn = card.querySelector('.course-button');
    if (btn) {
        btn.addEventListener('click', function() {
            window.location.href = `continue-learning.html`;
        });
    }
    return card;
}
// 남은 기간 계산 함수
function getDaysLeft(period) {
    // period: '2024-01-10 ~ 2024-06-30' 형식
    const endDateStr = period.split('~')[1]?.trim();
    if (!endDateStr) return '-';
    const endDate = new Date(endDateStr);
    const today = new Date();
    // 시간 차이 계산 (밀리초)
    const diffMs = endDate - today;
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return '종료';
    return `${diffDays}일 남음`;
}
// 전역 변수로 현재 데이터 저장
let allCourses = [];
let filteredCourses = [];

// 카드 렌더링 함수
async function renderCourseCards(courses = null) {
    const container = document.querySelector('.main-content .main-wrap');
    if (!container) return;

    // 기존 카드 제거
    container.innerHTML = '';

    const dataToRender = courses || filteredCourses;
    
    // 카드 생성 및 추가
    dataToRender.forEach(course => {
        const card = createCourseCard(course);
        // createCourseCard가 문자열을 반환하면 DOM 노드로 변환
        if (typeof card === 'string') {
            const temp = document.createElement('div');
            temp.innerHTML = card;
            container.appendChild(temp.firstElementChild);
        } else {
            container.appendChild(card);
        }
    });
}

// 필터링 함수
function filterCourses() {
    const categoryFilter = document.getElementById('categoryFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    
    filteredCourses = allCourses.filter(course => {
        const categoryMatch = categoryFilter === 'all' || course.category === categoryFilter;
        const statusMatch = statusFilter === 'all' || course.status === statusFilter;
        return categoryMatch && statusMatch;
    });
    
    renderCourseCards();
}

// 필터 초기화 함수
function resetFilters() {
    document.getElementById('categoryFilter').value = 'all';
    document.getElementById('statusFilter').value = 'all';
    filteredCourses = [...allCourses];
    renderCourseCards();
}

// 데이터 로드 및 초기화 (타임리프 변수 사용)
function loadCourses() {
    // window.courseData가 있으면 사용, 없으면 기본값
    allCourses = courseData;
    filteredCourses = [...allCourses];
    renderCourseCards();
}

// 이벤트 리스너 설정
function setupFilters() {
    document.getElementById('categoryFilter').addEventListener('change', filterCourses);
    document.getElementById('statusFilter').addEventListener('change', filterCourses);
    document.getElementById('resetFilter').addEventListener('click', resetFilters);
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    loadCourses();
    setupFilters();
});

function initMyCourse() {
  loadCourses();
  setupFilters();
}