package mnu.sofware.todayinhistory.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.adapter.HistoryAdapter
import mnu.sofware.todayinhistory.api.RetrofitClient
import mnu.sofware.todayinhistory.databinding.ActivityMainBinding
import java.util.Calendar

/**
 * 메인 액티비티 클래스
 * [교수님 조건] 8장 어댑터 뷰(RecyclerView)를 사용하여 메인 화면을 구성하며,
 * Wikipedia API를 통해 실제 데이터를 가져와 표시합니다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var interestAdapter: HistoryAdapter
    private lateinit var discoveryAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 뷰 바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UI 초기화
        initRecyclerViews()
        
        // 실제 데이터 가져오기 (2단계 보완)
        fetchTodayHistory()
    }

    /**
     * Wikipedia API를 통해 오늘 날짜의 역사적 사건을 가져옵니다.
     */
    private fun fetchTodayHistory() {
        val calendar = Calendar.getInstance()
        // API 포맷에 맞게 0을 붙여서 2자리 문자열로 변환 (예: 5월 -> 05)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))

        // 인사말 날짜 업데이트 (추가 보너스 구현)
        binding.tvMainGreeting.text = getString(R.string.main_greeting, "지민") // 이름은 나중에 DB에서 가져옴

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "데이터 호출 시작: $month/$day")
                // 1. 우선 'selected' (주요 사건) 데이터를 가져와봄
                var response = RetrofitClient.service.getOnThisDayEvents("selected", month, day)
                
                // 2. 만약 selected가 비어있으면 'events' (전체 사건)를 가져와봄
                if (response.selected.isNullOrEmpty()) {
                    Log.d("MainActivity", "selected 데이터가 없어 events 호출")
                    response = RetrofitClient.service.getOnThisDayEvents("events", month, day)
                }

                val allEvents = response.selected ?: response.events ?: emptyList()

                if (allEvents.isNotEmpty()) {
                    Log.d("MainActivity", "데이터 로드 성공: ${allEvents.size}건")
                    val shuffledEvents = allEvents.shuffled()
                    val midIndex = shuffledEvents.size / 2
                    
                    interestAdapter.submitList(shuffledEvents.subList(0, midIndex))
                    discoveryAdapter.submitList(shuffledEvents.subList(midIndex, shuffledEvents.size))
                } else {
                    Log.d("MainActivity", "가져온 데이터가 비어있습니다.")
                    Toast.makeText(this@MainActivity, "오늘의 사건 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 에러 원인을 정확히 로그에 남김
                Log.e("MainActivity", "데이터 로드 중 에러 발생: ${e.message}")
                e.printStackTrace() 
                Toast.makeText(this@MainActivity, "데이터 로딩 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 리사이클러뷰 초기 설정
     * 관심사 맞춤 섹션과 새로운 발견 섹션 각각의 어댑터를 설정합니다.
     */
    private fun initRecyclerViews() {
        // 1. 관심사 맞춤 리사이클러뷰 (가로형)
        interestAdapter = HistoryAdapter()
        binding.rvInterestMatch.apply {
            adapter = interestAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // 2. 새로운 발견 리사이클러뷰 (가로형)
        discoveryAdapter = HistoryAdapter()
        binding.rvNewDiscovery.apply {
            adapter = discoveryAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }
}
