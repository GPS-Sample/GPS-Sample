package edu.gtri.gpssample.fragments.sign_in

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
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
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.dialogs.ResetPinDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel


class SignInFragment : Fragment(), InputDialog.InputDialogDelegate, ResetPinDialog.ResetPinDialogDelegate
{
    private lateinit var expectedRole: String
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var samplingViewModel: SamplingViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        samplingViewModel = samplingVm
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

            val user_name = binding.nameEditText.text.toString()

            if (user_name.length > 0)
            {
                val user = DAO.userDAO.getUser(user_name)

                user?.let {
                    InputDialog( activity!!, it.recoveryQuestion, "", null, this@SignInFragment )
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

                    activity!!.setTitle( "GPSSample - ${user.role}" )

                    val bundle = Bundle()
                    bundle.putString(Keys.kRole.toString(), expectedRole.toString())

                    when(user.role)
                    {
                        Role.Enumerator.toString() ->
                        {
                            if(sharedViewModel.configurations.size > 0)
                            {
                                navigateToEnumeration()
                            }
                            else
                            {
                                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                            }
                        }

                        Role.DataCollector.toString() ->
                        {
                            if(sharedViewModel.configurations.size > 0)
                            {
                                navigateToCollection()
                            }
                            else
                            {
                                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                            }
                        }

                        Role.Admin.toString(), Role.Supervisor.toString() ->
                        {
                            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
                        }
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SignInFragment.value.toString() + ": " + this.javaClass.simpleName
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

    fun navigateToEnumeration()
    {
        if (sharedViewModel.configurations.size > 0)
        {
            val config = sharedViewModel.configurations[0]
            sharedViewModel.setCurrentConfig( config )

            // find the selected Enum Area
            val enumAreas = config.enumAreas.filter {
                it.id?.let { id ->
                    id == config.selectedEnumAreaId
                } ?: false
            }

            // find the selected study
            val studies = config.studies.filter {
                it.id?.let { id ->
                    id == config.selectedStudyId
                } ?: false
            }

            if (enumAreas.isNotEmpty() && studies.isNotEmpty())
            {
                val enumArea = enumAreas[0]
                val study = studies[0]

                // find the selected enumeration Team
                val enumTeams = enumArea.enumerationTeams.filter {
                    it.id?.let { id ->
                        id == enumArea.selectedTeamId
                    } ?: false
                }

                if (enumTeams.isNotEmpty())
                {
                    val enumTeam = enumTeams[0]
                    sharedViewModel.createStudyModel.setStudy( study )
                    sharedViewModel.teamViewModel.setCurrentTeam( enumTeam )
                    sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                    findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
                }
            }
        }
    }

    fun navigateToCollection()
    {
        if(sharedViewModel.configurations.size > 0)
        {
            val config = sharedViewModel.configurations[0]
            sharedViewModel.setCurrentConfig( config )

            // find the selected Enum Area
            val enumAreas = config.enumAreas.filter {
                it.id?.let { id ->
                    id == config.selectedEnumAreaId
                } ?: false
            }

            // find the selected study
            val studies = config.studies.filter {
                it.id?.let { id ->
                    id == config.selectedStudyId
                } ?: false
            }

            if (enumAreas.isNotEmpty() && studies.isNotEmpty())
            {
                val study = studies[0]
                val enumArea = enumAreas[0]
                val sampleArea = study.sampleArea

                sampleArea?.let { sampleArea ->

                    // find the selected collection Team
                    val collectionTeams = sampleArea.collectionTeams.filter {
                        it.id?.let { id ->
                            id == sampleArea.selectedTeamId
                        } ?: false
                    }

                    if (collectionTeams.isNotEmpty())
                    {
                        val collectionTeam = collectionTeams[0]
                        sharedViewModel.createStudyModel.setStudy( study )
                        samplingViewModel.setCurrentSampleArea( sampleArea )
                        sharedViewModel.teamViewModel.setCurrentTeam( collectionTeam )
                        sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                        samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
                        findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
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