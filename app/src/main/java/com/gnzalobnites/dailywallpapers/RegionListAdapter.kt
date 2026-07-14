package com.gnzalobnites.dailywallpapers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class RegionListAdapter(
    context: android.content.Context,
    private val regions: List<RegionOption>,
    private var selectedTag: String
) : ArrayAdapter<RegionOption>(context, 0, regions) {

    fun setSelected(tag: String) {
        selectedTag = tag
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_region, parent, false)

        val region = regions[position]
        view.findViewById<android.widget.ImageView>(R.id.ivRegionFlag).setImageResource(region.flagRes)
        view.findViewById<android.widget.TextView>(R.id.tvRegionCountry).text = region.countryName
        view.findViewById<android.widget.TextView>(R.id.tvRegionLanguage).text = region.languageName
        view.findViewById<android.widget.ImageView>(R.id.ivRegionCheck).visibility =
            if (region.localeTag == selectedTag) View.VISIBLE else View.GONE

        return view
    }
}