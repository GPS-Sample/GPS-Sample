/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.hotspot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.databinding.FragmentConfigurationBinding
import edu.gtri.gpssample.databinding.FragmentHotspotBinding
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.utils.NetworkConnectionStatus
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.models.NetworkHotspotModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HotspotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HotspotFragment : Fragment(), NetworkHotspotModel.NetworkHotspotDelegate
{
    private var _binding: FragmentHotspotBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedNetworkViewModel : NetworkViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            val networkVm : NetworkViewModel by activityViewModels()
            sharedNetworkViewModel = networkVm
            sharedNetworkViewModel.currentFragment = this
            sharedNetworkViewModel.networkHotspotModel.delegate = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHotspotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = Bundle()

        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedNetworkViewModel

            // Assign the fragment
            hotspotFragment = this@HotspotFragment
        }
    }

    override fun onResume() {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment =
            FragmentNumber.HotspotFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HotspotFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HotspotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun didStartImport()
    {
        activity!!.runOnUiThread {
            binding.doneButton.isEnabled = false
        }
    }
    override fun didFinishImport()
    {
        activity!!.runOnUiThread {
            binding.doneButton.isEnabled = true
            InfoDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.import_succeeded), resources.getString(R.string.ok), null, null)
        }
    }

    override fun importFailed( messageType: NetworkHotspotModel.MessageType )
    {
        activity!!.runOnUiThread {
            when (messageType)
            {
                NetworkHotspotModel.MessageType.ImportFailed ->
                    InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                NetworkHotspotModel.MessageType.ImportRequestFailed ->
                    InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_request_failed), resources.getString(R.string.ok), null, null)
                NetworkHotspotModel.MessageType.ExportRequestFailed ->
                    InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.export_request_failed), resources.getString(R.string.ok), null, null)
            }
        }
    }
}