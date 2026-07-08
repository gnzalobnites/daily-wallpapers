package com.gnzalobnites.dailywallpapers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gnzalobnites.dailywallpapers.databinding.FragmentOnboardingBinding
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionAndNavigate()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar si ya pasó el onboarding
        val prefs = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        // Si ya tiene el permiso o es versión vieja, saltar directamente
        if (hasExactAlarmPermission() && onboardingCompleted) {
            navigateToMain()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnGrantPermission.setOnClickListener {
            requestExactAlarmPermission()
        }

        binding.tvSkip.setOnClickListener {
            // Saltar onboarding sin conceder permiso
            saveOnboardingCompleted()
            navigateToMain()
            Toast.makeText(requireContext(), 
                getString(com.gnzalobnites.dailywallpapers.R.string.onboarding_toast_skip), 
                Toast.LENGTH_LONG).show()
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${requireContext().packageName}")
            }
            permissionLauncher.launch(intent)
        } else {
            // En versiones antiguas, no se requiere permiso
            saveOnboardingCompleted()
            navigateToMain()
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    private fun checkPermissionAndNavigate() {
        if (hasExactAlarmPermission()) {
            // Programar alarmas con las preferencias actuales
            lifecycleScope.launch {
                val prefsManager = com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager(requireContext())
                val autoUpdate = prefsManager.autoUpdate.first()

                if (autoUpdate) {
                    val hour = prefsManager.updateHour.first()
                    val minute = prefsManager.updateMinute.first()
                    AlarmScheduler.scheduleExactAlarm(requireContext(), hour, minute)
                }

                // También programar WorkManager como respaldo
                WorkerScheduler.scheduleFromPreferences(requireContext())
            }

            Toast.makeText(requireContext(),
                getString(com.gnzalobnites.dailywallpapers.R.string.onboarding_toast_granted),
                Toast.LENGTH_LONG).show()

            showBatteryStepIfNeeded()
        } else {
            Toast.makeText(requireContext(),
                getString(com.gnzalobnites.dailywallpapers.R.string.onboarding_toast_denied),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun showBatteryStepIfNeeded() {
        if (!ManufacturerBatteryHelper.isKnownRestrictiveManufacturer()) {
            // Fabricante cercano a AOSP (Samsung, Pixel, etc.): no hace falta este paso
            saveOnboardingCompleted()
            navigateToMain()
            return
        }

        binding.stepPermission.visibility = View.GONE
        binding.stepBattery.visibility = View.VISIBLE

        binding.btnOpenBatterySettings.setOnClickListener {
            ManufacturerBatteryHelper.openManufacturerBatterySettings(requireContext())
        }

        binding.tvContinueBattery.setOnClickListener {
            saveOnboardingCompleted()
            navigateToMain()
        }
    }

    private fun saveOnboardingCompleted() {
        val prefs = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    private fun navigateToMain() {
        findNavController().navigate(com.gnzalobnites.dailywallpapers.R.id.action_onboarding_to_main)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}