package com.gnzalobnites.dailywallpapers.ui.about

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.databinding.FragmentAboutBinding
import com.gnzalobnites.dailywallpapers.utils.AppUpdater
import com.gnzalobnites.dailywallpapers.utils.UpdateManager
import com.gnzalobnites.dailywallpapers.utils.UpdateInfo
import kotlinx.coroutines.launch

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AboutViewModel by viewModels()
    private var appUpdater: AppUpdater? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()
        setupToolbar()
        setupObservers()
        setupListeners()
        
        // Cargar versión
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = getString(R.string.version_unknown)
        }
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
            title = getString(R.string.title_about)
        }
    }

    private fun setupObservers() {
        viewModel.appName.observe(viewLifecycleOwner) { name ->
            binding.tvAppName.text = name
        }
    }

    private fun setupListeners() {
        // Botón de email
        binding.btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${getString(R.string.developer_email)}")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject_support))
            }
            startActivity(intent)
        }

        // Botón de GitHub
        binding.btnGithub.setOnClickListener {
            openUrl(getString(R.string.github_url))
        }

        // Botón de donaciones (Buy me a coffee)
        binding.btnCoffee.setOnClickListener {
            openUrl(getString(R.string.buy_me_coffee_url))
        }
        
        // Botón de buscar actualizaciones
        binding.btnCheckUpdates.setOnClickListener {
            checkForUpdatesManually()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // Comprobación manual de actualizaciones
    private fun checkForUpdatesManually() {
        // Deshabilitar el botón temporalmente para evitar spam de clics
        binding.btnCheckUpdates.isEnabled = false
        binding.btnCheckUpdates.text = getString(R.string.update_checking)

        lifecycleScope.launch {
            try {
                val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                val currentVersion = packageInfo.versionName

                val updateManager = UpdateManager()
                val updateInfo = updateManager.checkForUpdates(currentVersion)

                if (updateInfo != null) {
                    // Hay actualización
                    showUpdateAvailableDialog(updateInfo)
                } else {
                    // No hay actualización
                    Toast.makeText(requireContext(), R.string.update_no_update_available, Toast.LENGTH_SHORT).show()
                    // Restaurar el botón
                    binding.btnCheckUpdates.isEnabled = true
                    binding.btnCheckUpdates.text = getString(R.string.update_check_button)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.update_check_error, Toast.LENGTH_SHORT).show()
                // Restaurar el botón
                binding.btnCheckUpdates.isEnabled = true
                binding.btnCheckUpdates.text = getString(R.string.update_check_button)
            }
        }
    }

    // Diálogo para mostrar que hay actualización
    private fun showUpdateAvailableDialog(updateInfo: UpdateInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_dialog_title)
            .setMessage(getString(R.string.update_dialog_message, updateInfo.versionName))
            .setPositiveButton(R.string.update_dialog_download) { _, _ ->
                appUpdater = AppUpdater(requireContext())
                appUpdater?.downloadAndInstall(updateInfo.downloadUrl)
                binding.btnCheckUpdates.isEnabled = true
                binding.btnCheckUpdates.text = getString(R.string.update_check_button)
            }
            .setNegativeButton(R.string.update_dialog_later) { _, _ ->
                binding.btnCheckUpdates.isEnabled = true
                binding.btnCheckUpdates.text = getString(R.string.update_check_button)
            }
            .setOnCancelListener {
                binding.btnCheckUpdates.isEnabled = true
                binding.btnCheckUpdates.text = getString(R.string.update_check_button)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appUpdater?.cleanup()
        _binding = null
    }
}