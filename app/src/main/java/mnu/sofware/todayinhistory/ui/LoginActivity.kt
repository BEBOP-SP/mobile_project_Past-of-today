package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivityLoginBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager

/**
 * 로그인 액티비티
 * [수정 사항] MySQL 데이터베이스와 연동하여 실제 사용자 검증을 수행합니다.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    /**
     * 로그인 관련 이벤트 리스너 초기화
     */
    private fun initListeners() {
        // 로그인 버튼 클릭 시
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [실제 로그인 로직] DB 검증
            lifecycleScope.launch {
                val userData = MySqlDatabaseManager.loginUser(email, password)
                if (userData != null) {
                    // 로그인 성공: 유저 정보 로컬 저장
                    saveUserDataLocally(userData)
                    
                    Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    startMainActivity()
                } else {
                    // 로그인 실패
                    Toast.makeText(this@LoginActivity, "이메일 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // "회원가입" 텍스트(하이퍼링크 스타일) 클릭 시
        binding.tvGoSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 로그인 성공 시 유저 정보를 SharedPreferences에 저장
     */
    private fun saveUserDataLocally(userData: Map<String, String>) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", userData["uid"])
            putString("nickname", userData["nickname"])
            putStringSet("interests", userData["interests"]?.split(",")?.toSet() ?: emptySet())
            putBoolean("isFirstRun", false)
            apply()
        }
    }

    /**
     * 로그인 성공 후 메인 화면으로 이동
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
