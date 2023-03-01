package nextstep.subway.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.utils.GithubResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.acceptance.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.MemberSteps.깃허브_인증_로그인_요청;
import static nextstep.subway.acceptance.MemberSteps.회원_생성_요청_및_로그인;
import static nextstep.subway.acceptance.PathSteps.경로_조회_요청;
import static nextstep.subway.acceptance.PathSteps.로그인_후_요청_기준으로_경로_조회_요청;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("지하철 경로 검색")
class PathAcceptanceTest extends AcceptanceTest {

    private static final String 시간 = "DURATION";
    private static final String 거리 = "DISTANCE";

    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;

    /**
     *  // @formatter:off
     *                   5분, 10km, 100원
     *          교대역    --- *2호선* ---      강남역
     *          |                            |
     *       *3호선* 10분, 2km, 200        *신분당선*  3분, 10km, 1000원
     *          |                           |
     *          남부터미널역  --- *3호선* ---   양재
     *                     2분, 3km, 200
     *  // @formatter:on
     **/
    @BeforeEach
    public void setUp() {
        super.setUp();

        깃허브_인증_로그인_요청(GithubResponses.사용자1.getCode());

        교대역 = 지하철역_생성_요청("교대역").jsonPath().getLong("id");
        강남역 = 지하철역_생성_요청("강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청("양재역").jsonPath().getLong("id");
        남부터미널역 = 지하철역_생성_요청("남부터미널역").jsonPath().getLong("id");

        이호선 = 지하철_노선_생성_요청("2호선", "green", 교대역, 강남역, 10, 5, 100);
        신분당선 = 지하철_노선_생성_요청("신분당선", "red", 강남역, 양재역, 10, 3, 1000);
        삼호선 = 지하철_노선_생성_요청("3호선", "orange", 교대역, 남부터미널역, 2, 10, 200);

        지하철_노선에_지하철_구간_생성_요청(삼호선, createSectionCreateParams(남부터미널역, 양재역, 3, 2));
    }

    /**
     * Given 6세 이상 ~ 13세 미만의 나이로 로그인을 하고
     * When 출발역에서 도착역까지의 최단 거리 기준으로 경로 조회를 요청 시
     * Then 최단 거리 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * AND 추가 요금이 있는 노선을 이용 할 경우 측정된 요금에 추가
     * And 운임에서 350원을 공제한 금액의 50% 할인
     */
    @DisplayName("어린이 회원이 두 역의 최단 거리 경로를 조회 시 할인 정책 반영")
    @Test
    void 어린이_회원이_두_역의_최단_거리_경로를_조회_시_할인_정책_반영() {
        // Given
        String accessToken = 회원_생성_요청_및_로그인("children@naver.com", "pass", 12).jsonPath().getString("accessToken");

        // When
        ExtractableResponse<Response> response = 로그인_후_요청_기준으로_경로_조회_요청(교대역, 양재역, 거리, accessToken);

        // Then
        assertAll(() -> {
            assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(교대역, 남부터미널역, 양재역);
            총_거리와_소요_시간을_함께_응답한다(response, 5, 12);
            assertThat(response.jsonPath().getInt("fare")).isEqualTo(550);
            // 추가 운임 + 어린이 할인
        });
    }

    /**
     * Given 13세 이상~19세 미만의 나이로 로그인을 하고
     * When 출발역에서 도착역까지의 최단 거리 기준으로 경로 조회를 요청 시
     * Then 최단 거리 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 추가 요금이 있는 노선을 이용 할 경우 측정된 요금에 추가
     * And 운임에서 350원을 공제한 금액의 20% 할인
     */
    @DisplayName("청소년 회원이 두 역의 최단 거리 경로를 조회 시 할인 정책 반영")
    @Test
    void 청소년_회원이_두_역의_최단_거리_경로를_조회_시_할인_정책_반영() {
        // Given
        String accessToken = 회원_생성_요청_및_로그인("youth@naver.com", "pass", 13).jsonPath().getString("accessToken");

        // When
        ExtractableResponse<Response> response = 로그인_후_요청_기준으로_경로_조회_요청(교대역, 양재역, 거리, accessToken);

        // Then
        assertAll(() -> {
            assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(교대역, 남부터미널역, 양재역);
            총_거리와_소요_시간을_함께_응답한다(response, 5, 12);
            assertThat(response.jsonPath().getInt("fare")).isEqualTo(880);
            // (1250 + 200 - 350) * 0.8
            // 추가 운임 + 청소년 할인
        });
    }

    /**
     * When 출발역에서 도착역까지의 최단 거리 기준으로 경로 조회를 요청
     * Then 최단 거리 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함
     */
    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @Test
    void findPathByDistance() {
        // when
        ExtractableResponse<Response> response = 경로_조회_요청(교대역, 양재역, 거리);

        // then
        assertAll(() -> {
            assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(교대역, 남부터미널역, 양재역);
            총_거리와_소요_시간을_함께_응답한다(response, 5, 12);
            assertThat(response.jsonPath().getInt("fare")).isEqualTo(1450);
            // (1250 + 200)
            // 추가 운임
        });
    }

    /**
     * When 출발역에서 도착역까지의 최소 시간 기준으로 경로 조회를 요청
     * Then 최소 시간 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함
     */
    @DisplayName("두 역의 최소 시간 경로를 조회")
    @Test
    void 두_역의_최소_시간_경로를_조회() {
        // when
        ExtractableResponse<Response> response = 경로_조회_요청(양재역, 교대역, 시간);

        // then
        assertAll(() -> {
            assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(양재역, 강남역, 교대역);
            총_거리와_소요_시간을_함께_응답한다(response, 20, 8);
            assertThat(response.jsonPath().getInt("fare")).isEqualTo(2450);
            // 1250 + 1000 + 200
        });
    }

    public static Long 지하철_노선_생성_요청(String name, String color, Long upStation, Long downStation, int distance, int duration, int extraFee) {
        Map<String, String> lineCreateParams;
        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation + "");
        lineCreateParams.put("downStationId", downStation + "");
        lineCreateParams.put("distance", distance + "");
        lineCreateParams.put("duration", duration + "");
        lineCreateParams.put("extraFee", extraFee + "");

        return LineSteps.지하철_노선_생성_요청(lineCreateParams).jsonPath().getLong("id");
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId, int distance, int duration) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", distance + "");
        params.put("duration", duration + "");
        return params;
    }

    private void 총_거리와_소요_시간을_함께_응답한다(ExtractableResponse<Response> response, int distance, int duration) {
        assertThat(response.jsonPath().getInt("distance")).isEqualTo(distance);
        assertThat(response.jsonPath().getInt("duration")).isEqualTo(duration);
    }

}
