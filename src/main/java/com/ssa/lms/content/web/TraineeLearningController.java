package com.ssa.lms.content.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.service.ContentService;
import com.ssa.lms.content.service.ProgressService;
import com.ssa.lms.demo.SampleScreenData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 훈련생 콘텐츠 학습 화면 — 학습 콘텐츠 목록/차시별 학습/시청(VOD·문서) 페이지.
 * 진도 저장은 {@link ProgressApiController}(REST) 가 담당한다.
 *
 * <p>경로 {@code /trainee/**} 는 SecurityConfig catch-all(TRAINEE,ADMIN)로 커버된다.</p>
 */
@Controller
@RequestMapping("/trainee")
@RequiredArgsConstructor
public class TraineeLearningController {

    private static final String VIEW_DIR = "trainee/";

    private final ProgressService progressService;
    private final ContentService contentService;
    private final SampleScreenData sampleData;

    /** 내 학습 콘텐츠 전체 목록(승인/수료 과정). */
    @GetMapping("/contents")
    public String contents(@AuthenticationPrincipal LoginUser user, Model model) {
        var filled = sampleData.fill(
                progressService.myLearningContents(user.getId()), sampleData::learningContents);
        model.addAttribute("contents", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        return VIEW_DIR + "contents";
    }

    /** 차시별 학습(아코디언) — 과정 선택. courseId 미지정 시 학습 중인 첫 과정. */
    @GetMapping("/learning")
    public String learning(@RequestParam(required = false) Long courseId,
                           @AuthenticationPrincipal LoginUser user, Model model) {
        var courses = progressService.enrolledCourseOptions(user.getId());
        Long targetCourseId = courseId != null ? courseId
                : (courses.isEmpty() ? null : courses.get(0).id());
        var groups = targetCourseId != null
                ? progressService.learningGroups(user.getId(), targetCourseId)
                : java.util.List.<LearningSessionGroup>of();
        int progress = targetCourseId != null
                ? progressService.courseProgressRatio(user.getId(), targetCourseId) : 0;

        // 볼 콘텐츠가 하나도 없으면 예시로 채운다. 과정 자체가 없을 때만 과정 목록도 예시로 바꾼다
        // — 실제 수강 과정이 있다면 선택 상자는 본인 과정을 그대로 보여주는 게 맞다.
        boolean sample = groups.isEmpty() && sampleData.isEnabled();
        if (sample) {
            groups = sampleData.learningGroups();
            progress = sampleData.courseProgress();
            if (courses.isEmpty()) {
                courses = sampleData.courseOptions();
                targetCourseId = SampleScreenData.SAMPLE_COURSE_ID;
            }
        }

        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", targetCourseId);
        model.addAttribute("groups", groups);
        model.addAttribute("courseProgress", progress);
        model.addAttribute("sampleData", sample);
        return VIEW_DIR + "learning";
    }

    /**
     * 콘텐츠 시청(재생/열람) — 유형별 페이지로 분기, 이어보기용 진도 동봉.
     *
     * <p>예시 id 로 들어오면 DB 를 보지 않고 예시 콘텐츠를 렌더한다. 그러지 않으면 목록의
     * 「학습하기」를 눌렀을 때 존재하지 않는 콘텐츠를 찾다가 오류 화면으로 떨어진다.</p>
     */
    @GetMapping("/contents/{id}/play")
    public String play(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        ContentView sample = sampleData.contentDetail(id);
        ContentView content = sample != null ? sample : contentService.view(id);
        model.addAttribute("content", content);
        model.addAttribute("progress", sample != null
                ? sampleData.contentProgress(id) : progressService.getProgress(user.getId(), id));
        model.addAttribute("sampleData", sample != null);
        if (sample != null && sample.type() == ContentType.VIDEO) {
            // 예시 VOD 는 재생할 파일이 없다 — 강의 화면을 poster 로 깔아 플레이어를 채운다.
            model.addAttribute("posterUrl", SampleScreenData.SAMPLE_VIDEO_POSTER);
        }
        return VIEW_DIR + (content.type() == ContentType.DOCUMENT ? "play-document" : "play-video");
    }
}
