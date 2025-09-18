package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.Service.JeonnamPlainJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// JeonnamPlainJsonController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jeonnam")
public class JeonnamPlainJsonController {
    private final JeonnamPlainJsonService service;

    /** 전체 조회 */
    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getAll(
            @RequestParam(required = false) Integer sheet,
            @RequestParam(required = false) String sheetName
    ) {
        return ResponseEntity.ok(service.readAll(sheet, sheetName));
    }

    /** 단건 조회: ?row=0 방식 */
    @GetMapping(value = "/json/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getOneByQueryParam(
            @RequestParam int row,
            @RequestParam(required = false) Integer sheet,
            @RequestParam(required = false) String sheetName
    ) {
        return ResponseEntity.ok(service.readOneByIndex(sheet, sheetName, row));
    }


}

