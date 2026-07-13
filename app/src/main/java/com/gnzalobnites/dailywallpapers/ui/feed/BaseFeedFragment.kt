package com.gnzalobnites.dailywallpapers.ui.feed

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseFeedFragment : Fragment() {

    protected lateinit var adapter: WallpaperCardAdapter
    protected lateinit var recyclerView: RecyclerView

    abstract fun getEmptyTextResId(): Int
    abstract fun loadData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupEdgeToEdge()
        loadData()
    }

    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recyclerView) ?: return

        adapter = WallpaperCardAdapter(
            onFavoriteClick = { image ->
                toggleFavorite(image)
            },
            onApplyClick = { image ->
                showApplyOptions(image)
            },
            onSaveClick = { image ->
                saveToGallery(image)
            },
            onImageClick = { image ->
                showFullscreenPreview(image)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BaseFeedFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupEdgeToEdge() {
        val root = view ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            recyclerView.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )

            windowInsets
        }
    }

    protected fun showApplyOptions(image: BingImage) {
        val items = arrayOf(
            getString(R.string.wallpaper_home),
            getString(R.string.wallpaper_lock),
            getString(R.string.wallpaper_both)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.apply_wallpaper_title))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> Toast.makeText(requireContext(), 
                        getString(R.string.wallpaper_applied_home), Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(requireContext(), 
                        getString(R.string.wallpaper_applied_lock), Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), 
                        getString(R.string.wallpaper_applied_both), Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }

    protected fun toggleFavorite(image: BingImage) {
        val message = if (image.isFavorite) {
            getString(R.string.removed_from_favorites)
        } else {
            getString(R.string.added_to_favorites)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    protected fun saveToGallery(image: BingImage) {
        Toast.makeText(requireContext(), getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
    }

    
    protected fun showFullscreenPreview(image: BingImage, bitmap: Bitmap? = null) {
        val dialog = FullscreenPreviewDialog.newInstance(
            image = image,
            bitmap = bitmap,
            onFavoriteClick = { img ->
                toggleFavorite(img)
                loadData()
            },
            onSetWallpaper = { img ->
                showApplyOptions(img)
            },
            onSaveWallpaper = { img ->
                saveToGallery(img)
            }
        )
        dialog.show(childFragmentManager, "FullscreenPreview")
    }


    protected fun showEmptyState(empty: Boolean) {
        val tvEmpty = view?.findViewById<TextView>(R.id.tvEmpty)
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)

        if (empty) {
            tvEmpty?.visibility = View.VISIBLE
            tvEmpty?.text = getText(getEmptyTextResId())
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        progressBar?.visibility = View.GONE
    }

    protected fun showLoading(loading: Boolean) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
