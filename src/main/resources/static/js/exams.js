
// 시험 시간 계산 및 업데이트
function updateTestTimes() {
    const now = new Date();
    const testItems = document.querySelectorAll('.test-item');

    testItems.forEach(item => {
        const startTime = new Date(item.dataset.start);
        const endTime = new Date(item.dataset.end);
        const timeContainer = item.querySelector('.test-time');
        const timeRemaining = timeContainer.querySelector('.time-remaining');
        const timeElapsed = timeContainer.querySelector('.time-elapsed');

        // 시험 종료
        if (now > endTime) {
            timeRemaining.textContent = '종료';
            timeRemaining.style.fontWeight = 'bold';
            timeRemaining.style.color = '#333';
            const duration = Math.floor((endTime - startTime) / 60000);
            timeElapsed.textContent = `소요시간: ${duration}분`;
            timeElapsed.style.fontWeight = 'normal';
            timeElapsed.style.color = '#999';
        }
        // 시험 예정
        else if (now < startTime) {
            timeRemaining.textContent = '평가 예정';
            timeRemaining.style.fontWeight = 'bold';
            timeRemaining.style.color = '#333';
            const diffMs = startTime - now;
            const hours = Math.floor(diffMs / 3600000);
            const minutes = Math.floor((diffMs % 3600000) / 60000);
            const seconds = Math.floor((diffMs % 60000) / 1000);
            const timeStr = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
            timeElapsed.textContent = `시작까지 ${timeStr}`;
            timeElapsed.style.fontWeight = 'normal';
            timeElapsed.style.color = '#999';
        }
        // 시험 중
        else {
            timeRemaining.textContent = '평가 중';
            timeRemaining.style.fontWeight = 'bold';
            timeRemaining.style.color = '#D32F2F';
            const remainingMs = endTime - now;
            const remainingMinutes = Math.ceil(remainingMs / 60000);
            timeElapsed.textContent = `남은시간: ${remainingMinutes}분`;
            timeElapsed.style.fontWeight = 'bold';
        }
    });
}

// 초기 업데이트 및 1초마다 갱신
updateTestTimes();
setInterval(updateTestTimes, 1000);

// 필터링 기능
document.getElementById('courseFilter').addEventListener('change', filterTests);
document.getElementById('statusFilter').addEventListener('change', filterTests);
document.getElementById('instructorFilter').addEventListener('change', filterTests);
document.getElementById('dateFilter').addEventListener('change', handleDateFilter);
document.getElementById('registrationDateFilter').addEventListener('change', handleRegistrationDateFilter);
document.getElementById('searchInput').addEventListener('input', filterTests);

// 시험 기간 필터 처리
function handleDateFilter() {
    const dateFilter = document.getElementById('dateFilter').value;
    const customDateRange = document.getElementById('customDateRange');
    
    if (dateFilter === 'custom') {
        customDateRange.style.display = 'flex';
    } else {
        customDateRange.style.display = 'none';
    }
    
    filterTests();
}

// 등록일 필터 처리
function handleRegistrationDateFilter() {
    const registrationDateFilter = document.getElementById('registrationDateFilter').value;
    const customRegistrationDateRange = document.getElementById('customRegistrationDateRange');
    
    if (registrationDateFilter === 'custom') {
        customRegistrationDateRange.style.display = 'flex';
    } else {
        customRegistrationDateRange.style.display = 'none';
    }
    
    filterTests();
}

// 날짜 범위 선택 시 필터링
document.getElementById('startDate')?.addEventListener('change', filterTests);
document.getElementById('endDate')?.addEventListener('change', filterTests);
document.getElementById('registrationStartDate')?.addEventListener('change', filterTests);
document.getElementById('registrationEndDate')?.addEventListener('change', filterTests);

