package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.adapter.ScrapAdapter
import mnu.sofware.todayinhistory.databinding.ActivityScrapBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager

/**
 * 사용자가 보관함(Scrap)에 저장한 역사적 사건 목록을 관리하는 화면입니다.
 * [교수님 조건] 8장 리사이클러뷰 및 12장 MySQL 데이터베이스 연동 항목을 준수합니다.
 */
class ScrapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrapBinding
    private lateinit var scrapAdapter: ScrapAdapter

    /**
     * [교수님 조건] 다국어 대응을 위해 현재 언어 설정을 기기에서 가져옵니다.
     */
    private val currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        initBottomNavigation()
        // DB에서 저장된 데이터를 비동기로 불러와 화면에 표시합니다.
        loadScrappedEvents()
    }

    override fun onResume() {
        super.onResume()
        // 타 화면 이동 후 복귀 시 최신 언어 설정에 맞춰 UI 고정 텍스트를 갱신합니다.
        updateUITexts()
    }

    /**
     * [교수님 조건] 10장 지역화: 현재 선택된 언어에 맞춰 헤더 및 메시지를 실시간 변경합니다.
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") java.util.Locale.KOREA else java.util.Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)

        binding.apply {
            (layoutScrapHeader.getChildAt(0) as? android.widget.TextView)?.text = langContext.getString(R.string.scrap_title)
            (layoutEmptyScrap.getChildAt(1) as? android.widget.TextView)?.text = langContext.getString(R.string.scrap_empty_msg)

            bottomNavScrap.menu.findItem(R.id.nav_home).title = langContext.getString(R.string.nav_home)
            bottomNavScrap.menu.findItem(R.id.nav_scrap).title = langContext.getString(R.string.nav_scrap)
            bottomNavScrap.menu.findItem(R.id.nav_settings).title = langContext.getString(R.string.nav_settings)
        }
    }

    /**
     * 보관함 리스트를 위한 리사이클러뷰와 어댑터를 초기화합니다.
     */
    private fun initRecyclerView() {
        scrapAdapter = ScrapAdapter(
            onDeleteClick = { scrapId ->
                // 삭제 아이콘 클릭 시 DB에서 데이터 삭제 요청
                deleteScrap(scrapId)
            },
            onItemClick = { wikiUrl ->
                // 항목 클릭 시 관련 위키백과 상세 페이지로 이동 (언어 지능형 폴백 적용)
                lifecycleScope.launch {
                    var targetUrl = wikiUrl
                    
                    if (currentLang == "ko" && wikiUrl.contains("en.wikipedia.org")) {
                        val enTitle = wikiUrl.substringAfterLast("/")
                        val koUrl = mnu.sofware.todayinhistory.api.TranslationManager.getKoreanWikipediaUrl(enTitle)
                        if (koUrl != null) targetUrl = koUrl
                    }

                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@ScrapActivity, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvScrapList.apply {
            adapter = scrapAdapter
            layoutManager = LinearLayoutManager(this@ScrapActivity)
        }
    }

    private fun initBottomNavigation() {
        binding.bottomNavScrap.selectedItemId = R.id.nav_scrap
        binding.bottomNavScrap.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_scrap -> true
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 외부 MySQL 데이터베이스로부터 현재 사용자의 스크랩 목록을 조회합니다.
     */
    private fun loadScrappedEvents() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPref.getString("uid", "unknown_user") ?: "unknown_user"

        lifecycleScope.launch {
            val events = MySqlDatabaseManager.getScrappedEvents(uid)
            if (events.isEmpty()) {
                binding.layoutEmptyScrap.visibility = View.VISIBLE
                binding.rvScrapList.visibility = View.GONE
            } else {
                binding.layoutEmptyScrap.visibility = View.GONE
                binding.rvScrapList.visibility = View.VISIBLE
                scrapAdapter.submitList(events)
            }
        }
    }

    /**
     * 데이터베이스에서 특정 스크랩 항목을 물리적으로 제거합니다.
     */
    private fun deleteScrap(scrapId: Int) {
        lifecycleScope.launch {
            val success = MySqlDatabaseManager.deleteScrap(scrapId)
            if (success) {
                Toast.makeText(this@ScrapActivity, "데이터가 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                loadScrappedEvents() // 리스트 즉시 갱신
            } else {
                Toast.makeText(this@ScrapActivity, "삭제 실패 (네트워크를 확인해 주세요)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
