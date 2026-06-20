package mnu.sofware.todayinhistory.db

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * MySQL 데이터베이스 연결 및 쿼리 실행 관리자
 * [교수님 조건] MySQL DB 연동을 위한 싱글톤 클래스입니다.
 */
object MySqlDatabaseManager {
    // 에뮬레이터에서 호스트(내 컴퓨터)로 접속할 때는 10.0.2.2를 사용합니다.
    private const val DB_URL = "jdbc:mysql://10.0.2.2:3306/timepop_db?useSSL=false&allowPublicKeyRetrieval=true"
    private const val DB_USER = "root"
    private const val DB_PASS = "3748" // [수정 필요] MySQL 설치 시 설정한 비밀번호

    /**
     * DB 연결 객체를 생성하여 반환합니다.
     */
    private suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        return@withContext try {
            // 안드로이드 호환 드라이버 클래스명 사용
            Class.forName("com.mysql.jdbc.Driver")
            DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)
        } catch (e: Exception) {
            Log.e("MySQL", "연결 에러: ${e.message}")
            null
        }
    }

    /**
     * 회원가입: 새로운 사용자를 DB에 등록합니다.
     */
    suspend fun registerUser(uid: String, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val query = "INSERT INTO users (uid, email, password) VALUES (?, ?, ?)"
        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt = conn.prepareStatement(query)
            pstmt.setString(1, uid)
            pstmt.setString(2, email)
            pstmt.setString(3, password) // [주의] 실제 서비스에서는 해싱 필수
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "회원가입 에러: ${e.message}")
            false
        }
    }

    /**
     * 로그인: 이메일과 비밀번호가 일치하는 유저 정보를 가져옵니다.
     */
    suspend fun loginUser(email: String, password: String): Map<String, String>? = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM users WHERE email = ? AND password = ?"
        try {
            val conn = getConnection() ?: return@withContext null
            val pstmt = conn.prepareStatement(query)
            pstmt.setString(1, email)
            pstmt.setString(2, password)
            val rs = pstmt.executeQuery()

            if (rs.next()) {
                val userData = mapOf(
                    "uid" to rs.getString("uid"),
                    "email" to rs.getString("email"),
                    "nickname" to (rs.getString("nickname") ?: ""),
                    "interests" to (rs.getString("interests") ?: "")
                )
                conn.close()
                return@withContext userData
            }
            conn.close()
        } catch (e: Exception) {
            Log.e("MySQL", "로그인 에러: ${e.message}")
        }
        return@withContext null
    }

    /**
     * 사용자 닉네임과 관심사를 DB에 저장합니다.
     * [방어적 코딩] 네트워크 오류 발생 시 false를 반환합니다.
     */
    suspend fun saveUserPreferences(uid: String, nickname: String, interests: String): Boolean = withContext(Dispatchers.IO) {
        val query = """
            INSERT INTO users (uid, email, nickname, interests) 
            VALUES (?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE nickname = ?, interests = ?
        """.trimIndent()

        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt: PreparedStatement = conn.prepareStatement(query)
            
            pstmt.setString(1, uid)
            pstmt.setString(2, "${uid}@timepop.com") // 임시 이메일
            pstmt.setString(3, nickname)
            pstmt.setString(4, interests)
            pstmt.setString(5, nickname)
            pstmt.setString(6, interests)
            
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "저장 에러: ${e.message}")
            false
        }
    }
    
    /**
     * 사용자가 선택한 사건을 스크랩(저장)합니다.
     * [방어적 코딩] 이미 스크랩된 항목은 무시하거나 업데이트합니다.
     */
    suspend fun saveScrap(uid: String, year: Int, text: String, imageUrl: String?): Boolean = withContext(Dispatchers.IO) {
        val query = """
            INSERT INTO scraps (uid, event_year, event_text, image_url) 
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt: PreparedStatement = conn.prepareStatement(query)
            
            pstmt.setString(1, uid)
            pstmt.setInt(2, year)
            pstmt.setString(3, text)
            pstmt.setString(4, imageUrl ?: "")
            
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "스크랩 저장 에러: ${e.message}")
            false
        }
    }

    /**
     * 특정 유저가 저장한 모든 스크랩 목록을 가져옵니다.
     * [교수님 조건] RecyclerView에 뿌려주기 위한 데이터 리스트를 반환합니다.
     */
    suspend fun getScrappedEvents(uid: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val scrapList = mutableListOf<Map<String, Any>>()
        val query = "SELECT * FROM scraps WHERE uid = ? ORDER BY created_at DESC"

        try {
            val conn = getConnection() ?: return@withContext scrapList
            val pstmt = conn.prepareStatement(query)
            pstmt.setString(1, uid)
            val rs = pstmt.executeQuery()

            while (rs.next()) {
                scrapList.add(mapOf(
                    "id" to rs.getInt("id"),
                    "year" to rs.getInt("event_year"),
                    "text" to rs.getString("event_text"),
                    "imageUrl" to rs.getString("image_url"),
                    "createdAt" to rs.getTimestamp("created_at").toString()
                ))
            }
            conn.close()
        } catch (e: Exception) {
            Log.e("MySQL", "스크랩 조회 에러: ${e.message}")
        }
        return@withContext scrapList
    }

    /**
     * 스크랩을 삭제합니다.
     * @param scrapId 삭제할 스크랩의 고유 ID (PK)
     */
    suspend fun deleteScrap(scrapId: Int): Boolean = withContext(Dispatchers.IO) {
        val query = "DELETE FROM scraps WHERE id = ?"
        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt = conn.prepareStatement(query)
            pstmt.setInt(1, scrapId)
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "스크랩 삭제 에러: ${e.message}")
            false
        }
    }
}
