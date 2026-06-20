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
 * 회원가입 액티비티
 * [교수님 조건] 새로운 사용자 계정을 생성하는 화면입니다.
 * [수정 사항] MySQL 데이터베이스와 연동하여 실제 회원가입을 처리합니다.
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
     * 회원가입 관련 이벤트 리스너 초기화
     */
    private fun initListeners() {
        // 회원가입 완료 버튼 클릭
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()

            // [방어적 코딩] 빈 칸 확인
            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [방어적 코딩] 비밀번호 일치 여부 확인
            if (password != passwordConfirm) {
                Toast.makeText(this, getString(R.string.login_error_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [실제 회원가입 로직] MySQL DB에 저장
            val tempUid = "user_${System.currentTimeMillis()}"
            lifecycleScope.launch {
                val success = MySqlDatabaseManager.registerUser(tempUid, email, password)
                if (success) {
                    // 로컬에도 기본 UID 저장
                    saveUidLocally(tempUid)
                    
                    Toast.makeText(this@SignupActivity, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                    
                    // 회원가입 성공 후 관심사 선택 화면으로 이동
                    val intent = Intent(this@SignupActivity, CategorySelectionActivity::class.java)
                    startActivity(intent)
                    finishAffinity() 
                } else {
                    Toast.makeText(this@SignupActivity, "회원가입 실패 (네트워크 또는 중복 이메일 확인)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 로그인 화면으로 돌아가기
        binding.tvGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun saveUidLocally(uid: String) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("uid", uid).apply()
    }
}
