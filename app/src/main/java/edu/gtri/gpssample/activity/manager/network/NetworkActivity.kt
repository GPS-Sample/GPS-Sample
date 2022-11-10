package edu.gtri.gpssample.activity.manager.network

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import edu.gtri.gpssample.R
import edu.gtri.gpssample.activity.manager.network.ui.network.NetworkFragment

class NetworkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NetworkFragment.newInstance())
                .commitNow()
        }
    }
}