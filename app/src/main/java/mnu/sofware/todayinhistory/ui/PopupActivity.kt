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
 * 화면이 켜졌을 때 나타나는 팝업 액티비티입니다.
 * [교수님 조건] 최신 안드로이드 OS 대응을 위해 잠금화면 위 표시 및 화면 켬 설정을 포함합니다.
 */
class PopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPopupBinding
    private var currentEvent: HistoryEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 최신 안드로이드 OS 버전을 고려한 백그라운드 팝업 최적화 설정
        setupLockScreenFlags()
        
        binding = ActivityPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
        loadRandomHistory()
    }

    /**
     * [교수님 조건] 잠금화면 위에서도 팝업이 뜰 수 있도록 설정합니다.
     */
    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun initListeners() {
        // [잠금화면 구현] 아래쪽 영역을 스와이프하거나 클릭하면 닫히도록 설정 (잠금 해제 흉내)
        binding.btnPopupClose.setOnClickListener { 
            finish() 
        }
        
        // 투명 버튼 위에서 스와이프 액션 감지 (간단한 예시)
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
                        if (startY - endY > 100) { // 위로 스와이프
                            finish()
                        }
                    }
                }
                v?.performClick()
                return false
            }
        })

        binding.btnPopupScrap.setOnClickListener {
            currentEvent?.let { event ->
                saveScrap(event)
            }
        }
    }

    private fun loadRandomHistory() {
        val calendar = Calendar.getInstance()
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentLang = sharedPref.getString("app_lang", "en") ?: "en"

        lifecycleScope.launch {
            try {
                // 위키피디아 API에서 오늘의 사건들 가져오기
                val service = RetrofitClient.getService(this@PopupActivity, "en")
                val response = service.getOnThisDayEvents("all", month, day)
                
                val allEvents = mutableListOf<HistoryEvent>()
                allEvents.addAll(convertToList(response.selected))
                allEvents.addAll(convertToList(response.events))
                
                if (allEvents.isNotEmpty()) {
                    // 유저의 관심사가 있다면 그에 맞는 것을 우선, 아니면 랜덤
                    val randomEvent = allEvents.shuffled().first()
                    
                    val text = if (currentLang == "ko") {
                        TranslationManager.translateToKorean(randomEvent.text ?: "")
                    } else randomEvent.text ?: ""
                    
                    currentEvent = randomEvent.copy(text = text)
                    displayEvent(currentEvent!!)
                }
            } catch (e: Exception) {
                binding.tvPopupDescription.text = "데이터를 불러오는 중 오류가 발생했습니다."
            }
        }
    }

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

    private fun displayEvent(event: HistoryEvent) {
        binding.tvPopupYear.text = "${event.year}년"
        binding.tvPopupDescription.text = event.text
        
        val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.color.light_gray)
            .into(binding.ivPopupImage)
    }

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
                binding.btnPopupScrap.isEnabled = false
            } else {
                Toast.makeText(this@PopupActivity, "저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
