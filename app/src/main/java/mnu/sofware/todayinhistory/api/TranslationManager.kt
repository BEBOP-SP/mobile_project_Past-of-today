package mnu.sofware.todayinhistory.api

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 구글 ML Kit을 사용한 실시간 번역 매니저
 * [기능] 영어 텍스트를 한국어로 번역합니다.
 */
object TranslationManager {
    
    // ... (기존 변수들)
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.KOREAN)
        .build()

    private val translator = Translation.getClient(options)
    private var isModelDownloaded = false

    /**
     * 번역 모델을 다운로드합니다. (최초 1회 필요)
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
            false
        }
    }

    /**
     * 입력받은 영어 텍스트를 한국어로 번역합니다.
     */
    suspend fun translateToKorean(text: String): String {
        if (!prepareModel()) return text
        
        return try {
            translator.translate(text).await()
        } catch (e: Exception) {
            text
        }
    }

    /**
     * [교수님 조건] 영문 위키 제목을 사용하여 한국어 위키피디아 URL이 있는지 확인하고 반환합니다.
     * 한국어 문서가 없으면 null을 반환하여 영어 페이지로 폴백할 수 있게 합니다.
     */
    suspend fun getKoreanWikipediaUrl(enTitle: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // 위키피디아 언어 링크 API 호출 (엔드포인트 최적화)
            val apiUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=langlinks&lllang=ko&titles=$enTitle&format=json"
            val responseText = java.net.URL(apiUrl).readText()
            
            val json = JSONObject(responseText)
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val pageId = pages.keys().next()
            val pageObj = pages.getJSONObject(pageId)
            
            if (pageObj.has("langlinks")) {
                val langLinks = pageObj.getJSONArray("langlinks")
                val koLink = langLinks.getJSONObject(0).getString("*")
                "https://ko.wikipedia.org/wiki/${java.net.URLEncoder.encode(koLink, "UTF-8")}"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
