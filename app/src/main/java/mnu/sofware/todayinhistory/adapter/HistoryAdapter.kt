package mnu.sofware.todayinhistory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.api.HistoryEvent
import mnu.sofware.todayinhistory.databinding.ItemHistoryCardBinding

/**
 * 역사적 사건 목록을 표시하기 위한 리사이클러뷰 어댑터
 * [교수님 조건] 8장 어댑터 뷰 구현 및 Glide 이미지 로딩을 적용합니다.
 */
class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // 아이템 데이터를 담을 리스트
    private var items: List<HistoryEvent> = emptyList()

    /**
     * 외부에서 데이터를 설정할 때 사용합니다.
     */
    fun submitList(newList: List<HistoryEvent>) {
        items = newList
        notifyDataSetChanged()
    }

    /**
     * 아이템 뷰를 생성하고 뷰홀더를 반환합니다.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    /**
     * 뷰홀더에 데이터를 바인딩합니다.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * 개별 아이템 뷰를 관리하는 뷰홀더 클래스
     */
    class ViewHolder(private val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * 데이터를 실제 뷰에 연결하는 함수
         */
        fun bind(event: HistoryEvent) {
            // 연도와 제목 설정 (Null 안전 처리)
            val year = event.year ?: 0
            val text = event.text ?: ""
            
            binding.tvYearTitle.text = binding.root.context.getString(
                R.string.history_item_format, year, text
            )
            
            // 위키백과 썸네일 이미지가 있는 경우 Glide로 로드
            val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
            
            Glide.with(binding.ivThumbnail.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
                .error(R.drawable.ic_launcher_background)       // 에러 시 이미지
                .into(binding.ivThumbnail)
        }
    }
}
