package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.gtri.gpssample.R
import edu.gtri.gpssample.databinding.ActivityDefineEnumerationAreaBinding

class DefineEnumerationAreaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityDefineEnumerationAreaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDefineEnumerationAreaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)

        binding.backButton.setOnClickListener {
            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }

        binding.nextButton.setOnClickListener {
            finish()
            
            val intent = Intent(this, AdminSelectRoleActivity::class.java)
            startActivity( intent )
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)

            // clear the back stack of all previous activities
            ActivityCompat.finishAffinity(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val srb = LatLng(30.330603,-86.165004 )

        map.addMarker( MarkerOptions().position(srb).title("Grayton Beach, FL"))

        map.moveCamera(CameraUpdateFactory.newLatLngZoom( srb, 15.0f))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        val intent = Intent(this, SignInSignUpActivity::class.java)
        startActivity( intent )
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}