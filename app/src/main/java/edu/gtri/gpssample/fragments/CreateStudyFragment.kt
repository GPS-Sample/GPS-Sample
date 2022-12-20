package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.FieldsAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.models.FieldModel
import edu.gtri.gpssample.models.StudyModel

class CreateStudyFragment : Fragment()
{
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var fieldsAdapter: FieldsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "CreateStudyFragment", Toast.LENGTH_SHORT).show()
            }
        }

        fieldsAdapter = FieldsAdapter((activity!!.application as MainApplication).fields)
        fieldsAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = fieldsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager( activity )

        binding.backButton.setOnClickListener {

            findNavController().popBackStack()
        }

        binding.nextButton.setOnClickListener {

            val studyModel = StudyModel()
            studyModel.name = binding.studyNameEditText.text.toString();
            (activity!!.application as MainApplication).studies.add( studyModel )

            findNavController().popBackStack()
        }
    }

    fun onItemSelected(fieldModel: FieldModel, shouldDismissKeyboard: Boolean )
    {
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_field, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_create_field -> {
                findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment )
            }
        }

        return true
    }
}