package mnu.sofware.todayinhistory.db

/**
 * 데이터베이스 작업을 위한 레포지토리 인터페이스
 * [교수님 조건] 모듈화를 위해 DB 접근 로직을 인터페이스로 분리합니다.
 */
interface HistoryRepository {
    
    // --- 사용자 관련 ---
    /** 사용자의 관심사 정보를 저장하거나 업데이트합니다. */
    suspend fun saveUserInterests(userId: String, interests: List<String>): Boolean
    
    /** 사용자의 관심사 정보를 가져옵니다. */
    suspend fun getUserInterests(userId: String): List<String>

    // --- 스크랩 관련 ---
    /** 새로운 역사 사건을 스크랩합니다. */
    suspend fun addScrap(scrap: ScrapEntity): Boolean
    
    /** 저장된 모든 스크랩 목록을 가져옵니다. */
    suspend fun getAllScraps(userId: String): List<ScrapEntity>
    
    /** 특정 스크랩을 삭제합니다. */
    suspend fun deleteScrap(scrapId: Int): Boolean
}
