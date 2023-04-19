package edu.gtri.gpssample.fragments.add_location

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.databinding.FragmentAddLocationBinding
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel

class AddLocationFragment : Fragment()
{
    private var _binding: FragmentAddLocationBinding? = null
    private val binding get() = _binding!!
    private val OPEN_DOCUMENT_CODE = 2

    private lateinit var enumData: EnumData

    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )
        _binding = FragmentAddLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.enumDataViewModel.currentEnumData?.value?.let {
            enumData = it
        }

        binding.editText.setText( enumData.description )

        if (enumData.imageFileName.isNotEmpty())
        {
            val uri = Uri.parse(enumData.imageFileName )

            Log.d( "xxx", enumData.imageFileName )

            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(activity!!.getContentResolver(), uri)
            binding.imageView.setImageBitmap(bitmap)

            var width = bitmap.width.toDouble()
            val height = bitmap.height.toDouble()

            val orgWidth = binding.imageView.layoutParams.width.toDouble()

            width = width / height * orgWidth

            binding.imageView.layoutParams.width = width.toInt()
            binding.addImageButton.visibility = View.GONE
        }

        binding.imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, OPEN_DOCUMENT_CODE)
        }

        binding.addImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, OPEN_DOCUMENT_CODE)
        }

        binding.saveButton.setOnClickListener {

            enumData.description = binding.editText.text.toString()
            DAO.enumDataDAO.updateEnumData( enumData )

            findNavController().popBackStack()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()
        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.AddLocationFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId) {
            R.id.action_delete -> {
                DAO.enumDataDAO.delete( enumData )
                findNavController().popBackStack()
                return true
            }
        }

        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_DOCUMENT_CODE) {
                data?.data?.let {uri ->

                    binding.imageView.setImageURI(uri)
                    binding.imageView.setTag(uri)
                    binding.imageView.setScaleType(ImageView.ScaleType.FIT_XY)
                    binding.imageView.setClipToOutline(true)
                    binding.addImageButton.visibility = View.GONE

                    enumData.imageFileName = uri.toString()
                    DAO.enumDataDAO.updateEnumData( enumData )
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}