function filterTests() {
    const courseFilter = document.getElementById('courseFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    const instructorFilter = document.getElementById('instructorFilter').value;
    const dateFilter = document.getElementById('dateFilter').value;
    const registrationDateFilter = document.getElementById('registrationDateFilter').value;
    const searchText = document.getElementById('searchInput').value.toLowerCase();

    const testItems = document.querySelectorAll('.test-item');

    testItems.forEach(item => {
        let showItem = true;

        // 과정명 필터
        if (courseFilter) {
            const courseName = item.querySelector('.course-name').textContent;
            if (!courseName.includes(courseFilter)) {
                showItem = false;
            }
        }

        // 응시현황 필터
        if (statusFilter) {
            const statusBadge = item.querySelector('.status-badge');
            if (!statusBadge.classList.contains(`status-${statusFilter}`)) {
                showItem = false;
            }
        }

        // 강사명 필터
        if (instructorFilter) {
            const instructorName = item.querySelector('.test-instructor').textContent;
            if (!instructorName.includes(instructorFilter)) {
                showItem = false;
            }
        }

        // 시험 기간 필터 (현재는 시각적으로만 구현)
        if (dateFilter && dateFilter !== 'custom') {
            // 실제 구현 시 각 테스트 항목의 날짜 데이터와 비교
            // 여기서는 예시로 모든 항목을 표시
        }

        // 커스텀 날짜 범위 필터
        if (dateFilter === 'custom') {
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            // 실제 구현 시 각 테스트 항목의 날짜와 비교
        }

        // 등록일 필터 (현재는 시각적으로만 구현)
        if (registrationDateFilter && registrationDateFilter !== 'custom') {
            // 실제 구현 시 각 테스트 항목의 등록일 데이터와 비교
        }

        // 커스텀 등록일 범위 필터
        if (registrationDateFilter === 'custom') {
            const registrationStartDate = document.getElementById('registrationStartDate').value;
            const registrationEndDate = document.getElementById('registrationEndDate').value;
            // 실제 구현 시 각 테스트 항목의 등록일과 비교
        }

        // 검색어 필터
        if (searchText) {
            const courseName = item.querySelector('.course-name').textContent.toLowerCase();
            const instructor = item.querySelector('.test-instructor').textContent.toLowerCase();
            const testName = item.querySelector('.test-name').textContent.toLowerCase();
            
            if (!courseName.includes(searchText) && 
                !instructor.includes(searchText) && 
                !testName.includes(searchText)) {
                showItem = false;
            }
        }

        item.style.display = showItem ? 'grid' : 'none';
    });
}
   // 시험 데이터 예시 (타임리프 연동시 서버에서 내려줄 예정)
    const testList = [
        {
            number: 1,
            courseName: '풀스택 웹 개발 (React & Node.js)',
            courseId: 'COURSE-2024-001',
            testName: 'JavaScript 기초 중간 평가',
            instructor: '김민수',
            date: '2026.01.14',
            time: '14:00 ~ 15:00',
            timeRemaining: '-',
            timeElapsed: '-',
            inProgress: 3,
            completed: 12,
            notStarted: 2,
            start: '2026-01-14T14:00:00',
            end: '2026-01-14T15:00:00'
        },
        {
            number: 2,
            courseName: '풀스택 웹 개발 (React & Node.js)',
            courseId: 'COURSE-2024-001',
            testName: 'React 컴포넌트 심화',
            instructor: '김민수',
            date: '2026.01.12',
            time: '10:00 ~ 11:00',
            timeRemaining: '-',
            timeElapsed: '-',
            inProgress: 0,
            completed: 25,
            notStarted: 0,
            start: '2026-01-12T10:00:00',
            end: '2026-01-12T11:00:00'
        },
        {
            number: 3,
            courseName: '데이터 사이언스 기초',
            courseId: 'COURSE-2024-005',
            testName: 'Python 기본 문법 테스트',
            instructor: '이영희',
            date: '2026.01.15',
            time: '13:00 ~ 14:00',
            timeRemaining: '-',
            timeElapsed: '-',
            inProgress: 0,
            completed: 0,
            notStarted: 30,
            start: '2026-01-15T13:00:00',
            end: '2026-01-15T14:00:00'
        },
        {
            number: 4,
            courseName: '클라우드 아키텍처',
            courseId: 'COURSE-2024-012',
            testName: 'AWS 서비스 이해도 평가',
            instructor: '박철수',
            date: '2026.01.14',
            time: '15:00 ~ 16:00',
            timeRemaining: '-',
            timeElapsed: '-',
            inProgress: 5,
            completed: 18,
            notStarted: 4,
            start: '2026-01-14T15:00:00',
            end: '2026-01-14T16:00:00'
        },
        {
            number: 5,
            courseName: '데이터 사이언스 기초',
            courseId: 'COURSE-2024-005',
            testName: '데이터 분석 실습 평가',
            instructor: '이영희',
            date: '2026.01.14',
            time: '09:00 ~ 10:30',
            timeRemaining: '-',
            timeElapsed: '-',
            inProgress: 0,
            completed: 28,
            notStarted: 2,
            start: '2026-01-14T09:00:00',
            end: '2026-01-14T10:30:00'
        }
    ];

    function getTestLink(test) {
        // body의 data-user-role에 따라 링크 반환
        const role = document.body.getAttribute('data-user-role');
        if (role === 'instructor') {
            return 'proctor/recordings.html';
        }
        // 기본은 admin
        return 'recordings.html';
    }

    function renderTestList() {
        const container = document.getElementById('testListContainer');
        if (!container) return;
            container.innerHTML = testList.map(test => `
                <div class="test-item" data-start="${test.start}" data-end="${test.end}" data-test-number="${test.number}" style="cursor:pointer;">
                    <div class="test-number">${test.number}</div>
                    <div class="test-course">
                        <span class="course-name">${test.courseName}</span>
                        <span class="course-id">${test.courseId}</span>
                    </div>
                    <div class="test-name">${test.testName}</div>
                    <div class="test-instructor">${test.instructor}</div>
                    <div class="test-date">
                        <span style="font-size: 14px; color: #333;">${test.date}</span>
                        <span style="font-size: 13px; color: #666;">${test.time}</span>
                    </div>
                    <div class="test-time">
                        <span class="time-remaining">${test.timeRemaining}</span>
                        <span class="time-elapsed">${test.timeElapsed}</span>
                    </div>
                    <div class="test-status">
                        <span class="status-count">응시중 <span class="in-progress">${test.inProgress}명</span> / 완료 <span class="completed">${test.completed}명</span> / 미응시 <span class="not-started">${test.notStarted}명</span></span>
                    </div>
                </div>
            `).join('');

            // test-item 클릭 이벤트 연결
            document.querySelectorAll('.test-item').forEach(item => {
                item.addEventListener('click', function() {
                    const num = parseInt(this.getAttribute('data-test-number'));
                    const test = testList.find(t => t.number === num);
                    const link = getTestLink(test);
                    if (link) window.location.href = link;
                });
            });
    }

    document.addEventListener('DOMContentLoaded', renderTestList);