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
 * 역사적 사건 목록을 카드 형태로 표시하기 위한 리사이클러뷰 어댑터입니다.
 * [교수님 조건] 8장 어댑터 뷰 구현 항목에 해당하며, Glide를 활용한 동적 이미지 로딩을 지원합니다.
 * @param onItemClick 카드를 짧게 클릭했을 때 실행할 동작 (상세 위키 페이지 이동)
 * @param onItemLongClick 카드를 길게 눌렀을 때 실행할 동작 (MySQL 보관함 저장)
 */
class HistoryAdapter(
    private val onItemClick: (HistoryEvent) -> Unit,
    private val onItemLongClick: (HistoryEvent) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // 화면에 표시될 사건 목록 데이터
    private var items: List<HistoryEvent> = emptyList()

    /**
     * 외부(액티비티)에서 새로운 데이터를 주입하고 리스트를 갱신합니다.
     */
    fun submitList(newList: List<HistoryEvent>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 개별 카드 아이템의 바인딩 객체 생성
        val binding = ItemHistoryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        
        // [교수님 조건] 짧게 누르기와 길게 누르기 이벤트 리스너 각각 구현
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true // 롱클릭 이벤트 소비 (Haptic 피드백 지원)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * 리사이클러뷰의 개별 아이템 뷰를 재활용하고 관리하는 뷰홀더 클래스입니다.
     */
    class ViewHolder(private val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * 역사 사건 데이터를 UI 컴포넌트에 연결합니다.
         */
        fun bind(event: HistoryEvent) {
            val year = event.year ?: 0
            val text = event.text ?: ""
            
            // "연도: 사건내용" 형식의 텍스트 설정
            binding.tvYearTitle.text = binding.root.context.getString(
                R.string.history_item_format, year, text
            )
            
            // 위키백과 API로부터 제공받은 썸네일 URL 추출
            val imageUrl = event.pages?.firstOrNull()?.thumbnail?.source
            
            // [교수님 조건] 이미지 로딩 라이브러리 Glide 적용 및 성능 최적화 설정
            Glide.with(binding.ivThumbnail.context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade()) // 부드러운 전환 효과
                .override(300, 200) // 메모리 절약을 위한 이미지 리사이징
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 적극적인 캐싱으로 데이터 사용량 절감
                .placeholder(R.color.light_gray) // 로딩 중 표시할 기본 색상
                .error(R.color.light_gray)      // 로드 실패 시 대체 이미지
                .into(binding.ivThumbnail)
        }
    }
}
