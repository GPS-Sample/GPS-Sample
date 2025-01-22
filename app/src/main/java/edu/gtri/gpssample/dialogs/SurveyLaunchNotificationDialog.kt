/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import edu.gtri.gpssample.R

class SurveyLaunchNotificationDialog
{
    interface SurveyLaunchNotificationDialogDelegate
    {
        fun shouldLaunchODK()
    }

    constructor()
    {
    }

    constructor(context: Context?, delegate: SurveyLaunchNotificationDialogDelegate)
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_survey_launch_notification, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        Handler().postDelayed({
            alertDialog.dismiss()
            delegate.shouldLaunchODK()
        }, 2000)
    }
}