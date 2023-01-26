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
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.GPSSampleDAO
import edu.gtri.gpssample.databinding.FragmentManageStudiesBinding
import edu.gtri.gpssample.models.Configuration
import edu.gtri.gpssample.models.Study

class ManageStudiesFragment : Fragment()
{
    private var config: Configuration? = null;
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
                Toast.makeText(activity!!.applicationContext, "ManageStudiesFragment", Toast.LENGTH_SHORT).show()
            }
        }

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        val configId = arguments!!.getInt( Key.kConfigId.toString(), -1);

        if (configId < 0)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.", Toast.LENGTH_SHORT).show()
            return
        }

        config = GPSSampleDAO.sharedInstance().getConfiguration( configId )
        if (config == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Configuration with id: $configId not found.", Toast.LENGTH_SHORT).show()
            return
        }

        manageStudiesAdapter = ManageStudiesAdapter(listOf<Study>())
        manageStudiesAdapter.selectedItemCallback = this::onItemSelected

        binding.configNameTextView.text = "Configuration " + config!!.name + " Studies"
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageStudiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.createButton.setOnClickListener {

            val bundle = Bundle()

            bundle.putInt( Key.kConfigId.toString(), config!!.id )

            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val studies = GPSSampleDAO.sharedInstance().getStudies( config!!.id )

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

    fun onItemSelected(study: Study, shouldDismissKeyboard: Boolean )
    {
        val bundle = Bundle()

        bundle.putInt( Key.kConfigId.toString(), config!!.id )
        bundle.putInt( Key.kStudyId.toString(), study.id )

        findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_study, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_configuration -> {
                val bundle = Bundle()

                bundle.putInt( Key.kConfigId.toString(), config!!.id )

                findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment, bundle)
            }
            R.id.action_create_study -> {
                val bundle = Bundle()

                bundle.putInt( Key.kConfigId.toString(), config!!.id )

                findNavController().navigate( R.id.action_navigate_to_CreateStudyFragment, bundle )
                return true
            }
            R.id.action_delete_configuration -> {
                GPSSampleDAO.sharedInstance().deleteConfiguration( config!! )
                findNavController().popBackStack()
                return true
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