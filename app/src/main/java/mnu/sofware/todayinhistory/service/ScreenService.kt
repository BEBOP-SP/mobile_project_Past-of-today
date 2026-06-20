package mnu.sofware.todayinhistory.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.receiver.ScreenOnReceiver

/**
 * 백그라운드에서 화면 켬 이벤트를 계속 감지하기 위한 서비스입니다.
 * [교수님 조건] 최신 안드로이드 OS의 백그라운드 제한을 고려하여 포그라운드 서비스로 구현합니다.
 */
class ScreenService : Service() {

    private var screenReceiver: ScreenOnReceiver? = null

    override fun onCreate() {
        super.onCreate()
        
        // 포그라운드 서비스 알림 설정 (안드로이드 8.0 이상 필수)
        startForegroundServiceWithNotification()
        
        // 브로드캐스트 리시버 동적 등록 (ACTION_SCREEN_ON은 동적 등록만 가능)
        screenReceiver = ScreenOnReceiver()
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenReceiver, filter)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "screen_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TimePop 팝업 서비스",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TimePop 서비스 실행 중")
            .setContentText("오늘의 역사를 알려드리기 위해 화면을 감지하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // 서비스가 강제 종료되어도 시스템이 다시 재시작하도록 설정
    }

    override fun onDestroy() {
        super.onDestroy()
        // 리시버 등록 해제
        screenReceiver?.let {
            unregisterReceiver(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
