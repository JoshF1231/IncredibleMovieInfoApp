package com.example.incrediblemovieinfoapp.ui.add_movie


import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.incrediblemovieinfoapp.data.models.Movie
import com.example.incrediblemovieinfoapp.R
import com.example.incrediblemovieinfoapp.databinding.AddItemLayoutBinding
import com.example.incrediblemovieinfoapp.ui.ActivityViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar


class AddItemFragment : Fragment() {
    private var _binding: AddItemLayoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by activityViewModels()


    private val pickImageLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.setSelectedImageURI(it.toString())
                requireActivity().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = AddItemLayoutBinding.inflate(inflater, container, false)

        viewModel.selectedYear.observe(viewLifecycleOwner) {
            binding.tvSelectedYear.text = it?.toString() ?: getString(R.string.selected_year_label)
        }

        viewModel.selectedRuntimeHours.observe(viewLifecycleOwner) {
            binding.npHoursPicker.value = it ?: 0
        }

        viewModel.selectedRuntimeMinutes.observe(viewLifecycleOwner) {
            binding.npMinutesPicker.value = it ?: 0
        }

        viewModel.selectedImageURI.observe(viewLifecycleOwner) {
            binding.ivSelectedImage.setImageURI(it?.toUri())
        }



        binding.btnAddMovie.setOnClickListener {
            if (isFormValid()) {
                val currentMovie = Movie(
                    binding.tvItemTitle.text.toString(),
                    binding.etMoviePlot.text.toString(),
                    (viewModel.selectedRuntimeHours.value
                        ?: 0) * 60 + (viewModel.selectedRuntimeMinutes.value ?: 0),
                    viewModel.selectedYear.value!!,
                    binding.rbMovieRating.rating,
                    getSelectedGenres(),
                    viewModel.selectedImageURI.value
                )
                viewModel.addMovie(currentMovie)
                Toast.makeText(requireContext(), "Movie added successfully!", Toast.LENGTH_SHORT)
                    .show()
                clearAllData()
                findNavController().navigate(R.id.action_addItemFragment_to_allItemsFragment2)
            }
        }

        setupNumberPickers()

        binding.btnSelectYear.setOnClickListener {
            showYearPickerDialog()
        }

        binding.btnAddPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.npHoursPicker.setOnValueChangedListener { _, _, value ->
            viewModel.setSelectedRuntimeHours(value)
        }
        binding.npMinutesPicker.setOnValueChangedListener { _, _, value ->
            viewModel.setSelectedRuntimeMinutes(value)
        }


        return binding.root
    }


    private fun setupNumberPickers() {
        binding.npHoursPicker.minValue = 0
        binding.npHoursPicker.maxValue = 4
        binding.npHoursPicker.wrapSelectorWheel = true

        binding.npMinutesPicker.minValue = 0
        binding.npMinutesPicker.maxValue = 59
        binding.npMinutesPicker.wrapSelectorWheel = true
    }

    private fun showYearPickerDialog() {
        val numberPicker = NumberPicker(requireContext())
        numberPicker.minValue = 1900
        numberPicker.maxValue = Calendar.getInstance().get(Calendar.YEAR)
        numberPicker.value = Calendar.getInstance().get(Calendar.YEAR)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.choose_year)
            .setView(numberPicker)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.setSelectedYear(numberPicker.value.toString().toInt())
                binding.tvSelectedYear.text = numberPicker.value.toString()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    fun getSelectedGenres(): String {
        val checkboxesToLabels = listOf(
            binding.checkboxComedy to getString(R.string.comedy_label),
            binding.checkboxDoco to getString(R.string.doco_label),
            binding.checkboxWar to getString(R.string.war_label),
            binding.checkboxDrama to getString(R.string.drama_label),
            binding.checkboxAction to getString(R.string.action_label),
            binding.checkboxFamily to getString(R.string.family_label),
            binding.checkboxRomance to getString(R.string.romance_label),
            binding.checkboxAdventure to getString(R.string.adventure_label),
            binding.checkboxAnimation to getString(R.string.animation_label),
            binding.checkboxScienceFiction to getString(R.string.science_fiction_label),
            binding.checkboxHorror to getString(R.string.horror_label),
            binding.checkboxThriller to getString(R.string.thriller_label)
        )

        return checkboxesToLabels
            .filter { it.first.isChecked }
            .joinToString(", ") { it.second }
    }


    private fun isFormValid(): Boolean {
        val genre = getSelectedGenres()
        var isValid = true
        var errorMessage = ""

        when {
            binding.tvItemTitle.text.toString().isEmpty() -> {
                isValid = false
                errorMessage = "Please enter a title."
            }

            genre.isEmpty() -> {
                isValid = false
                errorMessage = "Please select at least one genre."
            }

            binding.etMoviePlot.text.toString().isEmpty() -> {
                isValid = false
                errorMessage = "Please enter a plot."
            }

            binding.npHoursPicker.value == 0 && binding.npMinutesPicker.value == 0 -> {
                isValid = false
                errorMessage = "Please select valid movie length."
            }

            binding.tvSelectedYear.text.toString() == getString(R.string.selected_year_label) ||
                    binding.tvSelectedYear.text.toString().toInt() < 1900 -> {
                isValid = false
                errorMessage = "Please select a year."
            }

            binding.rbMovieRating.rating <= 0 -> {
                isValid = false
                errorMessage = "Please select a rating."
            }

            viewModel.selectedImageURI.value.isNullOrEmpty() -> {
                isValid = false
                errorMessage = "Please add an image."
            }
        }

        if (!isValid) {
            showSnackbar(errorMessage)
        }
        return isValid
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.btnAddMovie)
            .show()
    }


    private fun clearAllData() {
        viewModel.setSelectedImageURI(null)
        viewModel.setSelectedRuntimeHours(0)
        viewModel.setSelectedRuntimeMinutes(0)
        viewModel.setSelectedYear(0)
    }


    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//        viewModel.selectedYear.observe(requireActivity()){
//            if (_binding != null) {
//                if (binding.tvSelectedYear.text != null) {
//                    binding.tvSelectedYear.text = viewModel.selectedYear.value.toString()
//                }
//            }
//        }
//        viewModel.selectedRuntimeHours.observe(requireActivity()) {
//            if (_binding != null) {
//                if (viewModel.selectedRuntimeHours.value == null) {
//                    binding.npHoursPicker.value = 0
//                } else
//                    binding.npHoursPicker.value = viewModel.selectedRuntimeHours.value!!
//            }
//        }
//        viewModel.selectedRuntimeMinutes.observe(requireActivity()){
//            if (_binding != null) {
//                if (viewModel.selectedRuntimeMinutes.value == null) {
//                    binding.npMinutesPicker.value = 0
//                } else
//                    binding.npMinutesPicker.value = viewModel.selectedRuntimeMinutes.value!!
//            }
//        }
//
//        viewModel.selectedImageURI.observe(requireActivity()){
//            binding.ivSelectedImage.setImageURI(viewModel.selectedImageURI.value?.toUri())
//        }
