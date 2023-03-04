package edu.gtri.gpssample.fragments.ManageSamples

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
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Sample
import edu.gtri.gpssample.databinding.FragmentManageSamplesBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.ManageStudies.ManageSamplesAdapter

class ManageSamplesFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentManageSamplesBinding? = null
    private val binding get() = _binding!!

    private lateinit var study_uuid: String
    private lateinit var viewModel: ManageSamplesViewModel
    private lateinit var manageSamplesAdapter: ManageSamplesAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageSamplesViewModel::class.java)
        (activity!!.application as? MainApplication)?.currentFragment = this.javaClass.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageSamplesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        study_uuid = arguments!!.getString( Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        manageSamplesAdapter = ManageSamplesAdapter(listOf<Sample>())
        manageSamplesAdapter.didSelectSample = this::didSelectSample
        manageSamplesAdapter.shouldDeleteSample = this::shouldDeleteSample

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageSamplesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.addButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateSampleFragment, bundle)
        }

        binding.createButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
            findNavController().navigate(R.id.action_navigate_to_CreateSampleFragment, bundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val samples = DAO.sampleDAO.getSamples( study_uuid )

        if (samples.isEmpty())
        {
            binding.recyclerView.visibility = View.GONE
            binding.relativeLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.recyclerView.visibility = View.VISIBLE
            binding.relativeLayout.visibility = View.GONE
        }

        manageSamplesAdapter.updateSamples(samples)
    }

    fun didSelectSample(sample: Sample)
    {
        val bundle = Bundle()
        bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
        bundle.putString( Keys.kSample_uuid.toString(), sample.uuid )

        findNavController().navigate(R.id.action_navigate_to_CreateSampleFragment, bundle)
    }

    private var selectedSample: Sample? = null

    fun shouldDeleteSample(sample: Sample)
    {
        selectedSample = sample
        ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this sample?", 0, this)
    }

    override fun didAnswerNo() {
    }

    override fun didAnswerYes( tag: Int )
    {
        selectedSample?.let {
            DAO.sampleDAO.deleteSample( it )
            manageSamplesAdapter.updateSamples(DAO.sampleDAO.getSamples())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_manage_samples, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
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