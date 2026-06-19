package mnu.sofware.todayinhistory.db

import java.util.Date

/**
 * 사용자 정보 엔티티
 * [MySQL 테이블 대응]: users (id, email, nickname, interests, created_at)
 */
data class UserEntity(
    val id: String,             // 유저 고유 ID (이메일 등)
    val nickname: String,       // 닉네임
    val interests: List<String>, // 관심사 리스트 (역사, 과학 등)
    val createdAt: Date = Date() // 계정 생성일
)

/**
 * 스크랩 데이터 엔티티
 * [MySQL 테이블 대응]: scraps (id, user_id, title, year, image_url, content_url, scrapped_at)
 */
data class ScrapEntity(
    val id: Int = 0,            // 자동 증가 PK
    val userId: String,         // 사용자 외래키
    val title: String,          // 사건 제목
    val year: Int,              // 발생 연도
    val imageUrl: String?,      // 이미지 링크
    val contentUrl: String?,    // 위키백과 상세 링크
    val scrappedAt: Date = Date() // 스크랩한 시간
)
