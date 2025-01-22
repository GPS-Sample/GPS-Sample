/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import edu.gtri.gpssample.R

class LaunchSurveyDialog
{
    interface LaunchSurveyDialogDelegate
    {
        fun launchSurveyButtonPressed()
        fun markAsIncompleteButtonPressed()
        fun showInfoButtonPressed()
    }

    constructor()
    {
    }

    constructor( context: Context?, gpsAccuracyIsGood: Boolean, delegate: LaunchSurveyDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_launch_survey, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val launchButton = view.findViewById<Button>(R.id.launch_button)

        launchButton.setOnClickListener {
            delegate.launchSurveyButtonPressed()
            alertDialog.dismiss()
        }

        val incompleteButton = view.findViewById<Button>(R.id.incomplete_button)

        incompleteButton.setOnClickListener {
            delegate.markAsIncompleteButtonPressed()
            alertDialog.dismiss()
        }

        val showInfoButton = view.findViewById<Button>(R.id.show_info_button)

        showInfoButton.setOnClickListener {
            delegate.showInfoButtonPressed()
            alertDialog.dismiss()
        }

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        if (!gpsAccuracyIsGood)
        {
            launchButton.isEnabled = false
            incompleteButton.isEnabled = false
        }
    }
}