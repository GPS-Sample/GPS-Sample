package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.databinding.DialogSelectRuleBinding

class SelectRuleDialogFragment: DialogFragment() {

    private var _binding: DialogSelectRuleBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : ConfigurationViewModel

    private lateinit var alertDialog : AlertDialog
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding?.apply {
            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            // Assign the view model to a property in the binding class
            viewModel = sharedViewModel

            // Assign the fragment
            selectRuleDialogFragment = this@SelectRuleDialogFragment

            this.executePendingBindings()

        }

        binding.cancelButton.setOnClickListener{
            alertDialog.cancel()
        }
        binding.saveButton.setOnClickListener{
            sharedViewModel.addFilerRule()
            alertDialog.dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        super.onCreateDialog(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
        val inflater = LayoutInflater.from(context)

        _binding = DialogSelectRuleBinding.inflate(inflater )

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select Rule").setView(binding.root)

        alertDialog = builder.create()
        alertDialog.setCancelable(false)

        return alertDialog
    }

    companion object {
        const val TAG = "PurchaseConfirmationDialog"
    }
}