/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import edu.gtri.gpssample.databinding.DialogSamplingInfoBinding

import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel

class SamplingInfoDialogFragment : DialogFragment() {
    private var _binding: DialogSamplingInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : SamplingViewModel

    private lateinit var alertDialog : AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            samplingInfoDialogFragment = this@SamplingInfoDialogFragment

            this.executePendingBindings()

        }
//        binding.cancelButton.setOnClickListener{
//            alertDialog.cancel()
//        }
//        binding.saveButton.setOnClickListener{
//            sharedViewModel.addFilerRule()
//            alertDialog.dismiss()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        super.onCreateDialog(savedInstanceState)
        val vm : SamplingViewModel by activityViewModels()
        sharedViewModel = vm
        val inflater = LayoutInflater.from(context)

        _binding = DialogSamplingInfoBinding.inflate(inflater )

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Sampling Method Info").setView(binding.root)

        alertDialog = builder.create()
        alertDialog.setCancelable(false)

        return alertDialog
    }

    companion object {
        const val TAG = "PurchaseConfirmationDialog"
    }
}