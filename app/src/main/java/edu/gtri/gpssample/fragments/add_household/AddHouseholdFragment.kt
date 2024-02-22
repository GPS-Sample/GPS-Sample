package edu.gtri.gpssample.fragments.add_household

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.dialogs.AdditionalInfoDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.ImageDialog
import edu.gtri.gpssample.fragments.perform_collection.PerformCollectionFragment
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
    private lateinit var enumTeam: EnumerationTeam
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    private var editMode = true
    private var fragmentResultListener = ""

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

        arguments?.getBoolean( Keys.kEditMode.toString())?.let { editMode ->
            this.editMode = editMode
        }

        arguments?.getString( Keys.kFragmentResultListener.toString())?.let { fragmentResultListener ->
            this.fragmentResultListener = fragmentResultListener
        }

        arguments?.getBoolean( Keys.kCollectionMode.toString())?.let { collectionMode ->
            if (collectionMode)
            {
                binding.launchSurveyButton.visibility = View.VISIBLE
                binding.cancelButton.setText(resources.getString(R.string.mark_status))
            }
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        if (!this::config.isInitialized)
        {
            // app was closed to make room for the camera app, force re-start
            val x = 0
            val y = 1/x
//            Toast.makeText(activity!!.applicationContext, "currentConfiguration was not initialized.", Toast.LENGTH_LONG).show()
//            findNavController().navigate(R.id.action_navigate_to_MainFragment)
            return
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        sharedViewModel.teamViewModel.currentEnumerationTeam?.value?.let {
            enumTeam = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.locationViewModel.currentEnumerationItem?.value?.let {
            enumerationItem = it
        }

        if (!editMode)
        {
            binding.deleteImageView.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
            binding.subaddressEditText.inputType = InputType.TYPE_NULL

            if (location.imageData.isEmpty())
            {
                binding.addPhotoImageView.visibility = View.GONE
            }
        }

        if (editMode && enumerationItem.id != null)
        {
            binding.addMultiButton.visibility = View.VISIBLE

            binding.addMultiButton.setOnClickListener {
                if (enumerationItem.subAddress.isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, context?.getString(R.string.please_enter_a_subaddress), Toast.LENGTH_SHORT).show()
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

        if (enumerationItem.enumerationIncompleteReason.isNotEmpty() || enumerationItem.enumerationNotes.isNotEmpty())
        {
            binding.additionalInfoLayout.visibility = View.VISIBLE
            binding.incompleteCheckBox.isChecked = enumerationItem.enumerationIncompleteReason.isNotEmpty()
            binding.notesEditText.setText( enumerationItem.enumerationNotes )
            when (enumerationItem.enumerationIncompleteReason)
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

        addHouseholdAdapter = AddHouseholdAdapter( editMode, config, enumerationItem, study.fields, filteredFieldDataList )
        binding.recyclerView.adapter = addHouseholdAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(0, 0 );

        val components = enumerationItem.uuid.split("-" )

        binding.UUIDEditText.setText( components[0] )
        binding.subaddressEditText.setText( enumerationItem.subAddress )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))

        if (enumerationItem.id != null)
        {
            binding.hideAdditionalInfoImageView.visibility = View.GONE
            binding.showAdditionalInfoImageView.visibility = View.VISIBLE
            binding.defaultInfoLayout.visibility = View.GONE
            binding.additionalInfoLayout.visibility = View.GONE
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
            if (enumerationItem.enumerationIncompleteReason.isNotEmpty() || enumerationItem.enumerationNotes.isNotEmpty())
            {
                binding.additionalInfoLayout.visibility = View.VISIBLE
            }
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_household_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {
            if (location.imageData.isEmpty())
            {
                resultLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            }
            else
            {
                ImageDialog( activity!!, location.imageData, this )
            }
        }

        binding.cancelButton.setOnClickListener {
            if (fragmentResultListener.isNotEmpty())
            {
                val bundle = Bundle()
                bundle.putString( Keys.kRequest.toString(), Keys.kAdditionalInfoRequest.toString())
                setFragmentResult( fragmentResultListener, bundle )
            }

            findNavController().popBackStack()
        }

        binding.launchSurveyButton.setOnClickListener {
            if (fragmentResultListener.isNotEmpty())
            {
                val bundle = Bundle()
                bundle.putString( Keys.kRequest.toString(), Keys.kLaunchSurveyRequest.toString())
                setFragmentResult( fragmentResultListener, bundle )
            }

            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            AdditionalInfoDialog( activity, enumerationItem.enumerationIncompleteReason, enumerationItem.enumerationNotes, this)
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

        DAO.configDAO.getConfig( config.id!! )?.let {
            sharedViewModel.setCurrentConfig( it )
        }

        DAO.enumAreaDAO.getEnumArea( enumArea.id!! )?.let {
            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
        }

        DAO.studyDAO.getStudy( study.id!! )?.let {
            sharedViewModel.createStudyModel.setStudy( it )
        }

        DAO.enumerationTeamDAO.getTeam( enumTeam.id!! )?.let {
            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( it )
        }

        findNavController().popBackStack()
    }

    override fun didSelectCancelButton()
    {
    }

    override fun didSelectSaveButton( incompleteReason: String, notes: String )
    {
        // first, validate the required inputs

        if (config.subaddressIsrequired && binding.subaddressEditText.text.toString().isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Subaddress is required", Toast.LENGTH_SHORT).show()
            return
        }

        for (fieldData in enumerationItem.fieldDataList) {
            fieldData.field?.let { field ->
                if (field.type == FieldType.Dropdown)
                {
                    fieldData.dropdownIndex?.let {
                        fieldData.textValue = field.fieldOptions[it].name
                    }
                }
                if (field.required) {
                    when (field.type) {
                        FieldType.Text -> {
                            if (fieldData.textValue.isEmpty()) {
                                Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}",Toast.LENGTH_SHORT).show()
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
                            if (fieldData.fieldDataOptions.isEmpty()) {
                                Toast.makeText(activity!!.applicationContext, "${context?.getString(R.string.oops)} ${field.name} ${context?.getString(R.string.field_is_required)}", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }

        // Next, save the enumerationItem

        if (incompleteReason.isNotEmpty())
        {
            enumerationItem.enumerationIncompleteReason = incompleteReason
            enumerationItem.enumerationState = EnumerationState.Incomplete
        }
        else
        {
            enumerationItem.enumerationIncompleteReason = ""
            enumerationItem.enumerationState = EnumerationState.Enumerated
        }

        enumerationItem.enumerationNotes = notes
        enumerationItem.enumerationDate = Date().time
        enumerationItem.syncCode = enumerationItem.syncCode + 1
        enumerationItem.subAddress = binding.subaddressEditText.text.toString()

        (activity!!.application as MainApplication).user?.let { user ->
            enumerationItem.enumeratorName = user.name
        }

        if (enumerationItem.id == null)
        {
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            location.enumerationItems.add(enumerationItem)
        }
        else
        {
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
        }

        DAO.locationDAO.createOrUpdateLocation( location, enumArea )

        DAO.configDAO.getConfig( config.id!! )?.let {
            sharedViewModel.setCurrentConfig( it )
        }

        DAO.enumAreaDAO.getEnumArea( enumArea.id!! )?.let {
            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
        }

        DAO.studyDAO.getStudy( study.id!! )?.let {
            sharedViewModel.createStudyModel.setStudy( it )
        }

        DAO.enumerationTeamDAO.getTeam( enumTeam.id!! )?.let {
            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( it )
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
                    ImageDialog( activity!!, location.imageData, this )
                }
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap)
    {
        try
        {
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
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}