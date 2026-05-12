package com.gnzalobnites.dailywallpapers.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gnzalobnites.dailywallpapers.databinding.FragmentAboutBinding
import com.gnzalobnites.dailywallpapers.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutFragment : Fragment() {
    
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AboutViewModel by viewModels()
    
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
    }
    
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Ajustar padding del AppBarLayout para que no choque con la barra de estado
            binding.appBarLayout.updatePadding(top = insets.top)
            
            // Ajustar padding del NestedScrollView para las barras laterales e inferior
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
            title = "Acerca de"
        }
    }
    
    private fun setupObservers() {
        viewModel.appVersion.observe(viewLifecycleOwner) { version ->
            binding.tvVersion.text = version
        }
        
        viewModel.appName.observe(viewLifecycleOwner) { name ->
            binding.tvAppName.text = name
        }
    }
    
    private fun setupListeners() {
        binding.layoutDeveloper.setOnClickListener {
            showDialog("Desarrollador", "Gonzalo Benites\n@gnzalobnites")
        }
        
        binding.layoutLibraries.setOnClickListener {
            showLibrariesDialog()
        }
        
        binding.layoutPrivacy.setOnClickListener {
            showDialog("Política de privacidad", 
                "Esta app no recopila ningún dato personal. " +
                "Todas las imágenes se obtienen de la API pública de Bing.")
        }
        
        binding.layoutOpenSource.setOnClickListener {
            showDialog("Código abierto", 
                "Esta app es de código abierto.\n" +
                "Puedes encontrar el código en GitHub:\n" +
                "github.com/gnzalobnites/dailywallpapers")
        }
    }
    
    private fun showDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    private fun showLibrariesDialog() {
        val libraries = arrayOf(
            "Kotlin Coroutines",
            "AndroidX Libraries",
            "Material Design",
            "Retrofit",
            "Coil",
            "Room Database",
            "DataStore",
            "WorkManager"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Librerías utilizadas")
            .setItems(libraries) { _, _ -> }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}