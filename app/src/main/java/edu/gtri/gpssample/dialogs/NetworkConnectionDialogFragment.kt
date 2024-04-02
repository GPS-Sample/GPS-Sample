package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import androidx.fragment.app.DialogFragment
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.DialogNetworkConnectionBinding

class NetworkConnectionDialogFragment : DialogFragment() {

    private var _binding: DialogNetworkConnectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel : NetworkViewModel

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
            networkConnectionDialogFragment = this@NetworkConnectionDialogFragment

            this.executePendingBindings()

        }
//        binding.cancelButton.setOnClickListener{
//            alertDialog.cancel()
//        }
//        binding.saveButton.setOnClickListener{
//            sharedViewModel.addFilerRule()
//            alertDialog.dismiss()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        super.onCreateDialog(savedInstanceState)
        val vm : NetworkViewModel by activityViewModels()
        sharedViewModel = vm
        val inflater = LayoutInflater.from(context)

        _binding = DialogNetworkConnectionBinding.inflate(inflater )

        val builder = AlertDialog.Builder(context)
        builder.setTitle(resources.getString( R.string.network_connection)).setView(binding.root)

        alertDialog = builder.create()
        alertDialog.setCancelable(false)

        return alertDialog
    }

    companion object {
        const val TAG = "PurchaseConfirmationDialog"
    }
}