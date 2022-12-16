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

class SignInFragment : Fragment()
{
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "SignInFragment", Toast.LENGTH_SHORT).show()
            }
        }

        val role = getArguments()?.getInt("role");

        binding.pinEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        when (role) {
            Role.Admin.value -> binding.titleTextView.text = resources.getString( R.string.admin_sign_in )
            Role.Supervisor.value -> binding.titleTextView.text = resources.getString( R.string.supervisor_sign_in )
            Role.Enumerator.value -> binding.titleTextView.text = resources.getString( R.string.data_collector_sign_in )
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.pinEditText.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->

            if (binding.pinEditText.getText().toString().length >= 4)
            {
                (activity!!.application as MainApplication).fields.clear()
                (activity!!.application as MainApplication).studies.clear()
                (activity!!.application as MainApplication).configurations.clear()

                var bundle = Bundle()
                bundle.putInt( "role", role!! )

                if (role == Role.Admin.value)
                {
                    findNavController().navigate(R.id.action_navigate_to_AdminSelectRoleFragment, bundle)
                }
                else if (role == Role.Supervisor.value)
                {
                    findNavController().navigate(R.id.action_navigate_to_BarcodeScanFragment, bundle)
                }
            }

            false
        })
    }
}