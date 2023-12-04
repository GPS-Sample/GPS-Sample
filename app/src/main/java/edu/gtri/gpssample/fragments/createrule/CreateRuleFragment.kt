package edu.gtri.gpssample.fragments.createrule

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.databinding.FragmentCreateRuleBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class CreateRuleFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var rule: Rule? = null

    private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        sharedViewModel.createRuleModel.fragment = this
        sharedViewModel.createRuleModel.currentRule?.value?.let { rule ->
            sharedViewModel.createStudyModel.currentStudy?.value?.let { study ->
                rule.field?.let{ field->
                    for (i in 0..study.fields.size-1)
                    {
                        if (study.fields[i].uuid == field.uuid)
                        {
                            sharedViewModel.createRuleModel.ruleFieldPosition.value = i
                            break
                        }
                    }
                }
            }
        }

        sharedViewModel.createRuleModel.currentRule?.value?.operator?.let { operator ->
            sharedViewModel.createRuleModel.ruleOperationPosition?.value = OperatorConverter.toIndex( operator )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateRuleBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            createRuleFragment = this@CreateRuleFragment
            this.executePendingBindings()
        }

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.createStudyModel.currentStudy?.value?.let  { study ->
                sharedViewModel.createRuleModel.deleteSelectedRule( study )
                findNavController().popBackStack()
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            sharedViewModel.addRule()
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CreateRuleFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    fun setKeyboardInputType( field: Field)
    {
        if (field.type == FieldType.Number)
        {
            if (field.integerOnly)
            {
                binding.valueEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            else
            {
                binding.valueEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }
        else
        {
            binding.valueEditText.inputType = InputType.TYPE_CLASS_TEXT
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete ->
            {
                ConfirmationDialog( activity,  resources.getString(R.string.please_confirm), resources.getString(R.string.delete_rule_message),
                     resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
                return true
            }
        }

        return false
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        rule?.let { rule ->
            DAO.ruleDAO.deleteRule( rule )
        }

        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}