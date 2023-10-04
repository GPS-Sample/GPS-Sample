package edu.gtri.gpssample.activities

import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.gtri.gpssample.application.MainApplication

class ODKLaunchedActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val intent = Intent()
        intent.putExtra("value", (this.application as? MainApplication)!!.currentEnumerationItemUUID)
        setResult(RESULT_OK, intent)

        finish()
    }
}