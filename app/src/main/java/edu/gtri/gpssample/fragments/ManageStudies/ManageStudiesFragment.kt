package edu.gtri.gpssample.fragments.ManageStudies

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentManageStudiesBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study

class ManageStudiesFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private lateinit var config: Config
    private var _binding: FragmentManageStudiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var manageStudiesAdapter: ManageStudiesAdapter
    private lateinit var viewModel: ManageStudiesViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageStudiesViewModel::class.java)
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

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val config_uuid = arguments!!.getString( Key.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.configDAO.getConfig( config_uuid )?.let {
            config = it
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Configuration with id: $config_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.selectedItemCallback = this::onItemSelected

        binding.configNameTextView.text = "Configuration " + config.name + " Studies"
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageStudiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString( Key.kConfig_uuid.toString(), config.uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }

        binding.createButton.setOnClickListener {

            val bundle = Bundle()
            bundle.putString( Key.kConfig_uuid.toString(), config.uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val studies = DAO.studyDAO.getStudies( config.uuid )

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

    fun onItemSelected(study: Study)
    {
        val bundle = Bundle()

        bundle.putString( Key.kConfig_uuid.toString(), config.uuid )
        bundle.putString( Key.kStudy_uuid.toString(), study.uuid )

        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
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

                bundle.putString( Key.kConfig_uuid.toString(), config.uuid )

                findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment, bundle)
            }
            R.id.action_delete_configuration -> {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this configuration?", 0, this)
                return true
            }
        }

        return false
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Int )
    {
        DAO.configDAO.deleteConfig( config )
        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}