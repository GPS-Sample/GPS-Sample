/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.networkconnectiondialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import edu.gtri.gpssample.databinding.FragmentClientConnectBinding
import edu.gtri.gpssample.viewmodels.NetworkViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [ClientConnectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ClientConnectFragment : Fragment() {
    private var _binding: FragmentClientConnectBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedNetworkViewModel : NetworkViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val networkVm : NetworkViewModel by activityViewModels()
        sharedNetworkViewModel = networkVm

        sharedNetworkViewModel.currentFragment = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentClientConnectBinding.inflate(inflater, container, false)
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
            clientFragment = this@ClientConnectFragment
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ClientFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ClientConnectFragment().apply {

            }
    }
}