package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentSignInBinding
import edu.gtri.gpssample.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment()
{
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "SignUpFragment", Toast.LENGTH_SHORT).show()
            }
        }

        val role = getArguments()?.getInt("role");

        when (role) {
            Role.Admin.value -> binding.titleTextView.text = resources.getString( R.string.admin_sign_up )
            Role.Supervisor.value -> binding.titleTextView.text = resources.getString( R.string.supervisor_sign_up )
            Role.Enumerator.value -> binding.titleTextView.text = resources.getString( R.string.data_collector_sign_up )
        }

        binding.nextButton.setOnClickListener {

            var bundle = Bundle()
            bundle.putInt( "role", role!! )
            findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}