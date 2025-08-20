package TourCompetition.ChonsreBack.Domain.Func.Controller;
import TourCompetition.ChonsreBack.Domain.Func.Service.TourApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
public class TourApiTestController {
    private final TourApiService tourApiService;

    @GetMapping("/tour/area")
    public Mono<JsonNode> areaBasedList(
            @RequestParam(required = false) Integer areaCode,
            @RequestParam(required = false) Integer sigunguCode,
            @RequestParam(required = false) Integer contentTypeId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer numOfRows
    ) {
        return tourApiService.getAreaBasedList(areaCode, sigunguCode, contentTypeId, pageNo, numOfRows);
    }
}
