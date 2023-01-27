package edu.gtri.gpssample.fragments.CreateConfiguration

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.databinding.FragmentCreateConfigurationBinding
import edu.gtri.gpssample.database.models.Config

class CreateConfigurationFragment : Fragment()
{
    private var config: Config? = null;
    private var _binding: FragmentCreateConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CreateConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateConfigurationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCreateConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        val configId = getArguments()?.getInt( Key.kConfigId.toString());

        if (configId != null)
        {
            config = DAO.configDAO.getConfig( configId!! )
        }

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        binding.minGpsPrecisionEditText.setInputType(InputType.TYPE_CLASS_NUMBER)

        ArrayAdapter.createFromResource(activity!!, R.array.distance_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.distanceFormatSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(activity!!, R.array.date_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.dateFormatSpinner.adapter = adapter
            }

        ArrayAdapter.createFromResource(activity!!, R.array.time_format, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.timeFormatSpinner.adapter = adapter
            }

        if (config != null)
        {
            binding.nextButton.text = "SAVE"
            binding.titleTextView.setText( "Edit Configuration" )
            binding.configNameEditText.setText( config!!.name )
            binding.minGpsPrecisionEditText.setText( config!!.minGpsPrecision.toString())

            when (config!!.distanceFormat)
            {
                DistanceFormat.Meters -> binding.distanceFormatSpinner.setSelection( 0 )
                DistanceFormat.Feet -> binding.distanceFormatSpinner.setSelection( 1 )
            }

            when (config!!.dateFormat)
            {
                DateFormat.DayMonthYear -> binding.dateFormatSpinner.setSelection( 0 )
                DateFormat.MonthDayYear -> binding.dateFormatSpinner.setSelection( 1 )
                DateFormat.YearMonthDay -> binding.dateFormatSpinner.setSelection( 2 )
            }

            when (config!!.timeFormat)
            {
                TimeFormat.twelveHour -> binding.timeFormatSpinner.setSelection( 0 )
                TimeFormat.twentyFourHour -> binding.timeFormatSpinner.setSelection( 1 )
            }
        }

        binding.nextButton.setOnClickListener {

            if (binding.configNameEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.minGpsPrecisionEditText.text.toString().isEmpty())
            {
                Toast.makeText(activity!!.applicationContext, "Please enter the minimum desired GPS precision", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeFormats by lazy { resources.getStringArray(R.array.time_format) }
            val dateFormats by lazy { resources.getStringArray(R.array.date_format) }
            val distFormats by lazy { resources.getStringArray(R.array.distance_format) }

            if (config == null)
            {
                config = Config()
            }

            config!!.name = binding.configNameEditText.text.toString()
            config!!.minGpsPrecision = binding.minGpsPrecisionEditText.text.toString().toInt()

            var selectedItem = binding.distanceFormatSpinner.selectedItem as String

            when (selectedItem)
            {
                distFormats[0] -> config!!.distanceFormat = DistanceFormat.Meters;
                distFormats[1] -> config!!.distanceFormat = DistanceFormat.Feet;
            }

            selectedItem = binding.dateFormatSpinner.selectedItem as String

            when (selectedItem)
            {
                dateFormats[0] -> config!!.dateFormat = DateFormat.DayMonthYear;
                dateFormats[1] -> config!!.dateFormat = DateFormat.MonthDayYear;
                dateFormats[2] -> config!!.dateFormat = DateFormat.YearMonthDay;
            }

            selectedItem = binding.timeFormatSpinner.selectedItem as String

            when (selectedItem)
            {
                timeFormats[0] -> config!!.timeFormat = TimeFormat.twelveHour;
                timeFormats[1] -> config!!.timeFormat = TimeFormat.twentyFourHour;
            }

            if (config!!.id < 0)
            {
                config!!.id = DAO.configDAO.createConfig( config!! )
                findNavController().navigate(R.id.action_navigate_to_DefineEnumerationAreaFragment)
            }
            else
            {
                DAO.configDAO.updateConfig( config!! )
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