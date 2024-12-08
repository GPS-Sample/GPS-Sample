package edu.gtri.gpssample.fragments.add_household

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.fragments.perform_collection.PerformCollectionFragment
import edu.gtri.gpssample.utils.CameraUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

class AddHouseholdFragment : Fragment(),
    ImageDialog.ImageDialogDelegate,
    InputDialog.InputDialogDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var location: Location
    private lateinit var enumArea : EnumArea
    private var inputDialog: InputDialog? = null
    private lateinit var enumTeam: EnumerationTeam
    private var propertyAdapter : PropertyAdapter? = null
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    private var editMode = true
    private var collectionMode = false
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

        arguments?.getBoolean( Keys.kEditMode.value)?.let { editMode ->
            this.editMode = editMode
        }

        arguments?.getString( Keys.kFragmentResultListener.value)?.let { fragmentResultListener ->
            this.fragmentResultListener = fragmentResultListener
        }

        arguments?.getBoolean( Keys.kCollectionMode.value)?.let { collectionMode ->
            this.collectionMode = collectionMode
            if (collectionMode)
            {
                binding.launchSurveyButton.visibility = View.VISIBLE
                binding.cancelButton.setText(resources.getString(R.string.mark_status))
            }
        }

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
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

        if (editMode && enumerationItem.uuid.isNotEmpty())
        {
            binding.addMultiButton.visibility = View.VISIBLE

            binding.addMultiButton.setOnClickListener {
                if (enumerationItem.subAddress.isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, context?.getString(R.string.please_enter_a_subaddress), Toast.LENGTH_SHORT).show()
                }
                else
                {
                    location.isMultiFamily = true
                    findNavController().popBackStack()
//                    findNavController().navigate(R.id.action_navigate_to_AddMultiHouseholdFragment)
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
            var creationDate = Date().time

            for (field in study.fields)
            {
                if (field.fieldBlockContainer || field.fieldBlockUUID == null)
                {
                    val fieldData = FieldData(creationDate++, field)
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

        if (!collectionMode)
        {
            if (enumerationItem.enumerationState == EnumerationState.Enumerated)
            {
                if (enumerationItem.enumerationNotes.isNotEmpty())
                {
                    binding.statusTextView.visibility= View.GONE
                    binding.incompleteCheckBox.visibility = View.GONE
                    binding.reasonIncompleteTextView.visibility = View.GONE
                    binding.reasonIncompleteRadioGroup.visibility = View.GONE
                    binding.notesEditText.setText( enumerationItem.enumerationNotes )
                }
            }
            else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
            {
                binding.incompleteCheckBox.isChecked = enumerationItem.enumerationIncompleteReason.isNotEmpty()
                binding.notesEditText.setText( enumerationItem.enumerationNotes )
                when (enumerationItem.enumerationIncompleteReason)
                {
                    resources.getString( R.string.nobody_home ) -> binding.nobodyHomeButton.isChecked = true
                    resources.getString( R.string.home_not_exist ) -> binding.doesNotExistButton.isChecked = true
                    resources.getString( R.string.other ) -> binding.otherButton.isChecked = true
                }
            }
        }
        else
        {
            if (enumerationItem.collectionState == CollectionState.Complete)
            {
                if (enumerationItem.collectionNotes.isNotEmpty())
                {
                    binding.statusTextView.visibility= View.GONE
                    binding.incompleteCheckBox.visibility = View.GONE
                    binding.reasonIncompleteTextView.visibility = View.GONE
                    binding.reasonIncompleteRadioGroup.visibility = View.GONE
                    binding.notesEditText.setText( enumerationItem.collectionNotes )
                }
            }
            else if (enumerationItem.collectionState == CollectionState.Incomplete)
            {
                binding.incompleteCheckBox.isChecked = enumerationItem.collectionIncompleteReason.isNotEmpty()
                binding.notesEditText.setText( enumerationItem.collectionNotes )
                when (enumerationItem.collectionIncompleteReason)
                {
                    resources.getString( R.string.nobody_home ) -> binding.nobodyHomeButton.isChecked = true
                    resources.getString( R.string.home_not_exist ) -> binding.doesNotExistButton.isChecked = true
                    resources.getString( R.string.other ) -> binding.otherButton.isChecked = true
                }
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

        if (enumerationItem.uuid.isEmpty())
        {
            binding.uuidLayout.visibility = View.GONE
        }
        else
        {
            val components = enumerationItem.uuid.split("-" )
            binding.UUIDEditText.setText( components[0] )
        }

        binding.subaddressEditText.setText( enumerationItem.subAddress )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))

        if (enumerationItem.uuid.isNotEmpty())
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

            if (enumerationItem.enumerationState == EnumerationState.Incomplete || enumerationItem.collectionState == CollectionState.Incomplete)
            {
                binding.additionalInfoLayout.visibility = View.VISIBLE
            }
            else if (!collectionMode)
            {
                if (enumerationItem.enumerationState == EnumerationState.Enumerated && enumerationItem.enumerationNotes.isNotEmpty())
                {
                    binding.additionalInfoLayout.visibility = View.VISIBLE
                }
            }
            else
            {
                if (enumerationItem.collectionState == CollectionState.Complete && enumerationItem.collectionNotes.isNotEmpty())
                {
                    binding.additionalInfoLayout.visibility = View.VISIBLE
                }
            }
        }

        if (location.properties.isNotEmpty())
        {
            val jsonObject = JSONObject( location.properties )

            val keys = ArrayList<String>()
            val values = ArrayList<String>()

            for (key in jsonObject.keys())
            {
                keys.add( key )
                values.add( jsonObject.getString(key))
            }

            propertyAdapter = PropertyAdapter( keys, values, editMode )

            binding.propertyRecyclerView.visibility = View.VISIBLE
            binding.propertyRecyclerView.adapter = propertyAdapter
            binding.propertyRecyclerView.itemAnimator = DefaultItemAnimator()
            binding.propertyRecyclerView.layoutManager = LinearLayoutManager(context)
        }

        if (location.imageData.isNotEmpty())
        {
            try
            {
                binding.imageView.setImageBitmap( CameraUtils.decodeString( location.imageData ))
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTrace.toString())
            }

            binding.imageCardView.visibility = View.VISIBLE

            binding.hideImageView.setOnClickListener {
                binding.hideImageView.visibility = View.GONE
                binding.showImageView.visibility = View.VISIBLE
                binding.imageFrameLayout.visibility = View.GONE
            }

            binding.showImageView.setOnClickListener {
                binding.hideImageView.visibility = View.VISIBLE
                binding.showImageView.visibility = View.GONE
                binding.imageFrameLayout.visibility = View.VISIBLE
            }

        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_household_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {

            // get the total size of all image data
            var size = 0

            for (location in enumArea.locations)
            {
                size += location.imageData.length
            }

            if (size > 25 * 1024 * 1024)
            {
                NotificationDialog( activity!!, resources.getString( R.string.warning), resources.getString( R.string.image_size_warning))
            }

            if (location.imageData.isEmpty())
            {
                findNavController().navigate(R.id.action_navigate_to_CameraFragment)
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
                bundle.putString( Keys.kRequest.value, Keys.kAdditionalInfoRequest.value)
                setFragmentResult( fragmentResultListener, bundle )
            }

            findNavController().popBackStack()
        }

        binding.launchSurveyButton.setOnClickListener {
            if (fragmentResultListener.isNotEmpty())
            {
                val bundle = Bundle()
                bundle.putString( Keys.kRequest.value, Keys.kLaunchSurveyRequest.value)
                setFragmentResult( fragmentResultListener, bundle )
            }

            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (enumerationItem.enumerationState == EnumerationState.Incomplete)
            {
                AdditionalInfoDialog( activity, enumerationItem.enumerationIncompleteReason, enumerationItem.enumerationNotes, this)
            }
            else
            {
                AdditionalInfoDialog( activity, "", "", this)
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddHouseholdFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didSelectFirstButton(tag: Any?)
    {
    }

    override fun didSelectSecondButton(tag: Any?)
    {
        location.enumerationItems.remove( enumerationItem )
        DAO.enumerationItemDAO.delete( enumerationItem )

        if (location.enumerationItems.size == 0)
        {
            enumArea.locations.remove( location )
            DAO.locationDAO.delete( location )
            enumTeam.locationUuids.remove( location.uuid )
        }

        DAO.configDAO.getConfig( config.uuid )?.let {
            sharedViewModel.setCurrentConfig( it )
        }

        DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
        }

        DAO.studyDAO.getStudy( study.uuid )?.let {
            sharedViewModel.createStudyModel.setStudy( it )
        }

        DAO.enumerationTeamDAO.getEnumerationTeam( enumTeam.uuid )?.let {
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
                        FieldType.Dropdown -> {
                            if (fieldData.dropdownIndex == null)
                            {
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

        if (location.properties.isNotEmpty())
        {
            propertyAdapter?.let { propertyAdapter ->
                val jsonObject = JSONObject( location.properties )

                val keys = ArrayList<String>()
                val values = ArrayList<String>()

                for (key in jsonObject.keys())
                {
                    keys.add( key )
                    values.add( jsonObject.getString(key))
                }

                for (i in 0..(values.size-1))
                {
                    if (values[i] != propertyAdapter.values[i])
                    {
                        jsonObject.put( keys[i], propertyAdapter.values[i])
                    }
                }

                location.properties = jsonObject.toString()
            }
        }

        (activity!!.application as MainApplication).user?.let { user ->
            enumerationItem.enumeratorName = user.name
        }

        if (enumerationItem.uuid.isEmpty())
        {
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            location.enumerationItems.add(enumerationItem)
        }
        else
        {
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
        }

        DAO.locationDAO.createOrUpdateLocation( location, enumArea )

        DAO.configDAO.getConfig( config.uuid )?.let {
            sharedViewModel.setCurrentConfig( it )
        }

        DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
        }

        DAO.studyDAO.getStudy( study.uuid )?.let {
            sharedViewModel.createStudyModel.setStudy( it )
        }

        DAO.enumerationTeamDAO.getEnumerationTeam( enumTeam.uuid )?.let {
            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( it )
        }

        findNavController().popBackStack()
    }

    override fun shouldDeleteImage()
    {
        location.imageData = ""
        binding.imageCardView.visibility = View.GONE
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    override fun didCancelText(tag: Any?)
    {
    }

    override fun didPressQrButton()
    {
        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
        getResult.launch(intent)
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.value)
                inputDialog?.editText?.let { editText ->
                    editText.setText( payload.toString())
                }
            }
        }

    override fun didEnterText(text: String, tag: Any?)
    {
        binding.subaddressEditText.setText( text )
    }
}