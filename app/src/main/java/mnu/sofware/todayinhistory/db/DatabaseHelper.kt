package mnu.sofware.todayinhistory.db

import android.content.Context

/**
 * 데이터베이스 관리 도우미 클래스
 * [교수님 조건] 12장 데이터베이스 연동을 위해 사용됩니다.
 * Firebase를 주력으로 사용하되, 로컬 데이터 저장이 필요할 경우 SQLite를 추가로 고려합니다.
 */
class DatabaseHelper(private val context: Context) {
    
    /**
     * 사용자가 선택한 스크랩 데이터를 저장합니다.
     * @param data 저장할 역사 사건 정보
     */
    fun saveScrapItem(data: Any) {
        // TODO: Firebase Realtime Database 또는 SQLite 저장 로직 구현
    }

    /**
     * 저장된 모든 스크랩 데이터를 가져옵니다.
     * @return 스크랩 목록
     */
    fun getAllScraps(): List<Any> {
        // TODO: 데이터 조회 로직 구현
        return emptyList()
    }
}
