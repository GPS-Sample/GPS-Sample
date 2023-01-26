package edu.gtri.gpssample.fragments.SignIn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentSignInBinding

class SignInFragment : Fragment()
{
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignInViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SignInViewModel::class.java)
    }

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

        val role_arg = getArguments()?.getString(Key.kRole.toString());

        val role = Role.valueOf(role_arg!!)

        binding.titleTextView.text = role.toString() + " Sign In"

        binding.nextButton.setOnClickListener {

            val sharedPreferences = activity!!.application.getSharedPreferences( "default", 0 )
            val expectedPin = sharedPreferences.getInt( Key.kPin.toString(), 0 )
            val userId = sharedPreferences.getInt( Key.kUserId.toString(), 0 )
            val expectedUserName = sharedPreferences.getString( Key.kUserName.toString(), null )
            val pin = binding.pinEditText.text.toString()
            val userName = binding.nameEditText.text.toString()

            if (userName.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter your User Name", Toast.LENGTH_SHORT).show()
            }
            else if (userName != expectedUserName)
            {
                Toast.makeText(activity!!.applicationContext, "The User Name is incorrect", Toast.LENGTH_SHORT).show()
            }
            else if (pin.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter your PIN", Toast.LENGTH_SHORT).show()
            }
            else if (pin.toInt() != expectedPin)
            {
                Toast.makeText(activity!!.applicationContext, "The PIN is incorrect", Toast.LENGTH_SHORT).show()
            }
            else
            {
                val user = DAO.userDAO.getUser( userId )

                if (user == null)
                {
                    Toast.makeText(activity!!.applicationContext, "Missing User Data", Toast.LENGTH_SHORT).show()
                }
                else if (user!!.role != role)
                {
                    Toast.makeText(activity!!.applicationContext, "The expected role for User " + userName + " is: " + user.role.toString() + ".  Please try again.", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    binding.pinEditText.setText("")

                    (activity!!.application as MainApplication).fields.clear()
                    (activity!!.application as MainApplication).studies.clear()

                    val bundle = Bundle()
                    bundle.putString( Key.kRole.toString(), role.toString())

                    if (role == Role.Admin)
                    {
                        findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                    }
                    else
                    {
                        findNavController().navigate(R.id.action_navigate_to_BarcodeScanFragment, bundle)
                    }
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}