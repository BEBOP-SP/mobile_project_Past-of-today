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
 * Retrofit 객체 생성을 담당하는 싱글톤 클라이언트
 * [최적화] 언어별(en/ko) 인스턴스를 동적으로 생성 및 캐싱합니다.
 */
object RetrofitClient {
    private const val BASE_URL_TEMPLATE = "https://%s.wikipedia.org/api/rest_v1/"
    
    // 언어별 서비스 인스턴스를 담는 맵
    private val instances = mutableMapOf<String, WikipediaService>()

    /**
     * 지정된 언어의 WikipediaService 인스턴스를 반환합니다.
     * @param context 캐시 설정을 위한 컨텍스트
     * @param lang 언어 코드 ("en" 또는 "ko")
     */
    fun getService(context: Context, lang: String = "en"): WikipediaService {
        return instances[lang] ?: synchronized(this) {
            instances[lang] ?: buildService(context, lang).also { instances[lang] = it }
        }
    }

    private fun buildService(context: Context, lang: String): WikipediaService {
        val cacheSize = 10 * 1024 * 1024L
        val cacheDir = File(context.cacheDir, "http_cache_$lang")
        val cache = Cache(cacheDir, cacheSize)

        // [최적화] GSON 설정을 더 유연하게 (한국어 API의 불규칙한 응답 대응)
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "TimePopApp/1.0 (https://github.com/yourusername/TimePop)") // 헤더 누락 방지
                    .header("Cache-Control", "public, max-age=3600")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(String.format(BASE_URL_TEMPLATE, lang))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WikipediaService::class.java)
    }
}
