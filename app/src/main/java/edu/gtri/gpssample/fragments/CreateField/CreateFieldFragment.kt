package edu.gtri.gpssample.fragments.CreateField

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentCreateFieldBinding
import edu.gtri.gpssample.models.FieldModel

class CreateFieldFragment : Fragment()
{
    private var _binding: FragmentCreateFieldBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CreateFieldViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateFieldViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentCreateFieldBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, "CreateFieldFragment", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelButton.setOnClickListener {

            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            if (binding.fieldNameEditText.text.toString().length > 0)
            {
                val fieldModel = FieldModel()
                fieldModel.name = binding.fieldNameEditText.text.toString()
                (activity!!.application as MainApplication).fields.add( fieldModel )

                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}