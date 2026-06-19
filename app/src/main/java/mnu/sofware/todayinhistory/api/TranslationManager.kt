package mnu.sofware.todayinhistory.api

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * 구글 ML Kit을 사용한 실시간 번역 매니저
 * [기능] 영어 텍스트를 한국어로 번역합니다.
 */
object TranslationManager {

    // 번역기 설정: 영어 -> 한국어
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
            .requireWifi() // 가급적 와이파이에서 다운로드
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
        if (!prepareModel()) return text // 모델 다운로드 실패 시 원문 반환
        
        return try {
            translator.translate(text).await()
        } catch (e: Exception) {
            text // 번역 실패 시 원문 반환
        }
    }
}
