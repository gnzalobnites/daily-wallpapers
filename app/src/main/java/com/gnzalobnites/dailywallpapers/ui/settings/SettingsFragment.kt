package com.gnzalobnites.dailywallpapers.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.WallpaperApp
import com.gnzalobnites.dailywallpapers.databinding.FragmentSettingsBinding
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

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
            binding.layoutUpdateTime.isEnabled = enabled
            binding.layoutUpdateTime.alpha = if (enabled) 1.0f else 0.5f
        }
        
        viewModel.updateHour.observe(viewLifecycleOwner) { hour ->
            viewModel.updateMinute.observe(viewLifecycleOwner) { minute ->
                val timeText = String.format("%02d:%02d", hour, minute)
                binding.tvUpdateTime.text = timeText
            }
        }
        
        viewModel.wallpaperResolution.observe(viewLifecycleOwner) { resolution ->
            val text = when (resolution) {
                "hd" -> getString(R.string.settings_resolution_hd)
                "mobile" -> getString(R.string.settings_resolution_mobile)
                else -> getString(R.string.settings_resolution_mobile)
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
            if (isChecked) {
                val hour = viewModel.updateHour.value ?: 0
                val minute = viewModel.updateMinute.value ?: 0
                WorkerScheduler.scheduleWallpaperWork(requireContext(), hour, minute)
            } else {
                WorkerScheduler.cancelScheduledWork(requireContext())
            }
        }
        
        binding.layoutUpdateTime.setOnClickListener {
            showTimePicker()
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
    
    private fun showTimePicker() {
        val currentHour = viewModel.updateHour.value ?: 0
        val currentMinute = viewModel.updateMinute.value ?: 0
        
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(getString(R.string.settings_update_time))
            .setPositiveButtonText(getString(R.string.apply))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
        
        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute
            
            viewModel.saveUpdateTime(selectedHour, selectedMinute)
            
            if (viewModel.autoUpdate.value == true) {
                WorkerScheduler.scheduleWallpaperWork(requireContext(), selectedHour, selectedMinute)
            }
        }
        
        picker.show(parentFragmentManager, "TIME_PICKER")
    }
    
    private fun showResolutionDialog() {
        val resolutions = arrayOf(
            getString(R.string.settings_resolution_hd),
            getString(R.string.settings_resolution_mobile)
        )
        val values = arrayOf("hd", "mobile")
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
                val selectedLang = values[which]
                
                // Guardar en preferencias
                viewModel.saveLanguage(selectedLang)
                
                // Guardar también en SharedPreferences de la Application
                WallpaperApp.prefs.edit().putString("language", selectedLang).apply()
                
                // Aplicar el idioma inmediatamente
                val localeList = LocaleListCompat.forLanguageTags(selectedLang)
                AppCompatDelegate.setApplicationLocales(localeList)
                
                // Recargar la actividad para ver los cambios inmediatamente
                requireActivity().recreate()
                
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}