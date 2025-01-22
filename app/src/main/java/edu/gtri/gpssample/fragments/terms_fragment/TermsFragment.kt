/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.terms_fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.databinding.FragmentTermsBinding

class TermsFragment : Fragment()
{
    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!
    private var isOnBoarding = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getBoolean(Keys.kIsOnBoarding.value)?.let { isOnBoarding ->
            this.isOnBoarding = isOnBoarding
        }

        if (!isOnBoarding)
        {
            binding.nextButton.setText(resources.getString(R.string.back))
        }

        binding.webView.loadUrl("file:///android_asset/terms.html")

        binding.nextButton.setOnClickListener {
            if (isOnBoarding)
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putBoolean(Keys.kTermsAccepted.value, true)
                editor.commit()

                val bundle = Bundle()
                bundle.putBoolean( Keys.kIsOnBoarding.value, true )
                findNavController().navigate(R.id.action_navigate_to_PrivacyFragment,bundle)
            }
            else
            {
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}