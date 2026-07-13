package com.gnzalobnites.dailywallpapers.ui.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.databinding.FragmentWallpaperFeedBinding

class WallpaperFeedFragment : Fragment() {

    private var _binding: FragmentWallpaperFeedBinding? = null
    private val binding get() = _binding!!

    companion object {
        // Configura aquí tus URLs de donación
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
        setupChips()
        setupViewPager()
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
        val regions = arrayOf(
            " Worldwide",
            " USA",
            " Australia",
            " España",
            " UK",
            " Japan"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.region_dialog_title))
            .setItems(regions) { _, which ->
                when (which) {
                    0 -> selectChip(binding.chipWorldwide)
                    1 -> selectChip(binding.chipUSA)
                    2 -> selectChip(binding.chipAustralia)
                    else -> {
                        Toast.makeText(requireContext(),
                            getString(R.string.region_selected, regions[which]),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun setupChips() {
        val chips = listOf(
            binding.chipWorldwide,
            binding.chipUSA,
            binding.chipAustralia
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                selectChip(chip)
            }
        }

        binding.chipWorldwide.isChecked = true
    }

    private fun selectChip(selectedChip: Chip) {
        val chips = listOf(
            binding.chipWorldwide,
            binding.chipUSA,
            binding.chipAustralia
        )

        chips.forEach { chip ->
            chip.isChecked = chip == selectedChip
        }

        val region = when (selectedChip.id) {
            R.id.chipWorldwide -> "worldwide"
            R.id.chipUSA -> "usa"
            R.id.chipAustralia -> "australia"
            else -> "worldwide"
        }
        onRegionSelected(region)
    }

    private fun onRegionSelected(region: String) {
        Toast.makeText(requireContext(),
            getString(R.string.region_filter_applied, region),
            Toast.LENGTH_SHORT).show()
    }

    private fun setupViewPager() {
        val adapter = WallpaperPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_week)
                1 -> getString(R.string.tab_collection)
                2 -> getString(R.string.tab_community)
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
