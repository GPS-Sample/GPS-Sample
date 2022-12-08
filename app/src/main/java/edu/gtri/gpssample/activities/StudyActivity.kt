package edu.gtri.gpssample.activities

import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.OnlineStatusAdapter
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.databinding.ActivityStudyBinding
import edu.gtri.gpssample.models.UserModel
import edu.gtri.gpssample.network.UDPBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class StudyActivity : AppCompatActivity(), UDPBroadcastReceiver.UDPBroadcastReceiverDelegate
{
    private lateinit var binding: ActivityStudyBinding
    private lateinit var onlineStatusAdapter: OnlineStatusAdapter
    private val udpBroadcastReceiver: UDPBroadcastReceiver = UDPBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studyName = intent.getStringExtra( Key.StudyName.toString())

        binding.studyNameTextView.text = studyName

        binding.imageView.setImageBitmap( (application as MainApplication).barcodeBitmap )

        if ((application as MainApplication).users.isEmpty())
        {
            var user1 = UserModel()
            user1.name = "Russell"
            (application as MainApplication).users.add( user1 )

            var user2 = UserModel()
            user2.name = "Brian"
            (application as MainApplication).users.add( user2 )

            var user3 = UserModel()
            user3.name = "Megan"
            (application as MainApplication).users.add( user3 )
        }

        onlineStatusAdapter = OnlineStatusAdapter((application as MainApplication).users)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = onlineStatusAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

        val oldWifiAdresses = getWifiApIpAddresses()

        binding.generateBarcodeButton.setOnClickListener {

            try
            {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
                {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                    {
                        super.onStarted(reservation)

                        val ssid = reservation.softApConfiguration.ssid
                        val pass = reservation.softApConfiguration.passphrase
                        Toast.makeText(applicationContext, "ssid = " + ssid, Toast.LENGTH_SHORT).show()

                        Log.d( "xxx", "ssid = " + ssid );
                        Log.d( "xxx", "pass = " + pass );

                        val newWifiAddresses = getWifiApIpAddresses()

                        var inetAddress: InetAddress? = null

                        if (oldWifiAdresses.isEmpty())
                        {
                            if (!newWifiAddresses.isEmpty())
                            {
                                inetAddress = newWifiAddresses[0]
                            }
                        }
                        else
                        {
                            for (oldAddr in oldWifiAdresses)
                            {
                                for (newAddr in newWifiAddresses)
                                {
                                    if (newAddr.hostAddress.equals( oldAddr.hostAddress ))
                                    {
                                        continue
                                    }
                                    else
                                    {
                                        inetAddress = newAddr
                                        break;
                                    }
                                }
                            }
                        }

                        if (inetAddress != null)
                        {
                            Log.d( "xxx", "inetAddress = " + inetAddress!!.hostAddress )

                            lifecycleScope.launchWhenStarted {
                                whenStarted {
                                    udpBroadcastReceiver.beginListening( inetAddress!!, this@StudyActivity )
                                }
                            }
                        }

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

        binding.backButton.setOnClickListener {

            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }

        Observable
            .interval(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (dataIsFresh)
                {
                    dataIsFresh = false;
                }
                else if ((application as MainApplication).users[0].isOnline)
                {
                    (application as MainApplication).users[0].isOnline = false
                    onlineStatusAdapter.updateUsers( (application as MainApplication).users )
                }
            }
    }

    private var dataIsFresh = false

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
        dataIsFresh = true

        (application as MainApplication).users[0].isOnline = true

        this.runOnUiThread{
            onlineStatusAdapter.updateUsers( (application as MainApplication).users )
        }

        Log.d( "xxx", "received : " + datagramPacket.length )
    }

    var countDownTimer: CountDownTimer = object : CountDownTimer(0, 4000)
    {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish()
        {
            Log.d( "xxx", "finish" )
            (application as MainApplication).users[0].isOnline = false

            this@StudyActivity.runOnUiThread{
                onlineStatusAdapter.updateUsers( (application as MainApplication).users )
            }
        }
    }

    fun getWifiApIpAddresses(): ArrayList<InetAddress> {
        val list = ArrayList<InetAddress>()
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                if (intf.getName().contains("wlan")) {
                    val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress: InetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress()) {
                            val inetAddr = inetAddress.hostAddress!!
                            if (!inetAddr.contains(":")) {
                                list.add( inetAddress)
                                Log.d("xxx", inetAddress.getHostAddress())
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("xxx", ex.toString())
        }
        return list
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_study, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_edit_study -> {
            }
            R.id.action_delete_study -> {
            }
        }

        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}

private fun <T> MutableLiveData<T>.observe(coroutineScope: CoroutineScope, observer: Observer) {

}
