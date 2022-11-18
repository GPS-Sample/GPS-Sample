package edu.gtri.gpssample.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.databinding.ActivityGenerateBarcodeBinding
import edu.gtri.gpssample.models.StudyModel

class GenerateBarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGenerateBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.saveButton.setOnClickListener {

            val studyModel = StudyModel()

            studyModel.name = studyName

            (application as MainApplication).studies.add( studyModel )

            setResult( ResultCode.GenerateBarcode.value )

            finish()

            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }

        binding.generateButton.setOnClickListener {

            val qrgEncoder = QRGEncoder("whoo Hoo Hoo", null, QRGContents.Type.TEXT, binding.imageView.width )
            qrgEncoder.setColorBlack(Color.WHITE);
            qrgEncoder.setColorWhite(Color.BLACK);

            try {
                val bitmap = qrgEncoder.bitmap
                binding.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.d("xxx", e.toString())
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}