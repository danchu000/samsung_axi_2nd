// 영상 컨트롤 상태
let isPlaying = false;
let isMuted = true; // 초기 음소거 상태
let allVideos = [];
let isDragging = false;
let duration = 0;
let currentTime = 0;

// 서버가 내려준 녹화 목록. 없으면(정적 미리보기) 기존 20분할 샘플 더미로 폴백한다.
const recordingRows = window._serverRecordingRows || null;
let selectedRecording = null;

// 녹화 그리드 생성 — 서버 데이터가 있으면 녹화 1건당 타일 1개
function createVideoGrid() {
    const videoGrid = document.getElementById('videoGrid');
    if (!videoGrid) return;
    const sampleVideo = '../sample-videos/sample-video.mp4';

    if (recordingRows) {
        if (!recordingRows.length) {
            videoGrid.innerHTML = '<p style="color:#888; padding:20px;">조회된 녹화가 없습니다.</p>';
            return;
        }
        videoGrid.innerHTML = recordingRows.map((r, i) => `
            <div class="video-item" data-recording-id="${r.recordingId}" data-index="${i}">
                ${r.playable
                    ? `<video data-video-id="${r.recordingId}" muted preload="metadata"><source src="${r.streamUrl}" type="video/mp4"></video>`
                    : `<div style="width:100%;height:100%;min-height:140px;display:flex;align-items:center;justify-content:center;color:#888;background:#111;">${r.statusLabel}</div>`}
                <div class="video-label">${r.traineeName} · ${r.statusLabel}</div>
            </div>
        `).join('');

        videoGrid.querySelectorAll('.video-item').forEach(item => {
            const row = recordingRows[parseInt(item.dataset.index, 10)];
            item.addEventListener('click', () => showDetail(row));
            const video = item.querySelector('video');
            if (video) {
                video.addEventListener('dblclick', function () {
                    showDetail(row);
                    openFullscreen(row.streamUrl, row.traineeName);
                });
            }
        });
        showDetail(recordingRows[0]);
    } else {
        for (let i = 1; i <= 20; i++) {
            const videoItem = document.createElement('div');
            videoItem.className = 'video-item';
            videoItem.innerHTML = `
                <video data-video-id="${i}" muted>
                    <source src="${sampleVideo}" type="video/mp4">
                </video>
                <div class="video-label">카메라 ${i}</div>
            `;
            const video = videoItem.querySelector('video');
            video.addEventListener('dblclick', function() {
                openFullscreen(this.querySelector('source').src, i);
            });
            videoGrid.appendChild(videoItem);
        }
    }

    // 모든 비디오 참조 저장
    allVideos = Array.from(document.querySelectorAll('.video-grid video'));

    // 첫 번째 비디오로 duration 설정
    if (allVideos.length > 0) {
        allVideos[0].addEventListener('loadedmetadata', function() {
            duration = this.duration;
            updateDurationDisplay();
            createTimelineMarkers();
        });
    }

    // 모든 비디오 자동 재생 (음소거 상태)
    allVideos.forEach(video => {
        video.muted = isMuted;
        video.playbackRate = 1.0;

        video.play().catch(err => console.log('자동 재생 실패:', err));

        video.addEventListener('timeupdate', function() {
            if (!isDragging && this === allVideos[0]) {
                currentTime = this.currentTime;
                updateTimeline();
            }
        });
    });
}

// 우측 "녹화 상세" 패널 채우기
function showDetail(row) {
    if (!row) return;
    selectedRecording = row;
    const set = (id, value) => {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    };
    set('detailCourse', `${row.courseName} (${row.courseId})`);
    set('detailExam', row.examName);
    set('detailTrainee', row.traineeName);
    set('detailRecordedAt', row.recordedAt);
    set('detailDuration', row.durationLabel);
    set('detailStatus', `${row.sizeLabel} / ${row.statusLabel}`);
    set('detailEvents', `${row.warnCount} / ${row.criticalCount}`);

    const select = document.getElementById('studentSelect');
    if (select) select.value = String(row.attemptId);
}

// 타임라인 마커 생성
function createTimelineMarkers() {
    const markersContainer = document.getElementById('timelineMarkers');
    const markerCount = 10;
    markersContainer.innerHTML = '';
    
    for (let i = 0; i <= markerCount; i++) {
        const time = (duration / markerCount) * i;
        const marker = document.createElement('div');
        marker.className = 'timeline-marker';
        marker.textContent = formatTime(time);
        markersContainer.appendChild(marker);
    }
}

// 시간 포맷 (mm:ss)
function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// Duration 표시 업데이트
function updateDurationDisplay() {
    document.getElementById('totalDuration').textContent = formatTime(duration);
}

// 타임라인 업데이트
function updateTimeline() {
    const percentage = (currentTime / duration) * 100;
    document.getElementById('timelineProgress').style.width = percentage + '%';
    document.getElementById('timelineHandle').style.left = percentage + '%';
    document.getElementById('currentTime').textContent = formatTime(currentTime);
}

// 타임라인 클릭/드래그로 시간 이동
function seekToPosition(clientX) {
    const timelineBar = document.getElementById('timelineBar');
    const rect = timelineBar.getBoundingClientRect();
    const percentage = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const newTime = percentage * duration;
    
    // 모든 비디오 시간 동기화
    allVideos.forEach(video => {
        video.currentTime = newTime;
    });
    
    currentTime = newTime;
    updateTimeline();
}

