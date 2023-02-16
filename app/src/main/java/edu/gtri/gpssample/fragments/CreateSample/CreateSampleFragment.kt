package edu.gtri.gpssample.fragments.CreateSample

import android.os.Bundle
import android.util.Log
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
import edu.gtri.gpssample.database.models.NavPlan
import edu.gtri.gpssample.database.models.Sample
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentCreateSampleBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.fragments.ManageStudies.ManageSamplesAdapter
import java.util.*
import kotlin.collections.ArrayList

class CreateSampleFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentCreateSampleBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var sample: Sample
    private lateinit var viewModel: CreateSampleViewModel
    private lateinit var createSampleAdapter: CreateSampleAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateSampleViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateSampleBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        // required: study_uuid
        val study_uuid = arguments!!.getString( Key.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty())
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        DAO.studyDAO.getStudy( study_uuid )?.let { study ->
            this.study = study
        }

        if (!this::study.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Study with id $study_uuid not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // optional: sample_uuid
        val sample_uuid = arguments!!.getString( Key.kSample_uuid.toString(), "");

        if (sample_uuid.isNotEmpty())
        {
            DAO.sampleDAO.getSample( sample_uuid )?.let { sample ->
                this.sample = sample
            }

            if (!this::sample.isInitialized)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal! Sample with id $sample_uuid not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!this::sample.isInitialized)
        {
            sample = Sample( UUID.randomUUID().toString(), study_uuid, "", 0 )
        }
        else
        {
            setHasOptionsMenu( true )
            binding.nameEditText.setText( sample.name )
            binding.numEnumsEditText.setText( sample.numEnumerators.toString())
        }

        createSampleAdapter = CreateSampleAdapter(listOf<NavPlan>())
        createSampleAdapter.didSelectNavPlan = this::didSelectNavPlan

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createSampleAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.generateButton.setOnClickListener {

            if (binding.nameEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.numEnumsEditText.text.toString().length == 0)
            {
                Toast.makeText(activity!!.applicationContext, "Please enter the number of enumerators.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sample.numEnumerators = binding.numEnumsEditText.text.toString().toInt()

            if (sample.numEnumerators > study.sampleSize)
            {
                Toast.makeText(activity!!.applicationContext, "The number of enumerators specified exceeds the sample size for this study.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val navPlans = ArrayList<NavPlan>()

            for (i in 1..sample.numEnumerators)
            {
                val navPlan = NavPlan( UUID.randomUUID().toString(), sample.uuid, "Navigation Plan ${i}" )
                DAO.navPlanDAO.createNavPlan( navPlan )
                navPlans.add( navPlan )
            }

            createSampleAdapter.updateNavPlans( navPlans )
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            if (createSampleAdapter.navPlans!!.isNotEmpty())
            {
                if (DAO.sampleDAO.doesNotExist( sample.uuid ))
                {
                    DAO.sampleDAO.createSample( sample )
                }

                sample.name = binding.nameEditText.text.toString()

                DAO.sampleDAO.updateSample( sample )

                findNavController().popBackStack()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        val navPlans = DAO.navPlanDAO.getNavPlans( sample.uuid )

        createSampleAdapter.updateNavPlans(navPlans)
    }

    fun didSelectNavPlan( navPlan: NavPlan )
    {
        Log.d( "xxx", navPlan.name )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_sample, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_delete_sample -> {
                ConfirmationDialog( activity, "Please Confirm", "Are you sure you want to permanently delete this sample?", 0, this)
            }
        }

        return false
    }

    override fun didAnswerNo()
    {
    }

    override fun didAnswerYes( tag: Int )
    {
        DAO.sampleDAO.deleteSample( sample )
        findNavController().popBackStack()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}