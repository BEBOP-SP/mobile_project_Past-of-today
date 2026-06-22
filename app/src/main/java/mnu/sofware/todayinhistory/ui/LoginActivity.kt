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
 * 사용자 로그인을 담당하는 액티비티입니다.
 * [교수님 조건] 12장 데이터베이스 구현 항목에 해당하며, MySQL 서버를 통해 사용자 인증을 수행합니다.
 * SharedPreferences를 통해 로그인 상태를 유지합니다.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 뷰 바인딩 설정 및 화면 초기화
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    /**
     * 버튼 클릭 등 화면의 이벤트 리스너를 설정합니다.
     */
    private fun initListeners() {
        // 로그인 버튼 클릭 시 실행
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 입력값 유효성 검사
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비동기 스코프에서 데이터베이스 검증 수행
            lifecycleScope.launch {
                val userData = MySqlDatabaseManager.loginUser(email, password)
                if (userData != null) {
                    // 로그인 성공 시: 유저 정보를 기기 내부에 저장
                    saveUserDataLocally(userData)
                    
                    Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    startMainActivity()
                } else {
                    // 로그인 실패 시: 경고 메시지 출력
                    Toast.makeText(this@LoginActivity, "이메일 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 회원가입 페이지로 이동
        binding.tvGoSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 로그인 성공 시 서버에서 받은 유저 정보를 로컬 SharedPreferences에 캐싱합니다.
     * @param userData 서버로부터 조회된 유저 데이터 맵
     */
    private fun saveUserDataLocally(userData: Map<String, String>) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", userData["uid"])
            putString("nickname", userData["nickname"])
            // 관심사 데이터는 쉼표 구분 문자열로 저장됨
            putStringSet("interests", userData["interests"]?.split(",")?.toSet() ?: emptySet())
            putBoolean("isFirstRun", false) // 초기 실행 여부 업데이트
            apply()
        }
    }

    /**
     * 메인 화면으로 이동하며 로그인 스택을 정리합니다.
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
