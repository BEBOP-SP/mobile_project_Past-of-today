package mnu.sofware.todayinhistory.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 화면 켬 이벤트 감지 리시버
 * 사용자가 화면을 켰을 때(ACTION_SCREEN_ON) 역사적 사건 팝업을 띄우는 역할을 합니다.
 */
class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("ScreenOnReceiver", "화면이 켜졌습니다. 팝업 실행을 준비합니다.")
            
            // TODO: 팝업 액티비티 실행 로직 추가
            // val popupIntent = Intent(context, PopupActivity::class.java)
            // popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // context.startActivity(popupIntent)
        }
    }
}
