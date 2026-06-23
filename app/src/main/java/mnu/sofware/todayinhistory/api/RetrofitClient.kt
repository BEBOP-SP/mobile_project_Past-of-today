package mnu.sofware.todayinhistory.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Wikipedia API 통신을 위한 Retrofit 클라이언트를 생성하는 싱글톤 객체입니다.
 * [최적화] 네트워크 성능 향상을 위해 언어별(en/ko) Retrofit 인스턴스를 캐싱하며 동적으로 생성합니다.
 */
object RetrofitClient {
    // 위키피디아 언어별 기본 URL 템플릿
    private const val BASE_URL_TEMPLATE = "https://%s.wikipedia.org/api/rest_v1/"
    
    // 이미 생성된 언어별 서비스 인스턴스를 저장하는 맵
    private val instances = mutableMapOf<String, WikipediaService>()

    /**
     * 요청된 언어에 최적화된 WikipediaService 인스턴스를 반환합니다.
     * @param context 앱 컨텍스트 (캐시 디렉토리 접근용)
     * @param lang 언어 코드 (영어 "en", 한국어 "ko" 등)
     * @return Retrofit 서비스 인터페이스 구현체
     */
    fun getService(context: Context, lang: String = "en"): WikipediaService {
        return instances[lang] ?: synchronized(this) {
            instances[lang] ?: buildService(context, lang).also { instances[lang] = it }
        }
    }

    /**
     * Retrofit 객체를 설정하고 빌드합니다.
     * [교수님 조건] 10장 API 연동을 위해 GSON 변환기와 OkHttpClient를 설정합니다.
     */
    private fun buildService(context: Context, lang: String): WikipediaService {
        // 네트워크 효율을 위한 10MB 크기의 HTTP 캐시 설정
        val cacheSize = 10 * 1024 * 1024L
        val cacheDir = File(context.cacheDir, "http_cache_$lang")
        val cache = Cache(cacheDir, cacheSize)

        // JSON 데이터 파싱을 위한 GSON 빌더 설정 (Lenient 모드로 안정성 강화)
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // 통신 타임아웃 및 인터셉터 설정을 위한 OkHttpClient 빌드
        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Wikipedia API 서버에서 요구하는 User-Agent 및 캐시 제어 헤더 추가
                val request = chain.request().newBuilder()
                    .header("User-Agent", "TimePopApp/1.0 (https://github.com/yourusername/TimePop)")
                    .header("Cache-Control", "public, max-age=3600")
                    .build()
                chain.proceed(request)
            }
            .build()

        // 최종 Retrofit 서비스 생성
        return Retrofit.Builder()
            .baseUrl(String.format(BASE_URL_TEMPLATE, lang))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WikipediaService::class.java)
    }
}
