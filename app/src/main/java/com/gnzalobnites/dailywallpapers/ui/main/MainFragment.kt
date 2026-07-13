package com.gnzalobnites.dailywallpapers.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.databinding.FragmentMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveToGallery()
        } else {
            Toast.makeText(requireContext(),
                getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()
        setupToolbar()
        setupObservers()
        setupListeners()
        viewModel.loadTodayImage()
        viewModel.checkForUpdatesSilently()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            binding.toolbar.updatePadding(top = insets.top)
            binding.infoContainer.updatePadding(bottom = insets.bottom + (24 * resources.displayMetrics.density).toInt())
            
            windowInsets
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_history -> {
                        findNavController().navigate(R.id.action_main_to_history)
                        true
                    }
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_main_to_settings)
                        true
                    }
                    R.id.action_about -> {
                        findNavController().navigate(R.id.action_main_to_about)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.currentImage.observe(viewLifecycleOwner) { wallpaper ->
            if (wallpaper != null) {
                binding.tvTitle.text = wallpaper.title
                
                val copyrightText = wallpaper.copyright
                val dateText = wallpaper.getFormattedDate()
                binding.tvCopyright.text = "$copyrightText · $dateText"
            }
        }

        viewModel.currentImage.observe(viewLifecycleOwner) { image ->
            image?.let {
                val favoriteIcon = if (it.isFavorite)
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite_filled)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite_border)
                binding.btnFavorite.icon = favoriteIcon
            }
        }

        viewModel.currentBitmap.observe(viewLifecycleOwner) { bitmap ->
            bitmap?.let {
                binding.btnApply.isEnabled = true
                binding.btnSave.isEnabled = true
                binding.btnFavorite.isEnabled = true
                binding.btnApply.text = getString(R.string.apply)
                
                Glide.with(this)
                    .load(it)
                    .centerCrop()
                    .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.ivWallpaper)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            binding.btnApply.isEnabled = !isLoading && viewModel.currentBitmap.value != null
            binding.btnSave.isEnabled = !isLoading && viewModel.currentBitmap.value != null
            binding.btnFavorite.isEnabled = !isLoading && viewModel.currentBitmap.value != null
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showErrorDialog(it)
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
        
        viewModel.autoUpdate.observe(viewLifecycleOwner) { enabled ->
            // No tenemos SwipeRefreshLayout visible, pero mantenemos la lógica
        }
    }

    private fun setupListeners() {
        binding.ivWallpaper.setOnClickListener {
            showImageDialog()
        }

        binding.btnApply.setOnClickListener {
            val currentImage = viewModel.currentImage.value ?: return@setOnClickListener
            
            val items = arrayOf(
                getString(R.string.wallpaper_home),
                getString(R.string.wallpaper_lock),
                getString(R.string.wallpaper_both)
            )
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.apply_wallpaper_title))
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> viewModel.applyWallpaper(currentImage, 1)
                        1 -> viewModel.applyWallpaper(currentImage, 2)
                        2 -> viewModel.applyWallpaper(currentImage, 3)
                    }
                }
                .setNeutralButton(getString(R.string.cancel), null)
                .show()
        }

        binding.btnSave.setOnClickListener {
            checkStoragePermissionAndSave()
        }

        binding.btnFavorite.setOnClickListener { view ->
            view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
            
            viewModel.toggleFavorite()
        }
    }
    
    private fun showImageDialog() {
        val bitmap = viewModel.currentBitmap.value
        val image = viewModel.currentImage.value
        if (bitmap != null && image != null) {
            val dialog = ImagePreviewDialog(
                bitmap = bitmap,
                imageUrl = null,
                title = "${image.title} · ${image.getFormattedDate()}"
            ) {
                val items = arrayOf(
                    getString(R.string.wallpaper_home),
                    getString(R.string.wallpaper_lock),
                    getString(R.string.wallpaper_both)
                )
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.apply_wallpaper_title))
                    .setItems(items) { _, which ->
                        when (which) {
                            0 -> viewModel.applyWallpaper(image, 1)
                            1 -> viewModel.applyWallpaper(image, 2)
                            2 -> viewModel.applyWallpaper(image, 3)
                        }
                    }
                    .setNeutralButton(getString(R.string.cancel), null)
                    .show()
            }
            dialog.show(childFragmentManager, "ImagePreview")
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error_loading, "").split(":")[0])
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkStoragePermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGallery()
        } else {
            when {
                ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    saveToGallery()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun saveToGallery() {
        viewModel.saveToGallery()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}