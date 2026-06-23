package mnu.sofware.todayinhistory.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import mnu.sofware.todayinhistory.ui.PopupActivity

/**
 * 기기의 화면이 켜지는 이벤트(ACTION_SCREEN_ON)를 감지하는 수신객체입니다.
 * [교수님 조건] 5단계 팝업 시스템 구현 항목에 해당하며, 화면이 켜질 때 역사 잠금화면을 실행합니다.
 */
class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 화면 켬 이벤트인지 확인
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("ScreenOnReceiver", "화면 감지됨: 잠금화면 액티비티를 실행합니다.")
            
            // 잠금화면(PopupActivity) 실행 인텐트 설정
            val popupIntent = Intent(context, PopupActivity::class.java)
            // 서비스나 리시버에서 액티비티를 띄울 때 필요한 플래그 설정
            popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            popupIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(popupIntent)
        }
    }
}
