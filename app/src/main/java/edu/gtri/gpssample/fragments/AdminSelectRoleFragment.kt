package edu.gtri.gpssample.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.FragmentAdminSelectRoleBinding

class AdminSelectRoleFragment : Fragment()
{
    private var _binding: FragmentAdminSelectRoleBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentAdminSelectRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "AdminSelectRoleFragment", Toast.LENGTH_SHORT).show()
            }
        }

        binding.adminButton.setOnClickListener {
            findNavController().navigate( R.id.action_navigate_to_ManageConfigurationsFragment )
        }

        binding.supervisorButton.setOnClickListener {
//            val intent = Intent(this, SupervisorSelectRoleActivity::class.java)
//            startActivity( intent )
//            overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
        }

        binding.signOutButton.setOnClickListener {
            findNavController().navigate( R.id.action_navigate_to_SignInSignUpFragment )
        }
    }
}