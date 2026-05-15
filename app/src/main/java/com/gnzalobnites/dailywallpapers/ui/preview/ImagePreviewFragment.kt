package com.gnzalobnites.dailywallpapers.ui.preview

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.size.Scale
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.databinding.DialogImagePreviewBinding
import com.gnzalobnites.dailywallpapers.ui.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ImagePreviewFragment : BottomSheetDialogFragment() {
    
    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!
    
    private val sharedViewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    
    companion object {
        private const val ARG_IMAGE = "image"
        private const val ARG_BITMAP = "bitmap"
        
        fun newInstance(image: BingImage, bitmap: Bitmap): ImagePreviewFragment {
            val fragment = ImagePreviewFragment()
            val args = Bundle().apply {
                putSerializable(ARG_IMAGE, image)
                putParcelable(ARG_BITMAP, bitmap)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    private val image: BingImage? by lazy {
        arguments?.getSerializable(ARG_IMAGE) as? BingImage
    }
    
    private val bitmap: Bitmap? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_BITMAP, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_BITMAP)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        setupListeners()
    }
    
    private fun setupViews() {
        image?.let { img ->
            binding.tvTitle.text = img.title
            binding.tvDate.text = img.getFormattedDate()
        }
        
        bitmap?.let { bmp ->
            binding.ivPreview.load(bmp) {
                crossfade(true)
                scale(Scale.FILL)
            }
        } ?: run {
            // Fallback: cargar desde URL si no hay bitmap
            image?.getFullHdUrl()?.let { url ->
                binding.ivPreview.load(url) {
                    crossfade(true)
                    scale(Scale.FILL)
                    placeholder(R.drawable.placeholder_image)
                    error(R.drawable.placeholder_image)
                }
            }
        }
    }
    
    private fun setupObservers() {
        // Observar mensajes de éxito/error del ViewModel compartido
        sharedViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnApply.isEnabled = !isLoading
        }
    }
    
    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnApply.setOnClickListener {
            showApplyOptions()
        }
    }
    
    private fun showApplyOptions() {
        val items = mutableListOf(getString(R.string.wallpaper_home))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            items.add(getString(R.string.wallpaper_lock))
            items.add(getString(R.string.wallpaper_both))
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.apply_wallpaper_title))
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> applyWallpaper(false, false)
                    1 -> applyWallpaper(true, false)
                    2 -> applyWallpaper(true, true)
                }
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun applyWallpaper(setOnLockScreen: Boolean, setOnHomeScreen: Boolean = true) {
        bitmap?.let { bmp ->
            lifecycleScope.launch {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(requireContext())
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (setOnLockScreen && setOnHomeScreen) {
                            // Ambas pantallas
                            wallpaperManager.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM)
                        } else if (setOnLockScreen) {
                            // Solo bloqueo
                            wallpaperManager.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
                        } else {
                            // Solo home
                            wallpaperManager.setBitmap(bmp)
                        }
                    } else {
                        // Versiones anteriores a N
                        wallpaperManager.setBitmap(bmp)
                    }
                    
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_applied),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    dismiss()
                    
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_applying, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } ?: run {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_image_available),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun getTheme(): Int = R.style.FullScreenDialogTheme
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}