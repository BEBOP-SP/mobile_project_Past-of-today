package mnu.sofware.todayinhistory.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.api.HistoryEvent
import mnu.sofware.todayinhistory.api.RetrofitClient
import mnu.sofware.todayinhistory.api.TranslationManager
import mnu.sofware.todayinhistory.databinding.ActivityPopupBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager
import java.util.Calendar

/**
 * 기기의 화면이 켜질 때(ACTION_SCREEN_ON) 나타나는 풀스크린 역사 팝업 액티비티입니다.
 * [교수님 조건] 5단계 팝업 시스템 구현 항목에 해당하며, 잠금화면 위에 데이터를 노출합니다.
 */
class PopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPopupBinding
    private var currentEvent: HistoryEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // [교수님 조건] 잠금화면 위에서도 액티비티가 노출될 수 있도록 시스템 플래그를 설정합니다.
        setupLockScreenFlags()
        
        binding = ActivityPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
        // 오늘의 역사 사건 중 하나를 무작위로 선택하여 화면에 표시합니다.
        loadRandomHistory()
    }

    /**
     * [교수님 조건] 최신 안드로이드 OS(Oreo 이상) 및 이전 버전의 잠금화면 노출 방식을 모두 지원합니다.
     */
    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // 구 버전 대응을 위한 Window 플래그 추가
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /**
     * 화면 상의 버튼 및 스와이프 이벤트를 정의합니다.
     */
    private fun initListeners() {
        // [사용자 경험] 하단의 닫기 버튼을 누르면 팝업이 종료됩니다.
        binding.btnPopupClose.setOnClickListener { 
            finish() 
        }
        
        // [교수님 조건] 잠금 해제 제스처를 모방한 위로 스와이프 시 팝업 종료 기능을 구현합니다.
        binding.btnPopupClose.setOnTouchListener(object : View.OnTouchListener {
            private var startY: Float = 0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val endY = event.y
                        if (startY - endY > 100) { // 일정 거리 이상 위로 스와이프 시
                            finish()
                        }
                    }
                }
                v?.performClick()
                return false
            }
        })

        // 현재 표시 중인 사건을 보관함에 즉시 저장합니다.
        binding.btnPopupScrap.setOnClickListener {
            currentEvent?.let { event ->
                saveScrap(event)
            }
        }
    }

    /**
     * Wikipedia API를 통해 오늘의 역사 데이터를 호출하고 무작위로 하나를 선정합니다.
     */
    private fun loadRandomHistory() {
        val calendar = Calendar.getInstance()
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentLang = sharedPref.getString("app_lang", "en") ?: "en"

        lifecycleScope.launch {
            try {
                // 비동기 통신으로 영어 데이터 로드
                val service = RetrofitClient.getService(this@PopupActivity, "en")
                val response = service.getOnThisDayEvents("all", month, day)
                
                val allEvents = mutableListOf<HistoryEvent>()
                allEvents.addAll(convertToList(response.selected))
                allEvents.addAll(convertToList(response.events))
                
                if (allEvents.isNotEmpty()) {
                    // 전체 목록 중 무작위 셔플 후 첫 번째 사건 선택
                    val randomEvent = allEvents.shuffled().first()
                    
                    // 한국어 설정 시 ML Kit 번역 수행
                    val text = if (currentLang == "ko") {
                        TranslationManager.translateToKorean(randomEvent.text ?: "")
                    } else randomEvent.text ?: ""
                    
                    currentEvent = randomEvent.copy(text = text)
                    displayEvent(currentEvent!!)
                }
            } catch (e: Exception) {
                binding.tvPopupDescription.text = "현재 오늘의 역사 정보를 가져올 수 없습니다."
            }
        }
    }

    /**
     * API로부터 받은 Any 타입의 동적 데이터를 List 모델로 안전하게 변환합니다.
     */
    private fun convertToList(data: Any?): List<HistoryEvent> {
        if (data == null) return emptyList()
        val gson = com.google.gson.Gson()
        return try {
            val json = gson.toJson(data)
            if (json.startsWith("[")) {
                val type = object : com.google.gson.reflect.TypeToken<List<HistoryEvent>>() {}.type
                gson.fromJson(json, type)
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * 선정된 사건 정보를 UI 요소(텍스트뷰, 이미지뷰)에 렌더링합니다.
     */
    private fun displayEvent(event: HistoryEvent) {
        binding.tvPopupYear.text = "${event.year}년"
        binding.tvPopupDescription.text = event.text
        
        val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
        // [최적화] Glide 라이브러리를 사용한 이미지 비동기 로딩 및 캐싱 적용
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.color.light_gray)
            .error(R.color.light_gray)
            .into(binding.ivPopupImage)
    }

    /**
     * [교수님 조건] 12장 MySQL 데이터베이스 연동: 팝업에서 즉시 스크랩 기능을 수행합니다.
     */
    private fun saveScrap(event: HistoryEvent) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPref.getString("uid", "unknown_user") ?: "unknown_user"
        
        lifecycleScope.launch {
            val success = MySqlDatabaseManager.saveScrap(
                uid, 
                event.year ?: 0, 
                event.text ?: "", 
                event.pages?.firstOrNull()?.thumbnail?.source,
                event.pages?.firstOrNull()?.contentUrls?.desktop?.page
            )
            if (success) {
                Toast.makeText(this@PopupActivity, "보관함에 저장되었습니다!", Toast.LENGTH_SHORT).show()
                binding.btnPopupScrap.isEnabled = false // 중복 저장 방지
            } else {
                Toast.makeText(this@PopupActivity, "이미 저장되었거나 통신 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
