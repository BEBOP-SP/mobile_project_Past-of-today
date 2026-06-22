package mnu.sofware.todayinhistory.api

import com.google.gson.annotations.SerializedName

/**
 * Wikipedia API의 전체 응답을 담는 데이터 클래스입니다.
 * [교수님 조건] 10장 API 구현 시 데이터 모델 정의가 필요하며, GSON 라이브러리를 통해 JSON과 매핑됩니다.
 * 한국어 API의 불규칙한 데이터 구조(객체 또는 리스트 형식 혼용)에 대응하기 위해 Any 타입과 Null 허용을 사용합니다.
 */
data class HistoryResponse(
    @SerializedName("selected") val selected: Any? = null, // 주요 선정 사건 목록
    @SerializedName("events") val events: Any? = null,     // 전체 사건 목록
    @SerializedName("births") val births: Any? = null,     // 탄생 정보 (미사용)
    @SerializedName("deaths") val deaths: Any? = null      // 사망 정보 (미사용)
)

/**
 * 개별 역사적 사건의 세부 정보를 담는 데이터 클래스입니다.
 */
data class HistoryEvent(
    @SerializedName("text") val text: String? = null,      // 사건 설명 텍스트
    @SerializedName("year") val year: Int? = null,        // 발생 연도
    @SerializedName("pages") val pages: List<WikipediaArticle>? = null // 관련 위키백과 페이지 정보
)

/**
 * 각 사건과 관련된 위키백과 문서 정보를 담는 데이터 클래스입니다.
 */
data class WikipediaArticle(
    @SerializedName("displaytitle") val title: String? = null, // 문서 제목
    @SerializedName("extract") val extract: String? = null,     // 문서 요약 내용
    @SerializedName("thumbnail") val thumbnail: Thumbnail? = null, // 썸네일 이미지 정보
    @SerializedName("content_urls") val contentUrls: ContentUrls? = null // 상세 페이지 URL 링크
)

/**
 * 이미지 썸네일의 소스 URL 및 크기 정보를 정의합니다.
 */
data class Thumbnail(
    @SerializedName("source") val source: String? = null, // 이미지 실제 URL 주소
    @SerializedName("width") val width: Int? = null,      // 이미지 너비
    @SerializedName("height") val height: Int? = null     // 이미지 높이
)

/**
 * 데스크탑 및 모바일용 위키백과 연결 주소를 담는 클래스입니다.
 */
data class ContentUrls(
    @SerializedName("desktop") val desktop: UrlInfo? = null
)

/**
 * 최종적인 페이지 URL 정보를 담는 클래스입니다.
 */
data class UrlInfo(
    @SerializedName("page") val page: String? = null
)
