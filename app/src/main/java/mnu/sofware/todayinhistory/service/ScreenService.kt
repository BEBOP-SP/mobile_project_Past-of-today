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
 * 백그라운드에서 지속적으로 화면 상태를 감지하기 위한 포그라운드 서비스입니다.
 * [교수님 조건] 최신 안드로이드 OS(Oreo 이상)의 백그라운드 제한을 우회하기 위해 포그라운드 방식으로 구현되었습니다.
 */
class ScreenService : Service() {

    private var screenReceiver: ScreenOnReceiver? = null

    /**
     * 서비스가 생성될 때 실행됩니다. 알림 채널을 생성하고 리시버를 등록합니다.
     */
    override fun onCreate() {
        super.onCreate()
        
        // 포그라운드 서비스 유지를 위한 상단바 알림 설정
        startForegroundServiceWithNotification()
        
        // 화면 켬/끔 이벤트는 매니페스트가 아닌 동적 등록이 필수임
        screenReceiver = ScreenOnReceiver()
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenReceiver, filter)
    }

    /**
     * 안드로이드 8.0 이상 대응을 위한 알림 채널 생성 및 포그라운드 시작 함수입니다.
     */
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

        // 사용자에게 서비스 실행 중임을 알리는 빌더
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TimePop 서비스 실행 중")
            .setContentText("오늘의 역사를 알려드리기 위해 화면을 감지하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // ID와 함께 포그라운드 상태로 진입
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 시스템에 의해 종료되더라도 자동으로 재시작하도록 설정
        return START_STICKY 
    }

    override fun onDestroy() {
        super.onDestroy()
        // 서비스 종료 시 리소스 해제 (리시버 등록 해제 필수)
        screenReceiver?.let {
            unregisterReceiver(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
