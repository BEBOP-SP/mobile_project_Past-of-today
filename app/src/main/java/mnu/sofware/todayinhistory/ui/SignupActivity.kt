package mnu.sofware.todayinhistory.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivitySignupBinding
import mnu.sofware.todayinhistory.db.MySqlDatabaseManager

/**
 * 새로운 사용자 등록을 처리하는 회원가입 액티비티입니다.
 * [교수님 조건] 12장 데이터베이스 구현 항목에 해당하며, MySQL 서버에 유저 정보를 신규 등록합니다.
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    /**
     * 회원가입 양식 제출 및 로그인 이동 등의 리스너를 설정합니다.
     */
    private fun initListeners() {
        // 회원가입 완료 버튼 클릭 시
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()

            // 입력 필드 누락 검사
            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 확인 검사
            if (password != passwordConfirm) {
                Toast.makeText(this, getString(R.string.login_error_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 고유 UID 생성 및 DB 저장 처리
            val tempUid = "user_${System.currentTimeMillis()}"
            lifecycleScope.launch {
                val success = MySqlDatabaseManager.registerUser(tempUid, email, password)
                if (success) {
                    // 내부 저장소에 임시 UID 기록
                    saveUidLocally(tempUid)
                    
                    Toast.makeText(this@SignupActivity, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                    
                    // 성공 후 초기 관심사 선택 페이지로 자동 연결
                    val intent = Intent(this@SignupActivity, CategorySelectionActivity::class.java)
                    startActivity(intent)
                    finishAffinity() // 가입 중 뒤로가기 방지를 위해 모든 액티비티 종료
                } else {
                    Toast.makeText(this@SignupActivity, "회원가입 실패 (중복 이메일 혹은 서버 연결 확인)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 로그인 화면으로 돌아가기
        binding.tvGoLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * 고유 사용자 식별자를 로컬에 보관합니다.
     */
    private fun saveUidLocally(uid: String) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("uid", uid).apply()
    }
}
