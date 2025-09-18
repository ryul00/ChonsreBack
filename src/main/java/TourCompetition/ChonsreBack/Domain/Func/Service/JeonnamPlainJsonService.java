package TourCompetition.ChonsreBack.Domain.Func.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

// JeonnamPlainJsonService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class JeonnamPlainJsonService {
    private final ResourceLoader resourceLoader;

    @Value("${dataset.jeonnam-json.path}")
    private String xlsxPath;

    @Value("${dataset.jeonnam-json.sheet-index:0}")
    private int defaultSheetIndex;

    public String getXlsxPath() { return xlsxPath; }

    // === 공통: 시트 얻기(이름 우선, 없으면 인덱스, 인덱스 범위보정) ===
    private Sheet getSheet(Workbook wb, Integer sheetIndex, String sheetName) {
        if (sheetName != null && !sheetName.isBlank()) {
            Sheet s = wb.getSheet(sheetName);
            if (s == null) throw new IllegalArgumentException("시트를 찾을 수 없습니다: " + sheetName);
            return s;
        }
        int idx = (sheetIndex != null ? sheetIndex : defaultSheetIndex);
        int n = wb.getNumberOfSheets();
        if (idx < 0 || idx >= n) idx = 0; // 범위 벗어나면 0번
        return wb.getSheetAt(idx);
    }

    private static String cellToString(Cell c) {
        if (c == null) return "";
        try {
            c.setCellType(CellType.STRING);
            return org.apache.commons.lang3.StringUtils.trimToEmpty(c.getStringCellValue());
        } catch (Exception ignore) {
            return org.apache.commons.lang3.StringUtils.trimToEmpty(c.toString());
        }
    }

    // === 전체 조회 ===
    public List<Map<String,String>> readAll(Integer sheetIndex, String sheetName) {
        try {
            Resource res = resourceLoader.getResource(xlsxPath);
            try (InputStream is = res.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
                Sheet sheet = getSheet(wb, sheetIndex, sheetName);
                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) throw new IllegalStateException("헤더 행이 없습니다.");

                List<String> headers = new ArrayList<>();
                for (Cell c : header) { c.setCellType(CellType.STRING); headers.add(c.getStringCellValue().trim()); }

                List<Map<String,String>> out = new ArrayList<>();
                int firstData = sheet.getFirstRowNum() + 1;
                for (int r = firstData; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Map<String,String> obj = new LinkedHashMap<>();
                    boolean allBlank = true;
                    for (int i=0;i<headers.size();i++) {
                        String key = headers.get(i);
                        String val = cellToString(row.getCell(i));
                        if (!val.isBlank()) allBlank = false;
                        obj.put(key, val);
                    }
                    if (!allBlank) out.add(obj);
                }
                return out;
            }
        } catch (Exception e) {
            log.error("엑셀 로딩 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // === 단일 행 조회 (데이터 구간 0-based 인덱스) ===
    public Map<String,String> readOneByIndex(Integer sheetIndex, String sheetName, int dataRowIndex) {
        List<Map<String,String>> all = readAll(sheetIndex, sheetName);
        if (dataRowIndex < 0 || dataRowIndex >= all.size()) {
            throw new IllegalArgumentException("dataRowIndex 범위를 벗어났습니다. (0.." + (all.size()-1) + ")");
        }
        return all.get(dataRowIndex);
    }

    // === 컬럼명/값으로 1건 찾기 (완전일치 우선, 없으면 부분 포함 검색) ===
    public Map<String,String> findOne(Integer sheetIndex, String sheetName, String column, String value) {
        if (column == null || value == null) throw new IllegalArgumentException("column, value는 필수입니다.");
        List<Map<String,String>> all = readAll(sheetIndex, sheetName);
        // 완전 일치(대소문자 무시)
        for (var row : all) {
            String v = row.get(column);
            if (v != null && v.equalsIgnoreCase(value)) return row;
        }
        // 부분 포함
        String vLower = value.toLowerCase(Locale.ROOT);
        for (var row : all) {
            String v = row.get(column);
            if (v != null && v.toLowerCase(Locale.ROOT).contains(vLower)) return row;
        }
        throw new IllegalArgumentException("해당 조건으로 데이터를 찾을 수 없습니다. column=" + column + ", value=" + value);
    }
}
