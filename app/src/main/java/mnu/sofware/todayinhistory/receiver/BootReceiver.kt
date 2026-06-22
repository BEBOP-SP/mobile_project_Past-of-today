package mnu.sofware.todayinhistory.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import mnu.sofware.todayinhistory.service.ScreenService

/**
 * 시스템 부팅 완료 이벤트를 수신하여 필요한 백그라운드 서비스를 자동으로 재시작하는 BroadcastReceiver입니다.
 * [최적화] 사용자가 수동으로 앱을 켜지 않아도 잠금화면 기능을 유지할 수 있도록 돕습니다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 부팅 완료 액션 확인
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ScreenService::class.java)
            // 안드로이드 8.0(Oreo) 이상에서는 포그라운드 서비스로 시작해야 함
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
