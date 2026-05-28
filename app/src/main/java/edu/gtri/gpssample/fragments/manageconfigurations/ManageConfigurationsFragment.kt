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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.*
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
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InfoDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.dialogs.NotificationDialog
import edu.gtri.gpssample.utils.ZipUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import kotlin.collections.ArrayList

class ManageConfigurationsFragment : Fragment(),
    NetworkClientModel.ConfigurationDelegate,
    NetworkViewModel.ManageConfigurationNetworkDelegate
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private var configurations = ArrayList<Config>()
    private var encryptionPassword = ""

    private lateinit var user: User
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var sharedNetworkViewModel: NetworkViewModel
    private lateinit var samplingViewModel: SamplingViewModel

    private val REQUEST_CONFIGURATION   = 1001

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        samplingViewModel = samplingVm

        sharedNetworkViewModel = networkVm

        setHasOptionsMenu(true)

        clearFragmentResultListener( this.javaClass.simpleName )

        setFragmentResultListener( this.javaClass.simpleName ) { key, bundle ->
            val errorCode = Config.ErrorCode.values()[bundle.getInt(Keys.kError.value)]
            didReceiveConfiguration( errorCode )
            clearFragmentResult( this.javaClass.simpleName )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)

        sharedNetworkViewModel.currentFragment = this
        sharedNetworkViewModel.networkClientModel.configurationDelegate = this

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

        configurations = DAO.configDAO.getMinimalConfigs()

        manageConfigurationsAdapter = ManageConfigurationsAdapter( configurations )
        manageConfigurationsAdapter.didSelectConfig = this::didSelectConfig
        manageConfigurationsAdapter.shouldCloneConfig = this::shouldCloneConfig

        if (user.role == Role.Enumerator.value && configurations.isNotEmpty()) // && configurations[0].selectedEnumAreaUuid.isEmpty())
        {
            binding.createButton.visibility = View.VISIBLE
        }

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageConfigurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        if (BuildConfig.DEBUG && (user.role == Role.Admin.toString() || user.role == Role.Supervisor.toString()))
        {
            binding.createButton.visibility = View.VISIBLE
        }

        binding.addButton.setOnClickListener {
            sharedViewModel.createNewConfiguration()
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.createButton.setOnClickListener {

            if (configurations.isNotEmpty())
            {
                getFullConfig( configurations[0] ) { config ->
                    sharedViewModel.setCurrentConfig( config )
                    val bundle = Bundle()
                    bundle.putBoolean( Keys.kEditMode.value, true )
                    findNavController().navigate(R.id.action_navigate_to_WalkEnumerationAreaFragment, bundle)
                }
            }
        }

        binding.importButton.setOnClickListener {
            var password = ""

            if (configurations.size == 1)
            {
                encryptionPassword = configurations[0].encryptionPassword
                for (i in 1..encryptionPassword.length)
                {
                    password += "*"
                }
            }

            if ((configurations.size == 1) && ((user.role == Role.Enumerator.toString() || user.role == Role.DataCollector.toString())))
            {
                ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.delete_configuration), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
                    when( buttonPressed )
                    {
                        ConfirmationDialog.ButtonPress.Left -> {
                        }
                        ConfirmationDialog.ButtonPress.Right -> {
                            DAO.deleteAll()
                            ImageDAO.deleteAll()
                            configurations.clear()
                            manageConfigurationsAdapter.updateConfigurations(configurations)
                            InputDialog(activity!!, false, resources.getString(R.string.enter_encryption_password), password, resources.getString(R.string.cancel), resources.getString(R.string.next), null, false )  { action, text, tag ->
                                when (action) {
                                    InputDialog.Action.DidCancel -> {}
                                    InputDialog.Action.DidEnterText -> {didEnterPassword( text )}
                                    InputDialog.Action.DidPressQRButton -> {}
                                }
                            }
                        }
                        ConfirmationDialog.ButtonPress.None -> {
                        }
                    }
                }
            }
            else
            {
                InputDialog(activity!!, false, resources.getString(R.string.enter_encryption_password), password, resources.getString(R.string.cancel), resources.getString(R.string.next), null, false)  { action, text, tag ->
                    when (action) {
                        InputDialog.Action.DidCancel -> {}
                        InputDialog.Action.DidEnterText -> {didEnterPassword( text )}
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

    private fun shouldCloneConfig( config: Config )
    {
        ConfirmationDialog( activity, resources.getString(R.string.clone_configuration), resources.getString(R.string.confirm_clone_configuration), resources.getString(R.string.no), resources.getString(R.string.yes), null, false ) { buttonPressed, tag ->
            when( buttonPressed )
            {
                ConfirmationDialog.ButtonPress.Left -> {
                }
                ConfirmationDialog.ButtonPress.Right -> {
                    cloneConfig( config )
                }
                ConfirmationDialog.ButtonPress.None -> {
                }
            }
        }
    }

    private fun cloneConfig( config: Config )
    {
        val newConfig = config.copy()

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
            newConfig.studies[0].primaryFilters.clear()
            newConfig.studies[0].subsetFilters.clear()

            // update fields
            for (field in newConfig.studies[0].fields)
            {
                val oldFieldUuid = field.uuid

                field.uuid = UUID.randomUUID().toString()
                field.creationDate = Date().time

                // update primary rules
                for (rule in newConfig.studies[0].primaryRules)
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
        DAO.configDAO.createOrUpdateConfig( newConfig )
        configurations.add( newConfig )
        manageConfigurationsAdapter.updateConfigurations( configurations )
    }

    private fun didSelectConfig( config: Config )
    {
        if (user.role == Role.Admin.value || user.role == Role.Supervisor.value)
        {
            getFullConfig( config ) { fullConfig ->
                sharedViewModel.setCurrentConfig( fullConfig )
                findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment)
            }
        }
        else
        {
            sharedViewModel.setCurrentConfig( config )
            navigateBasedOnRole()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.value)

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.value)
                val pass = jsonObject.getString(Keys.kPass.value)
                val serverIp = jsonObject.getString(Keys.kIpAddress.value)

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)
            }
        }

    override fun configurationReceived(config: Config)
    {
        runBlocking(Dispatchers.Main) {
            sharedViewModel.setCurrentConfig( config )
            configurations = DAO.configDAO.getMinimalConfigs()
            manageConfigurationsAdapter.updateConfigurations( configurations )
        }
    }

    override fun didReceiveConfiguration(errorCode: Config.ErrorCode)
    {
        if (errorCode != Config.ErrorCode.None)
        {
            val message = if (errorCode == Config.ErrorCode.PasswordError) resources.getString(R.string.password_error) else resources.getString(R.string.import_failed)
            InfoDialog( activity!!, resources.getString(R.string.error), message, resources.getString(R.string.ok), null, null)
        }
        else
        {
            InfoDialog( activity!!, resources.getString(R.string.success), resources.getString(R.string.import_succeeded), resources.getString(R.string.ok), null, null)

            if (user.role == Role.Enumerator.toString() && configurations.isNotEmpty() && configurations[0].selectedEnumAreaUuid.isEmpty())
            {
                binding.createButton.visibility = View.VISIBLE
            }

            navigateBasedOnRole()
        }
    }

    fun navigateBasedOnRole()
    {
        if (user.role == Role.Enumerator.toString() || user.role == Role.DataCollector.toString())
        {
            sharedViewModel.currentConfiguration?.value?.let { currentConfig ->
                getFullConfig( currentConfig ) { config ->
                    sharedViewModel.setCurrentConfig( config )

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

                                sharedViewModel.createStudyModel.setStudy( study )
                                sharedViewModel.currentCollectionTeamUuid = collectionTeam.uuid
                                sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                                samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

                                findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
                            }
                            else if (enumTeams.isNotEmpty())
                            {
                                val enumTeam = enumTeams[0]

                                sharedViewModel.createStudyModel.setStudy( study )
                                sharedViewModel.currentEnumerationTeamUuid = enumTeam.uuid
                                sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )

                                findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
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

                                sharedViewModel.createStudyModel.setStudy( study )
                                sharedViewModel.currentCollectionTeamUuid = collectionTeam.uuid
                                sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                                samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

                                findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun didEnterPassword( password: String )
    {
        if (password != "******")
        {
            encryptionPassword = password
        }

        ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), null, false ) { buttonPressed, tag ->
            when( buttonPressed )
            {
                ConfirmationDialog.ButtonPress.Left -> {
                    sharedNetworkViewModel.networkClientModel.encryptionPassword = encryptionPassword
                    sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.Configuration)
                    val intent = Intent(context, CameraXLivePreviewActivity::class.java)
                    getResult.launch(intent)
                }
                ConfirmationDialog.ButtonPress.Right -> {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.select_configuration_file), Toast.LENGTH_LONG).show()

                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)

                    startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), REQUEST_CONFIGURATION)
                }
                ConfirmationDialog.ButtonPress.None -> {
                }
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
                    binding.overlayView.visibility = View.VISIBLE

                    ZipUtils.unzip( activity!!, uri, encryptionPassword ) { result ->
                        val config = result.first
                        val errorCode = result.second

                        if (config == null)
                        {
                            binding.overlayView.visibility = View.GONE
                            val message = if (errorCode == Config.ErrorCode.PasswordError) resources.getString(R.string.password_error ) else resources.getString(R.string.import_failed)
                            InfoDialog( activity!!, resources.getString(R.string.error), message, resources.getString(R.string.ok), null, null)
                        }
                        else
                        {
                            DAO.configDAO.createOrUpdateConfig( config )

                            sharedViewModel.setCurrentConfig( config )

                            configurations.find { it.uuid == config.uuid } ?.let {
                                configurations.remove(it )
                            }

                            configurations.add( config )
                            manageConfigurationsAdapter.updateConfigurations( configurations )

                            binding.overlayView.visibility = View.GONE

                            didReceiveConfiguration(Config.ErrorCode.None )
                        }
                    }
                }
                catch( ex: java.lang.Exception )
                {
                    binding.overlayView.visibility = View.GONE
                    InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
                }
            }
        }
        else
        {
            binding.overlayView.visibility = View.GONE
            InfoDialog( activity!!, resources.getString(R.string.error), resources.getString(R.string.import_failed), resources.getString(R.string.ok), null, null)
        }
    }

    fun getFullConfig( config: Config, completion: ((config: Config)->Unit) )
    {
        if (config.studies.isNotEmpty() && config.enumAreas.isNotEmpty())
        {
            completion( config ) // we already have a full config
        }
        else
        {
            binding.overlayView.visibility = View.VISIBLE

            Thread {
                val fullConfig = DAO.configDAO.getConfig( config.uuid )

                requireActivity().runOnUiThread {
                    binding.overlayView.visibility = View.GONE

                    fullConfig?.let { fullConfig ->
                        completion( fullConfig )
                    } ?: completion( config )
                }
            }.start()
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}