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
import com.bumptech.glide.Glide
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.DialogFullscreenPreviewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FullscreenPreviewDialog(
    private val image: BingImage,
    private val bitmap: Bitmap? = null,
    private val onFavoriteClick: ((BingImage) -> Unit)? = null,
    private val onSetWallpaper: ((BingImage) -> Unit)? = null,
    private val onSaveWallpaper: ((BingImage) -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogFullscreenPreviewBinding? = null
    private val binding get() = _binding!!

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

        // Mostrar título opcional
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

    private fun setupListeners() {
        // Botón de retroceso
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        // Botón de favorito
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Botón "Set wallpaper"
        binding.btnSetWallpaper.setOnClickListener {
            showApplyOptions()
        }

        // Botón "Save wallpaper"
        binding.btnSaveWallpaper.setOnClickListener {
            onSaveWallpaper?.invoke(image) ?: run {
                Toast.makeText(requireContext(), 
                    getString(R.string.saved_to_gallery), 
                    Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        updateFavoriteIcon()
        onFavoriteClick?.invoke(image)
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
                when (which) {
                    0 -> onSetWallpaper?.invoke(image) ?: run {
                        Toast.makeText(requireContext(),
                            getString(R.string.wallpaper_applied_home),
                            Toast.LENGTH_SHORT).show()
                    }
                    1 -> onSetWallpaper?.invoke(image) ?: run {
                        Toast.makeText(requireContext(),
                            getString(R.string.wallpaper_applied_lock),
                            Toast.LENGTH_SHORT).show()
                    }
                    2 -> onSetWallpaper?.invoke(image) ?: run {
                        Toast.makeText(requireContext(),
                            getString(R.string.wallpaper_applied_both),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                dismiss()
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
            onSetWallpaper: ((BingImage) -> Unit)? = null,
            onSaveWallpaper: ((BingImage) -> Unit)? = null
        ): FullscreenPreviewDialog {
            return FullscreenPreviewDialog(
                image,
                bitmap,
                onFavoriteClick,
                onSetWallpaper,
                onSaveWallpaper
            )
        }
    }
}
