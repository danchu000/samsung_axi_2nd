package com.ssa.lms.ai;

import com.ssa.lms.ai.service.ContentTextExtractor;
import com.ssa.lms.content.entity.Content;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * [기능 3] 학습 자료 본문 추출을 고정한다.
 *
 * <p>여기서 막으려는 것은 두 가지다.
 * <ul>
 *   <li><b>본문을 못 읽는데 조용히 넘어가는 것</b> — 그러면 모델이 제목만 보고 답하는데
 *       화면에는 근거가 있는 것처럼 보인다</li>
 *   <li><b>업로드 폴더 밖 파일이 읽히는 것</b> — 서버 파일이 모델 답변으로 새어 나간다</li>
 * </ul>
 */
class ContentTextExtractorTest {

    /** 실제 서비스 URL 규약({@code ContentStorageConfig.URL_PREFIX})을 그대로 쓴다. */
    private Content pdfContent(String url) {
        Content c = mock(Content.class);
        when(c.getFileUrl()).thenReturn(url);
        when(c.getOriginalFileName()).thenReturn("sample.pdf");
        return c;
    }

    /** 테스트용 PDF 를 실제로 만든다 — 가짜 바이트로는 파서가 도는지 알 수 없다. */
    private void writePdf(Path file, String text) throws Exception {
        Files.createDirectories(file.getParent());
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(file.toFile());
        }
    }

    @Test
    @DisplayName("PDF 본문을 실제로 읽어 온다")
    void PDF_본문을_읽는다(@TempDir Path dir) throws Exception {
        writePdf(dir.resolve("2026/a.pdf"), "Transaction isolation level explained");
        ContentTextExtractor ex = new ContentTextExtractor(dir.toString());

        String text = ex.textOf(pdfContent("/content/files/2026/a.pdf"));

        assertThat(text)
                .as("본문을 못 읽으면 모델이 제목만 보고 답하는데 화면에는 근거가 있는 것처럼 보인다")
                .contains("Transaction isolation");
    }

    @Test
    @DisplayName("업로드 폴더 밖 경로는 읽지 않는다 — 서버 파일이 답변으로 새면 안 된다")
    void 폴더_밖_경로_차단(@TempDir Path dir) throws Exception {
        Path secret = dir.getParent().resolve("secret.pdf");
        writePdf(secret, "TOP SECRET");
        ContentTextExtractor ex = new ContentTextExtractor(dir.toString());

        String text = ex.textOf(pdfContent("/content/files/../secret.pdf"));

        assertThat(text).isEmpty();
    }

    @Test
    @DisplayName("PDF 가 아닌 자료는 열지 않는다 — 영상·이미지는 본문이 없다")
    void PDF가_아니면_건너뛴다(@TempDir Path dir) {
        Content video = mock(Content.class);
        when(video.getFileUrl()).thenReturn("/content/files/2026/a.mp4");
        when(video.getOriginalFileName()).thenReturn("lecture.mp4");

        assertThat(new ContentTextExtractor(dir.toString()).textOf(video)).isEmpty();
    }

    @Test
    @DisplayName("파일이 없어도 예외를 던지지 않는다 — 자료 하나가 깨졌다고 질문이 실패하면 안 된다")
    void 없는_파일도_조용히_넘어간다(@TempDir Path dir) {
        assertThat(new ContentTextExtractor(dir.toString())
                .textOf(pdfContent("/content/files/2026/none.pdf"))).isEmpty();
    }

    @Test
    @DisplayName("두 번째 호출은 캐시를 쓴다 — 매 질문마다 PDF 를 다시 열면 느려진다")
    void 캐시가_동작한다(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("2026/b.pdf");
        writePdf(file, "cached body");
        ContentTextExtractor ex = new ContentTextExtractor(dir.toString());

        String first = ex.textOf(pdfContent("/content/files/2026/b.pdf"));
        Files.delete(file);   // 파일을 지워도 캐시가 있으면 같은 값이 나와야 한다
        String second = ex.textOf(pdfContent("/content/files/2026/b.pdf"));

        assertThat(first).contains("cached body");
        assertThat(second).isEqualTo(first);
    }
}
