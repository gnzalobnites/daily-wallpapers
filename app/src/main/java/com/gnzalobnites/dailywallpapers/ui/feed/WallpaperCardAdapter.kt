package com.gnzalobnites.dailywallpapers.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.ItemWallpaperCardBinding

class WallpaperCardAdapter(
    private val onFavoriteClick: (BingImage) -> Unit,
    private val onApplyClick: (BingImage) -> Unit,
    private val onSaveClick: (BingImage) -> Unit,
    private val onImageClick: (BingImage) -> Unit
) : ListAdapter<BingImage, WallpaperCardAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<BingImage>() {
        override fun areItemsTheSame(oldItem: BingImage, newItem: BingImage): Boolean {
            return oldItem.startDate == newItem.startDate
        }

        override fun areContentsTheSame(oldItem: BingImage, newItem: BingImage): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(
        private val binding: ItemWallpaperCardBinding,
        private val onFavoriteClick: (BingImage) -> Unit,
        private val onApplyClick: (BingImage) -> Unit,
        private val onSaveClick: (BingImage) -> Unit,
        private val onImageClick: (BingImage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: BingImage) {
            binding.apply {
                tvTitle.text = image.title
                tvCopyright.text = image.copyright

                Glide.with(ivWallpaper.context)
                    .load(image.getFullHdUrl())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivWallpaper)

                val favoriteIcon = if (image.isFavorite) {
                    R.drawable.ic_favorite_filled
                } else {
                    R.drawable.ic_favorite_border
                }
                btnFavorite.setImageResource(favoriteIcon)
                btnFavorite.imageTintList = if (image.isFavorite) {
                    ContextCompat.getColorStateList(ivWallpaper.context, R.color.purple_500)
                } else {
                    ContextCompat.getColorStateList(ivWallpaper.context, R.color.grey_600)
                }

                ivWallpaper.setOnClickListener { onImageClick(image) }

                btnFavorite.setOnClickListener { onFavoriteClick(image) }
                btnApply.setOnClickListener { onApplyClick(image) }
                btnSave.setOnClickListener { onSaveClick(image) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWallpaperCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onFavoriteClick, onApplyClick, onSaveClick, onImageClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
