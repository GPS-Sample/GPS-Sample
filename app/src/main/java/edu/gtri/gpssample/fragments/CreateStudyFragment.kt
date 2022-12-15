package edu.gtri.gpssample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.databinding.FragmentCreateStudyBinding
import edu.gtri.gpssample.models.StudyModel

class CreateStudyFragment : Fragment()
{
    private var _binding: FragmentCreateStudyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateStudyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentRootLayout.setOnClickListener {
            Toast.makeText(activity!!.applicationContext, "CreateStudyFragment", Toast.LENGTH_SHORT).show()
        }

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
}