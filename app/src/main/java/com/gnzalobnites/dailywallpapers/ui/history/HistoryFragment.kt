package com.gnzalobnites.dailywallpapers.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.FragmentHistoryBinding
import com.gnzalobnites.dailywallpapers.ui.main.ImagePreviewDialog
import com.gnzalobnites.dailywallpapers.ui.main.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryFragment : Fragment() {
    
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoryViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        viewModel.loadHistory()
    }
    
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Ajustar padding del AppBarLayout
            binding.appBarLayout.updatePadding(top = insets.top)
            
            // Ajustar padding del RecyclerView para las barras laterales e inferior
            binding.recyclerView.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )
            
            windowInsets
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.history_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_all -> {
                        viewModel.loadHistory()
                        true
                    }
                    R.id.action_favorites -> {
                        viewModel.loadFavorites()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { image ->
                viewModel.selectImage(image)
                showPreviewDialog(image)
            },
            onItemLongClick = { image ->
                viewModel.selectImage(image)
                showImageOptions(image)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@HistoryFragment.adapter
            setHasFixedSize(true)
        }
    }
    
    private fun showPreviewDialog(image: BingImage) {
        val dialog = ImagePreviewDialog(
            bitmap = null,
            imageUrl = image.getMobileUrl(),
            title = "${image.title} · ${image.getFormattedDate()}"
        ) {
            showApplyOptions(image)
        }
        dialog.show(childFragmentManager, "ImagePreview")
    }
    
    private fun showApplyOptions(image: BingImage) {
        val items = arrayOf("Pantalla principal", "Pantalla de bloqueo", "Ambas pantallas")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Dónde deseas aplicar el fondo?")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> sharedViewModel.applyWallpaper(image, 1)
                    1 -> sharedViewModel.applyWallpaper(image, 2)
                    2 -> sharedViewModel.applyWallpaper(image, 3)
                }
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
    
    private fun setupObservers() {
        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            adapter.submitList(wallpapers)
            binding.tvEmpty.visibility = if (wallpapers.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        sharedViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                sharedViewModel.clearMessages()
            }
        }
        
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .setPositiveButton("OK", null)
                    .show()
                sharedViewModel.clearMessages()
            }
        }
        
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun showImageOptions(image: BingImage) {
        val items = mutableListOf(
            if (image.isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
        )
        
        if (image.localPath != null) {
            items.add("Ver en galería")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(image.title)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> viewModel.toggleFavorite(image)
                    1 -> openGallery(image)
                }
            }
            .show()
    }
    
    private fun openGallery(image: BingImage) {
        image.localPath?.let { path ->
            Toast.makeText(requireContext(), "Abrir galería: $path", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}