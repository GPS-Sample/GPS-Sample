package edu.gtri.gpssample.fragments.add_location

import android.R.attr
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentAddLocationBinding
import edu.gtri.gpssample.dialogs.ConfirmationDialog
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.*


class AddLocationFragment : Fragment(), ConfirmationDialog.ConfirmationDialogDelegate
{
    private var _binding: FragmentAddLocationBinding? = null
    private val binding get() = _binding!!
    private val OPEN_DOCUMENT_CODE = 2

    private lateinit var study: Study
    private lateinit var config: Config
    private lateinit var locationDate: Date
    private lateinit var enumArea : EnumArea
    private lateinit var location: Location
    private lateinit var enumerationItem: EnumerationItem
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentAddLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.currentConfiguration?.value?.let {
            config = it
        }

        sharedViewModel.createStudyModel.currentStudy?.value?.let {
            study = it
        }

        sharedViewModel.locationViewModel.currentLocation?.value?.let {
            location = it
        }

        sharedViewModel.locationViewModel.currentLocationUpdateTime?.value?.let {
            locationDate = it
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

        binding.deleteImageView.setOnClickListener {
            ConfirmationDialog( activity, resources.getString( R.string.please_confirm), resources.getString(R.string.delete_landmark_message),
                resources.getString(R.string.no), resources.getString(R.string.yes), 0, this)
        }

        binding.addPhotoImageView.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(cameraIntent)
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//            intent.addCategory(Intent.CATEGORY_OPENABLE)
//            intent.type = "image/*"
//            startActivityForResult(intent, OPEN_DOCUMENT_CODE)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            location.description = binding.descriptionEditText.text.toString()

            sharedViewModel.currentConfiguration?.value?.let {config ->
                sharedViewModel.updateConfiguration()
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddLocationFragment.value.toString() + ": " + this.javaClass.simpleName
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
                val bitmap = result.data?.extras?.get("data") as Bitmap
                binding.landmarkImageView.setImageBitmap(bitmap)
                saveBitmap( bitmap )
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap)
    {
        try
        {
            val root = File(Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS)
            val file = File(root, UUID.randomUUID().toString() + ".jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

//            val fileName: String = UUID.randomUUID().toString() + ".jpg"
//            val values = ContentValues()
//            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
//            {
//                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/")
//                values.put(MediaStore.MediaColumns.IS_PENDING, 1)
//            }
//            else
//            {
//                val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
//                val file = File(directory, fileName)
//                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath())
//            }
//
//            val uri = activity!!.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//
//            uri?.let { uri ->
//                activity!!.contentResolver.openOutputStream(uri).use { output ->
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
//                }
//            }
        } catch (e: Exception) {
            Log.d( "xxx", e.stackTrace.toString())
        }
    }
    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}