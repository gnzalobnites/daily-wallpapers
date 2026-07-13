package com.gnzalobnites.dailywallpapers.ui.feed

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.DialogFullscreenPreviewBinding
import com.gnzalobnites.dailywallpapers.ui.main.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FullscreenPreviewDialog(
    private val image: BingImage,
    private val bitmap: Bitmap? = null,
    private val onFavoriteClick: ((BingImage) -> Unit)? = null,
    private val onSaveWallpaper: ((BingImage) -> Unit)? = null,
    private val onWallpaperApplied: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogFullscreenPreviewBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: MainViewModel by activityViewModels()

    private var isFavorite: Boolean = image.isFavorite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFullscreenPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
        setupObservers()
    }

    private fun setupViews() {
        // Cargar imagen
        if (bitmap != null) {
            binding.ivPreview.setImageBitmap(bitmap)
        } else {
            Glide.with(this)
                .load(image.getFullHdUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.ivPreview)
        }

        // Mostrar título
        val title = image.title
        val date = image.getFormattedDate() ?: ""
        if (title.isNotEmpty()) {
            binding.tvTitle.text = if (date.isNotEmpty()) {
                getString(R.string.preview_title, title, date)
            } else {
                title
            }
            binding.tvTitle.visibility = View.VISIBLE
        }

        // Actualizar icono de favorito
        updateFavoriteIcon()
    }

    private fun setupObservers() {
        // Observar mensajes de éxito/error del MainViewModel
        sharedViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                sharedViewModel.clearMessages()
                onWallpaperApplied?.invoke()
                dismiss()
            }
        }

        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                sharedViewModel.clearMessages()
            }
        }

        sharedViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnSetWallpaper.isEnabled = !loading
            binding.btnSaveWallpaper.isEnabled = !loading
        }
    }

    private fun setupListeners() {
        // Botón de retroceso
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        // Botón de favorito
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Botón "Set wallpaper" - Aplica directamente
        binding.btnSetWallpaper.setOnClickListener {
            showApplyOptions()
        }

        // Botón "Save wallpaper"
        binding.btnSaveWallpaper.setOnClickListener {
            onSaveWallpaper?.invoke(image) ?: run {
                sharedViewModel.saveToGallery()
            }
        }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        updateFavoriteIcon()
        onFavoriteClick?.invoke(image)
        sharedViewModel.toggleFavorite()
    }

    private fun updateFavoriteIcon() {
        val icon = if (isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_border
        }
        binding.btnFavorite.setImageResource(icon)
        binding.btnFavorite.imageTintList = if (isFavorite) {
            ContextCompat.getColorStateList(requireContext(), R.color.purple_500)
        } else {
            ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        }
    }

    private fun showApplyOptions() {
        val items = arrayOf(
            getString(R.string.wallpaper_home),
            getString(R.string.wallpaper_lock),
            getString(R.string.wallpaper_both)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.apply_wallpaper_title))
            .setItems(items) { _, which ->
                val location = when (which) {
                    0 -> 1 // Home
                    1 -> 2 // Lock
                    2 -> 3 // Both
                    else -> 1
                }
                // Aplicar directamente usando el MainViewModel
                sharedViewModel.applyWallpaper(image, location)
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            // Ocultar la barra de estado
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            image: BingImage,
            bitmap: Bitmap? = null,
            onFavoriteClick: ((BingImage) -> Unit)? = null,
            onSaveWallpaper: ((BingImage) -> Unit)? = null,
            onWallpaperApplied: (() -> Unit)? = null
        ): FullscreenPreviewDialog {
            return FullscreenPreviewDialog(
                image,
                bitmap,
                onFavoriteClick,
                onSaveWallpaper,
                onWallpaperApplied
            )
        }
    }
}
