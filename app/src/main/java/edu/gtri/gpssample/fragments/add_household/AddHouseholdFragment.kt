package edu.gtri.gpssample.fragments.add_household

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import edu.gtri.gpssample.dialogs.ImageDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddHouseholdFragment : Fragment(), AdditionalInfoDialog.AdditionalInfoDialogDelegate, ConfirmationDialog.ConfirmationDialogDelegate, ImageDialog.ImageDialogDelegate
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var location: Location
    private lateinit var enumArea : EnumArea
    private lateinit var locationUpdateTime: Date
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        Log.d( "xxx", "AddHouseholdFragment.onCreate" )
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
        Log.d( "xxx", "AddHouseholdFragment.onCreateView" )
        _binding = FragmentAddHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        Log.d( "xxx", "AddHouseholdFragment.onViewCreated" )
        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "currentConfiguration was not initialized.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_navigate_to_MainFragment)
            return
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.locationViewModel.currentLocationUpdateTime?.value?.let {
            locationUpdateTime = it
        }

        sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let {
            enumerationItem = it
        }

        if (enumerationItem.id != null)
        {
            binding.addMultiButton.visibility = View.VISIBLE

            binding.addMultiButton.setOnClickListener {
                if (enumerationItem.subAddress.isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, "Please set the subaddress field, then save this enumeration item.", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment)
                }
            }
        }

        // TODO: if the study fields have changed we need to fix the fieldDataList
        // if (enumerationItem.fieldDataList.size != study.fields.size)
        // {
        //     enumerationItem.fieldDataList.clear()
        // }

        // create field data for every field that is not a block field
        if (enumerationItem.fieldDataList.isEmpty())
        {
            for (field in study.fields)
            {
                if (field.fieldBlockContainer || field.fieldBlockUUID == null)
                {
                    val fieldData = FieldData(field)
                    enumerationItem.fieldDataList.add(fieldData)

                    if (field.type == FieldType.Checkbox || field.type == FieldType.Dropdown)
                    {
                        // create a fiedDataOption for each fieldOption
                        for (fieldOption in field.fieldOptions)
                        {
                            val fieldDataOption = FieldDataOption(fieldOption.name, false)
                            fieldData.fieldDataOptions.add(fieldDataOption)
                        }
                    }
                }
            }
        }

        if (enumerationItem.incompleteReason.isNotEmpty() || enumerationItem.notes.isNotEmpty())
        {
            binding.additionalInfoLayout.visibility = View.VISIBLE
            binding.incompleteCheckBox.isChecked = enumerationItem.incompleteReason.isNotEmpty()
            binding.notesEditText.setText( enumerationItem.notes )
            when (enumerationItem.incompleteReason)
            {
                "Nobody home" -> binding.nobodyHomeButton.isChecked = true
                "Home does not exist" -> binding.doesNotExistButton.isChecked = true
                "Other" -> binding.otherButton.isChecked = true
            }
        }

        // filteredFieldDataList contains only non-block fields and block field containers

        val filteredFieldDataList = ArrayList<FieldData>()

        for (fieldData in enumerationItem.fieldDataList)
        {
            fieldData.field?.let { field ->
                if (field.fieldBlockContainer || field.fieldBlockUUID == null)
                {
                    filteredFieldDataList.add( fieldData )
                }
            }
        }

        addHouseholdAdapter = AddHouseholdAdapter( config, enumerationItem, study.fields, filteredFieldDataList )
        binding.recyclerView.adapter = addHouseholdAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        val components = location.uuid.split("-" )

        binding.UUIDEditText.setText( components[0] )
        binding.subaddressEditText.setText( enumerationItem.subAddress )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))

        if (sharedViewModel.locationViewModel.isLocationUpdateTimeValid.value == true)
        {
            sharedViewModel.locationViewModel.currentLocationUpdateTime?.value?.let { date ->
                val dt = (Date().time - date.time) / 1000.0
                binding.lastUpdatedEditText.setText( "${dt} seconds ago" )
            } ?: {binding.lastUpdatedEditText.setText( "Undefined" )}
        }
        else
        {
            binding.lastUpdatedLayout.visibility = View.GONE
        }

        binding.hideAdditionalInfoImageView.setOnClickListener {
            binding.hideAdditionalInfoImageView.visibility = View.GONE
            binding.showAdditionalInfoImageView.visibility = View.VISIBLE
            binding.defaultInfoLayout.visibility = View.GONE
            binding.additionalInfoLayout.visibility = View.GONE
        }

        binding.showAdditionalInfoImageView.setOnClickListener {
            binding.showAdditionalInfoImageView.visibility = View.GONE
            binding.hideAdditionalInfoImageView.visibility = View.VISIBLE
            binding.defaultInfoLayout.visibility = View.VISIBLE
            if (enumerationItem.incompleteReason.isNotEmpty() || enumerationItem.notes.isNotEmpty())
            {
                binding.additionalInfoLayout.visibility = View.VISIBLE
            }
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_household_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {
            if (location.imageFileName.isEmpty())
            {
                resultLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            }
            else
            {
                ImageDialog( activity!!, location.imageFileName, this )
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            var isMultiFamily = false

            location.isMultiFamily?.let {
                isMultiFamily = it
            }

            if (isMultiFamily && binding.subaddressEditText.text.isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "The Subaddress must be defined for a multi family location.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                AdditionalInfoDialog( activity, enumerationItem.incompleteReason, enumerationItem.notes, this)
            }
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
        location.enumerationItems.remove(enumerationItem)
        enumArea.locations.remove(location)

        DAO.locationDAO.delete( location )
        DAO.enumerationItemDAO.delete( enumerationItem )

        findNavController().popBackStack()
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        if (enumerationItem.id == null)
        {
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            location.enumerationItems.add(enumerationItem)
        }

        if (incompleteReason.isNotEmpty())
        {
            for (fieldData in enumerationItem.fieldDataList)
            {
                DAO.fieldDataDAO.updateFieldData( fieldData )
            }

            enumerationItem.incompleteReason = incompleteReason
            enumerationItem.enumerationState = EnumerationState.Incomplete
        }
        else
        {
            for (fieldData in enumerationItem.fieldDataList)
            {
                fieldData.field?.let { field ->
                    if (field.required)
                    {
                        when (field.type)
                        {
                            FieldType.Text -> {
                                if (fieldData.textValue.isEmpty()) {
                                    Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            FieldType.Number -> {
                                if (fieldData.numberValue == null) {
                                    Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            FieldType.Date -> {
                                if (fieldData.dateValue == null) {
                                    Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            FieldType.Checkbox -> {
                                if (fieldData.fieldDataOptions.isEmpty())
                                {
                                    Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }

                DAO.fieldDataDAO.createOrUpdateFieldData( fieldData, enumerationItem )
            }

            enumerationItem.incompleteReason = ""
            enumerationItem.enumerationState = EnumerationState.Enumerated
        }

        enumerationItem.notes = notes
        enumerationItem.subAddress = binding.subaddressEditText.text.toString()

        sharedViewModel.currentConfiguration?.value?.let {config ->
            sharedViewModel.updateConfiguration()
        }

        findNavController().popBackStack()
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            if (result?.data != null)
            {
                if (this::location.isInitialized)  // activity may have been destroyed by the Camera app
                {
                    val bitmap = result.data?.extras?.get("data") as Bitmap
                    saveBitmap( bitmap )
                    ImageDialog( activity!!, location.imageFileName, this )
                }
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap)
    {
        try
        {
            val imageFileName = UUID.randomUUID().toString() + ".png"
            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
            val file = File(root, imageFileName)
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            location.imageFileName = file.absolutePath

            // base64 encode the bitmap
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            location.imageData = Base64.getEncoder().encodeToString(byteArray)
        }
        catch (e: Exception)
        {
            Log.d( "xxx", e.stackTrace.toString())
        }
    }

    override fun shouldDeleteImage()
    {
        location.imageData = ""
        location.imageFileName = ""
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}