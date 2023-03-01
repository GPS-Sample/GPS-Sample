package edu.gtri.gpssample.fragments.ManageSupervisors

import android.os.Bundle
import android.util.Log
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
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.databinding.FragmentManageSupervisorsBinding
import edu.gtri.gpssample.fragments.ManageSamples.ManageSamplesViewModel

class ManageSupervisorsFragment : Fragment()
{
    private var _binding: FragmentManageSupervisorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ManageSamplesViewModel
    private lateinit var manageSupervisorsAdapter: ManageSupervisorsAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageSamplesViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentManageSupervisorsBinding.inflate(inflater, container, false)

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

        val config_uuid = arguments!!.getString(Key.kConfig_uuid.toString(), "");

        if (config_uuid.isEmpty()) {
            Toast.makeText( activity!!.applicationContext, "Fatal! Missing required parameter: studyId.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d( "xxx", config_uuid )
        val enumAreas = DAO.enumAreaDAO.getEnumAreas( config_uuid )

        manageSupervisorsAdapter = ManageSupervisorsAdapter( enumAreas )
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
//        val bundle = Bundle()
//        bundle.putString( Key.kStudy_uuid.toString(), study_uuid )
//        bundle.putString( Key.kSample_uuid.toString(), sample.uuid )
//
//        findNavController().navigate(R.id.action_navigate_to_CreateSampleFragment, bundle)
    }
}