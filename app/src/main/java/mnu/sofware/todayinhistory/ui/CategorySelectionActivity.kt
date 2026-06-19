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

class CategorySelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategorySelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategorySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    private fun initListeners() {
        binding.btnStart.setOnClickListener {
            val nickname = binding.etNickname.text.toString().trim()
            val selectedCategories = getAllSelectedCategories()

            if (nickname.isEmpty()) {
                Toast.makeText(this, mnu.sofware.todayinhistory.R.string.category_error_nickname_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, mnu.sofware.todayinhistory.R.string.category_select_at_least_one, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveToDatabaseAndStart(nickname, selectedCategories)
        }
    }

    /**
     * 모든 ChipGroup에서 체크된 카테고리를 가져옵니다.
     */
    private fun getAllSelectedCategories(): List<String> {
        val selectedItems = mutableListOf<String>()
        
        // 화면 내의 모든 ViewGroup을 뒤져서 ChipGroup을 찾아 선택된 칩 수집
        val root = binding.root as ViewGroup
        collectSelectedChips(root, selectedItems)
        
        return selectedItems
    }

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

    private fun saveToDatabaseAndStart(nickname: String, interests: List<String>) {
        val tempUid = "user_${System.currentTimeMillis()}" // 고유 ID 생성 
        val interestsString = interests.joinToString(",")

        lifecycleScope.launch {
            val isSuccess = MySqlDatabaseManager.saveUserPreferences(tempUid, nickname, interestsString)
            
            saveUserDataLocally(tempUid, nickname, interests)
            if (isSuccess) {
                Toast.makeText(this@CategorySelectionActivity, "동기화 성공!", Toast.LENGTH_SHORT).show()
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
            putBoolean("isFirstRun", false)
            apply()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
