package edu.gtri.gpssample.fragments.CreateStudy

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
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.database.GPSSampleDAO
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.models.Field
import edu.gtri.gpssample.models.Study

class CreateStudyFragment : Fragment()
{
    private var study: Study? = null;
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!
    private lateinit var createStudyAdapter: CreateStudyAdapter
    private lateinit var viewModel: CreateStudyViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateStudyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        // required: configId
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

        // optional: studyId
        val studyId = arguments!!.getInt( Key.kStudyId.toString(), -1);

        if (studyId > 0)
        {
            study = GPSSampleDAO.sharedInstance().getStudy( studyId!! )
            if (study == null)
            {
                Toast.makeText(activity!!.applicationContext, "Fatal Study with id: $studyId not found.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (study == null)
        {
            study = Study()
            study!!.isValid = false
            study!!.configId = configId!!
        }

        if (study!!.isValid)
        {
            binding.titleTextView.text = "Study ${study!!.name}"
        }

        binding.studyNameEditText.setText( study!!.name )

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "CreateStudyFragment", Toast.LENGTH_SHORT).show()
            }
        }

        createStudyAdapter = CreateStudyAdapter(listOf<Field>())
        createStudyAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = createStudyAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager( activity )

        binding.nextButton.setOnClickListener {

            if (binding.studyNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            study!!.isValid = true
            study!!.name = binding.studyNameEditText.text.toString()

            if (study!!.id < 0)
            {
                study!!.id = GPSSampleDAO.sharedInstance().createStudy( study!! )
            }
            else
            {
                GPSSampleDAO.sharedInstance().updateStudy( study!! )
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (study != null)
        {
            val fields = GPSSampleDAO.sharedInstance().getFields(study!!.id)

            createStudyAdapter.updateFields( fields )
        }
    }

    fun onItemSelected(fieldModel: Field, shouldDismissKeyboard: Boolean )
    {
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_create_field, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_create_field -> {

                if (study == null)
                {
                    study = Study()
                    study!!.isValid = false
                    study!!.id = GPSSampleDAO.sharedInstance().createStudy( study!! )
                }

                val bundle = Bundle()

                bundle.putInt( Key.kStudyId.toString(), study!!.id )

                findNavController().navigate( R.id.action_navigate_to_CreateFieldFragment, bundle )
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