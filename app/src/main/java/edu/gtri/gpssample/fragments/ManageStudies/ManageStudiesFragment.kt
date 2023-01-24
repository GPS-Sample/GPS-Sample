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
    private var configuration: Configuration? = null;
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

        val studies = GPSSampleDAO.sharedInstance().getStudies()

        manageStudiesAdapter = ManageStudiesAdapter(studies)
        manageStudiesAdapter.selectedItemCallback = this::onItemSelected

        val configId = getArguments()?.getInt( Key.kConfigId.toString());

        if (configId == null)
        {
            Toast.makeText(activity!!.applicationContext, "Oops! Config ID is NULL", Toast.LENGTH_SHORT).show()
            return
        }

        configuration = GPSSampleDAO.sharedInstance().getConfiguration( configId!! )

        if (configuration == null)
        {
            Toast.makeText(activity!!.applicationContext, "Oops! Missing Configuration with ID: " + configId.toString(), Toast.LENGTH_SHORT).show()
            return
        }

        binding.configNameTextView.text = "Configuration " + configuration!!.name + " Studies"
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageStudiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.createButton.setOnClickListener {

            var bundle = Bundle()

            bundle.putInt( Key.kConfigId.toString(), configuration!!.id )

            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment, bundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val studies = GPSSampleDAO.sharedInstance().getStudies()

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
        var bundle = Bundle()

        bundle.putInt( Key.kStudyId.toString(), study.id )

        findNavController().navigate( R.id.action_navigate_to_StudyFragment, bundle )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_study, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_configuration -> {
                var bundle = Bundle()

                bundle.putInt( Key.kConfigId.toString(), configuration!!.id )

                findNavController().navigate(R.id.action_navigate_to_CreateConfigurationFragment, bundle)
            }
            R.id.action_create_study -> {
                var bundle = Bundle()

                bundle.putInt( Key.kConfigId.toString(), configuration!!.id )

                findNavController().navigate( R.id.action_navigate_to_CreateStudyFragment, bundle )
                return true
            }
            R.id.action_delete_configuration -> {
                GPSSampleDAO.sharedInstance().deleteConfiguration( configuration!! )
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