package mnu.sofware.todayinhistory.api

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Google ML Kit(On-device Translation)을 활용한 텍스트 번역 및 위키백과 데이터 처리 매니저입니다.
 * [교수님 조건] 10장 지역화 대응을 위해 영어 데이터를 한국어로 실시간 번역하는 기능을 담당합니다.
 */
object TranslationManager {
    
    // 번역기 설정 (소스: 영어, 타겟: 한국어)
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.KOREAN)
        .build()

    // 온디바이스 번역 클라이언트 생성
    private val translator = Translation.getClient(options)
    
    // 번역 모델 다운로드 완료 여부 플래그
    private var isModelDownloaded = false

    /**
     * 번역에 필요한 언어 모델을 사전에 준비합니다.
     * [최적화] 데이터 비용 절감을 위해 Wi-Fi 연결 시에만 다운로드되도록 설정합니다.
     * @return 모델 다운로드 또는 준비 성공 여부
     */
    suspend fun prepareModel(): Boolean {
        if (isModelDownloaded) return true
        
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
            
        return try {
            translator.downloadModelIfNeeded(conditions).await()
            isModelDownloaded = true
            true
        } catch (e: Exception) {
            android.util.Log.e("MLKit", "모델 다운로드 실패: ${e.message}")
            false
        }
    }

    /**
     * 입력받은 영어 문장을 한국어로 번역합니다.
     * @param text 번역할 원본 영어 텍스트
     * @return 번역된 한국어 텍스트 (실패 시 원본 반환)
     */
    suspend fun translateToKorean(text: String): String {
        if (!prepareModel()) return text
        
        return try {
            translator.translate(text).await()
        } catch (e: Exception) {
            android.util.Log.e("MLKit", "번역 에러: ${e.message}")
            text
        }
    }

    /**
     * [교수님 조건] 10장 지역화 기능 확장을 위해 영문 위키 제목에 대응하는 한국어 위키 URL을 찾습니다.
     * 위키피디아 언어 링크 API를 사용하여 다국어 문서를 매핑합니다.
     * @param enTitle 검색할 영문 문서 제목
     * @return 한국어 위키백과 URL (문서가 없으면 null 반환)
     */
    suspend fun getKoreanWikipediaUrl(enTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // Wikipedia 언어 링크 쿼리 API URL 구성
            val apiUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=langlinks&lllang=ko&titles=$enTitle&format=json"
            val responseText = java.net.URL(apiUrl).readText()
            
            // JSON 응답 파싱 및 한국어 링크 추출
            val json = JSONObject(responseText)
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val pageId = pages.keys().next()
            val pageObj = pages.getJSONObject(pageId)
            
            if (pageObj.has("langlinks")) {
                val langLinks = pageObj.getJSONArray("langlinks")
                val koLink = langLinks.getJSONObject(0).getString("*")
                // URL 인코딩을 통해 한글 주소가 깨지지 않도록 처리
                "https://ko.wikipedia.org/wiki/${URLEncoder.encode(koLink, "UTF-8")}"
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("TranslationManager", "위키 URL 조회 실패: ${e.message}")
            null
        }
    }
}
