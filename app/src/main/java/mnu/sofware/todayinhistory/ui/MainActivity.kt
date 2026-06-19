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
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var interestAdapter: HistoryAdapter
    private lateinit var discoveryAdapter: HistoryAdapter

    /**
     * [교수님 조건] 언어 설정을 SharedPreferences에 저장하여 앱 재시작이나 화면 전환 시에도 유지되도록 합니다.
     */
    private var currentLang: String
        get() = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("app_lang", "en") ?: "en"
        set(value) = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().putString("app_lang", value).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFirstRun()) {
            startActivity(Intent(this, CategorySelectionActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRecyclerViews()
        initBottomNavigation()
        
        // 초기 언어 설정에 맞춰 UI 텍스트 초기화
        updateUITexts()
        
        binding.btnLanguageToggle.setOnClickListener { toggleLanguage() }
        fetchTodayHistory()
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면에서 돌아왔을 때 현재 언어 설정에 맞춰 UI를 다시 갱신합니다.
        updateUITexts()
        binding.btnLanguageToggle.text = if (currentLang == "en") "EN" else "KR"
        // 바텀 네비게이션 아이콘 위치 보정
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun toggleLanguage() {
        currentLang = if (currentLang == "en") "ko" else "en"
        binding.btnLanguageToggle.text = if (currentLang == "en") "EN" else "KR"
        
        // UI 텍스트 실시간 갱신
        updateUITexts()
        
        // 데이터 다시 불러오기
        fetchTodayHistory()
    }

    /**
     * 선택된 언어에 맞춰 화면의 모든 고정 텍스트를 업데이트합니다.
     */
    private fun updateUITexts() {
        val locale = if (currentLang == "ko") Locale.KOREA else Locale.ENGLISH
        val config = resources.configuration
        config.setLocale(locale)
        val langContext = createConfigurationContext(config)
        
        // 저장된 닉네임 가져오기
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("nickname", "사용자") ?: "사용자"

        // 텍스트 뷰 갱신
        binding.apply {
            tvMainTitle.text = langContext.getString(R.string.main_header_title)
            tvMainSubtitle.text = langContext.getString(R.string.main_header_subtitle)
            tvMainGreeting.text = langContext.getString(R.string.main_greeting, nickname)
            tvSectionInterest.text = langContext.getString(R.string.section_interest_match)
            tvSectionDiscovery.text = langContext.getString(R.string.section_new_discovery)
            tvLoadingText.text = langContext.getString(R.string.main_loading)
            
            // 하단 네비게이션 메뉴 갱신
            bottomNav.menu.findItem(R.id.nav_home).title = langContext.getString(R.string.nav_home)
            bottomNav.menu.findItem(R.id.nav_scrap).title = langContext.getString(R.string.nav_scrap)
            bottomNav.menu.findItem(R.id.nav_settings).title = langContext.getString(R.string.nav_settings)
        }
    }

    private fun isFirstRun(): Boolean {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("isFirstRun", true)
    }

    private fun fetchTodayHistory() {
        val calendar = Calendar.getInstance()
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))

        binding.layoutLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "데이터 호출 시작: $month/$day (베이스: 영어)")
                val service = RetrofitClient.getService(this@MainActivity, "en")
                val response = service.getOnThisDayEvents("all", month, day)

                var allEvents = mutableListOf<HistoryEvent>()
                allEvents.addAll(convertToList(response.selected))
                allEvents.addAll(convertToList(response.events))
                
                if (allEvents.isEmpty()) {
                    Toast.makeText(this@MainActivity, "오늘의 사건 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    binding.layoutLoading.visibility = View.GONE
                    return@launch
                }

                val processedEvents = if (currentLang == "ko") {
                    val targetEvents = allEvents.take(40)
                    targetEvents.map { event ->
                        async {
                            val translated = TranslationManager.translateToKorean(event.text ?: "")
                            event.copy(text = translated)
                        }
                    }.awaitAll()
                } else allEvents

                val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                val userInterests = sharedPref.getStringSet("interests", emptySet()) ?: emptySet()

                val matched = processedEvents.filter { event ->
                    userInterests.any { isKeywordRelated(event.text ?: "", it) }
                }.shuffled()
                val others = processedEvents.filter { event ->
                    userInterests.none { isKeywordRelated(event.text ?: "", it) }
                }.shuffled()

                // [교수님 조건] 7:3 믹싱 알고리즘 구현
                // 관심사 섹션(70%) : 일치 항목 7개 + 추천 항목 3개 구성
                val itemsFromMatched = matched.take(7)
                val itemsFromOthersForInterest = others.take(10 - itemsFromMatched.size)
                val interestList = (itemsFromMatched + itemsFromOthersForInterest).shuffled()
                
                interestAdapter.submitList(interestList)

                // 새로운 발견 섹션(30%) : 관심사와 관련 없는 나머지 항목들 출력
                val discoveryList = others.drop(itemsFromOthersForInterest.size).take(30)
                discoveryAdapter.submitList(discoveryList)

                preloadImages(interestList.take(5))
                binding.layoutLoading.visibility = View.GONE

            } catch (e: Exception) {
                Log.e("MainActivity", "에러: ${e.message}")
                binding.layoutLoading.visibility = View.GONE
                Toast.makeText(this@MainActivity, "데이터 로드 실패 (네트워크 확인)", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
     * 특정 텍스트가 유저의 관심사와 관련이 있는지 키워드 기반으로 판별합니다.
     * [교수님 조건] 영어와 한국어 키워드를 모두 체크하여 지역화 대응 및 매칭 정확도를 높였습니다.
     */
    private fun isKeywordRelated(text: String, interest: String): Boolean {
        // [교수님 조건] 관심사 키워드 맵 (한국어/영어 키 선택 모두 대응)
        val relatedMap = mapOf(
            // 역사 섹션
            "전쟁/군사" to listOf("war", "battle", "military", "army", "invasion", "navy", "soldier", "general", "forces", "전쟁", "전투", "군사", "군대", "침공", "해군", "병사", "장군", "부대", "항복", "종전"),
            "War/Military" to listOf("war", "battle", "military", "army", "invasion", "navy", "soldier", "general", "forces", "전쟁", "전투", "군사", "군대", "침공", "해군", "병사", "장군", "부대", "항복", "종전"),
            
            "위인/인물" to listOf("king", "queen", "president", "founder", "leader", "biography", "born", "emperor", "saint", "왕", "여왕", "대통령", "지도자", "설립자", "탄생", "사망", "인물", "황제", "성인"),
            "Great Figures" to listOf("king", "queen", "president", "founder", "leader", "biography", "born", "emperor", "saint", "왕", "여왕", "대통령", "지도자", "설립자", "탄생", "사망", "인물", "황제", "성인"),
            
            "혁명/정치" to listOf("revolution", "independence", "politics", "treaty", "election", "law", "government", "parliament", "혁명", "독립", "정치", "조약", "선거", "법", "정부", "국회", "시위"),
            "Revolution/Politics" to listOf("revolution", "independence", "politics", "treaty", "election", "law", "government", "parliament", "혁명", "독립", "정치", "조약", "선거", "법", "정부", "국회", "시위"),
            
            "고대/유적" to listOf("ancient", "empire", "dynasty", "bc", "archaeology", "temple", "medieval", "pyramid", "castle", "고대", "제국", "왕조", "고고학", "사원", "중세", "유적", "문명"),
            "Ancient/Ruins" to listOf("ancient", "empire", "dynasty", "bc", "archaeology", "temple", "medieval", "pyramid", "castle", "고대", "제국", "왕조", "고고학", "사원", "중세", "유적", "문명"),
            
            // 과학 섹션
            "우주/천문" to listOf("space", "nasa", "planet", "moon", "star", "apollo", "orbit", "astronomy", "telescope", "galaxy", "우주", "행성", "달", "별", "아폴로", "궤도", "천문", "망원경", "은하"),
            "Space/Astronomy" to listOf("space", "nasa", "planet", "moon", "star", "apollo", "orbit", "astronomy", "telescope", "galaxy", "우주", "행성", "달", "별", "아폴로", "궤도", "천문", "망원경", "은하"),
            
            "핵/에너지" to listOf("nuclear", "atomic", "energy", "power plant", "uranium", "reactor", "radiation", "fusion", "핵", "원자력", "에너지", "발전소", "우라늄", "원자로", "방사능", "핵분열", "융합"),
            "Nuclear/Energy" to listOf("nuclear", "atomic", "energy", "power plant", "uranium", "reactor", "radiation", "fusion", "핵", "원자력", "에너지", "발전소", "우라늄", "원자로", "방사능", "핵분열", "융합"),
            
            "IT/인공지능" to listOf("computer", "software", "internet", "digital", "ai", "robot", "apple", "google", "microsoft", "컴퓨터", "소프트웨어", "인터넷", "디지털", "인공지능", "로봇", "애플", "구글", "마이크로소프트"),
            "IT/AI" to listOf("computer", "software", "internet", "digital", "ai", "robot", "apple", "google", "microsoft", "컴퓨터", "소프트웨어", "인터넷", "디지털", "인공지능", "로봇", "애플", "구글", "마이크로소프트"),
            
            "의학/발견" to listOf("medicine", "discovery", "dna", "science", "invention", "cure", "health", "patent", "vaccine", "virus", "의학", "발견", "과학", "발명", "치료", "건강", "특허", "백신", "바이러스"),
            "Medicine/Discovery" to listOf("medicine", "discovery", "dna", "science", "invention", "cure", "health", "patent", "vaccine", "virus", "의학", "발견", "과학", "발명", "치료", "건강", "특허", "백신", "바이러스"),
            
            // 예술 및 기타
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
                    // 스크랩 화면으로 이동 (애니메이션 제거하여 부드럽게)
                    val intent = Intent(this, ScrapActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    // 설정 화면 (추후 구현)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 리사이클러뷰 및 어댑터를 초기화합니다.
     * [교수님 조건] 클릭(위키 이동) 및 길게 누르기(스크랩) 기능을 구현합니다.
     */
    private fun initRecyclerViews() {
        // 공통 클릭 핸들러: 위키피디아 주소로 이동
        val onEventClick: (HistoryEvent) -> Unit = { event ->
            val url = event.pages?.firstOrNull()?.contentUrls?.desktop?.page
            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "상세 페이지 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 공통 롱클릭 핸들러: MySQL에 스크랩 저장
        val onEventLongClick: (HistoryEvent) -> Unit = { event ->
            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val uid = sharedPref.getString("uid", "unknown_user") ?: "unknown_user"
            
            lifecycleScope.launch {
                val year = event.year ?: 0
                val text = event.text ?: ""
                val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
                
                val success = MySqlDatabaseManager.saveScrap(uid, year, text, imageUrl)
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
