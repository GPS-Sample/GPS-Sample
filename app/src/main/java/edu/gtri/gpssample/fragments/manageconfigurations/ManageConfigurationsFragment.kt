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
import edu.gtri.gpssample.viewmodels.models.NetworkClientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.InputStream

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

    private var selectedConfig: Config? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        val networkVm : NetworkViewModel by activityViewModels()

        sharedNetworkViewModel = networkVm
        sharedNetworkViewModel.currentFragment = this
        sharedNetworkViewModel.networkClientModel.configurationDelegate = this
        sharedNetworkViewModel.manageConfigurationNetworkDelegate = this

        sharedViewModel = vm
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

            if (user.role == Role.Enumerator.toString())
            {
                for (config in sharedViewModel.configurations)
                {
                    sharedViewModel.deleteConfig( config )
                    manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
                }
            }

            ConfirmationDialog( activity, "Import Configuration", "Select an import method", "QR Code", "File System", 0, this)
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
        findNavController().navigate(R.id.action_navigate_to_ConfigurationFragment, bundle)
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
        // set what client mode we are
        sharedNetworkViewModel.networkClientModel.setClientMode(ClientMode.Configuration)
        val intent = Intent(context, CameraXLivePreviewActivity::class.java)
        getResult.launch(intent)

        // FAKE
       // sharedNetworkViewModel.connectHotspotFake()

        //findNavController().navigate(R.id.action_navigate_to_NetworkConnectionDialogFragment)
    }

    override fun didSelectRightButton(tag: Any?)
    {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(Intent.createChooser(intent, "Select a configuration"), 1023)
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

                        Log.d( "xxx FROMCONFIG", text )

                        val config = Config.unpack( text )
                        var count = 0
                        config?.let { config ->

                            for(enumAreaa in config.enumAreas)
                            {
                                for(location in enumAreaa.locations)
                                {
                                    for(enumItem in location.enumerationItems)
                                    {
                                        for(fieldData in enumItem.fieldDataList)
                                        {
                                            count += 1
                                            Log.d("XXXXXX", "fieldData id ${fieldData.id} name ${fieldData.name} type ${fieldData.type} ${fieldData.numberValue}")
                                        }
                                    }
                                }
                            }
                            for(study in config.studies)
                            {
                                for(enumAreaa in study.sampleAreas)
                                {
                                    for(location in enumAreaa.locations)
                                    {
                                        for(enumItem in location.enumerationItems)
                                        {
                                            for(fieldData in enumItem.fieldDataList)
                                            {
                                                count += 1
                                                Log.d("XXXXXX", "fieldData id ${fieldData.id} name ${fieldData.name} type ${fieldData.type} ${fieldData.numberValue}")
                                            }
                                        }
                                    }
                                }
                            }

                            Log.d("XXXXX", "THE COUNT $count")
                            // HACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACK
                            // HACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACK
                            // HACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACKHACK
                            DAO.deleteAll()

                            DAO.configDAO.createConfig( config )

                            sharedViewModel.initializeConfigurations()

                            manageConfigurationsAdapter.updateConfigurations( sharedViewModel.configurations )

                            if (user.role == Role.Enumerator.toString())
                            {
                                sharedViewModel.setCurrentConfig(config)
                                val team = DAO.teamDAO.getTeam( config.teamId )
                                team?.let { _team ->
                                    sharedViewModel.teamViewModel.setCurrentTeam( _team )
                                    val study = DAO.studyDAO.getStudy( _team.studyId )
                                    study?.let { _study ->
                                        sharedViewModel.createStudyModel.setStudy( _study )
                                        val enumArea = DAO.enumAreaDAO.getEnumArea( _team.enumAreaId )
                                        enumArea?.let { _enumArea ->
                                            sharedViewModel.enumAreaViewModel.setCurrentEnumArea( _enumArea )
                                            findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
                                        }
                                    }
                                }
                            }
                        } ?: Toast.makeText(activity!!.applicationContext, "2Oops! The import failed.  Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                catch( ex: Exception )
                {
                    Toast.makeText(activity!!.applicationContext, "Oops! The import failed.  Please try again.", Toast.LENGTH_SHORT).show()
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
                manageConfigurationsAdapter.updateConfigurations(sharedViewModel.configurations)
            }


        }
    }

    override fun didReceiveConfiguration(complete: Boolean) {
        if(complete)
        {
            if (user.role == Role.Enumerator.toString())
            {
                Log.d("xxx", "WTF")
                // runBlocking (Dispatchers.Main){
                if(sharedViewModel.configurations.size > 0)
                {
                    val config = sharedViewModel.configurations[0]
                    sharedViewModel.setCurrentConfig(config)
                    val team = DAO.teamDAO.getTeam( config.teamId )
                    team?.let { _team ->
                        sharedViewModel.teamViewModel.setCurrentTeam( _team )
                        val study = DAO.studyDAO.getStudy( _team.studyId )
                        study?.let { _study ->
                            sharedViewModel.createStudyModel.setStudy( _study )
                            val enumArea = DAO.enumAreaDAO.getEnumArea( _team.enumAreaId )
                            enumArea?.let { _enumArea ->
                                sharedViewModel.enumAreaViewModel.setCurrentEnumArea( _enumArea )
                                findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment)
                            }
                        }
                    }
                }
            }
        }
    }
}