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
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.databinding.FragmentManageConfigurationsBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class ManageConfigurationsFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate,
        NetworkClientModel.ConfigurationDelegate,
        NetworkViewModel.ManageConfigurationNetworkDelegate
{
    private var _binding: FragmentManageConfigurationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: User
    private lateinit var manageConfigurationsAdapter: ManageConfigurationsAdapter
    private lateinit var sharedViewModel: ConfigurationViewModel
    private lateinit var sharedNetworkViewModel: NetworkViewModel
    private lateinit var samplingViewModel: SamplingViewModel

    private val kImportTag = 1
    private val kDeleteTag = 2

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageConfigurationsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        manageConfigurationsAdapter = ManageConfigurationsAdapter(listOf<Config>())
        manageConfigurationsAdapter.didSelectConfig = this::didSelectConfig

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageConfigurationsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        val _user = (activity!!.application as MainApplication).user

        _user?.let { user ->
            this.user = user

            if (user.role != Role.Admin.toString())
            {
                binding.addButton.visibility = View.GONE
            }
        }

        binding.addButton.setOnClickListener {
            sharedViewModel.createNewConfiguration()
            findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment)
        }

        binding.importButton.setOnClickListener {

            if ((user.role == Role.Enumerator.toString() || user.role == Role.DataCollector.toString())
                && (sharedViewModel.configurations.size > 0))
            {
                ConfirmationDialog( activity, resources.getString(R.string.import_configuration),
                    resources.getString(R.string.delete_configuration),
                    resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
            }
            else
            {
                ConfirmationDialog( activity, resources.getString(R.string.import_configuration),
                    resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code), resources.getString(R.string.file_system), kImportTag, this)
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageConfigurationsFragment.value.toString() + ": " + this.javaClass.simpleName

        // get this from the view controller
        manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
    }

    private fun didSelectConfig( config: Config )
    {
        val bundle = Bundle()
        sharedViewModel.setCurrentConfig(config)

        when (user.role)
        {
            Role.Enumerator.toString() ->
            {
                navigateToEnumeration()
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
        if (sharedViewModel.configurations.size > 0)
        {
            val config = sharedViewModel.configurations[0]
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
                val enumArea = enumAreas[0]
                val study = studies[0]

                // find the selected enumeration Team
                val enumTeams = study.enumerationTeams.filter {
                    it.id?.let { id ->
                        id == study.selectedEnumerationTeamId
                    } ?: false
                }

                if (enumTeams.isNotEmpty())
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

    fun navigateToCollection()
    {
        if(sharedViewModel.configurations.size > 0)
        {
            val config = sharedViewModel.configurations[0]
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
                val sampleArea = study.sampleAreas[0]

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
                }
            }
        }
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

//                findNavController().navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
                // need to pass this into the network view model
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun didSelectLeftButton(tag: Any?)
    {
        if (tag == kImportTag)
        {
            sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.Configuration)
            val intent = Intent(context, CameraXLivePreviewActivity::class.java)
            getResult.launch(intent)
        }
    }

    override fun didSelectRightButton(tag: Any?)
    {
        if (tag == kDeleteTag)
        {
            for (config in sharedViewModel.configurations)
            {
                sharedViewModel.deleteConfig( config )
                manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
            }

            ConfirmationDialog( activity, resources.getString(R.string.import_configuration),
                resources.getString(R.string.select_import_method), resources.getString(R.string.qr_code),
                resources.getString(R.string.file_system), kImportTag, this)
        }
        else
        {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_configuration)), 1023)
        }
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
                        val text = inputStream.bufferedReader().readText()
                        val config = Config.unpack( text )
                        config?.let { config ->

                            DAO.configDAO.createConfig( config )

                            sharedViewModel.configurations.add(config)
                            sharedViewModel.setCurrentConfig( config )

                            manageConfigurationsAdapter.updateConfigurations( sharedViewModel.configurations )

                            didReceiveConfiguration(true )
                        } ?: Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                catch( ex: Exception )
                {
                    Toast.makeText(activity!!.applicationContext, resources.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }

    override fun configurationReceived(config: Config) {
        runBlocking(Dispatchers.Main) {

            val saved = DAO.configDAO.createConfig(config)
            saved?.let { config ->
                sharedViewModel.configurations.add(config)
                sharedViewModel.setCurrentConfig(config)
                manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
            }
        }
    }

    override fun didReceiveConfiguration(complete: Boolean)
    {
        if(complete)
        {
            if (user.role == Role.Enumerator.toString())
            {
                if(sharedViewModel.configurations.size > 0)
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
                            val enumTeams = study.enumerationTeams.filter { enumTeam ->
                                enumTeam.id?.let { id ->
                                    id == study.selectedEnumerationTeamId
                                } ?: false
                            }

                            if (enumTeams.isNotEmpty())
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
                if(sharedViewModel.configurations.size > 0)
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
                            val study = studies[0]
                            val enumArea = enumAreas[0]
                            val sampleArea = study.sampleAreas[0]

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
                            }
                        }
                    }
                }
            }
        }
    }
}