package com.gnzalobnites.dailywallpapers.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.databinding.FragmentFavoritesBinding

class FavoritesFragment : BaseFeedFragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            adapter.submitList(wallpapers)
            showEmptyState(wallpapers.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            showLoading(loading)
        }
    }

    override fun getEmptyTextResId(): Int = R.string.empty_collection

    override fun loadData() {
        viewModel.loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
