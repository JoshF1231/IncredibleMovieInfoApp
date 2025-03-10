package com.example.incrediblemovieinfoapp.ui.add_edit_movie

import android.app.AlertDialog
import android.content.Intent
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
import com.example.incrediblemovieinfoapp.R
import com.example.incrediblemovieinfoapp.data.models.Movie
import com.example.incrediblemovieinfoapp.databinding.AddItemLayoutBinding
import com.example.incrediblemovieinfoapp.ui.ActivityViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class AddOrEditItemFragment : Fragment() {
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
    ): View {
        _binding = AddItemLayoutBinding.inflate(inflater, container, false)

        setupObservers()
        setupNumberPickers()

        val isEditMode = viewModel.isEditMode.value ?: false
        val movie = viewModel.chosenMovie.value
        setupAddOrEditMode(isEditMode, movie)


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

    private fun setupObservers() {
        viewModel.selectedYear.observe(viewLifecycleOwner) {
            binding.tvSelectedYear.text = it?.toString() ?: getString(R.string.selected_year_label)
        }

        viewModel.selectedRuntimeHours.observe(viewLifecycleOwner) {
            binding.npHoursPicker.value = it ?: 0
        }

        viewModel.selectedRuntimeMinutes.observe(viewLifecycleOwner) {
            binding.npMinutesPicker.value = it ?: 0
        }

        viewModel.selectedImageURI.observe(viewLifecycleOwner) { uri ->
            if (uri.isNullOrEmpty()) {
                binding.ivSelectedImage.setImageResource(R.drawable.movie_picture)
            } else {
                binding.ivSelectedImage.setImageURI(uri.toUri())
            }
        }
    }

    private fun setupAddOrEditMode(isEditMode: Boolean, movie: Movie? = null) {
        if (isEditMode) {
            binding.btnAddMovie.text = getString(R.string.edit_movie_bt)
            movie?.let { setParameters(it) }
        } else {
            binding.btnAddMovie.text = getString(R.string.button_mode)
        }

        binding.btnAddMovie.setOnClickListener {
            if (isFormValid()) {
                val movieToSave = createMovieFromInput().apply {
                    if (isEditMode) {
                        id = movie?.id ?: 0
                    }
                }
                if (isEditMode) {
                    viewModel.updateMovie(movieToSave)
                    val editMessage = getString(R.string.edit_success, movieToSave.title)
                    Toast.makeText(requireContext(), editMessage, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addMovie(movieToSave)
                    val addMessage = getString(R.string.movie_added, movieToSave.title)
                    Toast.makeText(requireContext(), addMessage, Toast.LENGTH_SHORT).show()
                }

                findNavController().navigate(R.id.action_addOrEditItemFragment_to_allItemsFragment2)
            }
        }
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
                viewModel.setSelectedYear(numberPicker.value)
                binding.tvSelectedYear.text = numberPicker.value.toString()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createMovieFromInput(): Movie {
        return Movie(
            binding.tvItemTitle.text.toString(),
            binding.etMoviePlot.text.toString(),
            (viewModel.selectedRuntimeHours.value
                ?: 0) * 60 + (viewModel.selectedRuntimeMinutes.value ?: 0),
            viewModel.selectedYear.value ?: 0,
            binding.rbMovieRating.rating,
            getSelectedGenresForCurrentMovie(),
            viewModel.selectedImageURI.value
        )
    }

    private fun setParameters(movie: Movie) {
        binding.tvItemTitle.setText(movie.title)
        binding.etMoviePlot.setText(movie.plot)
        setNumberPickers(movie)
        viewModel.setSelectedYear(movie.year)
        binding.rbMovieRating.rating = movie.rate
        showGenres(movie)
        viewModel.setSelectedImageURI(movie.photo)
    }

    private fun getSelectedGenresForCurrentMovie(): List<Int> {
        val checkboxesToLabels = listOf(
            binding.checkboxComedy to R.string.comedy_label,
            binding.checkboxDoco to R.string.doco_label,
            binding.checkboxWar to R.string.war_label,
            binding.checkboxDrama to R.string.drama_label,
            binding.checkboxAction to R.string.action_label,
            binding.checkboxFamily to R.string.family_label,
            binding.checkboxRomance to R.string.romance_label,
            binding.checkboxAdventure to R.string.adventure_label,
            binding.checkboxAnimation to R.string.animation_label,
            binding.checkboxScienceFiction to R.string.science_fiction_label,
            binding.checkboxHorror to R.string.horror_label,
            binding.checkboxThriller to R.string.thriller_label
        )

        val selectedGenres = checkboxesToLabels.filter { it.first.isChecked }
            .map { it.second }

        return selectedGenres
    }

    private fun showGenres(movie: Movie) {
        val genreIds = movie.genre

        val checkboxesToIds = mapOf(
            binding.checkboxComedy to R.string.comedy_label,
            binding.checkboxDoco to R.string.doco_label,
            binding.checkboxWar to R.string.war_label,
            binding.checkboxDrama to R.string.drama_label,
            binding.checkboxAction to R.string.action_label,
            binding.checkboxFamily to R.string.family_label,
            binding.checkboxRomance to R.string.romance_label,
            binding.checkboxAdventure to R.string.adventure_label,
            binding.checkboxAnimation to R.string.animation_label,
            binding.checkboxScienceFiction to R.string.science_fiction_label,
            binding.checkboxHorror to R.string.horror_label,
            binding.checkboxThriller to R.string.thriller_label
        )

        checkboxesToIds.forEach { (checkbox, genreId) ->
            checkbox.isChecked = genreIds.contains(genreId)
        }
    }


    private fun setNumberPickers(movie: Movie) {
        viewModel.setSelectedRuntimeHours(movie.length / 60)
        viewModel.setSelectedRuntimeMinutes(movie.length % 60)
    }

    private fun isFormValid(): Boolean {
        val genre = getSelectedGenresForCurrentMovie()
        var isValid = true
        var errorMessage = ""

        when {
            binding.tvItemTitle.text.toString().isEmpty() -> {
                isValid = false
                errorMessage = getString(R.string.valid_title)
            }

            genre.isEmpty() -> {
                isValid = false
                errorMessage = getString(R.string.valid_genre)
            }

            binding.etMoviePlot.text.toString().isEmpty() -> {
                isValid = false
                errorMessage = getString(R.string.valid_plot)
            }

            binding.npHoursPicker.value == 0 && binding.npMinutesPicker.value == 0 -> {
                isValid = false
                errorMessage = getString(R.string.valid_ength)
            }

            binding.tvSelectedYear.text.toString() == getString(R.string.selected_year_label) ||
                    binding.tvSelectedYear.text.toString().toInt() < 1900 -> {
                isValid = false
                errorMessage = getString(R.string.valid_year)
            }

            binding.rbMovieRating.rating <= 0 -> {
                isValid = false
                errorMessage = getString(R.string.valid_rating)
            }
        }

        if (!isValid) {
            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
