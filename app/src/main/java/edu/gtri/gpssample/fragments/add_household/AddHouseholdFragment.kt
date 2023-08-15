package edu.gtri.gpssample.fragments.add_household

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.managers.UriManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddHouseholdFragment : Fragment(), AdditionalInfoDialog.AdditionalInfoDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!
    private val OPEN_DOCUMENT_CODE = 2

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea : EnumArea
    private lateinit var location: Location
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentAddHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        // create an enumeration item
        if(location.items.isEmpty())
        {
            enumerationItem = EnumerationItem()
            location.items.add(enumerationItem)
        }else
        {
            // TODO: COME UP WITH INTERFACE OR SOMETHING TO DEAL WITH MULTIPLE ENUMITEMS
            val enum = location.items[0] as? EnumerationItem
            enum?.let{enum->
                enumerationItem = enum
            }
        }

        // create enum data for every field
        if (enumerationItem.fieldDataList.isEmpty())
        {
            for (field in study.fields)
            {
                val fieldData = FieldData(field) //DAO.fieldDataDAO.getOrCreateFieldData(field.id!!, enumerationItem.id!!)
                enumerationItem.fieldDataList.add(fieldData)

            }
        }

        if (enumerationItem.incompleteReason.isNotEmpty() || enumerationItem.notes.isNotEmpty())
        {
            binding.cardView.visibility = View.VISIBLE
            binding.incompleteCheckBox.isChecked = enumerationItem.incompleteReason.isNotEmpty()
            binding.notesEditText.setText( enumerationItem.notes )
            when (enumerationItem.incompleteReason)
            {
                "Nobody home" -> binding.nobodyHomeButton.isChecked = true
                "Home does not exist" -> binding.doesNotExistButton.isChecked = true
                "Other" -> binding.otherButton.isChecked = true
            }
        }
        else
        {
            binding.cardView.visibility = View.GONE
        }

        addHouseholdAdapter = AddHouseholdAdapter( config, enumerationItem.fieldDataList )
        binding.recyclerView.adapter = addHouseholdAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

//        if (enumData.imageFileName.isNotEmpty())
//        {
//            val uri = Uri.parse(enumData.imageFileName )
//            activity!!.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(activity!!.getContentResolver(), uri)
//            (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "${bitmap.width}:${bitmap.height}"
//            binding.imageView.setImageBitmap(bitmap)
//        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_household_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, OPEN_DOCUMENT_CODE)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            AdditionalInfoDialog( activity, enumerationItem.incompleteReason, enumerationItem.notes, this)
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddHouseholdFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        findNavController().popBackStack()
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        if (incompleteReason.isNotEmpty())
        {
            for (fieldData in enumerationItem.fieldDataList)
            {
                DAO.fieldDataDAO.updateFieldData( fieldData )
            }

            enumerationItem.enumerationState = EnumerationState.Incomplete
            enumerationItem.notes = notes
            enumerationItem.incompleteReason = incompleteReason
        }
        else
        {
            for (fieldData in enumerationItem.fieldDataList)
            {
                if (fieldData.field.required)
                {
                    when (fieldData.field.type)
                    {
                        FieldType.Text -> {
                            if (fieldData.textValue.isEmpty()) {
                                Toast.makeText(activity!!.applicationContext, "Oops! ${fieldData.field.name} field is required", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        FieldType.Number -> {
                            if (fieldData.numberValue == null) {
                                Toast.makeText(activity!!.applicationContext, "Oops! ${fieldData.field.name} field is required", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        FieldType.Date -> {
                            if (fieldData.dateValue == null) {
                                Toast.makeText(activity!!.applicationContext, "Oops! ${fieldData.field.name} field is required", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        FieldType.Checkbox -> {
                            val selection = fieldData.checkbox1 or fieldData.checkbox2 or fieldData.checkbox3 or fieldData.checkbox4
                            if (!selection) {
                                Toast.makeText(activity!!.applicationContext, "Oops! ${fieldData.field.name} field is required", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        else -> {
                        }
                    }
                }

                DAO.fieldDataDAO.createOrUpdateFieldData( fieldData, enumerationItem )
            }

            enumerationItem.notes = notes
            enumerationItem.incompleteReason = ""
            enumerationItem.enumerationState = EnumerationState.Enumerated
        }

        sharedViewModel.currentConfiguration?.value?.let {config ->
            sharedViewModel.updateConfiguration()
        }

        findNavController().popBackStack()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_DOCUMENT_CODE) {
                data?.data?.let {uri ->
                    activity!!.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(activity!!.getContentResolver(), uri)
                    (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "${bitmap.width}:${bitmap.height}"
                    binding.imageView.setImageBitmap(bitmap)
//                    enumData.imageFileName = uri.toString()
//                    DAO.enumDataDAO.updateEnumData( enumData )
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