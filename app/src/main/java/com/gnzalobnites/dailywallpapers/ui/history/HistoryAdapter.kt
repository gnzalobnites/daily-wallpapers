package com.gnzalobnites.dailywallpapers.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onItemClick: (BingImage) -> Unit,
    private val onItemLongClick: (BingImage) -> Unit
) : ListAdapter<BingImage, HistoryAdapter.ViewHolder>(DiffCallback()) {
    
    class DiffCallback : DiffUtil.ItemCallback<BingImage>() {
        override fun areItemsTheSame(oldItem: BingImage, newItem: BingImage): Boolean {
            return oldItem.startDate == newItem.startDate
        }
        
        override fun areContentsTheSame(oldItem: BingImage, newItem: BingImage): Boolean {
            return oldItem == newItem
        }
    }
    
    class ViewHolder(
        private val binding: ItemHistoryBinding,
        private val onItemClick: (BingImage) -> Unit,
        private val onItemLongClick: (BingImage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(image: BingImage) {
            binding.apply {
                tvTitle.text = image.title
                
                ivPreview.load(image.getFullHdUrl()) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_image)
                    size(400, 400)
                }
                
                ivFavorite.visibility = if (image.isFavorite) View.VISIBLE else View.GONE
                
                root.setOnClickListener { onItemClick(image) }
                root.setOnLongClickListener { 
                    onItemLongClick(image)
                    true
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
} 