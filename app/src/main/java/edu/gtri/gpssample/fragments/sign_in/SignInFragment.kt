package edu.gtri.gpssample.fragments.sign_in

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentSignInBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.dialogs.ResetPinDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel


class SignInFragment : Fragment(), InputDialog.InputDialogDelegate, ResetPinDialog.ResetPinDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var expectedRole: String
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
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
            this.expectedRole = role
        }

        if (!this::expectedRole.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.missing_parameter_rule), Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = expectedRole.toString() + " " + resources.getString(R.string.sign_in)

        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
        val userName = sharedPreferences.getString( Keys.kUserName.toString(), null)

        userName?.let {
            binding.nameEditText.setText( userName )
        }

        binding.forgotPinTextView.setOnClickListener {
            val userName = binding.nameEditText.text.toString()
            if (userName.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.please_enter_your_user_name), Toast.LENGTH_SHORT).show()
            }
            else if (userName == "@test-admin")
            {
                Toast.makeText(activity!!.applicationContext, "The PIN cannot be recovered for this account", Toast.LENGTH_SHORT).show()
            }
            else
            {
                DAO.userDAO.getUser(userName)?.let {
                    InputDialog( activity!!, false, it.recoveryQuestion, "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this@SignInFragment )
                } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.user_name_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.resetPinTextView.setOnClickListener {
            val userName = binding.nameEditText.text.toString()
            if (userName.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.please_enter_your_user_name), Toast.LENGTH_SHORT).show()
            }
            else if (userName == "@test-admin")
            {
                Toast.makeText(activity!!.applicationContext, "The PIN cannot be reset for this account", Toast.LENGTH_SHORT).show()
            }
            else
            {
                DAO.userDAO.getUser(binding.nameEditText.text.toString())?.let {
                    ResetPinDialog( activity!!, it.pin.toString(), this@SignInFragment )
                } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.user_name_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.pinEditText.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            checkPassword()
            false
        })

        binding.pinEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                checkPassword()
            }
            false
        })
    }

    fun checkPassword()
    {
        val pin = binding.pinEditText.text.toString()
        val userName = binding.nameEditText.text.toString()

        if (userName.isNotEmpty() && pin.isNotEmpty())
        {
            val user = DAO.userDAO.getUser(userName, pin)

            user?.let { user ->

                if (user.name == "@test-admin" && DAO.configDAO.getConfigs().isNotEmpty())
                {
                    ConfirmationDialog( activity, resources.getString(R.string.oops),
                        "The test-admin is not allowed to view/edit a previously saved configuration.  Would you like to delete the saved configuration(s)?", resources.getString(R.string.no), resources.getString(R.string.yes), null, this@SignInFragment)
                    return
                }

                if (user.role != expectedRole)
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

                    setTitle( user )

                    val bundle = Bundle()
                    bundle.putString(Keys.kRole.toString(), user.role.toString())
                    findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SignInFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun setTitle( user: User )
    {
        when (user.role)
        {
            Role.Admin.toString() -> activity!!.setTitle( "GPSSample - ${resources.getString(R.string.admin)}" )
            Role.Supervisor.toString() -> activity!!.setTitle( "GPSSample - ${resources.getString(R.string.supervisor)}" )
            Role.Enumerator.toString() -> activity!!.setTitle( "GPSSample - ${resources.getString(R.string.enumerator)}" )
            Role.DataCollector.toString() -> activity!!.setTitle( "GPSSample - ${resources.getString(R.string.data_collector)}" )
        }
    }

    override fun didCancelText( tag: Any? )
    {
    }

    override fun didEnterText( text: String, tag: Any? )
    {
        val userName = binding.nameEditText.text.toString()
        val user = DAO.userDAO.getUser(userName)

        user?.let {
            if (text == it.recoveryAnswer)
            {
                NotificationDialog( activity!!, resources.getString(R.string.your_pin_is), it.pin.toString())
            }
            else
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.incorrect_answer_message), Toast.LENGTH_SHORT).show()
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectRightButton(tag: Any?)
    {
        DAO.deleteAll( false )

        val pin = binding.pinEditText.text.toString()
        val userName = binding.nameEditText.text.toString()

        DAO.userDAO.getUser(userName, pin)?.let { user ->
            setTitle( user )

            val bundle = Bundle()
            bundle.putString(Keys.kRole.toString(), user.role.toString())
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}