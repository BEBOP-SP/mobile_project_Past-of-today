package mnu.sofware.todayinhistory.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivitySignupBinding

/**
 * 회원가입 액티비티
 * [교수님 조건] 새로운 사용자 계정을 생성하는 화면입니다.
 * 현재는 Firebase 설정 전이므로 가짜 성공 로직을 포함합니다.
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
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val passwordConfirm = binding.etPasswordConfirm.text.toString()

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

            // [가짜 회원가입] 추후 Firebase 연동 예정
            Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
            
            // 회원가입 성공 후 관심사 선택 화면으로 이동
            val intent = Intent(this, CategorySelectionActivity::class.java)
            startActivity(intent)
            finishAffinity() // 모든 이전 액티비티 종료
        }

        // 로그인 화면으로 돌아가기
        binding.tvGoLogin.setOnClickListener {
            finish()
        }
    }
}
