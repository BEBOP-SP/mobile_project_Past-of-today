package mnu.sofware.todayinhistory.api

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Wikipedia 'On This Day' Open API 연동을 위한 서비스 인터페이스
 * 지정된 날짜의 역사적 사건, 탄생, 사망 정보를 가져옵니다.
 */
interface WikipediaService {
    /**
     * 특정 날짜의 역사적 사건 목록을 조회합니다.
     * @param type 조회할 데이터 유형 (selected, births, deaths, events 등)
     * @param mm 월 (01~12)
     * @param dd 일 (01~31)
     * @return API 응답 데이터를 담은 콜백 (추후 모델 클래스 정의 후 수정 예정)
     */
    @GET("feed/onthisday/{type}/{mm}/{dd}")
    suspend fun getOnThisDayEvents(
        @Path("type") type: String,
        @Path("mm") mm: String,
        @Path("dd") dd: String
    ): HistoryResponse
}
