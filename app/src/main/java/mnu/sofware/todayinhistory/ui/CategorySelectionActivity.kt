package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.databinding.ActivityCategorySelectionBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager

/**
 * 사용자의 닉네임과 관심 카테고리를 선택받는 액티비티입니다.
 * [교수님 조건] 최초 가입 시 관심사를 수집하여 맞춤형 역사 정보를 제공하며, DB와 실시간 연동합니다.
 */
class CategorySelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategorySelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategorySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 기존 설정 데이터가 있다면 로드 (수정 모드 대응)
        loadExistingData()
        initListeners()
    }

    /**
     * [교수님 조건] 정보 수정 시 사용자 경험을 위해 기존 저장된 닉네임과 관심사를 화면에 미리 설정합니다.
     */
    private fun loadExistingData() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("nickname", "") ?: ""
        val interests = sharedPref.getStringSet("interests", emptySet()) ?: emptySet()

        if (nickname.isNotEmpty()) {
            binding.etNickname.setText(nickname)
        }

        if (interests.isNotEmpty()) {
            val root = binding.root as ViewGroup
            setSelectedChips(root, interests)
        }
    }

    /**
     * 화면 내의 모든 칩을 순회하며 기존에 선택된 카테고리를 체크 상태로 만듭니다.
     */
    private fun setSelectedChips(viewGroup: ViewGroup, interests: Set<String>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ChipGroup) {
                for (j in 0 until child.childCount) {
                    val chip = child.getChildAt(j) as Chip
                    if (interests.contains(chip.text.toString())) {
                        chip.isChecked = true
                    }
                }
            } else if (child is ViewGroup) {
                setSelectedChips(child, interests)
            }
        }
    }

    private fun initListeners() {
        // 시작하기 또는 저장 버튼 클릭 시
        binding.btnStart.setOnClickListener {
            val nickname = binding.etNickname.text.toString().trim()
            val selectedCategories = getAllSelectedCategories()

            // 필수 입력값 검증
            if (nickname.isEmpty()) {
                Toast.makeText(this, mnu.sofware.todayinhistory.R.string.category_error_nickname_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, mnu.sofware.todayinhistory.R.string.category_select_at_least_one, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // DB 업데이트 및 로컬 저장 후 메인으로 이동
            saveToDatabaseAndStart(nickname, selectedCategories)
        }
    }

    /**
     * 화면 상의 모든 ChipGroup으로부터 선택된 카테고리 텍스트 리스트를 수집합니다.
     */
    private fun getAllSelectedCategories(): List<String> {
        val selectedItems = mutableListOf<String>()
        val root = binding.root as ViewGroup
        collectSelectedChips(root, selectedItems)
        return selectedItems
    }

    /**
     * 뷰 트리를 재귀적으로 탐색하여 모든 Chip의 상태를 확인합니다.
     */
    private fun collectSelectedChips(viewGroup: ViewGroup, list: MutableList<String>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ChipGroup) {
                for (j in 0 until child.childCount) {
                    val chip = child.getChildAt(j) as Chip
                    if (chip.isChecked) list.add(chip.text.toString())
                }
            } else if (child is ViewGroup) {
                collectSelectedChips(child, list)
            }
        }
    }

    /**
     * 입력받은 프로필 정보를 MySQL DB에 동기화하고 로컬 설정을 저장합니다.
     * [교수님 조건] 12장 관심사 데이터베이스 연동 필수 규칙 적용
     */
    private fun saveToDatabaseAndStart(nickname: String, interests: List<String>) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        // 기존 UID가 있으면 사용, 없으면 생성 (중복 계정 방지)
        val uid = sharedPref.getString("uid", null) ?: "user_${System.currentTimeMillis()}"
        val interestsString = interests.joinToString(",")

        lifecycleScope.launch {
            // DB 서버 업데이트 수행
            val isSuccess = MySqlDatabaseManager.saveUserPreferences(uid, nickname, interestsString)
            
            // 로컬 SharedPreferences 업데이트
            saveUserDataLocally(uid, nickname, interests)
            
            if (isSuccess) {
                Toast.makeText(this@CategorySelectionActivity, "프로필이 동기화되었습니다.", Toast.LENGTH_SHORT).show()
            }
            startMainActivity()
        }
    }

    private fun saveUserDataLocally(uid: String, nickname: String, interests: List<String>) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", uid)
            putString("nickname", nickname)
            putStringSet("interests", interests.toSet())
            putBoolean("isFirstRun", false) // 초기 세팅 완료 표시
            apply()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
