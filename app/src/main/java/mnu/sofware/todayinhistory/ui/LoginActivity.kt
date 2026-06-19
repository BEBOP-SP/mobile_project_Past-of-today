package mnu.sofware.todayinhistory.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ActivityLoginBinding

/**
 * 로그인 액티비티
 * [수정 사항] 로그인 화면과 회원가입 화면을 분리하였습니다.
 * 하이퍼링크 스타일의 텍스트를 클릭하여 회원가입 화면으로 이동합니다.
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
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [가짜 로그인] 성공 처리 후 메인 이동
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
            startMainActivity()
        }

        // "회원가입" 텍스트(하이퍼링크 스타일) 클릭 시
        binding.tvGoSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
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
