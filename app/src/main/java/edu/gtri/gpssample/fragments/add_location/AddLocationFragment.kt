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
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var createMode = false
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

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (createMode)
                {
                    DAO.enumDataDAO.delete( enumData )
                }
                findNavController().popBackStack()
            }
        })
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

        if (enumData.id == null)
        {
            createMode = true
            DAO.enumDataDAO.createEnumData(enumData)
        }

        binding.editText.setText( enumData.description )

        if (enumData.imageFileName.isNotEmpty())
        {
            val uri = Uri.parse(enumData.imageFileName )
            activity!!.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(activity!!.getContentResolver(), uri)
            (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "${bitmap.width}:${bitmap.height}"
            binding.imageView.setImageBitmap(bitmap)
        }

        binding.addImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, OPEN_DOCUMENT_CODE)
        }

        binding.cancelButton.setOnClickListener {
            if (createMode)
            {
                DAO.enumDataDAO.delete( enumData )
            }

            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {

            enumData.description = binding.editText.text.toString()
            DAO.enumDataDAO.updateEnumData( enumData )

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
            android.R.id.home -> {
                if (createMode)
                {
                    DAO.enumDataDAO.delete( enumData )
                }
            }
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
                    activity!!.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(activity!!.getContentResolver(), uri)
                    (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = "${bitmap.width}:${bitmap.height}"
                    binding.imageView.setImageBitmap(bitmap)
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