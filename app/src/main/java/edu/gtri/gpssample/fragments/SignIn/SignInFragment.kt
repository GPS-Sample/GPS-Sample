package edu.gtri.gpssample.fragments.SignIn

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import edu.gtri.gpssample.database.models.NavPlan
import edu.gtri.gpssample.databinding.FragmentSignInBinding
import java.util.*
import kotlin.collections.ArrayList

class SignInFragment : Fragment()
{
    private lateinit var role: String
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
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        arguments?.getString(Key.kRole.toString())?.let { role ->
            this.role = role
        }

        if (!this::role.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: role.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = role.toString() + " Sign In"

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val userName = sharedPreferences.getString( Key.kUserName.toString(), null)

        userName?.let {
            binding.nameEditText.setText( userName )
        }

        binding.pinEditText.setOnEditorActionListener { v, actionId, event ->
            when(actionId){
                EditorInfo.IME_ACTION_NEXT -> {
                    handleNextButtonPress()
                    false
                }
                else -> false
            }
        }
    }

    fun handleNextButtonPress()
    {
        val pin = binding.pinEditText.text.toString()
        val userName = binding.nameEditText.text.toString()

        if (userName.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Please enter your User Name.", Toast.LENGTH_SHORT).show()
        }
        else if (pin.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Please enter your PIN.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val user = DAO.userDAO.getUser( userName, pin )

            if (user == null)
            {
                Toast.makeText(activity!!.applicationContext, "Invalid Username or PIN", Toast.LENGTH_SHORT).show()
            }
            else if (user!!.role != role)
            {
                Toast.makeText(activity!!.applicationContext, "The expected role for User " + userName + " is: " + user.role.toString() + ".  Please try again.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Key.kUserName.toString(), userName )
                editor.commit()

                (activity!!.application as? MainApplication)?.user = user

                binding.pinEditText.setText("")

                val bundle = Bundle()
                bundle.putString( Key.kRole.toString(), role.toString())

                if (role == Role.Admin.toString())
                {
                    findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                }
                else
                {
                    findNavController().navigate(R.id.action_navigate_to_SystemStatusFragment, bundle)
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