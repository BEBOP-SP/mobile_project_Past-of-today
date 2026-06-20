package mnu.sofware.todayinhistory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.databinding.ItemScrapListBinding

/**
 * 사용자가 스크랩한 사건 목록을 표시하는 어댑터
 * [교수님 조건] RecyclerView 및 커스텀 어댑터 필수 적용.
 * @param onDeleteClick 삭제 버튼 클릭 시 실행할 콜백
 * @param onItemClick 아이템 클릭 시 실행할 콜백 (상세 페이지 이동)
 */
class ScrapAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ScrapAdapter.ViewHolder>() {

    private var items: List<Map<String, Any>> = emptyList()

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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemScrapListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Map<String, Any>) {
            val id = item["id"] as Int
            val year = item["year"] as Int
            val text = item["text"] as String
            val imageUrl = item["imageUrl"] as? String
            val wikiUrl = item["wikiUrl"] as? String ?: ""
            val createdAt = item["createdAt"] as? String ?: ""

            // "1987년: 제목" 형식으로 표시
            binding.tvScrapYearTitle.text = binding.root.context.getString(
                R.string.history_item_format, year, ""
            ).replace(": ", "") + "년"
            
            binding.tvScrapDescription.text = text
            
            // 날짜 표시 (예: 2023-11-15 10:00:00 -> 11/15)
            val datePart = if (createdAt.length >= 10) {
                val parts = createdAt.substring(5, 10).split("-")
                "${parts[0]}/${parts[1]}"
            } else ""
            binding.tvScrapDate.text = binding.root.context.getString(R.string.scrap_date_label, datePart)

            // 이미지 로딩
            Glide.with(binding.ivScrapThumbnail.context)
                .load(imageUrl)
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .centerCrop()
                .into(binding.ivScrapThumbnail)

            // 삭제 버튼
            binding.btnDeleteScrap.setOnClickListener {
                onDeleteClick(id)
            }

            // [수정] 카드 클릭 시 위키피디아 이동 및 피드백 추가
            binding.root.setOnClickListener {
                if (wikiUrl.isNotEmpty()) {
                    onItemClick(wikiUrl)
                } else {
                    android.widget.Toast.makeText(
                        binding.root.context, 
                        "이 항목은 상세 페이지 정보가 없습니다. (최근 스크랩 항목만 가능)", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
