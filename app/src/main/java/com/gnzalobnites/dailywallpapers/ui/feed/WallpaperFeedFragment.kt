package com.gnzalobnites.dailywallpapers.ui.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import com.gnzalobnites.dailywallpapers.databinding.FragmentWallpaperFeedBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.gnzalobnites.dailywallpapers.RegionManager
import com.gnzalobnites.dailywallpapers.RegionListAdapter
import com.gnzalobnites.dailywallpapers.RegionOption

class WallpaperFeedFragment : Fragment() {

    private var _binding: FragmentWallpaperFeedBinding? = null
    private val binding get() = _binding!!

    private var currentRegion: String = "es-ES"
    private lateinit var chipAdapter: RegionChipAdapter

    companion object {
        private const val DONATION_URL = "https://www.buymeacoffee.com/tuusuario"
        private const val DONATION_URL_PAYPAL = "https://www.paypal.me/tuusuario"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWallpaperFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()
        setupToolbar()
        setupRegionChips()
        setupViewPager()
        loadSavedRegion()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.toolbar.updatePadding(top = insets.top)

            binding.viewPager.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )

            windowInsets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                showMenuDialog()
            }

            binding.ivDonate.setOnClickListener {
                showDonationDialog()
            }

            binding.ivFlag.setOnClickListener {
                showRegionDialog()
            }
        }
    }

    private fun setupRegionChips() {
        // Obtener todas las regiones de RegionManager
        val regions = RegionManager.REGIONS
        
        chipAdapter = RegionChipAdapter(regions) { selectedRegion ->
            onRegionSelected(selectedRegion)
        }
        
        binding.rvRegionChips.adapter = chipAdapter
    }

    private fun loadSavedRegion() {
        viewLifecycleOwner.lifecycleScope.launch {
            val prefs = PreferencesManager(requireContext())
            currentRegion = prefs.region.first()
            updateFlagIcon()
            
            // Seleccionar el chip correspondiente
            val index = RegionManager.REGIONS.indexOfFirst { it.localeTag == currentRegion }
            if (index >= 0) {
                // Notificar al adaptador que seleccione esta posición
                // (El adaptador maneja la selección internamente)
                // Por ahora solo actualizamos la bandera
            }
        }
    }

    private fun updateFlagIcon() {
        val region = RegionManager.findByTag(currentRegion)
        binding.ivFlag.setImageResource(region.flagRes)
    }

    private fun showMenuDialog() {
        val items = arrayOf(
            getString(R.string.menu_settings),
            getString(R.string.menu_about)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.app_name))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.action_feed_to_settings)
                    1 -> findNavController().navigate(R.id.action_feed_to_about)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDonationDialog() {
        val items = arrayOf(
            getString(R.string.donation_buy_me_coffee),
            getString(R.string.donation_paypal)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.donation_title))
            .setMessage(getString(R.string.donation_message))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openUrl(DONATION_URL)
                    1 -> openUrl(DONATION_URL_PAYPAL)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showRegionDialog() {
        val adapter = RegionListAdapter(requireContext(), RegionManager.REGIONS, currentRegion)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.region_dialog_title))
            .setAdapter(adapter) { dialog, which ->
                val selected = RegionManager.REGIONS[which]
                onRegionSelected(selected)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun onRegionSelected(region: RegionOption) {
        currentRegion = region.localeTag

        viewLifecycleOwner.lifecycleScope.launch {
            val prefs = PreferencesManager(requireContext())
            prefs.saveRegion(region.localeTag)
            // El save dispara la Flow de PreferencesManager.region; WeekViewModel
            // no la escucha automáticamente, así que forzamos la recarga:
            (childFragmentManager.findFragmentByTag("f0") as? WeekFragment)?.reload()
        }

        // Actualizar idioma
        val localeList = LocaleListCompat.forLanguageTags(region.localeTag)
        AppCompatDelegate.setApplicationLocales(localeList)

        // Actualizar bandera
        updateFlagIcon()

        // Mostrar mensaje
        Toast.makeText(
            requireContext(),
            getString(R.string.region_filter_applied, region.countryName),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun setupViewPager() {
        val adapter = WallpaperPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_week)
                1 -> getString(R.string.tab_favorites)
                2 -> getString(R.string.tab_history)
                else -> ""
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class WallpaperPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WeekFragment()
                1 -> FavoritesFragment()
                2 -> HistoryFeedFragment()
                else -> WeekFragment()
            }
        }
    }
}