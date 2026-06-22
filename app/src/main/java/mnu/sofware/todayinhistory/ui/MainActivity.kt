package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.adapter.HistoryAdapter
import mnu.sofware.todayinhistory.api.HistoryEvent
import mnu.sofware.todayinhistory.api.RetrofitClient
import mnu.sofware.todayinhistory.api.TranslationManager
import mnu.sofware.todayinhistory.databinding.ActivityMainBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager
import mnu.sofware.todayinhistory.service.ScreenService
import java.util.Calendar
import java.util.Locale

/**
 * 앱의 메인 대시보드 화면입니다.
 * [교수님 조건] 7:3 개인화 추천 알고리즘 및 8장 리사이클러뷰 기능을 구현한 핵심 액티비티입니다.
 * 다국어(ko/en) UI 갱신 및 API 연동을 실시간으로 처리하며, 유저 관심사에 따른 필터링을 수행합니다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    // 관심사 매칭 섹션과 일반 탐색 섹션을 위한 리사이클러뷰 어댑터
    private lateinit var interestAdapter: HistoryAdapter
    private lateinit var discoveryAdapter: HistoryAdapter

    /**
     * [교수님 조건] 10장 지역화 대응을 위해 현재 언어 설정을 기기에 저장하고 유지합니다.
     */
    private var currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"
        set(value) = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().putString("app_lang", value).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // [교수님 조건] 최초 실행 시 닉네임과 관심사를 입력받는 화면으로 자동 이동합니다.
        if (isFirstRun()) {
            startActivity(Intent(this, CategorySelectionActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // UI 구성요소 초기화
        initRecyclerViews()
        initBottomNavigation()
        
        // [교수님 조건] 5단계: 백그라운드에서 화면 켬 이벤트를 감지하기 위해 포그라운드 서비스를 시작합니다.
        startScreenService()
        // 최신 안드로이드 OS의 백그라운드 실행 정책에 따른 오버레이 권한을 확인합니다.
        checkOverlayPermission()
        
        // 설정된 언어(ko/en)에 따라 화면의 모든 텍스트를 즉시 업데이트합니다.
        updateUITexts()
        
        // 언어 전환 버튼 클릭 리스너 설정
        binding.btnLanguageToggle.setOnClickListener { toggleLanguage() }
        
        // Wikipedia 'On This Day' Open API 호출 시작
        fetchTodayHistory()
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면(설정 등)에서 돌아왔을 때 최신 유저 프로필(닉네임 등)을 반영합니다.
        updateUITexts()
        binding.btnLanguageToggle.text = if (currentLang == "en") "EN" else "KR"
        // 하단 네비게이션 바의 포커스를 홈 탭으로 고정합니다.
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    /**
     * [방어적 코딩] 안드로이드 10 이상에서 백그라운드 팝업 실행을 위해 '다른 앱 위에 표시' 권한을 요청합니다.
     */
    private fun checkOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "잠금화면 기능을 위해 권한 허용이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 화면 감지 및 팝업 제어를 담당하는 ScreenService를 가동합니다.
     */
    private fun startScreenService() {
        val serviceIntent = Intent(this, ScreenService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * 언어 설정을 토글하고 UI 및 데이터를 새로고침합니다.
     */
    private fun toggleLanguage() {
        currentLang = if (currentLang == "en") "ko" else "en"
        binding.btnLanguageToggle.text = if (currentLang == "en") "EN" else "KR"
        updateUITexts()
        fetchTodayHistory() // 변경된 언어로 API 다시 호출
    }

    /**
     * [교수님 조건] 10장 지역화 대응: 앱의 모든 고정 문자열 리소스를 현재 Locale에 맞춰 갱신합니다.
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") Locale.KOREA else Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)
        
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("nickname", "사용자") ?: "사용자"

        binding.apply {
            tvMainTitle.text = langContext.getString(R.string.main_header_title)
            tvMainSubtitle.text = langContext.getString(R.string.main_header_subtitle)
            tvMainGreeting.text = langContext.getString(R.string.main_greeting, nickname)
            tvSectionInterest.text = langContext.getString(R.string.section_interest_match)
            tvSectionDiscovery.text = langContext.getString(R.string.section_new_discovery)
            tvLoadingText.text = langContext.getString(R.string.main_loading)
            
            // 바텀 메뉴 언어 갱신
            bottomNav.menu.findItem(R.id.nav_home).title = langContext.getString(R.string.nav_home)
            bottomNav.menu.findItem(R.id.nav_scrap).title = langContext.getString(R.string.nav_scrap)
            bottomNav.menu.findItem(R.id.nav_settings).title = langContext.getString(R.string.nav_settings)
        }
    }

    private fun isFirstRun(): Boolean {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("isFirstRun", true)
    }

    /**
     * Wikipedia API에서 오늘의 역사 데이터를 가져와 관심사 기반으로 믹싱합니다.
     * [교수님 조건] Retrofit 비동기 통신 및 7:3 믹싱 알고리즘을 수행합니다.
     */
    private fun fetchTodayHistory() {
        val calendar = Calendar.getInstance()
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))

        binding.layoutLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Retrofit을 사용하여 영어 원문 데이터를 먼저 호출
                val service = RetrofitClient.getService(this@MainActivity, "en")
                val response = service.getOnThisDayEvents("all", month, day)

                // 다양한 응답 필드(selected, events)에서 데이터 병합
                var allEvents = mutableListOf<HistoryEvent>()
                allEvents.addAll(convertToList(response.selected))
                allEvents.addAll(convertToList(response.events))
                
                if (allEvents.isEmpty()) {
                    Toast.makeText(this@MainActivity, "데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                    binding.layoutLoading.visibility = View.GONE
                    return@launch
                }

                // [교수님 조건] 한국어 설정일 경우 Google ML Kit을 사용하여 비동기 번역 처리
                val processedEvents = if (currentLang == "ko") {
                    val targetEvents = allEvents.take(40) // 성능을 위해 상위 40개만 추출
                    targetEvents.map { event ->
                        async {
                            val translated = TranslationManager.translateToKorean(event.text ?: "")
                            event.copy(text = translated)
                        }
                    }.awaitAll()
                } else allEvents

                // [교수님 조건] 7:3 개인화 알고리즘: 유저 관심사 키워드와 매칭되는 데이터 분류
                val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                val userInterests = sharedPref.getStringSet("interests", emptySet()) ?: emptySet()

                val matched = processedEvents.filter { event ->
                    userInterests.any { isKeywordRelated(event.text ?: "", it) }
                }.shuffled()
                val others = processedEvents.filter { event ->
                    userInterests.none { isKeywordRelated(event.text ?: "", it) }
                }.shuffled()

                // 관심 섹션 (70%): 매칭 데이터 위주로 구성 (최대 7개)
                val itemsFromMatched = matched.take(7)
                val itemsFromOthersForInterest = others.take(10 - itemsFromMatched.size)
                val interestList = (itemsFromMatched + itemsFromOthersForInterest).shuffled()
                interestAdapter.submitList(interestList)

                // 새로운 발견 섹션 (30%): 일반 사건들을 랜덤하게 나열
                val discoveryList = others.drop(itemsFromOthersForInterest.size).take(30)
                discoveryAdapter.submitList(discoveryList)

                // [최적화] Glide 이미지 미리 읽기 기능을 사용하여 스크롤 성능 향상
                preloadImages(interestList.take(5))
                binding.layoutLoading.visibility = View.GONE

            } catch (e: Exception) {
                Log.e("MainActivity", "통신 실패: ${e.message}")
                binding.layoutLoading.visibility = View.GONE
                Toast.makeText(this@MainActivity, "데이터 로드 실패 (네트워크 확인)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 동적 타입의 API 응답(Any)을 안전하게 리스트 형식으로 변환합니다.
     */
    private fun convertToList(data: Any?): List<HistoryEvent> {
        if (data == null) return emptyList()
        val gson = Gson()
        return try {
            val json = gson.toJson(data)
            if (json.startsWith("[")) {
                val type = object : TypeToken<List<HistoryEvent>>() {}.type
                gson.fromJson(json, type)
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * 사건 텍스트와 유저의 관심 키워드를 매칭하는 지능형 검색 함수입니다.
     * [교수님 조건] 다국어 키워드 사전을 구축하여 매칭 정확도를 극대화했습니다.
     */
    private fun isKeywordRelated(text: String, interest: String): Boolean {
        val relatedMap = mapOf(
            "전쟁/군사" to listOf("war", "battle", "military", "army", "invasion", "navy", "soldier", "general", "forces", "전쟁", "전투", "군사", "군대", "침공", "해군", "병사", "장군", "부대", "항복", "종전"),
            "War/Military" to listOf("war", "battle", "military", "army", "invasion", "navy", "soldier", "general", "forces", "전쟁", "전투", "군사", "군대", "침공", "해군", "병사", "장군", "부대", "항복", "종전"),
            "위인/인물" to listOf("king", "queen", "president", "founder", "leader", "biography", "born", "emperor", "saint", "왕", "여왕", "대통령", "지도자", "설립자", "탄생", "사망", "인물", "황제", "성인"),
            "Great Figures" to listOf("king", "queen", "president", "founder", "leader", "biography", "born", "emperor", "saint", "왕", "여왕", "대통령", "지도자", "설립자", "탄생", "사망", "인물", "황제", "성인"),
            "혁명/정치" to listOf("revolution", "independence", "politics", "treaty", "election", "law", "government", "parliament", "혁명", "독립", "정치", "조약", "선거", "법", "정부", "국회", "시위"),
            "Revolution/Politics" to listOf("revolution", "independence", "politics", "treaty", "election", "law", "government", "parliament", "혁명", "독립", "정치", "조약", "선거", "법", "정부", "국회", "시위"),
            "고대/유적" to listOf("ancient", "empire", "dynasty", "bc", "archaeology", "temple", "medieval", "pyramid", "castle", "고대", "제국", "왕조", "고고학", "사원", "중세", "유적", "문명"),
            "Ancient/Ruins" to listOf("ancient", "empire", "dynasty", "bc", "archaeology", "temple", "medieval", "pyramid", "castle", "고대", "제국", "왕조", "고고학", "사원", "중세", "유적", "문명"),
            "우주/천문" to listOf("space", "nasa", "planet", "moon", "star", "apollo", "orbit", "astronomy", "telescope", "galaxy", "우주", "행성", "달", "별", "아폴로", "궤도", "천문", "망원경", "은하"),
            "Space/Astronomy" to listOf("space", "nasa", "planet", "moon", "star", "apollo", "orbit", "astronomy", "telescope", "galaxy", "우주", "행성", "달", "별", "아폴로", "궤도", "천문", "망원경", "은하"),
            "핵/에너지" to listOf("nuclear", "atomic", "energy", "power plant", "uranium", "reactor", "radiation", "fusion", "핵", "원자력", "에너지", "발전소", "우라늄", "원자로", "방사능", "핵분열", "융합"),
            "Nuclear/Energy" to listOf("nuclear", "atomic", "energy", "power plant", "uranium", "reactor", "radiation", "fusion", "핵", "원자력", "에너지", "발전소", "우라늄", "원자로", "방사능", "핵분열", "융합"),
            "IT/인공지능" to listOf("computer", "software", "internet", "digital", "ai", "robot", "apple", "google", "microsoft", "컴퓨터", "소프트웨어", "인터넷", "디지털", "인공지능", "로봇", "애플", "구글", "마이크로소프트"),
            "IT/AI" to listOf("computer", "software", "internet", "digital", "ai", "robot", "apple", "google", "microsoft", "컴퓨터", "소프트웨어", "인터넷", "디지털", "인공지능", "로봇", "애플", "구글", "마이크로소프트"),
            "의학/발견" to listOf("medicine", "discovery", "dna", "science", "invention", "cure", "health", "patent", "vaccine", "virus", "의학", "발견", "과학", "발명", "치료", "건강", "특허", "백신", "바이러스"),
            "Medicine/Discovery" to listOf("medicine", "discovery", "dna", "science", "invention", "cure", "health", "patent", "vaccine", "virus", "의학", "발견", "과학", "발명", "치료", "건강", "특허", "백신", "바이러스"),
            "음악/공연" to listOf("music", "album", "concert", "opera", "song", "band", "symphony", "composer", "음악", "앨범", "콘서트", "오페라", "노래", "밴드", "교향곡", "작곡가", "가수"),
            "Music/Performance" to listOf("music", "album", "concert", "opera", "song", "band", "symphony", "composer", "음악", "앨범", "콘서트", "오페라", "노래", "밴드", "교향곡", "작곡가", "가수"),
            "영화/엔터" to listOf("film", "movie", "actor", "hollywood", "premiere", "tv", "entertainment", "cinema", "영화", "배우", "할리우드", "개봉", "드라마", "예능", "시네마"),
            "Movie/Entertainment" to listOf("film", "movie", "actor", "hollywood", "premiere", "tv", "entertainment", "cinema", "영화", "배우", "할리우드", "개봉", "드라마", "예능", "시네마"),
            "미술/디자인" to listOf("art", "painting", "museum", "artist", "sculpture", "design", "gallery", "exhibition", "미술", "그림", "박물관", "화가", "조각", "디자인", "갤러리", "전시회"),
            "Art/Design" to listOf("art", "painting", "museum", "artist", "sculpture", "design", "gallery", "exhibition", "미술", "그림", "박물관", "화가", "조각", "디자인", "갤러리", "전시회"),
            "문학/철학" to listOf("literature", "book", "writer", "philosophy", "novel", "poet", "author", "essay", "문학", "책", "작가", "철학", "소설", "시인", "저자", "수필"),
            "Literature/Philosophy" to listOf("literature", "book", "writer", "philosophy", "novel", "poet", "author", "essay", "문학", "책", "작가", "철학", "소설", "시인", "저자", "수필"),
            "축구/야구/경기" to listOf("football", "soccer", "baseball", "match", "cup", "stadium", "fifa", "nba", "mlb", "축구", "야구", "경기", "월드컵", "경기장", "피파"),
            "Sports/Match" to listOf("football", "soccer", "baseball", "match", "cup", "stadium", "fifa", "nba", "mlb", "축구", "야구", "경기", "월드컵", "경기장", "피파"),
            "올림픽" to listOf("olympic", "games", "medal", "gold", "athlete", "championship", "올림픽", "메달", "금메달", "운동선수", "선수권"),
            "Olympics" to listOf("olympic", "games", "medal", "gold", "athlete", "championship", "올림픽", "메달", "금메달", "운동선수", "선수권"),
            "경제/비즈니스" to listOf("economy", "bank", "business", "stock", "company", "trade", "dollar", "finance", "market", "경제", "은행", "사업", "주식", "회사", "무역", "달러", "금융", "시장"),
            "Economy/Business" to listOf("economy", "bank", "business", "stock", "company", "trade", "dollar", "finance", "market", "경제", "은행", "사업", "주식", "회사", "무역", "달러", "금융", "시장")
        )
        val keywords = relatedMap[interest] ?: return false
        val lowerText = text.lowercase()
        return keywords.any { lowerText.contains(it) }
    }

    /**
     * Glide를 사용하여 이미지를 미리 다운로드하여 리스트 스크롤 시 멈춤 현상을 방지합니다.
     */
    private fun preloadImages(events: List<HistoryEvent>) {
        events.forEach { event ->
            val url = event.pages?.firstOrNull()?.thumbnail?.source
            if (!url.isNullOrEmpty()) Glide.with(this).downloadOnly().load(url).submit()
        }
    }

    /**
     * 하단 네비게이션 동작을 설정합니다.
     */
    private fun initBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_scrap -> {
                    val intent = Intent(this, ScrapActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 리사이클러뷰 및 어댑터를 초기화합니다.
     * [교수님 조건] 클릭(위키 이동) 및 길게 누르기(MySQL 스크랩) 기능을 정의합니다.
     */
    private fun initRecyclerViews() {
        // 아이템 클릭 시 상세 페이지로 이동 (한국어 우선 연결 로직 포함)
        val onEventClick: (HistoryEvent) -> Unit = { event ->
            val page = event.pages?.firstOrNull()
            val enUrl = page?.contentUrls?.desktop?.page
            val enTitle = page?.title

            if (enUrl.isNullOrEmpty()) {
                Toast.makeText(this, "상세 페이지 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                if (currentLang == "ko" && !enTitle.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        val koUrl = TranslationManager.getKoreanWikipediaUrl(enTitle)
                        if (koUrl != null) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(koUrl)))
                        } else {
                            // 한국어 문서가 없는 경우 영문으로 폴백
                            Toast.makeText(this@MainActivity, "한국어 문서가 없어 영문 페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enUrl)))
                        }
                    }
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enUrl)))
                }
            }
        }

        // 아이템 롱클릭 시 외부 MySQL 데이터베이스에 보관함 데이터 저장
        val onEventLongClick: (HistoryEvent) -> Unit = { event ->
            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val uid = sharedPref.getString("uid", "unknown_user") ?: "unknown_user"
            
            lifecycleScope.launch {
                val year = event.year ?: 0
                val text = event.text ?: ""
                val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
                val wikiUrl = event.pages?.firstOrNull()?.contentUrls?.desktop?.page
                
                val success = MySqlDatabaseManager.saveScrap(uid, year, text, imageUrl, wikiUrl)
                if (success) {
                    Toast.makeText(this@MainActivity, "보관함에 저장되었습니다!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "저장 실패 (네트워크 확인)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        interestAdapter = HistoryAdapter(onEventClick, onEventLongClick)
        binding.rvInterestMatch.adapter = interestAdapter
        binding.rvInterestMatch.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        discoveryAdapter = HistoryAdapter(onEventClick, onEventLongClick)
        binding.rvNewDiscovery.adapter = discoveryAdapter
        binding.rvNewDiscovery.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }
}
