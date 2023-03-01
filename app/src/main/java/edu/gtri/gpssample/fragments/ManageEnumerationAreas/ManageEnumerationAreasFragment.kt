package edu.gtri.gpssample.fragments.ManageEnumerationAreas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentManageEnumerationAreasBinding
import edu.gtri.gpssample.fragments.ManageSamples.ManageSamplesViewModel

class ManageEnumerationAreasFragment : Fragment()
{
    private var _binding: FragmentManageEnumerationAreasBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var viewModel: ManageSamplesViewModel
    private lateinit var manageSupervisorsAdapter: ManageEnumerationAreasAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageSamplesViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageEnumerationAreasBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG)
            {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        if (arguments == null)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: configId.",Toast.LENGTH_SHORT).show()
            return
        }

        val study_uuid = arguments!!.getString(Keys.kStudy_uuid.toString(), "");

        if (study_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
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

        val enumAreas = DAO.enumAreaDAO.getEnumAreas( study.config_uuid )

        manageSupervisorsAdapter = ManageEnumerationAreasAdapter( enumAreas )
        manageSupervisorsAdapter.didSelectEnumArea = this::didSelectEnumArea

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = manageSupervisorsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.finishButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
        }
    }

    fun didSelectEnumArea(enumArea: EnumArea)
    {
        val bundle = Bundle()
        bundle.putString( Keys.kStudy_uuid.toString(), study.uuid )
        bundle.putString( Keys.kEnumArea_uuid.toString(), enumArea.uuid )
        findNavController().navigate( R.id.action_navigate_to_ManageEnumerationAreaFragment, bundle )
    }
}