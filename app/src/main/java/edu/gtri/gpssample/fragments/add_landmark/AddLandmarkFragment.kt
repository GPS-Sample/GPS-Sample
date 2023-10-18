package edu.gtri.gpssample.fragments.add_landmark

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddLandmarkBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*


class AddLandmarkFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentAddLandmarkBinding? = null
    private val binding get() = _binding!!

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var enumArea : EnumArea
    private lateinit var location: Location
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View
    {
        _binding = FragmentAddLandmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        if (!this::config.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "currentConfiguration was not initialized.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_navigate_to_MainFragment)
            return
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.enumAreaViewModel.currentEnumArea?.value?.let{
            enumArea = it
        }

        val components = location.uuid.split("-" )

        binding.UUIDEditText.setText( components[0] )
        binding.latitudeEditText.setText( String.format( "%.6f", location.latitude ))
        binding.longitudeEditText.setText( String.format( "%.6f", location.longitude ))
        binding.descriptionEditText.setText( location.description )

        if (sharedViewModel.locationViewModel.isLocationUpdateTimeValid.value == true)
        {
            sharedViewModel.locationViewModel.currentLocationUpdateTime?.value?.let { date ->
                val dt = (Date().time - date.time) / 1000.0
                binding.lastUpdatedEditText.setText( "${dt} seconds ago" )
            } ?: {binding.lastUpdatedEditText.setText( "Undefined" )}
        }
        else
        {
            binding.lastUpdatedLayout.visibility = View.GONE
        }

        if (location.imageData.isNotEmpty())
        {
            try
            {
                // base64 decode the bitmap
                val byteArray = Base64.getDecoder().decode( location.imageData )
                val byteArrayInputStream = ByteArrayInputStream(byteArray)
                val bitmap = BitmapFactory.decodeStream(byteArrayInputStream)
                binding.landmarkImageView.setImageBitmap(bitmap)
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTrace.toString())
            }
        }

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_landmark_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {
            resultLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            location.description = binding.descriptionEditText.text.toString()
            DAO.locationDAO.updateLocation( location, enumArea )
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddLandmarkFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun didSelectLeftButton(tag: Any?)
    {
    }

    override fun didSelectRightButton(tag: Any?)
    {
        enumArea.locations.remove(location)

        DAO.locationDAO.delete( location )

        findNavController().popBackStack()
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            if (result?.data != null)
            {
                if (this::location.isInitialized)  // activity may have been destroyed by the Camera app
                {
                    val bitmap = result.data?.extras?.get("data") as Bitmap
                    binding.landmarkImageView.setImageBitmap(bitmap)
                    saveBitmap( bitmap )
                }
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap)
    {
        try
        {
            // base64 encode the bitmap
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            location.imageData = Base64.getEncoder().encodeToString(byteArray)
        }
        catch (e: Exception)
        {
            Log.d( "xxx", e.stackTrace.toString())
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}