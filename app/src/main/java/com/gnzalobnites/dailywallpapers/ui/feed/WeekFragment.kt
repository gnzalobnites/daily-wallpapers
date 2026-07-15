package com.gnzalobnites.dailywallpapers.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.databinding.FragmentWeekBinding

class WeekFragment : BaseFeedFragment() {

    private var _binding: FragmentWeekBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeekViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupObservers()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            adapter.submitList(wallpapers)
            showEmptyState(wallpapers.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            showLoading(loading)
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                viewModel.clearMessages()
            }
        }
    }

    override fun getEmptyTextResId(): Int = R.string.empty_week

    override fun loadData() {
        viewModel.loadWeekWallpapers()
    }

    fun reload() {
        if (view != null) loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
