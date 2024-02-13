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

        val app = (this.application as? MainApplication)!!
        val enumAreaName = app.currentEnumerationAreaName.replace(" ", "" ).uppercase()
        val id = "${app.currentEnumerationItemUUID}:${enumAreaName}:${app.currentSubAddress}"

        val intent = Intent()
        intent.putExtra("value", id )
        setResult(RESULT_OK, intent)

        finish()
    }
}