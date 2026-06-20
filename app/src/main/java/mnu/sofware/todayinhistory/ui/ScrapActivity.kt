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
 * 사용자가 스크랩한 사건 목록을 보여주는 화면입니다.
 * [교수님 조건] 데이터베이스(MySQL) 연동 및 리사이클러뷰 구현.
 */
class ScrapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrapBinding
    private lateinit var scrapAdapter: ScrapAdapter

    /**
     * [교수님 조건] 언어 설정을 유지하기 위해 SharedPreferences를 사용합니다.
     */
    private val currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        initBottomNavigation()
        loadScrappedEvents()
    }

    override fun onResume() {
        super.onResume()
        updateUITexts()
    }

    /**
     * 현재 언어 설정에 맞춰 UI 텍스트들을 갱신합니다.
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") java.util.Locale.KOREA else java.util.Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)

        binding.apply {
            // 헤더 타이틀 갱신
            (layoutScrapHeader.getChildAt(0) as? android.widget.TextView)?.text = langContext.getString(R.string.scrap_title)
            
            // 빈 상태 메시지 갱신
            (layoutEmptyScrap.getChildAt(1) as? android.widget.TextView)?.text = langContext.getString(R.string.scrap_empty_msg)

            // 바텀 네비게이션 갱신
            bottomNavScrap.menu.findItem(R.id.nav_home).title = langContext.getString(R.string.nav_home)
            bottomNavScrap.menu.findItem(R.id.nav_scrap).title = langContext.getString(R.string.nav_scrap)
            bottomNavScrap.menu.findItem(R.id.nav_settings).title = langContext.getString(R.string.nav_settings)
        }
    }

    private fun initRecyclerView() {
        scrapAdapter = ScrapAdapter(
            onDeleteClick = { scrapId ->
                deleteScrap(scrapId)
            },
            onItemClick = { wikiUrl ->
                // [수정] 한국어 모드일 경우 한국어 위키피디아 존재 여부 확인 후 이동
                lifecycleScope.launch {
                    var targetUrl = wikiUrl
                    
                    // 영어 위키 주소이고 현재 한국어 설정인 경우
                    if (currentLang == "ko" && wikiUrl.contains("en.wikipedia.org")) {
                        val enTitle = wikiUrl.substringAfterLast("/")
                        val koUrl = mnu.sofware.todayinhistory.api.TranslationManager.getKoreanWikipediaUrl(enTitle)
                        if (koUrl != null) {
                            targetUrl = koUrl
                        }
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

    /**
     * 하단 네비게이션 설정
     */
    private fun initBottomNavigation() {
        binding.bottomNavScrap.selectedItemId = R.id.nav_scrap
        binding.bottomNavScrap.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 메인 화면으로 이동 (기존 인스턴스가 있다면 재사용)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_scrap -> true
                R.id.nav_settings -> {
                    // 설정 화면 구현 예정
                    true
                }
                else -> false
            }
        }
    }

    /**
     * DB에서 스크랩 목록을 불러옵니다.
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
     * 선택한 스크랩을 삭제합니다.
     */
    private fun deleteScrap(scrapId: Int) {
        lifecycleScope.launch {
            val success = MySqlDatabaseManager.deleteScrap(scrapId)
            if (success) {
                Toast.makeText(this@ScrapActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                loadScrappedEvents() // 리스트 새로고침
            } else {
                Toast.makeText(this@ScrapActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
