package edu.gtri.gpssample.activities

import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
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

class GenerateBarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGenerateBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.generateButton.setOnClickListener {

            try
            {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
                {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                    {
                        super.onStarted(reservation)

                        Log.d( "xxx", reservation.toString())
                        val ssid = reservation.softApConfiguration.ssid
                        val pass = reservation.softApConfiguration.passphrase
                        Toast.makeText(applicationContext, "ssid = " + ssid, Toast.LENGTH_SHORT).show()

                        Log.d( "xxx", "ssid = " + ssid );
                        Log.d( "xxx", "pass = " + pass );

                        val jsonObject = JSONObject()
                        jsonObject.put( "ssid", ssid )
                        jsonObject.put( "pass", pass )

                        val qrgEncoder = QRGEncoder(jsonObject.toString(2),null, QRGContents.Type.TEXT, binding.imageView.width )
                        qrgEncoder.setColorBlack(Color.WHITE);
                        qrgEncoder.setColorWhite(Color.BLACK);

                        val bitmap = qrgEncoder.bitmap
                        binding.imageView.setImageBitmap(bitmap)
                        (application as MainApplication).barcodeBitmap = bitmap
                    }
                }, Handler())
            } catch(e: Exception) {}
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