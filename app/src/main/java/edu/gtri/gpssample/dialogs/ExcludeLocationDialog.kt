package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.ReviewStatus
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.Locale

class ExcludeLocationDialog
{
    enum class ButtonPress
    {
        Cancel,
        Save,
    }

    constructor( context: Context, enumerationItems: ArrayList<EnumerationItem>, completion : (ButtonPress) -> Unit )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_exclude_location, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val titleTextView = view.findViewById<TextView>(R.id.title_text_view)
        val keepButton = view.findViewById<RadioButton>(R.id.keep_button)
        val excludeButton = view.findViewById<RadioButton>(R.id.exclude_button)
        val duplicateButton = view.findViewById<RadioButton>(R.id.duplicate_button)
        val outsideButton = view.findViewById<RadioButton>(R.id.outside_button)
        val otherButton = view.findViewById<RadioButton>(R.id.other_button)
        val notesEditText = view.findViewById<EditText>(R.id.notes_edit_text)
        val reasonLayout = view.findViewById<LinearLayout>(R.id.reason_layout)

        val enumerationItem = enumerationItems.first()

        val title = context.resources.getString(R.string.location ) + " " + enumerationItem.subAddress.toString()
        titleTextView.text = title

        if (enumerationItem.reviewStatus == ReviewStatus.Ignore)
        {
            keepButton.isChecked = true
            excludeButton.isChecked = false
            reasonLayout.visibility = View.GONE
        }
        else
        {
            keepButton.isChecked = enumerationItem.reviewStatus == ReviewStatus.Keep
            excludeButton.isChecked = enumerationItem.reviewStatus == ReviewStatus.Exclude
            reasonLayout.visibility = if (keepButton.isChecked) View.GONE else View.VISIBLE
        }

        val englishConfig = Configuration(context.resources.configuration)
        englishConfig.setLocale(Locale.ENGLISH)
        val englishContext = context.createConfigurationContext(englishConfig )

        if (excludeButton.isChecked)
        {
            when (enumerationItem.exclusionReason)
            {
                englishContext.getString(R.string.duplicate) -> duplicateButton.isChecked = true
                englishContext.getString(R.string.outside_boundary) -> outsideButton.isChecked = true
                englishContext.getString(R.string.other) -> otherButton.isChecked = true
            }
        }

        notesEditText.setText( enumerationItem.exclusionNotes )

        keepButton.setOnClickListener {
            reasonLayout.visibility = if (keepButton.isChecked) View.GONE else View.VISIBLE
        }

        excludeButton.setOnClickListener {
            reasonLayout.visibility = if (excludeButton.isChecked) View.VISIBLE else View.GONE
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {
            if (excludeButton.isChecked && duplicateButton.isChecked == false && outsideButton.isChecked == false && otherButton.isChecked == false)
            {
                Toast.makeText( context, context.resources.getString(R.string.reason_is_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            for (enumerationItem in enumerationItems)
            {
                enumerationItem.reviewStatus = if (excludeButton.isChecked) ReviewStatus.Exclude else ReviewStatus.Keep

                if (enumerationItem.reviewStatus == ReviewStatus.Exclude)
                {
                    if (duplicateButton.isChecked) { enumerationItem.exclusionReason = englishContext.getString(R.string.duplicate) }
                    if (outsideButton.isChecked) { enumerationItem.exclusionReason = englishContext.getString(R.string.outside_boundary) }
                    if (otherButton.isChecked) { enumerationItem.exclusionReason = englishContext.getString(R.string.other) }
                }
                else
                {
                    enumerationItem.exclusionReason = ""
                }

                enumerationItem.exclusionNotes = notesEditText.text.toString()
            }

            completion( ButtonPress.Save )

            alertDialog.dismiss()
        }

        val cancelButton = view.findViewById<Button>(R.id.cancel_button )

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            completion(ButtonPress.Cancel )
        }
    }
}