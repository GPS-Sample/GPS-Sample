/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.add_household

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddHouseholdBinding
import edu.gtri.gpssample.dialogs.*
import edu.gtri.gpssample.utils.CameraUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import org.json.JSONObject
import java.util.*

class AddHouseholdFragment : Fragment(),
    ImageDialog.ImageDialogDelegate,
    AdditionalInfoDialog.AdditionalInfoDialogDelegate
{
    private var _binding: FragmentAddHouseholdBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var location: Location
    private lateinit var enumArea : EnumArea
    private lateinit var enumerationTeam: EnumerationTeam
    private var propertyAdapter : PropertyAdapter? = null
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel
    private lateinit var addHouseholdAdapter: AddHouseholdAdapter

    private var editMode = true
    private var collectionMode = false
    private var isMultiHousehold = false
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

        arguments?.getBoolean( Keys.kIsMultiHousehold.value)?.let { isMultiHousehold ->
            this.isMultiHousehold = isMultiHousehold
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

        enumArea.enumerationTeams.find { it.uuid == sharedViewModel.currentEnumerationTeamUuid }?.let { enumerationTeam ->
            this.enumerationTeam = enumerationTeam
        }

        enumArea.locations.find { it.uuid == sharedViewModel.currentLocationUuid }?.let { location: Location ->
            this.location = location
            this.location.enumerationItems.find { it.uuid == sharedViewModel.currentEnumerationItemUuid }?.let { enumerationItem ->
                this.enumerationItem = enumerationItem
            }
        }

        if (!editMode)
        {
            binding.deleteImageView.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
            binding.subaddressEditText.inputType = InputType.TYPE_NULL

            if (location.imageUuid.isEmpty())
            {
                binding.addPhotoImageView.visibility = View.GONE
            }
        }

        if (editMode && enumerationItem.uuid.isNotEmpty() && !isMultiHousehold)
        {
            binding.addMultiButton.visibility = View.VISIBLE

            binding.addMultiButton.setOnClickListener {
                if (enumerationItem.subAddress.isEmpty())
                {
                    Toast.makeText(activity!!.applicationContext, context?.getString(R.string.please_enter_a_subaddress), Toast.LENGTH_SHORT).show()
                }
                else
                {
                    findNavController().popBackStack()
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
            var creationDate = Date().time

            for (field in study.fields)
            {
                if (field.parentUUID == null)
                {
                    val fieldData = FieldData(creationDate++, field.uuid)
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

        if (enumerationItem.uuid.isEmpty())
        {
            binding.uuidLayout.visibility = View.GONE
            binding.additionalInfoLayout.visibility = View.GONE
        }
        else
        {
            val components = enumerationItem.uuid.split("-" )
            binding.UUIDEditText.setText( components[0] )
        }

        if (enumerationItem.enumerationState == EnumerationState.Enumerated)
        {
            binding.enumerationIncompleteCheckBox.isChecked = true
            binding.enumerationIncompleteCheckBox.text = resources.getString( R.string.complete )
            binding.enumerationReasonIncompleteLayout.visibility = View.GONE
        }
        else if (enumerationItem.enumerationState == EnumerationState.Incomplete)
        {
            binding.enumerationIncompleteCheckBox.isChecked = true
            binding.enumerationIncompleteCheckBox.text = resources.getString( R.string.incomplete )
            binding.enumerationReasonIncompleteLayout.visibility = View.VISIBLE
            when (enumerationItem.enumerationIncompleteReason)
            {
                resources.getString( R.string.nobody_home ) -> binding.enumerationNobodyHomeButton.isChecked = true
                resources.getString( R.string.home_not_exist ) -> binding.enumerationDoesNotExistButton.isChecked = true
                resources.getString( R.string.other ) -> binding.enumerationOtherButton.isChecked = true
            }
        }
        else
        {
            binding.enumerationStatusTextView.visibility = View.GONE
            binding.enumerationIncompleteCheckBox.visibility = View.GONE
            binding.enumerationReasonIncompleteLayout.visibility = View.GONE
        }

        if (enumerationItem.enumerationNotes.isNotEmpty())
        {
            binding.enumerationNotesEditText.setText( enumerationItem.enumerationNotes )
        }
        else
        {
            binding.enumerationNotesTextView.visibility = View.GONE
            binding.enumerationNotesEditText.visibility = View.GONE
        }

        if (enumerationItem.collectionState == CollectionState.Complete)
        {
            binding.collectionIncompleteCheckBox.isChecked = true
            binding.collectionIncompleteCheckBox.text = resources.getString( R.string.complete )
            binding.collectionReasonIncompleteLayout.visibility = View.GONE
        }
        else if (enumerationItem.collectionState == CollectionState.Incomplete)
        {
            binding.collectionIncompleteCheckBox.isChecked = true
            binding.collectionIncompleteCheckBox.text = resources.getString( R.string.incomplete )
            binding.collectionReasonIncompleteLayout.visibility = View.VISIBLE
            when (enumerationItem.collectionIncompleteReason)
            {
                resources.getString( R.string.nobody_home ) -> binding.collectionNobodyHomeButton.isChecked = true
                resources.getString( R.string.home_not_exist ) -> binding.collectionDoesNotExistButton.isChecked = true
                resources.getString( R.string.other ) -> binding.collectionOtherButton.isChecked = true
            }
        }
        else
        {
            binding.collectionStatusTextView.visibility = View.GONE
            binding.collectionIncompleteCheckBox.visibility = View.GONE
            binding.collectionReasonIncompleteLayout.visibility = View.GONE
        }

        if (enumerationItem.collectionNotes.isNotEmpty())
        {
            binding.collectionNotesEditText.setText( enumerationItem.collectionNotes )
        }
        else
        {
            binding.collectionNotesTextView.visibility = View.GONE
            binding.collectionNotesEditText.visibility = View.GONE
        }

        // filteredFieldDataList contains only non-block fields and block field containers

        val filteredFieldDataList = ArrayList<FieldData>()

        for (fieldData in enumerationItem.fieldDataList)
        {
            DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->
                if (field.parentUUID == null)
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

        binding.subaddressEditText.setText( enumerationItem.subAddress )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))

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

            if (enumerationItem.uuid.isNotEmpty())
            {
                binding.additionalInfoLayout.visibility = View.VISIBLE
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

        ImageDAO.instance().getImage( location )?.let { image ->
            CameraUtils.decodeString( image.data )?.let { bitmap ->
                binding.imageView.setImageBitmap( bitmap )
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
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_household_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                when( buttonPressed )
                {
                    ConfirmationDialog.ButtonPress.Left -> {}
                    ConfirmationDialog.ButtonPress.Right -> {
                        location.enumerationItems.remove( enumerationItem )
                        DAO.enumerationItemDAO.delete( enumerationItem )

                        if (location.enumerationItems.size == 0)
                        {
                            enumArea.locations.remove( location )
                            DAO.locationDAO.delete( location )
                            enumerationTeam.locationUuids.remove( location.uuid )
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

//                        DAO.enumerationTeamDAO.getEnumerationTeam( enumTeam.uuid )?.let {
//                            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( it )
//                        }

                        findNavController().popBackStack()
                    }
                    ConfirmationDialog.ButtonPress.None -> {}
                }
            }

        }

        binding.addPhotoImageView.setOnClickListener {

            // get the total size of all image data
//            var size = 0
//
//            for (location in enumArea.locations)
//            {
//                size += location.imageData.length
//            }
//
//            if (size > 25 * 1024 * 1024)
//            {
//                NotificationDialog( activity!!, resources.getString( R.string.warning), resources.getString( R.string.image_size_warning))
//            }

            findNavController().navigate(R.id.action_navigate_to_CameraFragment)
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

            for (fieldData in enumerationItem.fieldDataList) {
                DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->
                    if (field.type == FieldType.Number) {
                        fieldData.numberValue?.let { numberValue ->
                            field.minimum?.let { minVal ->
                                if (numberValue < minVal) {
                                    Toast.makeText(
                                        context!!.applicationContext,
                                        "${field.name}: " + "The minimum allowed value is" + " ${minVal}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@setOnClickListener
                                }
                            }

                            field.maximum?.let { maxVal ->
                                if (numberValue > maxVal) {
                                    Toast.makeText(
                                        context!!.applicationContext,
                                        "${field.name}: " + "The maximum allowed value is" + " (${maxVal})",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@setOnClickListener
                                }
                            }
                        }
                    }
                }
            }

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
            DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->
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

        DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )

        location.creationDate = Date().time
        DAO.locationDAO.createOrUpdateLocation( location, enumArea )

//        DAO.configDAO.getConfig( config.uuid )?.let {
//            it.selectedStudyUuid = config.selectedStudyUuid
//            it.selectedEnumAreaUuid = config.selectedEnumAreaUuid
//
//            val enumAreas = it.enumAreas.filter {
//                it.uuid == config.selectedEnumAreaUuid
//            }
//
//            if (enumAreas.isNotEmpty())
//            {
//                enumAreas[0].selectedCollectionTeamUuid = enumArea.selectedCollectionTeamUuid
//                enumAreas[0].selectedEnumerationTeamUuid = enumArea.selectedEnumerationTeamUuid
//            }
//
//            sharedViewModel.setCurrentConfig( it )
//        }
//
//        DAO.enumAreaDAO.getEnumArea( enumArea.uuid )?.let {
//            it.selectedCollectionTeamUuid = enumArea.selectedCollectionTeamUuid
//            it.selectedEnumerationTeamUuid = enumArea.selectedEnumerationTeamUuid
//            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( it )
//        }
//
//        DAO.studyDAO.getStudy( study.uuid )?.let {
//            sharedViewModel.createStudyModel.setStudy( it )
//        }
//
//        DAO.enumerationTeamDAO.getEnumerationTeam( enumTeam.uuid )?.let {
//            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( it )
//        }

        findNavController().popBackStack()
    }

    override fun shouldDeleteImage()
    {
        location.imageUuid = ""
        binding.imageCardView.visibility = View.GONE
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}