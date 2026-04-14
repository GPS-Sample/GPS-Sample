/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.createstudy

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

enum class DeleteMode(val value : Int)
{
    deleteStudyTag (1),
}

class CreateStudyFragment : Fragment()
{
    private lateinit var study: Study
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel

    private var expandableListState: SparseArray<Parcelable>? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createStudyModel.fragment = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createStudyFragment = this@CreateStudyFragment
        }

        ArrayAdapter.createFromResource(
            activity!!,
            R.array.samling_methods,
            android.R.layout.simple_spinner_item
        )
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.samplingMethodSpinner.adapter = adapter
            }

        sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
            this.study = study
        } ?: run {
            binding.deleteImageView.visibility = View.GONE
        }

        sharedViewModel.createStudyModel.samplingMethod.observe( this, androidx.lifecycle.Observer { samplingMethod ->
            when(study.samplingMethod)
            {
                SamplingMethod.SimpleRandom -> {
                    binding.sampleSizeTextView.text = resources.getString(R.string.simple_random_sampling_label)
                }
                SamplingMethod.Cluster -> {
                    binding.sampleSizeTextView.text = resources.getString(R.string.cluster_sampling_label)
                }
                else -> {
                }
            }
        })

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog(activity, resources.getString(R.string.please_confirm), resources.getString(R.string.delete_study_message), resources.getString(R.string.no), resources.getString(R.string.yes), DeleteMode.deleteStudyTag.value, false) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {
                    }
                    ConfirmationDialog.ButtonPress.Right -> {
                        sharedViewModel.deleteCurrentStudy()
                        findNavController().popBackStack()
                    }
                    ConfirmationDialog.ButtonPress.None -> {
                    }
                }
            }
        }

        binding.primarySampleButton.setOnClickListener {
            if (study.samplingMethod == SamplingMethod.Strata)
            {
                findNavController().navigate( R.id.action_navigate_to_StrataSampleFragment )
            }
            else
            {
                findNavController().navigate( R.id.action_navigate_to_PrimarySampleFragment )
            }
        }

        binding.subsetSampleButton.setOnClickListener {
            findNavController().navigate( R.id.action_navigate_to_SubsetSampleFragment )
        }

        binding.saveButton.setOnClickListener {
            updateStudy()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateStudyFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun updateStudy()
    {
        if (study.name.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (study.sampleSize == 0)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.sample_size_error), Toast.LENGTH_SHORT).show()
            return
        }

        sharedViewModel.addStudy()

        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        expandableListState = SparseArray()
//        binding.expandableListView.saveHierarchyState(expandableListState)

        _binding = null
    }
}