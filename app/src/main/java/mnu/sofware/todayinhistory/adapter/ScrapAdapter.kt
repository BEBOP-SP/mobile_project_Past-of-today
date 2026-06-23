package mnu.sofware.todayinhistory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ItemScrapListBinding

/**
 * 사용자가 보관한 스크랩 목록을 리사이클러뷰에 바인딩하는 어댑터입니다.
 * [교수님 조건] 커스텀 리사이클러뷰 구현 및 외부 DB(MySQL) 데이터 시각화 항목을 포함합니다.
 * @param onDeleteClick 특정 항목 삭제 시 DB와 연동하기 위한 콜백 함수
 * @param onItemClick 항목 클릭 시 상세 페이지(위키백과)로 이동하기 위한 콜백 함수
 */
class ScrapAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ScrapAdapter.ViewHolder>() {

    // 외부에서 주입받는 데이터 리스트 (Map 형식을 사용하여 동적 필드 대응)
    private var items: List<Map<String, Any>> = emptyList()

    /**
     * 새로운 데이터 리스트를 설정하고 화면을 갱신합니다.
     */
    fun submitList(newList: List<Map<String, Any>>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScrapListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 뷰홀더를 통해 개별 행(Row) 데이터를 바인딩합니다.
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * 보관함 아이템의 개별 뷰 요소를 관리하는 클래스입니다.
     */
    inner class ViewHolder(private val binding: ItemScrapListBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * DB로부터 조회된 데이터를 실제 UI 위젯에 연결합니다.
         */
        fun bind(item: Map<String, Any>) {
            val id = item["id"] as Int
            val year = item["year"] as Int
            val text = item["text"] as String
            val imageUrl = item["imageUrl"] as? String
            val wikiUrl = item["wikiUrl"] as? String ?: ""
            val createdAt = item["createdAt"] as? String ?: ""

            // "19XX년" 형식의 텍스트 구성
            binding.tvScrapYearTitle.text = "${year}년"
            binding.tvScrapDescription.text = text
            
            // 저장 날짜 포맷팅 (YYYY-MM-DD -> MM/DD)
            val datePart = if (createdAt.length >= 10) {
                val parts = createdAt.substring(5, 10).split("-")
                "${parts[0]}/${parts[1]}"
            } else ""
            binding.tvScrapDate.text = binding.root.context.getString(R.string.scrap_date_label, datePart)

            // [최적화] Glide를 사용하여 썸네일 이미지를 효율적으로 로딩합니다.
            Glide.with(binding.ivScrapThumbnail.context)
                .load(imageUrl)
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .centerCrop()
                .into(binding.ivScrapThumbnail)

            // 삭제 아이콘 클릭 리스너 연결
            binding.btnDeleteScrap.setOnClickListener {
                onDeleteClick(id)
            }

            // [교수님 조건] 상세 정보 연동: 카드 전체 클릭 시 위키피디아 상세 페이지로 이동합니다.
            binding.root.setOnClickListener {
                if (wikiUrl.isNotEmpty()) {
                    onItemClick(wikiUrl)
                } else {
                    // 과거 데이터 등 URL 정보가 없는 경우 피드백을 제공합니다.
                    android.widget.Toast.makeText(
                        binding.root.context, 
                        "이 항목은 상세 페이지 정보가 없습니다.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
