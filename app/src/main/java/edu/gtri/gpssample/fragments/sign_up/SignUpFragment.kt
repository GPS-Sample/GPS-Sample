package edu.gtri.gpssample.fragments.sign_up

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentSignUpBinding
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.dialogs.InputDialog

class SignUpFragment : Fragment(), InputDialog.InputDialogDelegate
{
    private lateinit var role: String
    private lateinit var viewModel: SignUpViewModel

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

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
            Toast.makeText(activity!!.applicationContext, resources.getString(R.string.missing_parameter_rule), Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = role.toString() + " " + resources.getString(R.string.sign_up)

        ArrayAdapter.createFromResource(activity!!, R.array.forgot_pin_questions, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.questionSpinner.adapter = adapter
            }

        binding.questionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long)
            {
                if (position == 7)
                {
                    InputDialog( activity!!, false, resources.getString(R.string.enter_other_question), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this@SignUpFragment )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)
            {
            }
        }

        binding.nextButton.setOnClickListener {

            val name = binding.nameEditText.text.toString()
            val pin1 = binding.pin1EditText.text.toString().toIntOrNull()
            val pin2 = binding.pin2EditText.text.toString().toIntOrNull()
            var question = binding.questionSpinner.selectedItem as String
            val answer = binding.answerEditText.text.toString()

            if (name == "@test-admin")
            {
                Toast.makeText(activity!!.applicationContext, "Invalid user name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin1 == null)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_pin), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin2 == null)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.reenter_pin), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (question == resources.getString(R.string.other_question))
            {
                question = binding.otherQuestionTextView.text.toString()

                if (question.length == 0)
                {
                    InputDialog( activity!!, false, resources.getString(R.string.enter_other_question), "", resources.getString(R.string.cancel), resources.getString(R.string.save), null, this@SignUpFragment )
                    return@setOnClickListener
                }
            }

            if (answer.length == 0)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.enter_answer), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin1 != pin2)
            {
                Toast.makeText(activity!!.applicationContext, resources.getString(R.string.pin_not_match), Toast.LENGTH_SHORT).show()
            }
            else
            {
                val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)
                val editor = sharedPreferences.edit()
                editor.putString( Keys.kUserName.toString(), name )
                editor.commit()

                DAO.deleteAll( true )

                val user = User( name, pin1, role, question, answer, false )
                user.id = DAO.userDAO.createUser( user )

                val bundle = Bundle()
                bundle.putString( Keys.kRole.toString(), role )

                findNavController().navigate(R.id.action_navigate_to_SignInFragment, bundle)
            }
        }
    }

    override fun didCancelText( tag: Any? )
    {
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        binding.otherQuestionTextView.text = name
        binding.otherQuestionTextView.visibility = View.VISIBLE
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