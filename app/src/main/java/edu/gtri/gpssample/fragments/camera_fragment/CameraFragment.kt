package edu.gtri.gpssample.fragments.camera_fragment

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.FragmentNumber
import edu.gtri.gpssample.databinding.FragmentCameraBinding
import edu.gtri.gpssample.utils.CameraUtils
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment()
{
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private var bitmap: Bitmap?= null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sharedViewModel : ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val vm : ConfigurationViewModel by activityViewModels()
        sharedViewModel = vm
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        startCamera()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.cameraButton.setOnClickListener {
            if (binding.cameraButton.text == "Take Photo")
            {
                binding.cameraButton.isEnabled = false
                takePhoto()
            }
            else
            {
                binding.cameraButton.text = "Take Photo"
                binding.imageView.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
            }
        }

        binding.deleteImageView.setOnClickListener {
            sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                location.imageData = ""
                binding.cameraButton.text = "Take Photo"
                binding.imageView.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            bitmap?.let { bitmap ->
                CameraUtils.encodeBitmap( bitmap )?.let { imageData ->
                    sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                        location.imageData = imageData
                    }
                }
            }

            findNavController().popBackStack()
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.CameraFragment.value.toString() + ": " + this.javaClass.simpleName
    }

    private fun getOutputDirectory(): File
    {
        val mediaDir = this.activity!!.externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else this.activity!!.filesDir
    }

    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.activity!!)

        cameraProviderFuture.addListener(Runnable
        {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try
            {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                sharedViewModel.locationViewModel.currentLocation?.value?.let { location ->
                    if (location.imageData.isNotEmpty())
                    {
                        binding.viewFinder.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                        binding.cameraButton.text = "Retake Photo"
                        binding.imageView.setImageBitmap( CameraUtils.decodeString( location.imageData ))
                    }
                }
            }
            catch (exc: Exception)
            {
                Log.d("xxx", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this.activity!!))
    }

    private fun takePhoto()
    {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this.activity!!),
            object : ImageCapture.OnImageSavedCallback
            {
                override fun onError(exc: ImageCaptureException)
                {
                    binding.viewFinder.visibility = View.VISIBLE
                    binding.imageView.visibility = View.GONE
                    binding.cameraButton.text = "Take Photo"
                    binding.cameraButton.isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults)
                {
                    val uri = Uri.fromFile(photoFile)

                    MediaStore.Images.Media.getBitmap(this@CameraFragment.activity!!.getContentResolver(), uri)?.let {
                        val angle = CameraUtils.getRotationAngle( this@CameraFragment.activity!!, uri )
                        binding.viewFinder.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                        binding.cameraButton.text = "Retake Photo"
                        bitmap = CameraUtils.rotate( it, angle )
                        binding.imageView.setImageBitmap( bitmap )
                        binding.cameraButton.isEnabled = true
                    }
                }
            }
        )
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
        cameraExecutor.shutdown()
    }
}