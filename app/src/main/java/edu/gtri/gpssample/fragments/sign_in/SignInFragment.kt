package edu.gtri.gpssample.fragments.sign_in

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentSignInBinding
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.dialogs.ResetPinDialog

class SignInFragment : Fragment(), InputDialog.InputDialogDelegate, ResetPinDialog.ResetPinDialogDelegate
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

        arguments?.getString(Keys.kRole.toString())?.let { role ->
            this.role = role
        }

        if (!this::role.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: role.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = role.toString() + " Sign In"

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val userName = sharedPreferences.getString( Keys.kUserName.toString(), null)

        userName?.let {
            binding.nameEditText.setText( userName )
        }

        binding.forgotPinTextView.setOnClickListener {

            val user_name = binding.nameEditText.text.toString()

            if (user_name.length > 0)
            {
                val user = DAO.userDAO.getUser(user_name)

                user?.let {
                    InputDialog( activity!!, it.recoveryQuestion, "", this@SignInFragment )
                }
            }
        }

        binding.resetPinTextView.setOnClickListener {
            val userName = binding.nameEditText.text.toString()
            val user = DAO.userDAO.getUser(userName)

            user?.let {
                ResetPinDialog( activity!!, it.pin.toString(), this@SignInFragment )
            }
        }

        binding.pinEditText.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            val pin = binding.pinEditText.text.toString()
            val userName = binding.nameEditText.text.toString()

            if (userName.isNotEmpty() && pin.isNotEmpty()) {
                val user = DAO.userDAO.getUser(userName, pin)

                user?.let {
                    user
                    if (user.role != role)
                    {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "The expected role for User " + userName + " is: " + user.role.toString() + ".  Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else
                    {
                        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                        val editor = sharedPreferences.edit()
                        editor.putString(Keys.kUserName.toString(), userName)
                        editor.commit()

                        (activity!!.application as? MainApplication)?.user = user

                        binding.pinEditText.setText("")

                        val bundle = Bundle()
                        bundle.putString(Keys.kRole.toString(), role.toString())

                        if (role == Role.Admin.toString()) {
                            findNavController().navigate(
                                R.id.action_navigate_to_ManageConfigurationsFragment,
                                bundle
                            )
                        }
                        else if (role == Role.Supervisor.toString()) {
                            findNavController().navigate(
                                R.id.action_navigate_to_ManageConfigurationsFragment,
                                bundle
                            )
                        }
                        else {
                            findNavController().navigate(
                                R.id.action_navigate_to_EnumeratorFragment,
                                bundle
                            )
                        }
                    }
                }
            }

            false
        })

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

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SignInFragment.value.toString() + ": " + this.javaClass.simpleName
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
                editor.putString( Keys.kUserName.toString(), userName )
                editor.commit()

                (activity!!.application as? MainApplication)?.user = user

                binding.pinEditText.setText("")

                val bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), role.toString())

                if (role == Role.Admin.toString())
                {
                    findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                }
                else
                {
                    findNavController().navigate(R.id.action_navigate_to_EnumeratorFragment, bundle)
                }
            }
        }
    }

    override fun didEnterText( text: String )
    {
        val userName = binding.nameEditText.text.toString()
        val user = DAO.userDAO.getUser(userName)

        user?.let {
            if (text == it.recoveryAnswer)
            {
                NotificationDialog( activity!!, "Your PIN is:", it.pin.toString())
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, "Oops! Incorrect answer.  Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun didUpdatePin( pin: String )
    {
        val userName = binding.nameEditText.text.toString()
        val user = DAO.userDAO.getUser(userName)

        user?.let {
            it.pin = pin.toInt()
            DAO.userDAO.updateUser( it )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}