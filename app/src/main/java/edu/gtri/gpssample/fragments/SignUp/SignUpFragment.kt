package edu.gtri.gpssample.fragments.SignUp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentSignUpBinding
import edu.gtri.gpssample.database.models.User
import java.util.*

class SignUpFragment : Fragment()
{
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

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
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        val role_arg = getArguments()?.getString(Key.kRole.toString());

        val role = Role.valueOf(role_arg!!)

        binding.titleTextView.text = role.toString() + " Sign Up"

        ArrayAdapter.createFromResource(activity!!, R.array.forgot_pin_questions, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.questionSpinner.adapter = adapter
            }

        binding.nextButton.setOnClickListener {

            val name = binding.nameEditText.text.toString()
            val pin1 = binding.pin1EditText.text.toString().toInt()
            val pin2 = binding.pin2EditText.text.toString().toInt()
            val question = binding.questionSpinner.selectedItem as String
            val answer = binding.answerEditText.text.toString()

            if (pin1 != pin2)
            {
                Toast.makeText(activity!!.applicationContext, "The PIN's do not match", Toast.LENGTH_SHORT).show()
            }
            else
            {
                val user = User()

                user.pin = pin1
                user.role = role
                user.name = name
                user.recoveryAnswer = answer
                user.recoveryQuestion = question
                user.uuid = UUID.randomUUID().toString()

                user.id = DAO.userDAO.createUser( user )

                val sharedPreferences = activity!!.application.getSharedPreferences( "default", 0 )
                val editor = sharedPreferences.edit()

                editor.putInt( Key.kPin.toString(), user.pin )
                editor.putInt( Key.kUserId.toString(), user.id )
                editor.putString( Key.kUserName.toString(), user.name )
                editor.commit()

                val bundle = Bundle()
                bundle.putString( Key.kRole.toString(), role.toString() )

                findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}