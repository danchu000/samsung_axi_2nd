package com.ssa.lms.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * xlsx 한 벌을 만드는 공용 유틸 — 시험 성적 / 설문 결과 / 문제은행 / (필요 시) 대시보드가 함께 쓴다.
 *
 * <p><b>왜 {@code XSSFWorkbook} 이 아니라 {@code SXSSFWorkbook}(스트리밍) 인가</b><br>
 * 성적표는 응시자 수만큼, 문제은행은 문항 수만큼 행이 늘어난다(수천 행). XSSF 는 모든 행을
 * 힙에 들고 있다가 마지막에 직렬화하므로 행 수에 비례해 메모리를 먹는다. SXSSF 는 최근
 * {@value #WINDOW} 행만 메모리에 두고 나머지는 임시 파일로 흘려보내므로 행 수가 늘어도
 * 사용량이 평평하다. 대신 (1) 이미 흘려보낸 행은 다시 읽을 수 없고 (2) 임시 파일을
 * {@code dispose()} 로 지워야 한다 — 그래서 이 클래스는 {@link AutoCloseable} 이고,
 * <b>반드시 try-with-resources 로 써야 한다</b>.</p>
 *
 * <p>이 프로젝트의 다운로드는 전부 "헤더 한 줄 + 본문 여러 줄"의 단순한 표라서 스트리밍의
 * 제약(뒤로 못 감)이 걸리지 않는다. 컬럼 너비만 흘려보낸 행까지 포함해 직접 누적한다
 * ({@code autoSizeColumn} 은 SXSSF 에서 창 밖 행을 못 보고, 한글 폭도 제대로 못 잰다).</p>
 *
 * <pre>
 * try (ExcelWriter w = ExcelWriter.create()) {
 *     w.sheet("성적", "번호", "이름", "총점");
 *     w.row(1, "홍길동", 90);
 *     return w.toBytes();
 * }
 * </pre>
 */
public final class ExcelWriter implements AutoCloseable {

    /** 메모리에 유지하는 행 수. 이보다 오래된 행은 임시 파일로 내려간다. */
    private static final int WINDOW = 200;

    /** 엑셀 셀 한 칸의 최대 문자 수 (POI 가 넘으면 예외를 던진다). */
    private static final int MAX_CELL_CHARS = 32_000;

    private static final int MIN_WIDTH = 6;
    private static final int MAX_WIDTH = 60;

    private final SXSSFWorkbook workbook;
    private final CellStyle headerStyle;

    private SXSSFSheet sheet;
    private int rowIndex;
    private int[] widths = new int[0];

    private ExcelWriter() {
        this.workbook = new SXSSFWorkbook(WINDOW);
        this.workbook.setCompressTempFiles(true);

        Font bold = workbook.createFont();
        bold.setBold(true);

        this.headerStyle = workbook.createCellStyle();
        this.headerStyle.setFont(bold);
        this.headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        this.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
        this.headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.headerStyle.setBorderBottom(BorderStyle.THIN);
    }

    public static ExcelWriter create() {
        return new ExcelWriter();
    }

    /**
     * 새 시트를 열고 헤더 행을 쓴다. 이후 {@link #row(Object...)} 는 이 시트에 쌓인다.
     *
     * <p>시트명은 엑셀 제약(31자, {@code / \ ? * [ ]} 금지)에 맞춰 자동 보정하고,
     * 같은 이름이 이미 있으면 뒤에 번호를 붙인다 — 시험명·설문명을 그대로 넘겨도 안전하다.</p>
     */
    public ExcelWriter sheet(String name, String... headers) {
        applyWidths();
        this.sheet = workbook.createSheet(uniqueName(name));
        this.rowIndex = 0;
        this.widths = new int[headers.length];

        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(clean(headers[i]));
            cell.setCellStyle(headerStyle);
            track(i, headers[i]);
        }
        sheet.createFreezePane(0, 1);   // 스크롤해도 헤더가 붙어 있게
        return this;
    }

    /**
     * 본문 한 행. {@code null} 은 빈 칸, {@link Number} 는 숫자 셀(엑셀에서 합계가 먹는다),
     * {@link Boolean} 은 "예/아니오", 나머지는 문자열로 쓴다.
     */
    public ExcelWriter row(Object... cells) {
        requireSheet();
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < cells.length; i++) {
            Object value = cells[i];
            if (value == null) {
                continue;
            }
            Cell cell = row.createCell(i);
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
                track(i, String.valueOf(value));
            } else if (value instanceof Boolean flag) {
                String text = flag ? "예" : "아니오";
                cell.setCellValue(text);
                track(i, text);
            } else {
                String text = clean(String.valueOf(value));
                cell.setCellValue(text);
                track(i, text);
            }
        }
        return this;
    }

    /**
     * 데이터가 0건일 때 첫 칸에 남기는 안내 행.
     * 빈 시트만 있으면 "다운로드가 깨진 것"과 "정말 데이터가 없는 것"을 구분할 수 없다.
     */
    public ExcelWriter emptyNote(String message) {
        return row(message);
    }

    /** 지금까지 쓴 내용을 xlsx 바이트로 직렬화한다. 한 번만 호출한다. */
    public byte[] toBytes() {
        applyWidths();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 파일을 만들지 못했습니다.", e);
        }
    }

    /**
     * 임시 파일 정리. 호출하지 않으면 디스크에 SXSSF 조각이 남는다.
     *
     * <p>POI 5.4 부터 {@code dispose()} 는 deprecated 이고 {@code close()} 가 임시 파일까지
     * 정리한다. 예전 예제를 보고 {@code dispose()} 를 다시 붙이지 말 것.</p>
     */
    @Override
    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 워크북을 닫지 못했습니다.", e);
        }
    }

    /* ===================== 내부 ===================== */

    private void requireSheet() {
        if (sheet == null) {
            throw new IllegalStateException("sheet(...) 를 먼저 호출해야 한다.");
        }
    }

    private String uniqueName(String raw) {
        String base = WorkbookUtil.createSafeSheetName(
                raw == null || raw.isBlank() ? "Sheet" : raw.strip());
        String candidate = base;
        for (int i = 2; workbook.getSheet(candidate) != null; i++) {
            String suffix = "(" + i + ")";
            String head = base.length() + suffix.length() > 31
                    ? base.substring(0, 31 - suffix.length())
                    : base;
            candidate = head + suffix;
        }
        return candidate;
    }

    /**
     * XML 로 직렬화할 수 없는 제어문자를 걷어내고 셀 길이 상한을 지킨다.
     * 주관식 설문 답변처럼 사용자가 입력한 값이 그대로 들어오는 칸이 있어 필요하다.
     */
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length() && sb.length() < MAX_CELL_CHARS; i++) {
            char c = value.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void track(int column, String value) {
        if (column >= widths.length || value == null) {
            return;
        }
        widths[column] = Math.max(widths[column], displayWidth(value));
    }

    /** 한글·한자·가나는 라틴 문자의 두 배 폭으로 잡는다. 줄바꿈이 있으면 가장 긴 줄 기준. */
    private static int displayWidth(String value) {
        int max = 0;
        int current = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') {
                max = Math.max(max, current);
                current = 0;
            } else {
                current += c > 0x2E80 ? 2 : 1;
            }
        }
        return Math.max(max, current);
    }

    private void applyWidths() {
        if (sheet == null) {
            return;
        }
        for (int i = 0; i < widths.length; i++) {
            int chars = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, widths[i] + 2));
            sheet.setColumnWidth(i, chars * 256);
        }
    }
}
