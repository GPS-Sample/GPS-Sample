package edu.gtri.gpssample.fragments.SignUp

import android.content.SharedPreferences
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
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentSignUpBinding
import edu.gtri.gpssample.database.models.User
import java.util.*

class SignUpFragment : Fragment()
{
    private lateinit var role: String
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

        arguments?.getString(Keys.kRole.toString())?.let { role ->
            this.role = role
        }

        if (!this::role.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: role.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = role.toString() + " Sign Up"

        ArrayAdapter.createFromResource(activity!!, R.array.forgot_pin_questions, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.questionSpinner.adapter = adapter
            }

        binding.nextButton.setOnClickListener {

            val name = binding.nameEditText.text.toString()
            val pin1 = binding.pin1EditText.text.toString().toIntOrNull()
            val pin2 = binding.pin2EditText.text.toString().toIntOrNull()
            val question = binding.questionSpinner.selectedItem as String
            val answer = binding.answerEditText.text.toString()

            if (name.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin1 == null)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin2 == null)
            {
                Toast.makeText(activity!!.applicationContext, "Please re-enter the PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (answer.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter an answer.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin1 != pin2)
            {
                Toast.makeText(activity!!.applicationContext, "The PIN's do not match", Toast.LENGTH_SHORT).show()
            }
            else
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kUserName.toString(), name )
                editor.commit()

                val user = User( UUID.randomUUID().toString(), name, pin1, role, answer, question, false )
                DAO.userDAO.createUser( user )

                val bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), role )

                findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SignUpFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}