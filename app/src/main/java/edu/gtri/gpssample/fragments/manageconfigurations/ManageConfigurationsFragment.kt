package edu.gtri.gpssample.fragments.manageconfigurations

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.dialogs.BusyIndicatorDialog
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.dialogs.InputDialog
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class ManageConfigurationsFragment : Fragment(),
    InputDialog.InputDialogDelegate,
    MapboxManager.MapTileCacheDelegate,
    NetworkClientModel.ConfigurationDelegate,
    ConfirmationDialog.ConfirmationDialogDelegate,
    BusyIndicatorDialog.BusyIndicatorDialogDelegate,
    NetworkViewModel.ManageConfigurationNetworkDelegate
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!
    private var busyIndicatorDialog: BusyIndicatorDialog? = null
    private var configurations = ArrayList<Config>()
    private var encryptionPassword = ""

    private lateinit var user: User
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var sharedNetworkViewModel: NetworkViewModel
    private lateinit var samplingViewModel: SamplingViewModel

    private val kImportTag = 1
    private val kDeleteTag = 2
    private val kExportTag = 3

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()
        val samplingVm : SamplingViewModel by activityViewModels()

        sharedViewModel = vm
        samplingViewModel = samplingVm

        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this
        sharedNetworkViewModel.networkClientModel.configurationDelegate = this
        sharedNetworkViewModel.manageConfigurationNetworkDelegate = this

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        configurations = DAO.configDAO.getConfigs()

        manageConfigurationsAdapter = ManageConfigurationsAdapter(configurations)
        manageConfigurationsAdapter.didSelectConfig = this::didSelectConfig

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageConfigurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        (activity!!.application as MainApplication).user?.let { user ->
            this.user = user

            if (user.role != Role.Admin.toString())
            {
                binding.addButton.visibility = View.GONE
            }

            if (user.role == Role.Enumerator.toString() && configurations.isNotEmpty() && configurations[0].selectedEnumAreaId < 0)
            {
                binding.createButton.visibility = View.VISIBLE
                binding.exportButton.visibility = View.VISIBLE
            }
        }

        binding.addButton.setOnClickListener {
            sharedViewModel.createNewConfiguration()
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.createButton.setOnClickListener {

            if (configurations.isNotEmpty())
            {
                sharedViewModel.setCurrentConfig( configurations[0])
                val bundle = Bundle()
                bundle.putBoolean( Keys.kEditMode.toString(), true )
                findNavController().navigate(R.id.action_navigate_to_WalkEnumerationAreaFragment, bundle)
            }
        }

        binding.exportButton.setOnClickListener {
            ConfirmationDialog( activity, resources.getString(R.string.export_configuration), resources.getString(R.string.select_export_message), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kExportTag, this)
        }

        binding.importButton.setOnClickListener {
            if ((user.role == Role.Admin.toString() || user.role == Role.Supervisor.toString()) && configurations.size == 1)
            {
                encryptionPassword = configurations[0].encryptionPassword
                ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kImportTag, this)
            }
            else
            {
                InputDialog(activity!!, resources.getString(R.string.enter_encryption_password), "", null, this, false)
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
        val bundle = Bundle()
        sharedViewModel.setCurrentConfig(config)

        when (user.role)
        {
            Role.Enumerator.toString() ->
            {
                if (!navigateToCollection())
                {
                    navigateToEnumeration()
                }
            }
            Role.DataCollector.toString() ->
            {
                navigateToCollection()
            }
            Role.Admin.toString(), Role.Supervisor.toString() ->
            {
                findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment, bundle)
            }
        }
    }

    fun navigateToEnumeration()
    {
        if (configurations.isNotEmpty())
        {
            val config = configurations[0]
            sharedViewModel.setCurrentConfig( config )

            // find the selected Enum Area
            val enumAreas = config.enumAreas.filter {
                it.id?.let { id ->
                    id == config.selectedEnumAreaId
                } ?: false
            }

            var enumArea: EnumArea? = null

            if (enumAreas.isNotEmpty())
            {
                enumArea = enumAreas[0]
            }
            else if (config.selectedEnumAreaId < 0 && config.enumAreas.isNotEmpty())
            {
                enumArea = config.enumAreas[0]
            }

            // find the selected study
            val studies = config.studies.filter {
                it.id?.let { id ->
                    id == config.selectedStudyId
                } ?: false
            }

            var study: Study? = null

            if (studies.isNotEmpty())
            {
                study = studies[0]
            }
            else if (config.selectedStudyId < 0 && config.studies.isNotEmpty())
            {
                study = config.studies[0]
            }

            if (enumArea != null && study != null)
            {
                // find the selected enumeration Team
                val enumTeams = enumArea.enumerationTeams.filter {
                    it.id?.let { id ->
                        id == enumArea.selectedEnumerationTeamId
                    } ?: false
                }

                var enumTeam: EnumerationTeam? = null

                if (enumTeams.isNotEmpty())
                {
                    enumTeam = enumTeams[0]
                }
                else if (enumArea.selectedEnumerationTeamId < 0 && enumArea.enumerationTeams.isNotEmpty())
                {
                    enumTeam = enumArea.enumerationTeams[0]
                }

                enumTeam?.let { enumTeam ->
                    sharedViewModel.createStudyModel.setStudy( study )
                    sharedViewModel.teamViewModel.setCurrentEnumerationTeam( enumTeam )
                    sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                    findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
                }
            }
        }
    }

    fun navigateToCollection() : Boolean
    {
        if(configurations.size > 0)
        {
            val config = configurations[0]
            sharedViewModel.setCurrentConfig( config )

            // find the selected Enum Area
            val enumAreas = config.enumAreas.filter {
                it.id?.let { id ->
                    id == config.selectedEnumAreaId
                } ?: false
            }

            // find the selected study
            val studies = config.studies.filter {
                it.id?.let { id ->
                    id == config.selectedStudyId
                } ?: false
            }

            if (enumAreas.isNotEmpty() && studies.isNotEmpty())
            {
                val study = studies[0]
                val enumArea = enumAreas[0]

                // find the selected collection Team
                val collectionTeams = study.collectionTeams.filter { collectionTeam ->
                    collectionTeam.id?.let { id ->
                        id == study.selectedCollectionTeamId
                    } ?: false
                }

                if (collectionTeams.isNotEmpty())
                {
                    val collectionTeam = collectionTeams[0]
                    sharedViewModel.createStudyModel.setStudy( study )
                    sharedViewModel.teamViewModel.setCurrentCollectionTeam( collectionTeam )
                    sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                    samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
                    findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
                    return true
                }
            }
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResultCode.BarcodeScanned.value) {
                val payload = it.data!!.getStringExtra(Keys.kPayload.toString())

                val jsonObject = JSONObject(payload);

                Log.d("xxx", jsonObject.toString(2))

                val ssid = jsonObject.getString(Keys.kSSID.toString())
                val pass = jsonObject.getString(Keys.kPass.toString())
                val serverIp = jsonObject.getString(Keys.kIpAddress.toString())

                Log.d("xxxx", "the ssid, pass, serverIP ${ssid}, ${pass}, ${serverIp}")

                sharedNetworkViewModel.connectHotspot(ssid, pass, serverIp)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        val sharedPreferences: SharedPreferences = activity!!.getSharedPreferences("default", 0)

        if (tag == kImportTag)
        {
            sharedNetworkViewModel.networkClientModel.encryptionPassword = encryptionPassword
            sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.Configuration)
            val intent = Intent(context, CameraXLivePreviewActivity::class.java)
            getResult.launch(intent)
        }
        else if (tag == kExportTag)
        {
            if (configurations.size == 1)
            {
                view?.let { view ->
                    sharedNetworkViewModel.networkHotspotModel.encryptionPassword = encryptionPassword
                    sharedNetworkViewModel.networkHotspotModel.setHotspotMode( HotspotMode.Admin )

                    sharedNetworkViewModel.setCurrentConfig(configurations[0])

                    sharedNetworkViewModel.createHotspot(view)
                }
            }
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag == kDeleteTag)
        {
            DAO.deleteAll()
            configurations.clear()
            manageConfigurationsAdapter.updateConfigurations(configurations)

            ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kImportTag, this)
        }
        else if (tag == kImportTag)
        {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), 1023)
        }
        else if (tag == kExportTag)
        {
            if (configurations.size == 1)
            {
                exportToDevice()
            }
        }
    }

    fun exportToDevice( )
    {
        val packedConfig = configurations[0].pack()

        val user = (activity!!.application as MainApplication).user

        var userName = user!!.name.replace(" ", "" ).uppercase()

        if (userName.length > 4)
        {
            userName = userName.substring(0,4)
        }

        val role = user.role.toString().substring(0,2).uppercase()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
        val dateTime = LocalDateTime.now().format(formatter)

        val fileName = "C-${role}-${userName}-${dateTime!!}.json"

        val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS + "/GPSSample")
        root.mkdirs()
        val file = File(root, fileName)
        val writer = FileWriter(file)
        writer.append(packedConfig)
        writer.flush()
        writer.close()

        Toast.makeText(activity!!.applicationContext, resources.getString(R.string.config_saved_doc), Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1023 && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data

            uri?.let { uri ->

                try
                {
                    val inputStream = activity!!.getContentResolver().openInputStream(uri)

                    inputStream?.let {  inputStream ->
                        binding.overlayView.visibility = View.VISIBLE

                        Thread {
                            val text = inputStream.bufferedReader().readText()

                            val config = Config.unpack( text, encryptionPassword )

                            if (config != null)
                            {
                                DAO.instance().writableDatabase.beginTransaction()

                                val savedConfig = DAO.configDAO.createOrUpdateConfig( config )

                                DAO.instance().writableDatabase.setTransactionSuccessful()
                                DAO.instance().writableDatabase.endTransaction()

                                activity!!.runOnUiThread {
                                    binding.overlayView.visibility = View.GONE

                                    savedConfig?.let { savedConfig ->
                                        configurations = DAO.configDAO.getConfigs()
                                        sharedViewModel.setCurrentConfig( savedConfig )
                                        manageConfigurationsAdapter.updateConfigurations( configurations )

                                        didReceiveConfiguration(true)
                                    }
                                }
                            }
                            else
                            {
                                activity!!.runOnUiThread {
                                    binding.overlayView.visibility = View.GONE
                                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.start()
                    }
                }
                catch( ex: Exception )
                {
                    binding.overlayView.visibility = View.GONE
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun configurationReceived(config: Config)
    {
        runBlocking(Dispatchers.Main) {

            DAO.instance().writableDatabase.beginTransaction()

            val savedConfig = DAO.configDAO.createOrUpdateConfig(config)

            DAO.instance().writableDatabase.setTransactionSuccessful()
            DAO.instance().writableDatabase.endTransaction()

            savedConfig?.let { savedConfig ->
                configurations = DAO.configDAO.getConfigs()
                sharedViewModel.setCurrentConfig( savedConfig )
                manageConfigurationsAdapter.updateConfigurations( configurations )
            }
        }
    }

    override fun didReceiveConfiguration(complete: Boolean)
    {
        if (complete)
        {
            sharedViewModel.currentConfiguration?.value?.let { config ->

                if (user.role == Role.Enumerator.toString() && configurations.isNotEmpty() && configurations[0].selectedEnumAreaId < 0)
                {
                    binding.createButton.visibility = View.VISIBLE
                    binding.exportButton.visibility = View.VISIBLE
                }

                navigateBasedOnRole()

//                if (config.mapTileRegions.size > 0)
//                {
//                    busyIndicatorDialog = BusyIndicatorDialog( activity!!, resources.getString(R.string.downloading_map_tiles), this )
//                    MapboxManager.loadStylePack( activity!!, this )
//                }
//                else
//                {
//                    navigateBasedOnRole()
//                }
            }
        }
    }

    override fun stylePackLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.style_pack_download_failed), Toast.LENGTH_SHORT).show()
                    navigateBasedOnRole()
                }
            }
            else
            {
                sharedViewModel.currentConfiguration?.value?.let { config ->
                    MapboxManager.loadTilePacks( activity!!, config.mapTileRegions, this )
                }
            }
        }
    }

    override fun mapLoadProgress( numLoaded: Long, numNeeded: Long )
    {
        busyIndicatorDialog?.let {
            activity!!.runOnUiThread {
                it.updateProgress(resources.getString(R.string.downloading_map_tiles) + " ${numLoaded}/${numNeeded}")
            }
        }
    }

    override fun tilePacksLoaded( error: String )
    {
        activity!!.runOnUiThread {
            if (error.isNotEmpty())
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                    Toast.makeText(activity!!.applicationContext,  resources.getString(R.string.tile_pack_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                busyIndicatorDialog?.let{
                    it.alertDialog.cancel()
                }
            }

            navigateBasedOnRole()
        }
    }

    override fun didPressCancelButton()
    {
        MapboxManager.cancelStylePackDownload()
        MapboxManager.cancelTilePackDownload()
    }

    fun navigateBasedOnRole()
    {
        if (user.role == Role.Enumerator.toString())
        {
            if(configurations.size > 0)
            {
                sharedViewModel.currentConfiguration?.value?.let{ config->

                    // find the selected Enum Area
                    val enumAreas = config.enumAreas.filter {
                        it.id?.let { id ->
                            id == config.selectedEnumAreaId
                        } ?: false
                    }

                    // find the selected study
                    val studies = config.studies.filter {
                        it.id?.let { id ->
                            id == config.selectedStudyId
                        } ?: false
                    }

                    if (enumAreas.isNotEmpty() && studies.isNotEmpty())
                    {
                        val enumArea = enumAreas[0]
                        val study = studies[0]

                        // find the selected enumeration Team
                        val enumTeams = enumArea.enumerationTeams.filter { enumTeam ->
                            enumTeam.id?.let { id ->
                                id == enumArea.selectedEnumerationTeamId
                            } ?: false
                        }

                        // find the selected collection Team
                        val collectionTeams = study.collectionTeams.filter { collectionTeam ->
                            collectionTeam.id?.let { id ->
                                id == study.selectedCollectionTeamId
                            } ?: false
                        }

                        if (collectionTeams.isNotEmpty())
                        {
                            val collectionTeam = collectionTeams[0]

                            // find the selected Enum Area
                            val enumAreas = config.enumAreas.filter {
                                it.id?.let { id ->
                                    id == collectionTeam.enumAreaId
                                } ?: false
                            }

                            val enumArea = enumAreas[0]

                            sharedViewModel.createStudyModel.setStudy( study )
                            sharedViewModel.teamViewModel.setCurrentCollectionTeam( collectionTeam )
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                            samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy
                            findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
                        }
                        else if (enumTeams.isNotEmpty())
                        {
                            val enumTeam = enumTeams[0]

                            sharedViewModel.createStudyModel.setStudy( study )
                            sharedViewModel.teamViewModel.setCurrentEnumerationTeam( enumTeam )
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
                        }
                    }
                }
            }
        }
        else if (user.role == Role.DataCollector.toString())
        {
            if(configurations.size > 0)
            {
                sharedViewModel.currentConfiguration?.value?.let{ config->

                    // find the selected study
                    val studies = config.studies.filter {
                        it.id?.let { id ->
                            id == config.selectedStudyId
                        } ?: false
                    }

                    if (studies.isNotEmpty())
                    {
                        val study = studies[0]

                        // find the selected collection Team
                        val collectionTeams = study.collectionTeams.filter { collectionTeam ->
                            collectionTeam.id?.let { id ->
                                id == study.selectedCollectionTeamId
                            } ?: false
                        }

                        if (collectionTeams.isNotEmpty())
                        {
                            val collectionTeam = collectionTeams[0]

                            // find the selected Enum Area
                            val enumAreas = config.enumAreas.filter {
                                it.id?.let { id ->
                                    id == collectionTeam.enumAreaId
                                } ?: false
                            }

                            val enumArea = enumAreas[0]

                            sharedViewModel.createStudyModel.setStudy( study )
                            sharedViewModel.teamViewModel.setCurrentCollectionTeam( collectionTeam )
                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( enumArea )
                            samplingViewModel.currentStudy = sharedViewModel.createStudyModel.currentStudy

                            findNavController().navigate(R.id.action_navigate_to_PerformCollectionFragment)
                        }
                    }
                }
            }
        }
    }

    override fun didEnterText( name: String, tag: Any? )
    {
        encryptionPassword = name

        if ((user.role == Role.Enumerator.toString() || user.role == Role.DataCollector.toString()) && (configurations.size > 0))
        {
            ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.delete_configuration), resources.getString(R.string.no), resources.getString(R.string.yes), kDeleteTag, this)
        }
        else
        {
            ConfirmationDialog( activity, resources.getString(R.string.import_configuration), resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kImportTag, this)
        }
    }

    override fun didCancelText( tag: Any? )
    {
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}