package edu.gtri.gpssample.fragments.ManageStudies

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentManageStudiesBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.fragments.createconfiguration.ManageStudiesAdapter
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class ManageStudiesFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var config: Config
    private var _binding: FragmentManageStudiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var manageStudiesAdapter: ManageStudiesAdapter

    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm

        Log.d("BIG TEST ", "THE CONFIG ${sharedViewModel.currentConfiguration!!.value!!.name}")

        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.didSelectStudy = this::didSelectStudy
        manageStudiesAdapter.shouldDeleteStudy = this::shouldDeleteStudy
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageStudiesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }
        val test = arguments!!.getSerializable("currentConfig")
        test?.let {
            val config = it as Config
            Log.d("TEST", "config name ${config.name}")
        }

        val config_uuid = arguments!!.getString( Keys.kConfig_uuid.toString(), "");

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }?: {
            Toast.makeText(
                activity!!.applicationContext,
                "Fatal! Live data binding is null.",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Configuration with id: $config_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }
//
//        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
//        manageStudiesAdapter.didSelectStudy = this::didSelectStudy
//        manageStudiesAdapter.shouldDeleteStudy = this::shouldDeleteStudy

        binding.configNameTextView.text = "Configuration " + config.name + " Studies"
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageStudiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }

        binding.createButton.setOnClickListener {

            val bundle = Bundle()
            bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.ManageStudiesFragment.value.toString() + ": " + this.javaClass.simpleName

        sharedViewModel.currentConfiguration?.value?.let {curConfig ->

            val studies = curConfig.studies//DAO.studyDAO.getStudies( config.uuid )

            if (studies.isEmpty())
            {
                binding.recyclerView.visibility = View.GONE
                binding.relativeLayout.visibility = View.VISIBLE
            }
            else
            {
                binding.recyclerView.visibility = View.VISIBLE
                binding.relativeLayout.visibility = View.GONE
            }

            manageStudiesAdapter.updateStudies(studies)
        }

    }

    fun didSelectStudy(study: Study)
    {
        val bundle = Bundle()

        bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
    }

    private var selectedStudy: Study? = null

    fun shouldDeleteStudy(study: Study)
    {
        selectedStudy = study
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this study?", 0, this)
    }

    override fun didAnswerNo() {
    }

    override fun didAnswerYes( tag: Int )
    {
        selectedStudy?.let {
            DAO.studyDAO.deleteStudy( it )
            //manageStudiesAdapter.updateStudies(DAO.studyDAO.getStudies())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_studies, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_configuration -> {
                val bundle = Bundle()

                bundle.putString( Keys.kConfig_uuid.toString(), config.uuid )

                findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment, bundle)
            }

            R.id.action_home -> {
                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
            }
        }

        return false
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}