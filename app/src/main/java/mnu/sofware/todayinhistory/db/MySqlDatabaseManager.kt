package mnu.sofware.todayinhistory.db

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * MySQL 데이터베이스와 직접 통신하여 사용자 및 스크랩 데이터를 관리하는 싱글톤 클래스입니다.
 * [교수님 조건] 12장 데이터베이스 구현 항목에 해당하며, 외부 MySQL 서버와의 JDBC 연동을 담당합니다.
 * 백그라운드 스레드(Dispatchers.IO)에서 안전하게 쿼리를 실행합니다.
 */
object MySqlDatabaseManager {
    // 호스트 시스템의 MySQL 서버에 접속하기 위한 URL (에뮬레이터용 10.0.2.2 주소 사용)
    private const val DB_URL = "jdbc:mysql://10.0.2.2:3306/timepop_db?useSSL=false&allowPublicKeyRetrieval=true"
    private const val DB_USER = "root"
    private const val DB_PASS = "3748" // 데이터베이스 비밀번호

    /**
     * JDBC 드라이버를 로드하고 데이터베이스 연결을 생성합니다.
     * @return 성공 시 Connection 객체, 실패 시 null
     */
    private suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        return@withContext try {
            // MySQL JDBC 드라이버 클래스 강제 로드
            Class.forName("com.mysql.jdbc.Driver")
            DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)
        } catch (e: Exception) {
            Log.e("MySQL", "연결 실패: ${e.message}")
            null
        }
    }

    /**
     * 새로운 사용자를 데이터베이스에 등록합니다. (회원가입)
     * @param uid 고유 사용자 ID
     * @param email 사용자 이메일
     * @param password 비밀번호 (암호화 권장)
     * @return 성공 여부
     */
    suspend fun registerUser(uid: String, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val query = "INSERT INTO users (uid, email, password) VALUES (?, ?, ?)"
        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt = conn.prepareStatement(query)
            pstmt.setString(1, uid)
            pstmt.setString(2, email)
            pstmt.setString(3, password)
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "회원가입 에러: ${e.message}")
            false
        }
    }

    /**
     * 이메일과 비밀번호를 검증하여 로그인 처리를 수행하고 유저 정보를 반환합니다.
     * @return 로그인 성공 시 사용자 데이터 맵, 실패 시 null
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
     * 사용자 닉네임과 관심사 설정을 업데이트합니다.
     * [교수님 조건] 12장 관심사 데이터 실시간 저장 및 연동 규칙을 따릅니다.
     * @param uid 대상 사용자 ID
     * @param nickname 설정할 닉네임
     * @param interests 쉼표로 구분된 관심사 태그 문자열
     * @return 성공 여부
     */
    suspend fun saveUserPreferences(uid: String, nickname: String, interests: String): Boolean = withContext(Dispatchers.IO) {
        val query = "UPDATE users SET nickname = ?, interests = ? WHERE uid = ?"

        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            val pstmt: PreparedStatement = conn.prepareStatement(query)
            
            pstmt.setString(1, nickname)
            pstmt.setString(2, interests)
            pstmt.setString(3, uid)
            
            val result = pstmt.executeUpdate()
            conn.close()
            result > 0
        } catch (e: Exception) {
            Log.e("MySQL", "프로필 업데이트 에러: ${e.message}")
            false
        }
    }
    
    /**
     * 사용자가 선택한 사건 정보를 보관함(Scraps)에 저장합니다.
     * [방어적 코딩] 테이블 구조의 호환성을 위해 wiki_url 유무에 따른 2단계 시도를 수행합니다.
     * @param uid 저장할 사용자 ID
     * @param year 사건 발생 연도
     * @param text 사건 설명 내용
     * @param imageUrl 썸네일 이미지 주소
     * @param wikiUrl 관련 위키백과 URL
     * @return 성공 여부
     */
    suspend fun saveScrap(uid: String, year: Int, text: String, imageUrl: String?, wikiUrl: String?): Boolean = withContext(Dispatchers.IO) {
        val queryWithWiki = "INSERT INTO scraps (uid, event_year, event_text, image_url, wiki_url) VALUES (?, ?, ?, ?, ?)"
        val queryWithoutWiki = "INSERT INTO scraps (uid, event_year, event_text, image_url) VALUES (?, ?, ?, ?)"

        return@withContext try {
            val conn = getConnection() ?: return@withContext false
            
            // 1차 시도: 상세 URL을 포함하여 저장 시도
            val success = try {
                val pstmt = conn.prepareStatement(queryWithWiki)
                pstmt.setString(1, uid)
                pstmt.setInt(2, year)
                pstmt.setString(3, text)
                pstmt.setString(4, imageUrl ?: "")
                pstmt.setString(5, wikiUrl ?: "")
                pstmt.executeUpdate() > 0
            } catch (e: Exception) {
                // 테이블에 wiki_url 컬럼이 없는 경우 예외가 발생하므로, 이전 형식으로 재시도
                Log.w("MySQL", "상세 주소 저장 실패, 기본 형식으로 재시도")
                val pstmt = conn.prepareStatement(queryWithoutWiki)
                pstmt.setString(1, uid)
                pstmt.setInt(2, year)
                pstmt.setString(3, text)
                pstmt.setString(4, imageUrl ?: "")
                pstmt.executeUpdate() > 0
            }
            
            conn.close()
            success
        } catch (e: Exception) {
            Log.e("MySQL", "스크랩 최종 실패: ${e.message}")
            false
        }
    }

    /**
     * 특정 사용자가 저장한 모든 스크랩 데이터를 조회합니다.
     * [교수님 조건] 8장 어댑터 뷰(RecyclerView)에 데이터를 공급하기 위해 List<Map> 형식으로 반환합니다.
     * @param uid 조회할 사용자 ID
     * @return 스크랩 정보 리스트
     */
    suspend fun getScrappedEvents(uid: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val scrapList = mutableListOf<Map<String, Any>>()
        val query = "SELECT * FROM scraps WHERE uid = ? ORDER BY created_at DESC"

        try {
            val conn = getConnection() ?: return@withContext scrapList
            val pstmt = conn.prepareStatement(query)
            pstmt.setString(1, uid)
            val rs = pstmt.executeQuery()

            // [방어적 코딩] 테이블에 wiki_url 컬럼이 있는지 동적으로 확인하여 에러 방지
            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            var hasWikiUrlColumn = false
            for (i in 1..columnCount) {
                if (metaData.getColumnName(i).equals("wiki_url", ignoreCase = true)) {
                    hasWikiUrlColumn = true
                    break
                }
            }

            // 결과셋(ResultSet)을 순회하며 리스트에 담기
            while (rs.next()) {
                scrapList.add(mapOf(
                    "id" to rs.getInt("id"),
                    "year" to rs.getInt("event_year"),
                    "text" to rs.getString("event_text"),
                    "imageUrl" to rs.getString("image_url"),
                    "wikiUrl" to if (hasWikiUrlColumn) (rs.getString("wiki_url") ?: "") else "",
                    "createdAt" to rs.getTimestamp("created_at").toString()
                ))
            }
            conn.close()
        } catch (e: Exception) {
            Log.e("MySQL", "조회 실패: ${e.message}")
        }
        return@withContext scrapList
    }

    /**
     * 특정 스크랩 데이터를 삭제합니다.
     * @param scrapId 삭제할 데이터의 고유 ID (PK)
     * @return 성공 여부
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
            Log.e("MySQL", "삭제 실패: ${e.message}")
            false
        }
    }
}
