package com.ssa.lms.content.service;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.content.web.ContentForm;
import com.ssa.lms.content.web.ContentView;
import com.ssa.lms.content.web.SessionOption;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 강사 콘텐츠(VOD/문서) CRUD + 업로드. 진도(Progress) 는 {@link ProgressService} 가 담당한다.
 *
 * <p>과정(Course)/차시(Session) 는 A 과정 트랙이 소유한 엔티티를 <b>읽기만</b> 한다(연결 FK 설정용).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;

    public List<Content> findByCourse(Long courseId) {
        return contentRepository.findByCourseIdOrderByOrderNoAscIdAsc(courseId);
    }

    public Content get(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));
    }

    /**
     * 강사 콘텐츠 목록 뷰. courseId 가 있으면 해당 과정만, 없으면 전체.
     * lazy 연관(course/session)을 트랜잭션 안에서 스칼라로 뽑아 {@link ContentView} 로 반환한다.
     */
    public List<ContentView> listViews(Long courseId) {
        List<Content> contents = (courseId != null)
                ? contentRepository.findByCourseIdOrderByOrderNoAscIdAsc(courseId)
                : contentRepository.findAll();
        return contents.stream().map(ContentView::of).toList();
    }

    /** 상세/편집 표시용 단건 뷰. */
    public ContentView view(Long id) {
        return ContentView.of(get(id));
    }

    /** 수정 폼 — lazy 연관 접근이 트랜잭션 안에서 일어나도록 서비스에서 폼을 만든다. */
    public ContentForm editForm(Long id) {
        return ContentForm.from(get(id));
    }

    /** 등록/수정 폼의 과정 선택 목록 (시작일 최신순). */
    public List<Course> courseOptions() {
        return courseRepository.findAllByOrderByStartDateDesc();
    }

    /** 전체 차시 선택 옵션 — 과정 선택에 따라 클라이언트에서 필터링한다. */
    public List<SessionOption> sessionOptions() {
        List<SessionOption> options = new ArrayList<>();
        for (Course course : courseRepository.findAllByOrderByStartDateDesc()) {
            List<Session> sessions =
                    sessionRepository.findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(course.getId());
            for (Session s : sessions) {
                options.add(new SessionOption(s.getId(), course.getId(), s.getSeq() + "차시 - " + s.getName()));
            }
        }
        return options;
    }

    /**
     * 콘텐츠 등록 — 업로드 파일 필수.
     *
     * @return 저장된 콘텐츠 id
     */
    @Transactional
    public Long create(ContentForm form, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("콘텐츠 파일을 첨부하세요.");
        }
        Course course = courseRepository.findById(form.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("과정을 찾을 수 없습니다: " + form.getCourseId()));
        Session session = resolveSession(form.getSessionId());

        StoredFile stored = fileStorageService.store(file);
        Content content = Content.builder()
                .course(course)
                .session(session)
                .type(form.getType())
                .title(form.getTitle())
                .description(form.getDescription())
                .fileUrl(stored.fileUrl())
                .originalFileName(stored.originalFileName())
                .fileSize(stored.size())
                .mimeType(stored.mimeType())
                .durationSeconds(form.getDurationSeconds())
                .pageCount(form.getPageCount())
                .orderNo(form.getOrderNo())
                .required(form.getRequired())
                .status(form.getStatus())
                .build();
        return contentRepository.save(content).getId();
    }

    /**
     * 콘텐츠 수정 — 메타 갱신, 파일은 새로 첨부된 경우에만 교체(이전 파일은 삭제).
     */
    @Transactional
    public void update(Long id, ContentForm form, MultipartFile file) {
        Content content = get(id);
        Session session = resolveSession(form.getSessionId());
        content.update(session, form.getTitle(), form.getDescription(),
                form.getDurationSeconds(), form.getPageCount(),
                form.getOrderNo(), form.getRequired(), form.getStatus());

        if (file != null && !file.isEmpty()) {
            String oldUrl = content.getFileUrl();
            StoredFile stored = fileStorageService.store(file);
            content.replaceFile(stored.fileUrl(), stored.originalFileName(), stored.size(), stored.mimeType());
            fileStorageService.deleteByUrl(oldUrl);
        }
    }

    @Transactional
    public void changeStatus(Long id, ContentStatus status) {
        get(id).changeStatus(status);
    }

    /** soft delete — 실제 파일은 3년 보존 요건상 삭제하지 않는다(레코드만 마킹). */
    @Transactional
    public void delete(Long id) {
        contentRepository.delete(get(id));
    }

    private Session resolveSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("차시를 찾을 수 없습니다: " + sessionId));
    }
}
