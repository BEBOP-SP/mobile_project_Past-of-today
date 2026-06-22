package mnu.sofware.todayinhistory.api

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * Wikipedia 'On This Day' Open API 연동을 위한 Retrofit 서비스 인터페이스입니다.
 * [교수님 조건] 10장 지역화 및 API 구현 항목에 해당하며, 지정된 날짜의 역사적 사건 정보를 비동기적으로 가져옵니다.
 */
interface WikipediaService {
    /**
     * 특정 날짜(월/일)의 역사적 사건 목록을 조회하는 API 엔드포인트입니다.
     * @param type 조회할 데이터 필터 유형 (기본값 "all" 사용)
     * @param mm 조회할 월 (두 자리 문자열 형식, 예: "06")
     * @param dd 조회할 일 (두 자리 문자열 형식, 예: "20")
     * @return Wikipedia 서버로부터 받은 역사 데이터 모델 객체
     */
    @Headers("User-Agent: TimePopApp/1.0 (https://github.com/yourusername/TimePop)")
    @GET("feed/onthisday/{type}/{mm}/{dd}")
    suspend fun getOnThisDayEvents(
        @Path("type") type: String,
        @Path("mm") mm: String,
        @Path("dd") dd: String
    ): HistoryResponse
}
