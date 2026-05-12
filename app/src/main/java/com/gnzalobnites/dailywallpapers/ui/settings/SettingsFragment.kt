package com.gnzalobnites.dailywallpapers.ui.settings

import android.os.Bundle 
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gnzalobnites.dailywallpapers.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gnzalobnites.dailywallpapers.R

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupEdgeToEdge()
        setupToolbar()
        setupObservers()
        setupListeners()
    }
    
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = insets.top)
            binding.nestedScrollView.updatePadding(
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
            title = getString(R.string.title_settings)
        }
    }
    
    private fun setupObservers() {
        viewModel.autoUpdate.observe(viewLifecycleOwner) { enabled ->
            binding.swAutoUpdate.isChecked = enabled
        }
        
        viewModel.wallpaperResolution.observe(viewLifecycleOwner) { resolution ->
            // CAMBIADO: Mostrar "Móvil (Retrato)" como opción predeterminada en lugar de "HD"
            val text = when (resolution) {
                "hd" -> getString(R.string.settings_resolution_hd)
                "mobile" -> getString(R.string.settings_resolution_mobile)
                else -> getString(R.string.settings_resolution_mobile)  // mobile como predeterminado
            }
            binding.tvResolution.text = text
        }
        
        viewModel.saveToHistory.observe(viewLifecycleOwner) { enabled ->
            binding.swSaveHistory.isChecked = enabled
        }
        
        viewModel.autoApply.observe(viewLifecycleOwner) { enabled ->
            binding.swAutoApply.isChecked = enabled
        }
        
        viewModel.darkMode.observe(viewLifecycleOwner) { mode ->
            val text = when (mode) {
                "light" -> getString(R.string.settings_dark_mode_light)
                "dark" -> getString(R.string.settings_dark_mode_dark)
                "system" -> getString(R.string.settings_dark_mode_system)
                else -> getString(R.string.settings_dark_mode_system)
            }
            binding.tvDarkMode.text = text
        }
        
        viewModel.language.observe(viewLifecycleOwner) { lang ->
            val text = when (lang) {
                "es" -> getString(R.string.settings_spanish)
                "en" -> getString(R.string.settings_english)
                else -> getString(R.string.settings_spanish)
            }
            binding.tvLanguage.text = text
        }
    }
    
    private fun setupListeners() {
        binding.swAutoUpdate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveAutoUpdate(isChecked)
        }
        
        binding.layoutResolution.setOnClickListener {
            showResolutionDialog()
        }
        
        binding.swSaveHistory.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveSaveToHistory(isChecked)
        }
        
        binding.swAutoApply.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveAutoApply(isChecked)
        }
        
        binding.layoutDarkMode.setOnClickListener {
            showDarkModeDialog()
        }
        
        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }
    
    private fun showResolutionDialog() {
        val resolutions = arrayOf(
            getString(R.string.settings_resolution_hd),
            getString(R.string.settings_resolution_mobile)
        )
        val values = arrayOf("hd", "mobile")
        // CAMBIADO: Valor predeterminado "mobile" en lugar de "hd"
        val currentValue = viewModel.wallpaperResolution.value ?: "mobile"
        val selectedIndex = values.indexOf(currentValue)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.resolution_dialog_title))
            .setSingleChoiceItems(resolutions, selectedIndex) { dialog, which ->
                viewModel.saveWallpaperResolution(values[which])
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showDarkModeDialog() {
        val modes = arrayOf(
            getString(R.string.settings_dark_mode_light),
            getString(R.string.settings_dark_mode_dark),
            getString(R.string.settings_dark_mode_system)
        )
        val values = arrayOf("light", "dark", "system")
        val currentValue = viewModel.darkMode.value ?: "system"
        val selectedIndex = values.indexOf(currentValue)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dark_mode_dialog_title))
            .setSingleChoiceItems(modes, selectedIndex) { dialog, which ->
                viewModel.saveDarkMode(values[which])
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.settings_spanish),
            getString(R.string.settings_english)
        )
        val values = arrayOf("es", "en")
        val currentValue = viewModel.language.value ?: "es"
        val selectedIndex = values.indexOf(currentValue)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.language_dialog_title))
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                viewModel.saveLanguage(values[which])
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}