// 영상 컨트롤 상태
let isPlaying = false;
let isMuted = true; // 초기 음소거 상태
let allVideos = [];
let isDragging = false;
let duration = 0;
let currentTime = 0;

// 비디오 그리드 생성 (5x4 = 20개)
function createVideoGrid() {
    const videoGrid = document.getElementById('videoGrid');
    const sampleVideo = '../sample-videos/sample-video.mp4';

    for (let i = 1; i <= 20; i++) {
        const videoItem = document.createElement('div');
        videoItem.className = 'video-item';
        videoItem.innerHTML = `
            <video data-video-id="${i}" muted>
                <source src="${sampleVideo}" type="video/mp4">
            </video>
            <div class="video-label">카메라 ${i}</div>
        `;

        // 더블클릭 이벤트 추가
        const video = videoItem.querySelector('video');
        video.addEventListener('dblclick', function() {
            openFullscreen(this.querySelector('source').src, i);
        });

        videoGrid.appendChild(videoItem);
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
        // 초기 설정 적용
        video.muted = isMuted;
        video.playbackRate = 1.0;
        
        video.play().catch(err => console.log('자동 재생 실패:', err));
        
        // timeupdate 이벤트로 타임라인 업데이트
        video.addEventListener('timeupdate', function() {
            if (!isDragging && this === allVideos[0]) {
                currentTime = this.currentTime;
                updateTimeline();
            }
        });
    });
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

// 제재 처리 저장
function saveSanction() {
    const student = document.getElementById('studentSelect').value;
    const memo = document.getElementById('adminMemo').value;
    const sanction = document.querySelector('input[name="sanction"]:checked').value;
    
    if (!student) {
        alert('학생을 선택해주세요.');
        return;
    }
    
    const sanctionText = {
        'none': '없음',
        'warning': '경고',
        'retest': '재시험',
        'invalid': '무효처리'
    };

    console.log('저장된 내용:');
    console.log('학생:', student);
    console.log('관리자 메모:', memo);
    console.log('제재 처리:', sanctionText[sanction]);
    
    alert(`저장되었습니다.\n학생: ${student}\n제재: ${sanctionText[sanction]}`);
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
