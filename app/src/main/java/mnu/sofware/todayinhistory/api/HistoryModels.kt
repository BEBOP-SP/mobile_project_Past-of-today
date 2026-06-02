package mnu.sofware.todayinhistory.api

import com.google.gson.annotations.SerializedName

/**
 * Wikipedia 'On This Day' API 전체 응답 모델
 * [방어적 코딩] 모든 필드를 Null 허용으로 설정하여 파싱 오류를 방지합니다.
 */
data class HistoryResponse(
    @SerializedName("selected") val selected: List<HistoryEvent>? = null,
    @SerializedName("events") val events: List<HistoryEvent>? = null,
    @SerializedName("births") val births: List<HistoryEvent>? = null,
    @SerializedName("deaths") val deaths: List<HistoryEvent>? = null
)

/**
 * 개별 역사적 사건 정보
 */
data class HistoryEvent(
    @SerializedName("text") val text: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("pages") val pages: List<WikipediaArticle>? = null
)

/**
 * 위키백과 문서 상세 정보
 */
data class WikipediaArticle(
    @SerializedName("displaytitle") val title: String? = null,
    @SerializedName("extract") val extract: String? = null,
    @SerializedName("thumbnail") val thumbnail: Thumbnail? = null,
    @SerializedName("content_urls") val contentUrls: ContentUrls? = null
)

/**
 * 이미지 썸네일 정보
 */
data class Thumbnail(
    @SerializedName("source") val source: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

/**
 * 관련 웹 페이지 링크 정보
 */
data class ContentUrls(
    @SerializedName("desktop") val desktop: UrlInfo? = null
)

data class UrlInfo(
    @SerializedName("page") val page: String? = null
)
