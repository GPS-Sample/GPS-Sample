package edu.gtri.gpssample.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.databinding.ActivityGenerateBarcodeBinding
import edu.gtri.gpssample.models.StudyModel
import org.json.JSONObject
import java.util.UUID

class GenerateBarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGenerateBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.generateButton.setOnClickListener {

            val jsonObject = JSONObject()

            jsonObject.put( "wifi_ssid", "GPS Sample App" )
            jsonObject.put( "wifi_password", "Pa\$\$W0rd")
            jsonObject.put( "configuration_id", "1be2a0fe")
            jsonObject.put( "study_id", "6e70e86f")

            val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
            qrgEncoder.setColorBlack(Color.WHITE);
            qrgEncoder.setColorWhite(Color.BLACK);

            try {
                val bitmap = qrgEncoder.bitmap
                binding.imageView.setImageBitmap(bitmap)
                (application as MainApplication).barcodeBitmap = bitmap

            } catch (e: Exception) {
                Log.d("xxx", e.toString())
            }
        }

        binding.nextButton.setOnClickListener {

            val studyModel = StudyModel()

            studyModel.name = studyName

            (application as MainApplication).studies.add( studyModel )

            setResult( ResultCode.GenerateBarcode.value )

            finish()

            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }

        binding.backButton.setOnClickListener {
            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}