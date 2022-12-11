package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.StudiesAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentManageStudiesBinding
import edu.gtri.gpssample.models.StudyModel

class ManageStudiesFragment : Fragment()
{
    private var _binding: FragmentManageStudiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var studiesAdapter: StudiesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentManageStudiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        studiesAdapter = StudiesAdapter((activity!!.application as MainApplication).studies)
        studiesAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = studiesAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity )

        binding.createButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigate_to_CreateStudyFragment)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if ((activity!!.application as MainApplication).studies.isEmpty())
        {
            binding.recyclerView.visibility = View.GONE
            binding.relativeLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.recyclerView.visibility = View.VISIBLE
            binding.relativeLayout.visibility = View.GONE
        }

        studiesAdapter.updateStudies((activity!!.application as MainApplication).studies)
    }

    fun onItemSelected(studyModel: StudyModel, shouldDismissKeyboard: Boolean )
    {
        findNavController().navigate( R.id.action_navigate_to_StudyFragment )
    }

}