package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivitySettingsBinding

/**
 * 앱의 설정을 관리하고 유저 정보를 수정하는 액티비티입니다.
 * [교수님 조건] 개인화 프로필 관리 및 로그아웃 기능을 포함합니다.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    /**
     * [교수님 조건] 다국어 대응을 위해 저장된 언어 코드를 확인합니다.
     */
    private val currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 데이터 로드 및 컴포넌트 설정
        initUserInfo()
        initBottomNavigation()
        initListeners()
    }

    override fun onResume() {
        super.onResume()
        // 프로필 수정 후 돌아왔을 때 최신 정보를 반영합니다.
        initUserInfo()
        // 언어 설정에 따른 UI 텍스트 동적 갱신
        updateUITexts()
    }

    /**
     * SharedPreferences에 저장된 사용자 프로필(닉네임, 관심사)을 화면에 표시합니다.
     */
    private fun initUserInfo() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("nickname", "") ?: ""
        val interests = sharedPref.getStringSet("interests", emptySet()) ?: emptySet()

        binding.tvSettingsNickname.text = nickname
        binding.tvSettingsInterests.text = interests.joinToString(", ")
    }

    /**
     * 각종 버튼의 클릭 이벤트를 설정합니다.
     */
    private fun initListeners() {
        // 정보 수정: 관심사 선택 화면으로 재이동하여 데이터를 덮어씌웁니다.
        binding.btnSettingsEdit.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java)
            startActivity(intent)
        }

        // 로그아웃: 사용자에게 의사를 묻는 다이얼로그를 호출합니다.
        binding.btnSettingsLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    /**
     * 로그아웃 전 사용자의 확인을 받는 알림창을 생성합니다.
     */
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_btn_logout)
            .setMessage(R.string.settings_logout_confirm)
            .setPositiveButton("확인") { _, _ ->
                performLogout()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 기기에 저장된 유저 세션 정보를 초기화하고 로그인 화면으로 복귀합니다.
     */
    private fun performLogout() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        // 로그인 화면 이동 시 기존 액티비티 스택을 모두 비웁니다.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * [교수님 조건] 10장 지역화 대응: 현재 언어 환경에 맞춰 설정 화면의 모든 텍스트를 변경합니다.
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") java.util.Locale.KOREA else java.util.Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)

        binding.apply {
            // 헤더 텍스트 갱신
            (layoutSettingsHeader.getChildAt(0) as? android.widget.TextView)?.text = langContext.getString(R.string.settings_title)
            
            // 바텀 탭 메뉴 텍스트 갱신
            bottomNavSettings.menu.findItem(R.id.nav_home).title = langContext.getString(R.string.nav_home)
            bottomNavSettings.menu.findItem(R.id.nav_scrap).title = langContext.getString(R.string.nav_scrap)
            bottomNavSettings.menu.findItem(R.id.nav_settings).title = langContext.getString(R.string.nav_settings)
        }
    }

    private fun initBottomNavigation() {
        binding.bottomNavSettings.selectedItemId = R.id.nav_settings
        binding.bottomNavSettings.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_scrap -> {
                    val intent = Intent(this, ScrapActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
