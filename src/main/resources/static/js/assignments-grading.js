/* ===== 서버 연동 (개발자 B) =====
   컨트롤러로 전환된 페이지는 인라인 스크립트로 아래를 먼저 채운다:
     window._serverGradingStudents : GradingStudentRow[] (미제출자 포함, 각 행에 history[] 포함)
   값이 없으면 아래 더미로 떨어진다 (아직 전환 안 된 정적 페이지 하위호환).
   변경 이력은 학생마다 다르므로 전역 배열이 아니라 선택된 행의 history 를 쓴다. */

// [변경 이력] 더미 데이터 (서버 미연동 시 폴백)
const gradingHistoryDummy = [
    {
        date: '2026-01-10',
        instructor: '홍길동(강사)',
        score: '65 → 75',
        result: '통과',
        reason: '기준 미달 예외 승인'
    }
    // 추후 서버 데이터로 대체
];
// [학생 목록] 더미 데이터 (서버 미연동 시 폴백)
const gradingStudentDummy = [
    {
        no: 1,
        name: '홍길동',
        submitStatus: '제출',
        gradingStatus: '미채점',
        score: '-',
        resubmit: '0회',
        btnType: 'grading', // 채점
        birth: '1998.01.01',
        course: '풀스택 웹 개발 3기',
        assignment: 'REST API 구현 과제',
        submitDate: '2026-01-09 23:12'
    },
    {
        no: 2,
        name: '김철수',
        submitStatus: '미제출',
        gradingStatus: '-',
        score: '-',
        resubmit: '0회',
        btnType: 'disabled', // 비활성화
        birth: '1999.05.21',
        course: '풀스택 웹 개발 3기',
        assignment: 'REST API 구현 과제',
        submitDate: '-'
    },
    {
        no: 3,
        name: '이영희',
        submitStatus: '제출',
        gradingStatus: '채점완료',
        score: '85',
        resubmit: '1회',
        btnType: 'edit', // 수정
        birth: '2000.11.15',
        course: '풀스택 웹 개발 3기',
        assignment: 'REST API 구현 과제',
        submitDate: '2026-01-08 21:30'
    }
];

const gradingStudentData = window._serverGradingStudents || gradingStudentDummy;

// 학생 신상 더미 데이터 (서버 미연동 시 폴백)
const studentInfoData = {
    name: '김민수',
    birth: '1999.03.12',
    course: '풀스택 웹 개발 3기',
    assignment: 'REST API 구현 과제',
    submitDate: '2026-01-09 23:12',
    resubmit: '1회',
    score: '75점'
};

function renderStudentInfoTable(data) {
    const table = document.getElementById('studentInfoTable');
    if (!table) return;
    table.innerHTML = `
        <tbody>
            <tr>
                <th style="width:90px;">학생</th>
                <td>
                    <!-- birth 자리에는 생년월일이 아니라 loginId 가 온다.
                         User.birthDate 는 AES 암호문이고 채점 화면에 나갈 이유도 없다. -->
                    ${data.name} (${data.birth})
                </td>
                <th style="width:90px;">과정</th>
                <td>
                    ${data.course}
                </td>
            </tr>
            <tr>
                <th>과제</th>
                <td>${data.assignment}</td>
                <th>제출일</th>
                <td>${data.submitDate}</td>
            </tr>
            <tr>
                <th>재제출횟수</th>
                <td>${data.resubmit}</td>
                <th>점수</th>
                <td>${data.score}</td>
            </tr>
        </tbody>
    `;
}
// 채점 모달(팝업) 오픈 및 학생신상 렌더링
document.getElementById('gradingStudentTbody').addEventListener('click', function(e) {
    // 채점/수정 버튼 클릭 시 모달 오픈
    if (e.target.classList.contains('grading-open-modal-btn')) {
        // 서버 연동 시 행마다 실제 채점 URL 이 붙어 있다 (data-url).
        const url = e.target.getAttribute('data-url') || 'grading-modal.html';
        window.open(url, 'gradingModal', 'width=950,height=700,scrollbars=yes');
        e.stopPropagation();
        return;
    }
    // tr 클릭 시 학생 정보 변경
    let tr = e.target.closest('tr');
    if (tr && tr.hasAttribute('data-idx')) {
        const idx = tr.getAttribute('data-idx');
        const student = gradingStudentData[idx];
        renderStudentInfoTable({
            name: student.name,
            birth: student.birth,
            course: student.course,
            assignment: student.assignment,
            submitDate: student.submitDate,
            resubmit: student.resubmit,
            score: student.score
        });
        // 변경 이력은 학생별이라 행을 바꿀 때마다 다시 그린다.
        renderGradingHistoryTable(student.history || []);
    }
});

function renderGradingStudentTable(data) {
    const tbody = document.getElementById('gradingStudentTbody');
    if (!tbody) return;
    tbody.innerHTML = data.map((item, idx) => {
        let btnHtml = '';
        const url = item.gradingUrl || '';
        if (item.btnType === 'grading') {
            btnHtml = '<button class="btn btn-primary btn-sm grading-open-modal-btn" data-url="' + url + '">채점</button>';
        } else if (item.btnType === 'disabled') {
            btnHtml = '<button class="btn btn-secondary btn-sm" disabled>채점</button>';
        } else if (item.btnType === 'edit') {
            btnHtml = '<button class="btn btn-gray btn-sm grading-open-modal-btn" data-url="' + url + '">수정</button>';
        }
        return `
            <tr data-idx="${idx}">
                <td>${item.no}</td>
                <td>${item.name}</td>
                <td>${item.submitStatus}${item.late ? ' <span style="color:#b42318;">(지각)</span>' : ''}</td>
                <td>${item.gradingStatus}</td>
                <td>${item.score}</td>
                <td>${item.resubmit}</td>
                <td>${btnHtml}</td>
            </tr>
        `;
    }).join('');
}

function renderGradingHistoryTable(data) {
    const tbody = document.getElementById('gradingHistoryTbody');
    if (!tbody) return;
    tbody.innerHTML = data.map(item => `
        <tr>
            <td>
                ${item.date}
            </td>
            <td>
                ${item.instructor}
            </td>
            <td>
                ${item.score}
            </td>
            <td>
                ${item.result}
            </td>
            <td>
                ${item.reason}
            </td>
        </tr>
    `).join('');
}

// 페이지 로드 시 렌더링
document.addEventListener('DOMContentLoaded', function() {
    if (!gradingStudentData.length) {
        renderGradingStudentTable([]);
        renderGradingHistoryTable([]);
        return;
    }
    renderStudentInfoTable(gradingStudentData[0]);
    renderGradingStudentTable(gradingStudentData);
    renderGradingHistoryTable(gradingStudentData[0].history || gradingHistoryDummy);
});