// 타임라인 이벤트 리스너
document.getElementById('timelineBar').addEventListener('mousedown', function(e) {
    isDragging = true;
    seekToPosition(e.clientX);
});

document.addEventListener('mousemove', function(e) {
    if (isDragging) {
        seekToPosition(e.clientX);
    }
});

document.addEventListener('mouseup', function() {
    isDragging = false;
});

document.getElementById('timelineHandle').addEventListener('mousedown', function(e) {
    e.stopPropagation();
    isDragging = true;
});

// 전체화면으로 비디오 열기
function openFullscreen(videoSrc, cameraId) {
    const fullscreenDiv = document.getElementById('fullscreenVideo');
    const fullscreenPlayer = document.getElementById('fullscreenPlayer');
    
    // Sidebar 상태 확인
    const sidebar = document.querySelector('.sidebar');
    const isSidebarCollapsed = sidebar.classList.contains('collapsed');
    const sidebarWidth = isSidebarCollapsed ? 60 : 240;
    
    // 전체화면 위치 및 크기 조정 (right-sidebar 400px 포함)
    fullscreenDiv.style.left = sidebarWidth + 'px';
    fullscreenDiv.style.width = `calc(100vw - ${sidebarWidth}px - 400px)`;
    
    fullscreenPlayer.querySelector('source').src = videoSrc;
    fullscreenPlayer.load();
    fullscreenPlayer.play();
    fullscreenDiv.classList.add('active');
    
}

// 전체화면 닫기
function closeFullscreen() {
    const fullscreenDiv = document.getElementById('fullscreenVideo');
    const fullscreenPlayer = document.getElementById('fullscreenPlayer');
    
    fullscreenPlayer.pause();
    fullscreenDiv.classList.remove('active');
}

// ESC 키로 전체화면 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeFullscreen();
    }
});

// 페이지 로드 시 비디오 그리드 생성
window.addEventListener('DOMContentLoaded', function() {
    createVideoGrid();
});

// 재생/일시정지 버튼
document.getElementById('playPauseBtn').addEventListener('click', function() {
    isPlaying = !isPlaying;
    this.textContent = isPlaying ? '⏸' : '▶';
    
    // 모든 비디오 재생/일시정지 동기화
    allVideos.forEach(video => {
        if (isPlaying) {
            video.play();
        } else {
            video.pause();
        }
    });
});

// 이전 버튼
document.getElementById('prevBtn').addEventListener('click', function() {
    console.log('이전 영상');
    // 이전 영상으로 이동 로직 추가
});

// 다음 버튼
document.getElementById('nextBtn').addEventListener('click', function() {
    console.log('다음 영상');
    // 다음 영상으로 이동 로직 추가
});

// 배속 조절
document.getElementById('playbackSpeed').addEventListener('change', function() {
    const speed = parseFloat(this.value);
    
    // 모든 비디오 배속 동기화
    allVideos.forEach(video => {
        video.playbackRate = speed;
    });
});

// 음소거 버튼
document.getElementById('muteBtn').addEventListener('click', function() {
    isMuted = !isMuted;
    this.textContent = isMuted ? '🔇' : '🔊';
    
    // 모든 비디오 음소거 동기화
    allVideos.forEach(video => {
        video.muted = isMuted;
    });
});

// 제재 처리 저장 — 서버로 실제 전송한다.
//  경고    → ProctorWarning INSERT (관리자·강사 모두 가능)
//  무효처리 → ExamAttempt.voidAttempt (관리자만. 강사가 누르면 서버가 403 을 준다)
//  재시험  → 재응시 정책은 시험(Exam.retakeAllowed) 소관이라 이 슬라이스 범위 밖이다
function saveSanction() {
    const attemptId = document.getElementById('studentSelect').value;
    const memo = document.getElementById('adminMemo').value;
    const sanction = document.querySelector('input[name="sanction"]:checked').value;

    if (!attemptId) {
        alert('학생을 선택해주세요.');
        return;
    }
    if (sanction === 'none') {
        alert('제재 항목을 선택해주세요.');
        return;
    }
    if (sanction === 'retest') {
        alert('재시험 처리는 시험 설정(재응시 허용)에서 진행합니다. 이 화면에서는 지원하지 않습니다.');
        return;
    }

    const prefix = window._proctorAttemptPrefix;
    if (!prefix) {
        alert('서버 연동 정보가 없습니다.');
        return;
    }
    const action = sanction === 'warning' ? 'warning' : 'void';
    if (action === 'void' && !confirm('이 응시를 무효 처리합니다. 계속할까요?')) {
        return;
    }

    const form = document.createElement('form');
    form.method = 'post';
    form.action = `${prefix}${attemptId}/${action}`;
    form.style.display = 'none';

    const add = (name, value) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    };
    add(action === 'warning' ? 'message' : 'reason', memo);
    add('redirect', window._proctorRedirect || '');

    const csrfToken = (document.querySelector('meta[name="_csrf"]') || {}).content;
    if (csrfToken) add('_csrf', csrfToken);

    document.body.appendChild(form);
    form.submit();
}

// 전체화면 버튼
document.getElementById('fullscreenBtn').addEventListener('click', function() {
    console.log('전체화면');
    // 전체화면 전환 로직 추가
    if (document.fullscreenElement) {
        document.exitFullscreen();
    } else {
        document.documentElement.requestFullscreen();
    }
});
