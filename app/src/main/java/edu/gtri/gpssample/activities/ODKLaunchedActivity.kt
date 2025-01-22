/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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

        // This activity is launched by ODK, when the user presses the 'Launch' button.
        // The Launch button is defined in the ODK xlsx form, ex: edu.gtri.gpssample.odk.
        // The HH id is created here, and sent back to ODK via the activity result.

        val app = (this.application as? MainApplication)!!
        val enumAreaName = app.currentEnumerationAreaName.replace(" ", "" ).uppercase()
        val id = "${app.currentEnumerationItemUUID}:${enumAreaName}:${app.currentSubAddress}"

        val intent = Intent()
        intent.putExtra("value", id )
        setResult(RESULT_OK, intent)

        finish()
    }
}