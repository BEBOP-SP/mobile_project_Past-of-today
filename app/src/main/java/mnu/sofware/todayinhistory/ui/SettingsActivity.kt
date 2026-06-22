package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivitySettingsBinding

/**
 * 설정 화면 액티비티
 * 유저 정보 확인, 정보 수정, 로그아웃 기능을 제공합니다.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUserInfo()
        initBottomNavigation()
        initListeners()
    }

    override fun onResume() {
        super.onResume()
        initUserInfo() // 유저 정보 최신화
        updateUITexts()
    }

    /**
     * SharedPreferences에서 유저 정보를 가져와 화면에 표시합니다.
     */
    private fun initUserInfo() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("nickname", "") ?: ""
        val interests = sharedPref.getStringSet("interests", emptySet()) ?: emptySet()

        binding.tvSettingsNickname.text = nickname
        binding.tvSettingsInterests.text = interests.joinToString(", ")
    }

    private fun initListeners() {
        // 정보 수정 버튼: 관심사 선택 화면으로 다시 이동
        binding.btnSettingsEdit.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java)
            // 수정 모드임을 알리는 플래그나 별도 처리가 필요할 수 있으나, 
            // 현재 구조에서는 다시 설정하고 저장하면 덮어씌워짐
            startActivity(intent)
        }

        // 로그아웃 버튼
        binding.btnSettingsLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    /**
     * 로그아웃 확인 다이얼로그를 띄웁니다.
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
     * 로컬 저장된 유저 정보를 삭제하고 로그인 화면으로 이동합니다.
     */
    private fun performLogout() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * 언어 설정에 따른 UI 텍스트 업데이트
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") java.util.Locale.KOREA else java.util.Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)

        binding.apply {
            // 헤더 및 라벨 갱신
            (layoutSettingsHeader.getChildAt(0) as? android.widget.TextView)?.text = langContext.getString(R.string.settings_title)
            
            // 바텀 네비게이션 갱신
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
