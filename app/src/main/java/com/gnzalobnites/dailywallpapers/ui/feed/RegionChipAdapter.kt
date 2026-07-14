package com.gnzalobnites.dailywallpapers.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.RegionOption
import com.gnzalobnites.dailywallpapers.databinding.ItemRegionChipBinding

class RegionChipAdapter(
    private val regions: List<RegionOption>,
    private val onRegionSelected: (RegionOption) -> Unit
) : RecyclerView.Adapter<RegionChipAdapter.ChipViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemRegionChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val region = regions[position]
        holder.bind(region, position == selectedPosition) {
            selectedPosition = position
            onRegionSelected(region)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = regions.size

    class ChipViewHolder(private val binding: ItemRegionChipBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(region: RegionOption, isSelected: Boolean, onClick: () -> Unit) {
            binding.root.apply {
                text = region.countryName
                chipIcon = context.getDrawable(region.flagRes)
                isChecked = isSelected
                setOnClickListener { onClick() }
            }
        }
    }
}