package com.gnzalobnites.dailywallpapers.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.gnzalobnites.dailywallpapers.AlarmScheduler
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.WallpaperApp
import com.gnzalobnites.dailywallpapers.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    
    // Registrar el lanzador del permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), 
                getString(R.string.notification_permission_granted), 
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), 
                getString(R.string.notification_permission_denied), 
                Toast.LENGTH_SHORT).show()
        }
    }
    
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
            // Solo actualizar si el valor es diferente para evitar bucles
            if (binding.swAutoUpdate.isChecked != enabled) {
                binding.swAutoUpdate.isChecked = enabled
            }
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
            if (binding.swSaveHistory.isChecked != enabled) {
                binding.swSaveHistory.isChecked = enabled
            }
        }
        
        viewModel.autoApply.observe(viewLifecycleOwner) { enabled ->
            // Solo actualizar si el valor es diferente para evitar bucles
            if (binding.swAutoApply.isChecked != enabled) {
                binding.swAutoApply.isChecked = enabled
            }
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
        // Usamos setOnClickListener en lugar de setOnCheckedChangeListener para evitar el bucle
        binding.swAutoUpdate.setOnClickListener {
            val isChecked = binding.swAutoUpdate.isChecked
            viewModel.saveAutoUpdate(isChecked)
            
            if (isChecked) {
                val hour = viewModel.updateHour.value ?: 0
                val minute = viewModel.updateMinute.value ?: 0
                val success = AlarmScheduler.scheduleExactAlarm(requireContext(), hour, minute)
                if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(requireContext(), 
                        getString(R.string.need_exact_alarm_permission), 
                        Toast.LENGTH_LONG).show()
                    AlarmScheduler.requestExactAlarmPermission(requireContext())
                }
                checkAndRequestNotificationPermission()
            } else {
                AlarmScheduler.cancelAlarm(requireContext())
            }
        }
        
        binding.swSaveHistory.setOnClickListener {
            viewModel.saveSaveToHistory(binding.swSaveHistory.isChecked)
        }
        
        binding.swAutoApply.setOnClickListener {
            viewModel.saveAutoApply(binding.swAutoApply.isChecked)
        }
        
        // Listener para seleccionar hora
        binding.layoutUpdateTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.layoutResolution.setOnClickListener {
            showResolutionDialog()
        }
        
        binding.layoutDarkMode.setOnClickListener {
            showDarkModeDialog()
        }
        
        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }
    
    /**
     * TimePicker con activación automática completa
     * Este es el método corregido que evita el bucle infinito
     */
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
            
            viewLifecycleOwner.lifecycleScope.launch {
                // 1. Guardar en el ViewModel (Los Observers actualizarán los switches solos)
                viewModel.saveUpdateTime(selectedHour, selectedMinute)
                viewModel.saveAutoUpdate(true)
                viewModel.saveAutoApply(true)
                
                // 2. Solo actualizamos el texto de la hora
                binding.tvUpdateTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
                
                // 3. Programar alarma inmediatamente
                val success = AlarmScheduler.scheduleExactAlarm(requireContext(), selectedHour, selectedMinute)
                
                // 4. Manejar caso de permiso denegado en Android 12+
                if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AlarmScheduler.requestExactAlarmPermission(requireContext())
                    Toast.makeText(requireContext(), 
                        getString(R.string.need_exact_alarm_permission), 
                        Toast.LENGTH_LONG).show()
                } else if (success) {
                    Toast.makeText(
                        requireContext(), 
                        getString(R.string.alarm_scheduled, String.format("%02d:%02d", selectedHour, selectedMinute)), 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // 5. Solicitar permiso de notificaciones si es necesario
                checkAndRequestNotificationPermission()
            }
        }
        
        picker.show(parentFragmentManager, "TIME_PICKER")
    }
    
    /**
     * Método alternativo usando TimePickerDialog (Android nativo)
     * Por si prefieres este estilo en lugar de MaterialTimePicker
     */
    private fun showTimePickerNative() {
        val calendar = Calendar.getInstance()
        val currentHour = viewModel.updateHour.value ?: calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = viewModel.updateMinute.value ?: calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            
            viewLifecycleOwner.lifecycleScope.launch {
                // 1. Guardar horario en el ViewModel
                viewModel.saveUpdateTime(selectedHour, selectedMinute)
                
                // 2. Activar actualización automática
                viewModel.saveAutoUpdate(true)
                
                // 3. Activar también la aplicación automática
                viewModel.saveAutoApply(true)
                
                // 4. Actualizar UI inmediatamente (solo el texto)
                binding.tvUpdateTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
                
                // 5. Programar alarma
                val success = AlarmScheduler.scheduleExactAlarm(requireContext(), selectedHour, selectedMinute)
                
                if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AlarmScheduler.requestExactAlarmPermission(requireContext())
                    Toast.makeText(requireContext(), 
                        getString(R.string.need_exact_alarm_permission), 
                        Toast.LENGTH_LONG).show()
                } else if (success) {
                    Toast.makeText(
                        requireContext(), 
                        // "Actualización automática activada a las ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        getString(R.string.auto_update_activated_at, String.format("%02d:%02d", selectedHour, selectedMinute)),  
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                checkAndRequestNotificationPermission()
            }
            
        }, currentHour, currentMinute, true).show()
    }
    
    /**
     * Comprueba y solicita el permiso de notificaciones con diálogo explicativo
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), 
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.notification_permission_title))
                .setMessage(getString(R.string.notification_permission_message))
                .setPositiveButton(getString(R.string.notification_permission_continue)) { _, _ ->
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(getString(R.string.notification_permission_later), null)
                .show()
        }
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
                
                viewModel.saveLanguage(selectedLang)
                WallpaperApp.prefs.edit().putString("language", selectedLang).apply()
                
                val localeList = LocaleListCompat.forLanguageTags(selectedLang)
                AppCompatDelegate.setApplicationLocales(localeList)
                
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