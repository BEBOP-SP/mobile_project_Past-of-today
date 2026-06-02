package mnu.sofware.todayinhistory.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

/**
 * Retrofit 객체 생성을 담당하는 싱글톤 클라이언트
 * [교수님 조건] 시스템 언어(Locale)에 따라 한국어/영어 API 엔드포인트를 분기합니다.
 */
object RetrofitClient {
    // 위키백과 API 기본 경로 (언어 부분은 동적으로 결정)
    private const val BASE_URL_TEMPLATE = "https://%s.wikipedia.org/api/rest_v1/"

    /**
     * 현재 시스템 언어 설정을 기반으로 Base URL을 반환합니다.
     * [테스트] 한국어 위키백과의 피드 지원이 불안정할 수 있어 우선 영어(en)로 테스트합니다.
     */
    private fun getBaseUrl(): String {
        // val language = Locale.getDefault().language
        // val langCode = if (language == "ko") "ko" else "en"
        return String.format(BASE_URL_TEMPLATE, "en")
    }

    /**
     * WikipediaService 인스턴스를 생성하여 반환합니다.
     */
    val service: WikipediaService by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikipediaService::class.java)
    }
}
