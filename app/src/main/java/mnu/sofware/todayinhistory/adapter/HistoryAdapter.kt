package mnu.sofware.todayinhistory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import mnu.sofware.todayinhistory.R
import mnu.sofware.todayinhistory.api.HistoryEvent
import mnu.sofware.todayinhistory.databinding.ItemHistoryCardBinding

/**
 * 역사적 사건 목록을 표시하기 위한 리사이클러뷰 어댑터
 * [교수님 조건] 8장 어댑터 뷰 구현 및 Glide 이미지 로딩을 적용합니다.
 * @param onItemClick 카드를 클릭했을 때 실행할 동작 (위키피디아 이동 등)
 * @param onItemLongClick 카드를 길게 눌렀을 때 실행할 동작 (스크랩 저장 등)
 */
class HistoryAdapter(
    private val onItemClick: (HistoryEvent) -> Unit,
    private val onItemLongClick: (HistoryEvent) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

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
        val item = items[position]
        holder.bind(item)
        
        // [교수님 조건] 이벤트 리스너 연결
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true // 이벤트 소비 완료
        }
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
                .transition(DrawableTransitionOptions.withCrossFade()) // [최적화] 부드러운 페이드인 효과
                .override(300, 200) // [최적화] 이미지 크기 제한 (메모리 및 속도)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // [최적화] 디스크 캐싱 활성화
                .placeholder(R.color.light_gray) // [최적화] 가벼운 색상 플레이스홀더
                .error(R.color.light_gray)
                .into(binding.ivThumbnail)
        }
    }
}
