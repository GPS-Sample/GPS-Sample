/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.manageconfigurations

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.dialogs.ButtonPress
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.MultiConfirmationDialog
import edu.gtri.gpssample.dialogs.NearbySessionStatusDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.managers.NearbySessionClientManager
import edu.gtri.gpssample.managers.PerformanceManager
import edu.gtri.gpssample.ui.ComposableConfirmationDialogHost
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import kotlin.collections.ArrayList

class ManageConfigurationsFragment : Fragment()
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private var minimalConfigurations = ArrayList<Config>()
    private var encryptionPassword = ""
    private var nearbySessionStatusDialog: NearbySessionStatusDialog? = null
    private var nearbySessionClientManager: NearbySessionClientManager? = null
    private lateinit var user: User
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var samplingViewModel: SamplingViewModel
    private val REQUEST_CONFIGURATION   = 1001
    private lateinit var composableConfirmationDialogHost: ComposableConfirmationDialogHost

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        samplingViewModel = samplingVm

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if ((requireActivity().application as MainApplication).user == null)
        {
            findNavController().navigate(R.id.action_navigate_to_MainFragment, null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.action_navigate_to_MainFragment, false)
                    .build())
            return
        }

        composableConfirmationDialogHost = ComposableConfirmationDialogHost()

        binding.confirmationComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        binding.confirmationComposeView.setContent {
            composableConfirmationDialogHost.Content()
        }

        val distanceFormats = resources.getTextArray( R.array.distance_formats )
        sharedViewModel.distanceFormats[0] = distanceFormats[0].toString()
        sharedViewModel.distanceFormats[1] = distanceFormats[1].toString()

        val timeFormats = resources.getTextArray( R.array.time_formats )
        sharedViewModel.timeFormats[0] = timeFormats[0].toString()
        sharedViewModel.timeFormats[1] = timeFormats[1].toString()

        val dateFormats = resources.getTextArray( R.array.date_formats )
        sharedViewModel.dateFormats[0] = dateFormats[0].toString()
        sharedViewModel.dateFormats[1] = dateFormats[1].toString()
        sharedViewModel.dateFormats[2] = dateFormats[2].toString()

        sharedViewModel.minimumGpsPrecisionFormats[0] = resources.getString(R.string.meters)
        sharedViewModel.minimumGpsPrecisionFormats[1] = resources.getString(R.string.feet)

        (activity!!.application as MainApplication).user?.let { user ->
            this.user = user
        }

        if (user.role != Role.Admin.value)
        {
            binding.addButton.visibility = View.GONE
        }

        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                minimalConfigurations = DAO.configDAO.getMinimalConfigs()
            }

            // back on the main thread...
            binding.progressOverlayView.visibility = View.GONE

            manageConfigurationsAdapter = ManageConfigurationsAdapter( minimalConfigurations )
            manageConfigurationsAdapter.didSelectConfig = this@ManageConfigurationsFragment::didSelectConfig

            if (user.role == Role.Enumerator.value && minimalConfigurations.isNotEmpty()) // && configurations[0].selectedEnumAreaUuid.isEmpty())
            {
                binding.walkButton.visibility = View.VISIBLE
            }

            binding.recyclerView.itemAnimator = DefaultItemAnimator()
            binding.recyclerView.adapter = manageConfigurationsAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(activity )

            if (BuildConfig.DEBUG && (user.role == Role.Admin.toString() || user.role == Role.Supervisor.toString()))
            {
                binding.walkButton.visibility = View.VISIBLE
            }

            binding.addButton.setOnClickListener {
                sharedViewModel.createNewConfiguration()
                findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
            }

            binding.walkButton.setOnClickListener {

                if (minimalConfigurations.isNotEmpty())
                {
                    DAO.configDAO.getConfig( minimalConfigurations[0].uuid )?.let { config ->
                        sharedViewModel.setCurrentConfig( config )
                        val bundle = Bundle()
                        bundle.putBoolean( Keys.kEditMode.value, true )
                        findNavController().navigate(R.id.action_navigate_to_WalkEnumerationAreaFragment, bundle)
                    }
                }
            }

            binding.importButton.setOnClickListener {
                var password = ""

                if (minimalConfigurations.size == 1)
                {
                    encryptionPassword = minimalConfigurations[0].encryptionPassword
                    for (i in 1..encryptionPassword.length)
                    {
                        password += "*"
                    }
                }

                InputDialog(activity!!, false, resources.getString(R.string.enter_encryption_password), password, resources.getString(R.string.cancel), resources.getString(R.string.next), null, false)  { action, password, tag ->
                    when (action) {
                        InputDialog.Action.DidCancel -> {}
                        InputDialog.Action.DidEnterText -> {importConfiguration( password )}
                        InputDialog.Action.DidPressQRButton -> {}
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageConfigurationsFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.about -> findNavController().navigate(R.id.action_navigate_to_AboutFragment)
            R.id.terms -> findNavController().navigate(R.id.action_navigate_to_TermsFragment)
            R.id.privacy -> findNavController().navigate(R.id.action_navigate_to_PrivacyFragment)
            R.id.eula -> findNavController().navigate(R.id.action_navigate_to_EulaFragment)
            R.id.code -> findNavController().navigate(R.id.action_navigate_to_CodeFragment)
            R.id.cache_map_tiles -> findNavController().navigate(R.id.action_navigate_to_MapFragment)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun didSelectConfig( config: Config )
    {
        val items = ArrayList<String>()

        if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
        {
            items.add(resources.getString(R.string.open))
            items.add(resources.getString(R.string.edit))
            items.add(resources.getString(R.string.clone))
            items.add(resources.getString(R.string.delete))
        }
        else
        {
            items.add(resources.getString(R.string.open))
            items.add(resources.getString(R.string.delete))
        }


        if (config.studies.isNotEmpty() && config.studies.first().samplingMethod == SamplingMethod.Strata)
        {
            items.add(resources.getString(R.string.select_strata))
        }

        MultiConfirmationDialog( requireActivity(),config.name, "", items, null ) { selection, tag ->
            if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
            {
                when (selection)
                {
                    items[0] -> { navigateBasedOnRole( config ) }
                    items[1] -> { editConfig( config ) }
                    items[2] -> { cloneConfig( config ) }
                    items[3] -> { deleteConfig( config ) }
                }
            }
            else
            {
                when (selection)
                {
                    items[0] -> { navigateBasedOnRole( config ) }
                    items[1] -> { deleteConfig( config ) }
                }
            }
        }
    }

    fun editConfig( config: Config )
    {
        sharedViewModel.setCurrentConfig( config )

        binding.progressOverlayView.visibility= View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO)
            {
                if (config.studies.isEmpty())
                {
                    config.studies = DAO.studyDAO.getStudies( config )
                }

                if (config.enumAreas.isEmpty())
                {
                    config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
                }
            }

            // back on the main thread...

            binding.progressOverlayView.visibility = View.GONE

            if (config.studies.count() > 0)
            {
                sharedViewModel.createStudyModel.setCurrentStudy(config.studies[0])
            }

            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }
    }

    fun deleteConfig( config: Config )
    {
        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.delete_config),
            message = resources.getString(R.string.delete_configuration_message),
            leftButtonText = resources.getString(R.string.no),
            rightButtonText = resources.getString(R.string.yes)
        ) { buttonPressed ->
            if (buttonPressed == ButtonPress.Right)
            {
                binding.progressOverlayView.visibility= View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO)
                    {
                        minimalConfigurations.remove(config)
                        DAO.configDAO.deleteConfig(config)
                    }

                    // back on the main thread...

                    binding.progressOverlayView.visibility = View.GONE
                    manageConfigurationsAdapter.updateConfigurations(minimalConfigurations)
                }
            }
        }
    }

    private fun cloneConfig( config: Config )
    {
        composableConfirmationDialogHost.show(
            title = "Clone Configuration",
            message = "Are you sure you want to Clone this Configuration?",
            leftButtonText = "No",
            rightButtonText = "Yes"
        ) { buttonPressed ->
            if (buttonPressed == ButtonPress.Right)
            {
                binding.progressOverlayView.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val newConfig = config.copy()

                        if (newConfig.studies.isEmpty())
                        {
                            newConfig.studies = DAO.studyDAO.getStudies( newConfig )
                        }

                        if (newConfig.enumAreas.isEmpty())
                        {
                            newConfig.enumAreas = DAO.enumAreaDAO.getEnumAreas( newConfig )
                        }

                        // update config base
                        newConfig.uuid = UUID.randomUUID().toString()
                        newConfig.creationDate = Date().time
                        newConfig.name += "-copy"
                        newConfig.selectedStudyUuid = ""
                        newConfig.validUsers = ""

                        // update enumAreas
                        for (enumArea in newConfig.enumAreas)
                        {
                            enumArea.uuid = UUID.randomUUID().toString()
                            enumArea.creationDate = Date().time
                            enumArea.configUuid = newConfig.uuid
                            enumArea.enumerationTeams.clear()
                            enumArea.collectionTeams.clear()
                            enumArea.selectedEnumerationTeamUuid = ""
                            enumArea.selectedCollectionTeamUuid = ""

                            var creationDate = Date().time

                            // update vertices
                            for (vertice in enumArea.vertices)
                            {
                                vertice.uuid = UUID.randomUUID().toString()
                                vertice.creationDate = creationDate
                                creationDate += 1
                            }

                            // update locations
                            for (location in enumArea.locations)
                            {
                                location.uuid = UUID.randomUUID().toString()
                                location.creationDate = Date().time
                                location.enumerationItems.clear()

                                // update images
                                if (location.imageUuid.isNotEmpty())
                                {
                                    // create a copy of the image
                                    ImageDAO.instance().getImage( location.imageUuid )?.let { image ->
                                        image.uuid = UUID.randomUUID().toString()
                                        image.creationDate = Date().time
                                        image.locationUuid = location.uuid
                                        location.imageUuid = image.uuid
                                        // save the copy of the image to the image database
                                        ImageDAO.instance().createImage( image )
                                    }
                                }
                            }
                        }

                        // update studies
                        if (newConfig.studies.isNotEmpty())
                        {
                            var numStudies = newConfig.studies.count()

                            // HACK! remove all studies except the first
                            while (numStudies > 1)
                            {
                                newConfig.studies.remove(newConfig.studies.last())
                                numStudies -= 1
                            }

                            newConfig.studies[0].uuid = UUID.randomUUID().toString()
                            newConfig.studies[0].creationDate = Date().time
                            newConfig.studies[0].filters.clear()
                            newConfig.studies[0].subsetFilters.clear()

                            // update fields
                            for (field in newConfig.studies[0].fields)
                            {
                                val oldFieldUuid = field.uuid

                                field.uuid = UUID.randomUUID().toString()
                                field.creationDate = Date().time

                                // update primary rules
                                for (rule in newConfig.studies[0].rules)
                                {
                                    if (rule.fieldUuid == oldFieldUuid)
                                    {
                                        rule.fieldUuid = field.uuid
                                        rule.uuid = UUID.randomUUID().toString()
                                        break
                                    }
                                }

                                // update subset rules
                                for (rule in newConfig.studies[0].subsetRules)
                                {
                                    if (rule.fieldUuid == oldFieldUuid)
                                    {
                                        rule.fieldUuid = field.uuid
                                        rule.uuid = UUID.randomUUID().toString()
                                        break
                                    }
                                }
                            }

                            // update stratas
                            for (strata in newConfig.studies[0].stratas)
                            {
                                val oldStrataUuid = strata.uuid

                                strata.uuid = UUID.randomUUID().toString()
                                strata.creationDate = Date().time
                                strata.studyUuid = newConfig.studies[0].uuid

                                for (enumArea in newConfig.enumAreas)
                                {
                                    if (enumArea.strataUuid == oldStrataUuid)
                                    {
                                        enumArea.strataUuid = strata.uuid
                                        break
                                    }
                                }
                            }
                        }

                        // save the new config to the database
                        DAO.configDAO.createOrUpdateConfig( newConfig, config.version )

                        minimalConfigurations.add( newConfig )
                    }

                    // back on the main thread...
                    manageConfigurationsAdapter.updateConfigurations( minimalConfigurations )

                    binding.progressOverlayView.visibility = View.GONE
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getQrCode = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == ResultCode.BarcodeScanned.value)
        {
            // payload contains the sessionId
            result.data!!.getStringExtra(Keys.kPayload.value )?.let { sessionId ->
                nearbySessionClientManager = NearbySessionClientManager( requireContext().applicationContext )

                nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(),getString(R.string.import_configuration))
                {
                    nearbySessionClientManager?.cancel()
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED )
                    {
                        nearbySessionClientManager?.state?.collect { state ->
                            nearbySessionStatusDialog?.updateState(state)
                        }
                    }
                }

                PerformanceManager.startTimer()

                nearbySessionClientManager?.connect( sessionId ) { config ->
                    nearbySessionStatusDialog?.setStatus( "Saving Configuration..." )

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO)
                        {
                            DAO.configDAO.createOrUpdateConfig( config,config.version )
                        }

                        // back on the main thread...

                        Log.d("xxx", "Transfer time: ${PerformanceManager.elapsedTime()}")

                        nearbySessionStatusDialog?.dismiss()

                        minimalConfigurations.find { it.uuid == config.uuid } ?.let {
                            minimalConfigurations.remove(it )
                        }

                        minimalConfigurations.add( config )
                        manageConfigurationsAdapter.updateConfigurations( minimalConfigurations )

                        didReceiveConfiguration( config )
                    }
                }
            }
        }
    }

    fun didReceiveConfiguration( config: Config )
    {
        if (user.role == Role.Enumerator.toString() && config.selectedEnumAreaUuid.isEmpty())
        {
            binding.walkButton.visibility = View.VISIBLE
        }

        navigateBasedOnRole( config )
    }

    fun navigateBasedOnRole( config: Config )
    {
        binding.progressOverlayView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (config.studies.isEmpty())
                {
                    config.studies = DAO.studyDAO.getStudies( config )
                }

                if ((user.role == Role.Enumerator.value || user.role == Role.DataCollector.value) && (config.selectedEnumAreaUuid.isNotEmpty()))
                {
                    DAO.enumAreaDAO.getEnumArea( config.selectedEnumAreaUuid )?.let { enumArea ->
                        config.enumAreas.clear()
                        config.enumAreas.add( enumArea )
                    }
                }
            }

            // back on the main thread...

            binding.progressOverlayView.visibility = View.GONE

            sharedViewModel.setCurrentConfig( config )

            if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
            {
                findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment )
            }
            else
            {
                // find the selected Enum Area
                val enumAreas = config.enumAreas.filter { it.uuid == config.selectedEnumAreaUuid }

                // find the selected study
                val studies = config.studies.filter { it.uuid == config.selectedStudyUuid }

                if (user.role == Role.Enumerator.toString())
                {
                    if (enumAreas.isEmpty())
                    {
                        NotificationDialog( requireActivity(), resources.getString( R.string.oops ), resources.getString(R.string.missing_enumeration_area))
                    }
                    else if (studies.isEmpty())
                    {
                        NotificationDialog( requireActivity(), resources.getString( R.string.oops ), resources.getString(R.string.missing_study))
                    }
                    else
                    {
                        val study = studies[0]
                        val enumArea = enumAreas[0]

                        // find the selected enumeration Team
                        val enumTeams = enumArea.enumerationTeams.filter { enumTeam -> enumTeam.uuid == enumArea.selectedEnumerationTeamUuid }

                        // find the selected collection Team
                        val collectionTeams = enumArea.collectionTeams.filter { collectionTeam -> collectionTeam.uuid == enumArea.selectedCollectionTeamUuid }

                        if (collectionTeams.isNotEmpty())
                        {
                            val collectionTeam = collectionTeams[0]

                            // remove locations that are not in the selected team
                            val locationUuids = collectionTeam.locationUuids.toHashSet()
                            enumArea.locations.retainAll { it.uuid in locationUuids }

                            sharedViewModel.createStudyModel.setCurrentStudy( study )
                            sharedViewModel.currentCollectionTeamUuid = collectionTeam.uuid
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                            samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

                            findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment )
                        }
                        else if (enumTeams.isNotEmpty())
                        {
                            val enumTeam = enumTeams[0]

                            // remove locations that are not in the selected team
                            val locationUuids = enumTeam.locationUuids.toHashSet()
                            enumArea.locations.retainAll { it.uuid in locationUuids }

                            sharedViewModel.createStudyModel.setCurrentStudy( study )
                            sharedViewModel.currentEnumerationTeamUuid = enumTeam.uuid
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )

                            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment )
                        }
                    }
                }
                else if (user.role == Role.DataCollector.toString())
                {
                    if (enumAreas.isEmpty())
                    {
                        NotificationDialog( requireActivity(), resources.getString( R.string.oops ), resources.getString(R.string.missing_enumeration_area))
                    }
                    else if (studies.isEmpty())
                    {
                        NotificationDialog( requireActivity(), resources.getString( R.string.oops ), resources.getString(R.string.missing_study))
                    }
                    else
                    {
                        val study = studies[0]
                        val enumArea = enumAreas[0]

                        // find the selected collection Team
                        val collectionTeams = enumArea.collectionTeams.filter { collectionTeam -> collectionTeam.uuid == enumArea.selectedCollectionTeamUuid }

                        if (collectionTeams.isEmpty())
                        {
                            NotificationDialog( requireActivity(), resources.getString( R.string.oops ), resources.getString(R.string.missing_collection_team))
                        }
                        else
                        {
                            val collectionTeam = collectionTeams[0]

                            // remove locations that are not in the selected team
                            val locationUuids = collectionTeam.locationUuids.toHashSet()
                            enumArea.locations.retainAll { it.uuid in locationUuids }

                            sharedViewModel.createStudyModel.setCurrentStudy( study )
                            sharedViewModel.currentCollectionTeamUuid = collectionTeam.uuid
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                            samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

                            findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment )
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun importConfiguration( password: String )
    {
        val allStars = password.isNotEmpty() && password.all { it == '*' }

        if (!allStars)
        {
            encryptionPassword = password
        }

        composableConfirmationDialogHost.show(
            title = resources.getString(R.string.import_configuration),
            message = resources.getString(R.string.select_import_method),
            leftButtonText = resources.getString(R.string.qr_code),
            rightButtonText = resources.getString(R.string.file_system),
            layoutVertically = true
        ) { buttonPressed ->
            if (buttonPressed == ButtonPress.Left)
            {
                val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                getQrCode.launch(intent)
            }
            else if (buttonPressed == ButtonPress.Right)
            {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), REQUEST_CONFIGURATION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CONFIGURATION && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data

            uri?.let { uri ->
                try
                {
                    val zipUtils = ZipUtils()

                    nearbySessionStatusDialog = NearbySessionStatusDialog(requireContext(), resources.getString( R.string.import_configuration )) {
                        zipUtils.cancel()
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        zipUtils.state.collect { state ->
                            nearbySessionStatusDialog?.updateState(state)
                        }
                    }

                    PerformanceManager.startTimer()

                    zipUtils.unzip( activity!!, uri, encryptionPassword ) { result ->
                        val config = result.first
                        val errorCode = result.second

                        if (config == null)
                        {
                            if (errorCode != ErrorCode.None)
                            {
                                val message = if (errorCode == ErrorCode.DecryptError) resources.getString(R.string.password_error ) else resources.getString(R.string.import_failed)
                                NotificationDialog( activity!!, resources.getString(R.string.error), message)
                            }
                        }
                        else
                        {
                            DAO.configDAO.createOrUpdateConfig( config, config.version )

                            minimalConfigurations.find { it.uuid == config.uuid } ?.let {
                                minimalConfigurations.remove(it )
                            }

                            minimalConfigurations.add( config )
                            manageConfigurationsAdapter.updateConfigurations( minimalConfigurations )

                            didReceiveConfiguration( config )
                        }

                        nearbySessionStatusDialog?.dismiss()
                        nearbySessionStatusDialog = null

                        Log.d( "xxx", "Import time : ${PerformanceManager.elapsedTime()}")
                    }
                }
                catch( ex: java.lang.Exception )
                {
                    binding.progressOverlayView.visibility = View.GONE
                    InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                }
            }
        }
        else
        {
            binding.progressOverlayView.visibility = View.GONE
            InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
        }
    }

    override fun onDestroyView()
    {
        nearbySessionStatusDialog?.dismiss()
        nearbySessionStatusDialog = null

        nearbySessionClientManager?.cancel()

        binding.recyclerView.adapter = null
        _binding = null

        super.onDestroyView()
    }

    override fun onDestroy()
    {
        nearbySessionClientManager?.shutdown()

        super.onDestroy()
    }
}
