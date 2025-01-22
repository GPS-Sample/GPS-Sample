/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.privacy_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.databinding.FragmentPrivacyBinding

class PrivacyFragment : Fragment()
{
    private var _binding: FragmentPrivacyBinding? = null
    private val binding get() = _binding!!
    private var isOnBoarding = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentPrivacyBinding.inflate(inflater, container, false)
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

        binding.webView.loadUrl("file:///android_asset/privacy.html")

        binding.nextButton.setOnClickListener {
            if (isOnBoarding)
            {
                val bundle = Bundle()
                bundle.putBoolean( Keys.kIsOnBoarding.value, true )
                findNavController().navigate(R.id.action_navigate_to_EulaFragment,bundle)
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