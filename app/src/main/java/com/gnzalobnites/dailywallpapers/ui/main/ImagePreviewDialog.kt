package com.gnzalobnites.dailywallpapers.ui.main

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import coil.load
import com.gnzalobnites.dailywallpapers.databinding.DialogImagePreviewBinding

class ImagePreviewDialog(
    private val bitmap: Bitmap? = null,
    private val imageUrl: String? = null,
    private val title: String,
    private val onApplyClick: () -> Unit
) : DialogFragment() {

    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (bitmap != null) {
            binding.ivPreview.setImageBitmap(bitmap)
        } else if (imageUrl != null) {
            binding.ivPreview.load(imageUrl) {
                crossfade(true)
            }
        }
        
        binding.tvTitle.text = title
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            onApplyClick()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 